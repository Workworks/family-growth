package com.familygrowth.application;

import com.familygrowth.domain.Stage20Models.SchoolStage;
import com.familygrowth.domain.Stage23LearningModels.RewardPolicy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface Stage23LearningStore {
    record SyncReplay(UUID childId, SchoolStage stage, String payloadHash, int createdCount) { }
    RewardPolicy policy(UUID familyId, UUID childId);
    RewardPolicy updatePolicy(UUID familyId, UUID childId, UUID actorId, BigDecimal money, long coin, long xp,
                              long expectedVersion, String reason, Instant now);
    Optional<SyncReplay> syncReplay(UUID familyId, String key);
    int syncAssignments(UUID familyId, UUID childId, SchoolStage stage, UUID actorId, String key,
                        String payloadHash, Instant now);
    void snapshotAssignment(UUID familyId, UUID childId, UUID assignmentId, Instant now);
    void settleReward(UUID familyId, UUID childId, UUID assignmentId, UUID actorId, Instant now);
}
