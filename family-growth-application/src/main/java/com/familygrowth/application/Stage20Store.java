package com.familygrowth.application;

import com.familygrowth.domain.Stage20Models.DocumentarySource;
import com.familygrowth.domain.Stage20Models.DocumentaryStatus;
import com.familygrowth.domain.Stage20Models.ExperienceAudit;
import com.familygrowth.domain.Stage20Models.SchoolStage;
import com.familygrowth.domain.Stage20Models.PrimaryGradeBand;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Stage20Store {
    record StoredExperience(
        LocalDate birthDate,
        SchoolStage stageOverride,
        PrimaryGradeBand primaryBandOverride,
        String overrideReason,
        boolean hapticsEnabled,
        long version,
        Instant updatedAt
    ) {
    }

    record ExperienceUpdate(
        LocalDate birthDate,
        SchoolStage stageOverride,
        PrimaryGradeBand primaryBandOverride,
        String overrideReason,
        boolean hapticsEnabled,
        long expectedVersion,
        String auditReason
    ) {
    }

    StoredExperience experience(UUID familyId, UUID childId, Instant now);
    StoredExperience updateExperience(UUID familyId, UUID childId, UUID actorId, ExperienceUpdate update, Instant now);
    List<ExperienceAudit> experienceAudit(UUID familyId, UUID childId, int limit);
    int countStageArchiveCandidates(UUID familyId,UUID childId,SchoolStage oldStage);
    int countStageRestoreCandidates(UUID familyId,UUID childId,SchoolStage newStage);
    void applyStageTransition(UUID familyId,UUID childId,UUID actorId,SchoolStage oldStage,SchoolStage newStage,String reason,Instant now);

    Optional<DocumentarySource> documentaryByKey(UUID familyId, String idempotencyKey);
    DocumentarySource createDocumentary(UUID familyId, DocumentarySource source, String idempotencyKey, String payloadHash);
    List<DocumentarySource> documentaries(UUID familyId);
    Optional<DocumentarySource> documentary(UUID familyId, UUID sourceId);
    Optional<DocumentarySource> documentaryActionByKey(UUID familyId, String idempotencyKey);
    DocumentarySource transitionDocumentary(UUID familyId, UUID sourceId, DocumentaryStatus target,
                                            UUID actorId, String idempotencyKey, Instant now);
}
