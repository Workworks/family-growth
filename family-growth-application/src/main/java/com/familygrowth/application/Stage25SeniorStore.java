package com.familygrowth.application;

import com.familygrowth.domain.Stage23LearningModels.SubjectLearningFacts;
import com.familygrowth.domain.Stage25SeniorModels.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Stage25SeniorStore {
    record ModuleReplay(UUID childId,String payloadHash) { }
    record GoalReplay(UUID childId,UUID goalId,String actionType,String payloadHash) { }
    record ReflectionReplay(UUID childId,UUID reflectionId,String payloadHash) { }
    ModuleConfiguration modules(UUID familyId,UUID childId,Instant now);
    ModuleConfiguration updateModules(UUID familyId,UUID childId,List<ModuleSelection> selections,long expectedRevision,
                                      String reason,UUID actorId,String key,String payloadHash,Instant now);
    Optional<ModuleReplay> moduleReplay(UUID familyId,String key);
    boolean moduleEnabled(UUID familyId,UUID childId,ModuleSelection module);
    List<WeeklyGoal> goals(UUID familyId,UUID childId);
    WeeklyGoal createGoal(UUID familyId,UUID childId,UUID assignmentId,ModuleSelection module,LocalDate weekStart,
                          String title,String evidenceTarget,String nextAction,UUID actorId,String key,String payloadHash,Instant now);
    WeeklyGoal updateGoal(UUID familyId,UUID childId,UUID goalId,String title,String evidenceTarget,String nextAction,
                          long expectedRevision,UUID actorId,String key,String payloadHash,Instant now);
    WeeklyGoal archiveGoal(UUID familyId,UUID childId,UUID goalId,long expectedRevision,UUID actorId,String key,String payloadHash,Instant now);
    Optional<GoalReplay> goalReplay(UUID familyId,String key);
    List<Reflection> reflections(UUID familyId,UUID childId);
    Reflection reflect(UUID familyId,UUID childId,UUID goalId,UUID assignmentId,String evidenceSummary,
                       ReflectionStrategy strategy,String nextAction,boolean supportRequested,UUID actorId,
                       String key,String payloadHash,Instant now);
    Optional<ReflectionReplay> reflectionReplay(UUID familyId,String key);
    List<SubjectLearningFacts> facts(UUID familyId,UUID childId,Instant now);
    long recordedLearningMinutes(UUID familyId,UUID childId,Instant from,Instant to);
    long countGoals(UUID familyId,UUID childId,GoalStatus status);
    long countReflections(UUID familyId,UUID childId,Instant from,boolean supportOnly);
}
