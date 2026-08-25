package com.familygrowth.infrastructure;
import com.familygrowth.application.FamilyGrowthStore; import com.familygrowth.domain.*; import java.util.*; import org.springframework.stereotype.Repository;
@Repository
class JpaFamilyGrowthStore implements FamilyGrowthStore {
 private final FamilyJpaRepository families; private final ParentJpaRepository parents; private final ChildJpaRepository children; private final PlanJpaRepository plans; private final GoalJpaRepository goals; private final TaskJpaRepository tasks;
 JpaFamilyGrowthStore(FamilyJpaRepository a,ParentJpaRepository b,ChildJpaRepository c,PlanJpaRepository d,GoalJpaRepository e,TaskJpaRepository f){families=a;parents=b;children=c;plans=d;goals=e;tasks=f;}
 public Family save(Family v){var e=new FamilyEntity();e.id=v.id();e.name=v.name();e.createdAt=v.createdAt();families.save(e);return v;}
 public ParentProfile save(ParentProfile v){var e=new ParentEntity();e.id=v.id();e.familyId=v.familyId();e.displayName=v.displayName();e.createdAt=v.createdAt();parents.saveAndFlush(e);return v;}
 public ChildProfile save(ChildProfile v){var e=new ChildEntity();e.id=v.id();e.familyId=v.familyId();e.displayName=v.displayName();e.birthDate=v.birthDate();e.ageStage=v.ageStage().name();e.createdAt=v.createdAt();children.saveAndFlush(e);return v;}
 public GrowthPlan save(GrowthPlan v){var e=new PlanEntity();e.id=v.id();e.familyId=v.familyId();e.childId=v.childId();e.title=v.title();e.description=v.description();e.startDate=v.startDate();e.endDate=v.endDate();e.active=v.active();e.createdAt=v.createdAt();plans.save(e);return v;}
 public GrowthGoal save(GrowthGoal v){var e=new GoalEntity();e.id=v.id();e.familyId=v.familyId();e.planId=v.planId();e.title=v.title();e.description=v.description();e.createdAt=v.createdAt();goals.save(e);return v;}
 public GrowthTask save(GrowthTask v){var e=new TaskEntity();e.id=v.id();e.familyId=v.familyId();e.goalId=v.goalId();e.title=v.title();e.description=v.description();e.category=v.category().name();e.difficulty=v.difficulty().name();e.expectedMinutes=v.expectedMinutes();e.active=v.active();e.createdAt=v.createdAt();tasks.save(e);return v;}
 public Optional<Family> findFamily(UUID id){return families.findById(id).map(e->new Family(e.id,e.name,e.createdAt));}
 public Optional<ParentProfile> findParent(UUID id){return parents.findById(id).map(e->new ParentProfile(e.id,e.familyId,e.displayName,e.createdAt));}
 public Optional<ChildProfile> findChild(UUID id){return children.findById(id).map(e->new ChildProfile(e.id,e.familyId,e.displayName,e.birthDate,AgeStage.valueOf(e.ageStage),e.createdAt));}
 public Optional<GrowthPlan> findPlan(UUID id){return plans.findById(id).map(e->new GrowthPlan(e.id,e.familyId,e.childId,e.title,e.description,e.startDate,e.endDate,e.active,e.createdAt));}
 public Optional<GrowthGoal> findGoal(UUID id){return goals.findById(id).map(e->new GrowthGoal(e.id,e.familyId,e.planId,e.title,e.description,e.createdAt));}
 public Optional<GrowthTask> findTask(UUID id){return tasks.findById(id).map(e->new GrowthTask(e.id,e.familyId,e.goalId,e.title,e.description,TaskCategory.valueOf(e.category),TaskDifficulty.valueOf(e.difficulty),e.expectedMinutes,e.active,e.createdAt));}
}
