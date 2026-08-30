package com.familygrowth.application;

import com.familygrowth.domain.Stage20Models.SchoolStage;
import com.familygrowth.domain.Stage21TeachingModels.LearningAssignment;
import com.familygrowth.domain.Stage23LearningModels.RewardPolicy;
import com.familygrowth.domain.Stage23LearningModels.MisconceptionCategory;
import com.familygrowth.domain.Stage23LearningModels.SupportEvent;
import com.familygrowth.domain.Stage23LearningModels.PrimaryLearningReport;
import com.familygrowth.domain.Stage3Models.Actor;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.time.Instant;
import java.time.Duration;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class Stage23LearningService {
    private final Stage3Service auth;
    private final Stage20Service experience;
    private final Stage21TeachingStore teaching;
    private final Stage23LearningStore store;
    private final Clock clock;

    public Stage23LearningService(Stage3Service auth, Stage20Service experience, Stage21TeachingStore teaching,
                                  Stage23LearningStore store, Clock clock) {
        this.auth = auth; this.experience = experience; this.teaching = teaching; this.store = store; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public RewardPolicy policy(Actor actor, UUID familyId, UUID childId) {
        auth.requireParent(actor, familyId);
        auth.requireChildOrParent(actor, familyId, childId);
        return store.policy(familyId, childId);
    }

    public RewardPolicy updatePolicy(Actor actor, UUID familyId, UUID childId, BigDecimal money, long coin, long xp,
                                     long expectedVersion, String reason) {
        auth.requireParent(actor, familyId);
        auth.requireChildOrParent(actor, familyId, childId);
        String auditReason = reason == null ? "" : reason.trim();
        if (auditReason.isBlank() || auditReason.length() > 500) throw new IllegalArgumentException("A short audit reason is required");
        return store.updatePolicy(familyId, childId, actor.actorId(), money, coin, xp, expectedVersion,
            auditReason, clock.instant());
    }

    public List<LearningAssignment> sync(Actor actor, UUID familyId, UUID childId, String rawKey) {
        auth.requireChildOrParent(actor, familyId, childId);
        SchoolStage stage = experience.experience(actor, familyId, childId).effectiveStage();
        if (stage == SchoolStage.PARENT_ONLY) return List.of();
        String key = key(rawKey);
        String payload = hash(childId + "|" + stage);
        var replay = store.syncReplay(familyId, key);
        if (replay.isPresent()) {
            var value = replay.get();
            if (!value.childId().equals(childId) || value.stage() != stage || !value.payloadHash().equals(payload)) {
                throw new Stage3Service.ConflictException("Idempotency key payload mismatch");
            }
        } else {
            store.syncAssignments(familyId, childId, stage, actor.actorId(), key, payload, clock.instant());
        }
        return teaching.assignments(familyId, childId, stage);
    }

    @Transactional(readOnly = true)
    public List<SupportEvent> supportEvents(Actor actor, UUID familyId, UUID childId, UUID assignmentId) {
        auth.requireChildOrParent(actor, familyId, childId);
        List<SupportEvent> events = store.supportEvents(familyId, childId, assignmentId);
        return actor.role() == com.familygrowth.domain.Stage3Models.ActorRole.CHILD
            ? events.stream().map(SupportEvent::childSafe).toList() : events;
    }

    @Transactional(readOnly = true)
    public PrimaryLearningReport primaryReport(Actor actor, UUID familyId, UUID childId) {
        auth.requireParent(actor, familyId);
        auth.requireChildOrParent(actor, familyId, childId);
        var profile = experience.experience(actor, familyId, childId);
        Instant end = clock.instant();
        Instant start = end.minus(Duration.ofDays(7));
        return new PrimaryLearningReport(childId, profile.effectiveStage(), profile.effectivePrimaryBand(),
            start, end, store.recordedLearningMinutes(familyId, childId, start, end),
            store.primaryLearningFacts(familyId, childId, end), end);
    }

    public LearningAssignment requestHelp(Actor actor, UUID familyId, UUID childId, UUID assignmentId,
                                          UUID activityId, String message, String rawKey) {
        auth.requireChildOrParent(actor, familyId, childId);
        if (actor.role() != com.familygrowth.domain.Stage3Models.ActorRole.CHILD) {
            throw new Stage3Service.ForbiddenException();
        }
        String childMessage = message == null ? "" : message.trim();
        if (childMessage.isBlank() || childMessage.length() > 160) throw new IllegalArgumentException("A short help message is required");
        String key = key(rawKey); String payload = hash(assignmentId + "|" + activityId + "|" + childMessage);
        store.requestHelp(familyId, childId, assignmentId, activityId, actor.actorId(), childMessage, key, payload, clock.instant());
        return teaching.assignment(familyId, childId, assignmentId).orElseThrow(FamilyGrowthService.NotFoundException::new).projection();
    }

    public List<SupportEvent> classifySupport(Actor actor, UUID familyId, UUID childId, UUID assignmentId,
                                              UUID sourceEventId, MisconceptionCategory category,
                                              String privateNote, Instant revisitAt, String rawKey) {
        auth.requireParent(actor, familyId); auth.requireChildOrParent(actor, familyId, childId);
        String note = privateNote == null ? "" : privateNote.trim();
        if (note.length() > 500) throw new IllegalArgumentException("Private note is too long");
        Instant now = clock.instant();
        if (revisitAt != null) {
            if (revisitAt.isBefore(now.plus(Duration.ofHours(23))) || revisitAt.isAfter(now.plus(Duration.ofDays(30)))) {
                throw new IllegalArgumentException("Revisit must be scheduled 1 to 30 days later");
            }
        }
        String key = key(rawKey); String payload = hash(assignmentId + "|" + sourceEventId + "|" + category + "|" + note + "|" + revisitAt);
        store.classifySupport(familyId, childId, assignmentId, sourceEventId, actor.actorId(), category,
            note, revisitAt, key, payload, now);
        return store.supportEvents(familyId, childId, assignmentId);
    }

    private static String key(String value) {
        if (value == null || value.isBlank() || value.trim().length() > 100) throw new IllegalArgumentException("Valid Idempotency-Key is required");
        return value.trim();
    }
    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }
}
