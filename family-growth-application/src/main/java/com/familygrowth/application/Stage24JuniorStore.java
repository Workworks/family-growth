package com.familygrowth.application;

import com.familygrowth.domain.Stage24JuniorModels.JuniorLearningPlan;
import com.familygrowth.domain.Stage24JuniorModels.MoveDirection;
import com.familygrowth.domain.Stage23LearningModels.SubjectLearningFacts;
import java.util.List;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface Stage24JuniorStore {
    record Replay(UUID childId, UUID assignmentId, MoveDirection direction, String payloadHash) { }
    JuniorLearningPlan plan(UUID familyId, UUID childId, Instant now);
    JuniorLearningPlan move(UUID familyId, UUID childId, UUID assignmentId, MoveDirection direction,
                            long expectedRevision, UUID actorId, String key, String payloadHash, Instant now);
    Optional<Replay> replay(UUID familyId, String key);
    List<SubjectLearningFacts> facts(UUID familyId, UUID childId, Instant now);
    long recordedLearningMinutes(UUID familyId, UUID childId, Instant from, Instant to);
}
