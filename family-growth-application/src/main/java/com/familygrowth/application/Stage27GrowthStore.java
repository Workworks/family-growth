package com.familygrowth.application;
import com.familygrowth.domain.Stage27GrowthModels.*;import java.time.*;import java.util.*;
public interface Stage27GrowthStore{
 record Replay(UUID childId,UUID resultId,String actionType,String payloadHash){}
 List<Plan> plans(UUID family,UUID child);Optional<Plan> plan(UUID family,UUID child,UUID plan);Optional<Replay> planReplay(UUID family,String key);
 Plan createPlan(UUID family,UUID child,String title,String description,GrowthCategory category,String ageStage,String target,LocalDate start,LocalDate end,PlanStatus status,UUID actor,String key,String hash,Instant now);
 Plan transitionPlan(UUID family,UUID child,UUID plan,PlanStatus from,PlanStatus to,long revision,String reason,UUID actor,String key,String hash,Instant now);
 List<Goal> goals(UUID family,UUID child,UUID plan);Optional<Goal> goal(UUID family,UUID child,UUID goal);Optional<Replay> goalReplay(UUID family,String key);
 Goal createGoal(UUID family,UUID child,UUID plan,String title,String description,String target,UUID actor,String key,String hash,Instant now);
 Goal transitionGoal(UUID family,UUID child,UUID goal,GoalStatus from,GoalStatus to,long revision,String reason,UUID actor,String key,String hash,Instant now);
 List<Milestone> milestones(UUID family,UUID child);Optional<Milestone> milestone(UUID family,UUID child,UUID id);Optional<Replay> milestoneReplay(UUID family,String key);
 Milestone createMilestone(UUID family,UUID child,UUID plan,UUID goal,LocalDate occurred,MilestoneCategory category,String title,String observation,UUID actor,String key,String hash,Instant now);
 Milestone updateMilestone(UUID family,UUID child,UUID id,LocalDate occurred,MilestoneCategory category,String title,String observation,long revision,UUID actor,String key,String hash,Instant now);
 Optional<Replay> artifactReplay(UUID family,String key);ArtifactMetadata createArtifact(UUID family,UUID child,UUID milestone,String type,byte[] bytes,String sha,String alt,UUID actor,String key,String hash,Instant now);Optional<Artifact> artifact(UUID family,UUID child,UUID id);
 GrowthReport report(UUID family,UUID child,Instant now);
}
