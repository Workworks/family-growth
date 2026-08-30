package com.familygrowth.application;

import com.familygrowth.domain.Stage20Models;
import com.familygrowth.domain.Stage20Models.DocumentaryAccessMode;
import com.familygrowth.domain.Stage20Models.DocumentaryListing;
import com.familygrowth.domain.Stage20Models.DocumentarySource;
import com.familygrowth.domain.Stage20Models.DocumentaryStatus;
import com.familygrowth.domain.Stage20Models.ExperienceAudit;
import com.familygrowth.domain.Stage20Models.ExperienceProfile;
import com.familygrowth.domain.Stage20Models.SchoolStage;
import com.familygrowth.domain.Stage20Models.PrimaryGradeBand;
import com.familygrowth.domain.Stage3Models.Actor;
import com.familygrowth.domain.Stage3Models.ActorRole;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Stage20Service {
    private final Stage3Service auth;
    private final Stage20Store store;
    private final Clock clock;

    public Stage20Service(Stage3Service auth, Stage20Store store, Clock clock) {
        this.auth = auth;
        this.store = store;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ExperienceProfile experience(Actor actor, UUID familyId, UUID childId) {
        auth.requireChildOrParent(actor, familyId, childId);
        return profile(familyId, childId, store.experience(familyId, childId, clock.instant()));
    }

    @Transactional
    public ExperienceProfile updateExperience(
        Actor actor,
        UUID familyId,
        UUID childId,
        LocalDate birthDate,
        SchoolStage stageOverride,
        PrimaryGradeBand primaryBandOverride,
        String overrideReason,
        boolean hapticsEnabled,
        long expectedVersion,
        String auditReason
    ) {
        auth.requireParent(actor, familyId);
        validateOverride(stageOverride, overrideReason);
        if (birthDate.isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException("birthDate cannot be in the future");
        }
        SchoolStage recommended = SchoolStage.recommended(birthDate, LocalDate.now(clock));
        SchoolStage effective = stageOverride == null ? recommended : stageOverride;
        if (primaryBandOverride != null && effective != SchoolStage.PRIMARY) {
            throw new IllegalArgumentException("primaryBandOverride requires PRIMARY effective stage");
        }
        var stored = store.updateExperience(familyId, childId, actor.actorId(),
            new Stage20Store.ExperienceUpdate(birthDate, stageOverride, primaryBandOverride, normalize(overrideReason),
                hapticsEnabled, expectedVersion, requireText(auditReason, "auditReason")), clock.instant());
        return profile(familyId, childId, stored);
    }

    @Transactional(readOnly = true)
    public List<ExperienceAudit> audit(Actor actor, UUID familyId, UUID childId, int limit) {
        auth.requireParent(actor, familyId);
        store.experience(familyId, childId, clock.instant());
        return store.experienceAudit(familyId, childId, Math.max(1, Math.min(limit, 100)));
    }

    @Transactional
    public DocumentarySource createDocumentary(
        Actor actor,
        UUID familyId,
        SchoolStage schoolStage,
        String title,
        String description,
        String languageTag,
        Integer durationSeconds,
        DocumentaryAccessMode accessMode,
        String sourceReference,
        String rightsHolder,
        String rightsReference,
        LocalDate licenseExpiresOn,
        String idempotencyKey
    ) {
        auth.requireParent(actor, familyId);
        String key = requireKey(idempotencyKey);
        var source = new DocumentarySource(UUID.randomUUID(), familyId, schoolStage, title, description,
            languageTag, durationSeconds, accessMode, sourceReference, rightsHolder, rightsReference,
            licenseExpiresOn, DocumentaryStatus.DRAFT, actor.actorId(), actor.actorId(), 0,
            clock.instant(), clock.instant());
        String payloadHash = hash(source.schoolStage() + "|" + source.title() + "|" + source.description()
            + "|" + source.languageTag() + "|" + source.durationSeconds() + "|" + source.accessMode()
            + "|" + source.sourceReference() + "|" + source.rightsHolder() + "|" + source.rightsReference()
            + "|" + source.licenseExpiresOn());
        return store.documentaryByKey(familyId, key).map(existing -> {
            String existingHash = hash(existing.schoolStage() + "|" + existing.title() + "|" + existing.description()
                + "|" + existing.languageTag() + "|" + existing.durationSeconds() + "|" + existing.accessMode()
                + "|" + existing.sourceReference() + "|" + existing.rightsHolder() + "|" + existing.rightsReference()
                + "|" + existing.licenseExpiresOn());
            if (!existingHash.equals(payloadHash)) throw new Stage3Service.ConflictException("Idempotency key payload mismatch");
            return existing;
        }).orElseGet(() -> store.createDocumentary(familyId, source, key, payloadHash));
    }

    @Transactional(readOnly = true)
    public List<DocumentaryListing> parentDocumentaries(Actor actor, UUID familyId) {
        auth.requireParent(actor, familyId);
        return store.documentaries(familyId).stream().map(source -> listing(source, true)).toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentaryListing> childDocumentaries(Actor actor, UUID familyId, UUID childId) {
        auth.requireChildOrParent(actor, familyId, childId);
        SchoolStage stage = experience(actor, familyId, childId).effectiveStage();
        return store.documentaries(familyId).stream()
            .filter(source -> source.status() == DocumentaryStatus.APPROVED)
            .filter(source -> source.schoolStage() == stage)
            .filter(source -> source.licenseExpiresOn() == null || !source.licenseExpiresOn().isBefore(LocalDate.now(clock)))
            .map(source -> listing(source, actor.role() == ActorRole.PARENT))
            .toList();
    }

    @Transactional
    public DocumentarySource approveDocumentary(Actor actor, UUID familyId, UUID sourceId, String key) {
        auth.requireParent(actor, familyId);
        var source = store.documentary(familyId, sourceId).orElseThrow(FamilyGrowthService.NotFoundException::new);
        if (source.licenseExpiresOn() != null && source.licenseExpiresOn().isBefore(LocalDate.now(clock))) {
            throw new Stage3Service.ConflictException("Documentary license is expired");
        }
        return transition(familyId, sourceId, DocumentaryStatus.APPROVED, actor.actorId(), key);
    }

    @Transactional
    public DocumentarySource withdrawDocumentary(Actor actor, UUID familyId, UUID sourceId, String key) {
        auth.requireParent(actor, familyId);
        return transition(familyId, sourceId, DocumentaryStatus.WITHDRAWN, actor.actorId(), key);
    }

    private DocumentarySource transition(UUID familyId, UUID sourceId, DocumentaryStatus target, UUID actorId, String rawKey) {
        String key = requireKey(rawKey);
        return store.documentaryActionByKey(familyId, key).map(existing -> {
            if (!existing.id().equals(sourceId) || existing.status() != target) {
                throw new Stage3Service.ConflictException("Idempotency key payload mismatch");
            }
            return existing;
        }).orElseGet(() -> store.transitionDocumentary(familyId, sourceId, target, actorId, key, clock.instant()));
    }

    private ExperienceProfile profile(UUID familyId, UUID childId, Stage20Store.StoredExperience stored) {
        LocalDate today = LocalDate.now(clock);
        SchoolStage recommended = SchoolStage.recommended(stored.birthDate(), today);
        SchoolStage effective = stored.stageOverride() == null ? recommended : stored.stageOverride();
        PrimaryGradeBand recommendedBand = effective == SchoolStage.PRIMARY
            ? PrimaryGradeBand.recommended(stored.birthDate(), today) : null;
        PrimaryGradeBand effectiveBand = effective == SchoolStage.PRIMARY
            ? (stored.primaryBandOverride() == null ? recommendedBand : stored.primaryBandOverride()) : null;
        return new ExperienceProfile(familyId, childId, stored.birthDate(),
            Period.between(stored.birthDate(), today).getYears(), recommended, stored.stageOverride(), effective,
            recommendedBand, effective == SchoolStage.PRIMARY ? stored.primaryBandOverride() : null, effectiveBand,
            stored.overrideReason(), stored.hapticsEnabled(), Stage20Models.feedbackFor(effective, stored.hapticsEnabled()),
            Stage20Models.capabilitiesFor(effective), stored.version(), stored.updatedAt());
    }

    private DocumentaryListing listing(DocumentarySource source, boolean parentView) {
        boolean parentActionRequired = source.accessMode() == DocumentaryAccessMode.OFFICIAL_LINK;
        String reference = parentView ? source.sourceReference() : null;
        return new DocumentaryListing(source.id(), source.schoolStage(), source.title(), source.description(),
            source.languageTag(), source.durationSeconds(), source.accessMode(), source.status(), reference,
            parentView ? source.rightsHolder() : null, parentView ? source.rightsReference() : null,
            parentView ? source.licenseExpiresOn() : null, parentActionRequired);
    }

    private static void validateOverride(SchoolStage override, String reason) {
        if (override == SchoolStage.PARENT_ONLY) {
            throw new IllegalArgumentException("PARENT_ONLY cannot be selected as a school override");
        }
        if (override != null && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("overrideReason is required when stageOverride is set");
        }
    }

    private static String requireKey(String value) {
        String key = requireText(value, "Idempotency-Key");
        if (key.length() > 120) throw new IllegalArgumentException("Idempotency-Key is too long");
        return key;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
