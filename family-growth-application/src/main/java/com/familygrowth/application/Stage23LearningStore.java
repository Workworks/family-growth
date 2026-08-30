package com.familygrowth.application;

import com.familygrowth.domain.Stage20Models.SchoolStage;
import com.familygrowth.domain.Stage23LearningModels.RewardPolicy;
import com.familygrowth.domain.Stage23LearningModels.MisconceptionCategory;
import com.familygrowth.domain.Stage23LearningModels.SupportEvent;
import com.familygrowth.domain.Stage23LearningModels.SubjectLearningFacts;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
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
    List<SupportEvent> supportEvents(UUID familyId, UUID childId, UUID assignmentId);
    void requestHelp(UUID familyId, UUID childId, UUID assignmentId, UUID activityId, UUID actorId,
                     String message, String key, String payloadHash, Instant now);
    void classifySupport(UUID familyId, UUID childId, UUID assignmentId, UUID sourceEventId, UUID actorId,
                         MisconceptionCategory category, String privateNote, Instant revisitAt,
                         String key, String payloadHash, Instant now);
    void recordAttemptSupport(UUID familyId, UUID childId, UUID assignmentId, UUID activityId,
                              UUID actorId, Boolean correct, String attemptKey, Instant now);
    List<SubjectLearningFacts> primaryLearningFacts(UUID familyId, UUID childId, Instant now);
    long recordedLearningMinutes(UUID familyId, UUID childId, Instant from, Instant to);
}
