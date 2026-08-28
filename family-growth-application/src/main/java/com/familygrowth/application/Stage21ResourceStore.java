package com.familygrowth.application;

import com.familygrowth.domain.Stage21ResourceModels.EducationResourceSource;
import com.familygrowth.domain.Stage21ResourceModels.ResourceSourceStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Stage21ResourceStore {
    record ActionReplay(UUID sourceId, String actionType) {
    }

    Optional<EducationResourceSource> byCreateKey(UUID familyId, String key);
    EducationResourceSource create(EducationResourceSource source, String key, String payloadHash);
    List<EducationResourceSource> sources(UUID familyId);
    Optional<EducationResourceSource> source(UUID familyId, UUID sourceId);
    Optional<ActionReplay> action(UUID familyId, String key);
    EducationResourceSource refreshSuccess(UUID familyId, UUID sourceId,
                                           List<EducationResourceDiscovery.DiscoveredCategory> categories,
                                           UUID actorId, String key, Instant now);
    EducationResourceSource refreshFailure(UUID familyId, UUID sourceId, String safeError,
                                           UUID actorId, String key, Instant now);
    EducationResourceSource transition(UUID familyId, UUID sourceId, ResourceSourceStatus target,
                                       UUID actorId, String key, Instant now);
}
