package com.familygrowth.application;

import com.familygrowth.domain.Stage20Models.SchoolStage;
import com.familygrowth.domain.Stage21TeachingModels.ActivityContent;
import com.familygrowth.domain.Stage21TeachingModels.AssignmentFacts;
import com.familygrowth.domain.Stage21TeachingModels.AssignmentStatus;
import com.familygrowth.domain.Stage21TeachingModels.CourseVersion;
import com.familygrowth.domain.Stage21TeachingModels.CourseVersionStatus;
import com.familygrowth.domain.Stage21TeachingModels.EvidenceType;
import com.familygrowth.domain.Stage21TeachingModels.LearningAssignment;
import com.familygrowth.domain.Stage21TeachingModels.ParentCourseSummary;
import com.familygrowth.domain.Stage21TeachingModels.ReviewDecision;
import com.familygrowth.domain.Stage21TeachingModels.VersionDraft;
import com.familygrowth.domain.Stage3Models.Actor;
import com.familygrowth.domain.Stage3Models.ActorRole;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class Stage21TeachingService {
    private final Stage3Service auth;
    private final Stage20Service experience;
    private final Stage21TeachingStore store;
    private final Clock clock;

    public Stage21TeachingService(Stage3Service auth, Stage20Service experience,
                                  Stage21TeachingStore store, Clock clock) {
        this.auth = auth;
        this.experience = experience;
        this.store = store;
        this.clock = clock;
    }

    public CourseVersion createCourse(Actor actor, UUID familyId, SchoolStage stage, String subjectCode,
                                      String title, VersionDraft draft, String rawKey) {
        auth.requireParent(actor, familyId);
        if (stage == null || stage == SchoolStage.PARENT_ONLY) throw new IllegalArgumentException("A child school stage is required");
        String subject = code(subjectCode);
        String courseTitle = text(title, 160);
        String key = key(rawKey);
        String payload = hash(stage + "|" + subject + "|" + courseTitle + "|" + draft);
        return store.courseByCreateKey(familyId, key).map(existing -> {
            match(existing.payloadHash(), payload);
            return existing.version();
        }).orElseGet(() -> store.createCourse(familyId, stage, subject, courseTitle, draft,
            actor.actorId(), key, payload, clock.instant()));
    }

    public CourseVersion createVersion(Actor actor, UUID familyId, UUID courseId,
                                       VersionDraft draft, String rawKey) {
        auth.requireParent(actor, familyId);
        String key = key(rawKey);
        String payload = hash(courseId + "|" + draft);
        var replay = store.action(familyId, key);
        if (replay.isPresent()) {
            verify(replay.get(), "CREATE_VERSION", courseId, payload);
            return version(familyId, replay.get().resultId());
        }
        return store.createVersion(familyId, courseId, draft, actor.actorId(), key, payload, clock.instant());
    }

    public CourseVersion publish(Actor actor, UUID familyId, UUID versionId, String rawKey) {
        auth.requireParent(actor, familyId);
        String key = key(rawKey);
        String payload = hash(versionId.toString());
        var replay = store.action(familyId, key);
        if (replay.isPresent()) {
            verify(replay.get(), "PUBLISH", versionId, payload);
            return version(familyId, versionId);
        }
        CourseVersion version = version(familyId, versionId);
        if (version.status() != CourseVersionStatus.DRAFT) throw new Stage3Service.ConflictException("Course version is already published");
        return store.publish(familyId, versionId, actor.actorId(), key, payload, clock.instant());
    }

    @Transactional(readOnly = true)
    public List<ParentCourseSummary> courses(Actor actor, UUID familyId) {
        auth.requireParent(actor, familyId);
        return store.courses(familyId);
    }

    @Transactional(readOnly = true)
    public CourseVersion courseVersion(Actor actor, UUID familyId, UUID versionId) {
        auth.requireParent(actor, familyId);
        return version(familyId, versionId);
    }

    public LearningAssignment assign(Actor actor, UUID familyId, UUID childId, UUID versionId,
                                     UUID lessonId, String rawKey) {
        auth.requireParent(actor, familyId);
        SchoolStage childStage = experience.experience(actor, familyId, childId).effectiveStage();
        CourseVersion version = version(familyId, versionId);
        if (version.status() != CourseVersionStatus.PUBLISHED || version.schoolStage() != childStage) {
            throw new Stage3Service.ConflictException("Only a published course matching the child stage can be assigned");
        }
        boolean lessonExists = version.units().stream().flatMap(unit -> unit.lessons().stream()).anyMatch(lesson -> lesson.id().equals(lessonId));
        if (!lessonExists) throw new FamilyGrowthService.NotFoundException();
        String key = key(rawKey);
        String payload = hash(childId + "|" + versionId + "|" + lessonId);
        var replay = store.action(familyId, key);
        if (replay.isPresent()) {
            verify(replay.get(), "ASSIGN", lessonId, payload);
            return facts(familyId, childId, replay.get().resultId()).projection();
        }
        return store.assign(familyId, childId, versionId, lessonId, actor.actorId(), key, payload, clock.instant());
    }

    @Transactional(readOnly = true)
    public List<LearningAssignment> catalog(Actor actor, UUID familyId, UUID childId) {
        auth.requireChildOrParent(actor, familyId, childId);
        SchoolStage stage = experience.experience(actor, familyId, childId).effectiveStage();
        return store.assignments(familyId, childId, stage);
    }

    public LearningAssignment attempt(Actor actor, UUID familyId, UUID childId, UUID assignmentId,
                                      UUID activityId, String responseText, Integer playedSeconds,
                                      Integer durationSeconds, String rawKey) {
        requireChild(actor, familyId, childId);
        String response = responseText == null ? "" : responseText.trim();
        if (response.length() > 1000) throw new IllegalArgumentException("responseText is too long");
        String key = key(rawKey);
        String payload = hash(assignmentId + "|" + activityId + "|" + response + "|" + playedSeconds + "|" + durationSeconds);
        var replay = store.action(familyId, key);
        if (replay.isPresent()) {
            verify(replay.get(), "ATTEMPT", assignmentId, payload);
            return facts(familyId, childId, assignmentId).projection();
        }
        AssignmentFacts facts = facts(familyId, childId, assignmentId);
        if (facts.projection().status() == AssignmentStatus.SUBMITTED || facts.projection().status() == AssignmentStatus.COMPLETED) {
            throw new Stage3Service.ConflictException("Assignment is not open for attempts");
        }
        ActivityContent activity = facts.contentByActivity().get(activityId);
        if (activity == null) throw new FamilyGrowthService.NotFoundException();
        EvidenceType evidence;
        Boolean correct = null;
        if (activity.type().objective()) {
            if (response.isBlank()) throw new IllegalArgumentException("Objective activity requires a response");
            evidence = EvidenceType.CHECKED;
            correct = canonical(response).equals(canonical(activity.answerKey()));
        } else if (activity.type().requiredEvidence() == EvidenceType.VIEWED) {
            if (!"VIEWED".equalsIgnoreCase(response) || playedSeconds == null || durationSeconds == null
                || playedSeconds < 0 || durationSeconds <= 0 || playedSeconds * 10L < durationSeconds * 9L) {
                throw new IllegalArgumentException("Video requires at least 90 percent verified playback");
            }
            evidence = EvidenceType.VIEWED;
        } else {
            evidence = EvidenceType.ATTEMPTED;
        }
        return store.attempt(familyId, childId, assignmentId, activityId, actor.actorId(), response,
            evidence, correct, key, payload, clock.instant());
    }

    public LearningAssignment submit(Actor actor, UUID familyId, UUID childId, UUID assignmentId,
                                     long expectedVersion, String rawKey) {
        requireChild(actor, familyId, childId);
        String key = key(rawKey);
        String payload = hash(assignmentId + "|" + AssignmentStatus.SUBMITTED + "||" + expectedVersion);
        var replay = store.action(familyId, key);
        if (replay.isPresent()) {
            verify(replay.get(), "SUBMIT", assignmentId, payload);
            return facts(familyId, childId, assignmentId).projection();
        }
        AssignmentFacts facts = facts(familyId, childId, assignmentId);
        if (facts.projection().status() != AssignmentStatus.IN_PROGRESS
            && facts.projection().status() != AssignmentStatus.REWORK_REQUIRED) {
            throw new Stage3Service.ConflictException("Assignment is not ready to submit");
        }
        if (!facts.projection().activities().stream().allMatch(com.familygrowth.domain.Stage21TeachingModels::childEvidenceSatisfied)) {
            throw new Stage3Service.ConflictException("Required activity evidence is incomplete");
        }
        return store.transition(familyId, childId, assignmentId, AssignmentStatus.SUBMITTED, actor.actorId(), "",
            expectedVersion, key, payload, clock.instant());
    }

    public LearningAssignment review(Actor actor, UUID familyId, UUID childId, UUID assignmentId,
                                     ReviewDecision decision, String note, long expectedVersion, String rawKey) {
        auth.requireParent(actor, familyId);
        String reviewNote = note == null ? "" : note.trim();
        if (reviewNote.length() > 500) throw new IllegalArgumentException("review note is too long");
        if (decision == ReviewDecision.REWORK && reviewNote.isBlank()) throw new IllegalArgumentException("Rework requires a short reason");
        AssignmentStatus target = decision == ReviewDecision.APPROVE ? AssignmentStatus.COMPLETED : AssignmentStatus.REWORK_REQUIRED;
        String key = key(rawKey);
        String payload = hash(assignmentId + "|" + target + "|" + reviewNote + "|" + expectedVersion);
        String action = decision == ReviewDecision.APPROVE ? "APPROVE" : "REWORK";
        var replay = store.action(familyId, key);
        if (replay.isPresent()) {
            verify(replay.get(), action, assignmentId, payload);
            return facts(familyId, childId, assignmentId).projection();
        }
        AssignmentFacts facts = facts(familyId, childId, assignmentId);
        if (facts.projection().status() != AssignmentStatus.SUBMITTED) {
            throw new Stage3Service.ConflictException("Only a submitted assignment can be reviewed");
        }
        return store.transition(familyId, childId, assignmentId, target, actor.actorId(), reviewNote,
            expectedVersion, key, payload, clock.instant());
    }

    private AssignmentFacts facts(UUID familyId, UUID childId, UUID assignmentId) {
        return store.assignment(familyId, childId, assignmentId).orElseThrow(FamilyGrowthService.NotFoundException::new);
    }
    private CourseVersion version(UUID familyId, UUID versionId) {
        return store.version(familyId, versionId).orElseThrow(FamilyGrowthService.NotFoundException::new);
    }
    private void requireChild(Actor actor, UUID familyId, UUID childId) {
        auth.requireChildOrParent(actor, familyId, childId);
        if (actor.role() != ActorRole.CHILD) throw new Stage3Service.ForbiddenException();
    }
    private static void verify(Stage21TeachingStore.ActionReplay replay, String action, UUID target, String payload) {
        if (!replay.actionType().equals(action) || !replay.targetId().equals(target)) {
            throw new Stage3Service.ConflictException("Idempotency key payload mismatch");
        }
        match(replay.payloadHash(), payload);
    }
    private static void match(String stored, String expected) {
        if (!stored.equals(expected)) throw new Stage3Service.ConflictException("Idempotency key payload mismatch");
    }
    private static String key(String value) {
        if (value == null || value.isBlank() || value.trim().length() > 120) throw new IllegalArgumentException("Valid Idempotency-Key is required");
        return value.trim();
    }
    private static String code(String value) {
        String result = text(value, 40).toUpperCase(Locale.ROOT);
        if (!result.matches("[A-Z0-9_-]+")) throw new IllegalArgumentException("subjectCode contains unsupported characters");
        return result;
    }
    private static String text(String value, int max) {
        if (value == null || value.isBlank() || value.trim().length() > max) throw new IllegalArgumentException("Required text is invalid");
        return value.trim();
    }
    private static String canonical(String value) { return value.trim().replaceAll("\\s+", " "); }
    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) { throw new IllegalStateException(ex); }
    }
}
