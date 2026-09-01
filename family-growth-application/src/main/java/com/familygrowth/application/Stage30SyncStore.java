package com.familygrowth.application;

import static com.familygrowth.domain.Stage30SyncModels.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Stage30SyncStore {
    List<SyncFact> facts(UUID familyId, UUID childId, UUID actorId, boolean parent);
    Optional<SyncCheckpoint> replay(UUID familyId, UUID actorId, String clientId, String idempotencyKey);
    long checkpoint(UUID familyId, UUID childId, UUID actorId, String clientId, String idempotencyKey,
                    String requestHash, String projectionHash, Instant now);
}
