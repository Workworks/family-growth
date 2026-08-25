package com.familygrowth.application;
import com.familygrowth.domain.*; import java.time.*; import java.util.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service @Transactional
public class FamilyGrowthService {
 private final FamilyGrowthStore store; private final Clock clock;
 public FamilyGrowthService(FamilyGrowthStore store,Clock clock){this.store=store;this.clock=clock;}
 public Family createFamily(String name){return store.save(Family.create(name,clock.instant()));}
 public ParentProfile addParent(UUID familyId,String name){family(familyId);return store.save(ParentProfile.create(familyId,name,clock.instant()));}
 public ChildProfile addChild(UUID familyId,String name,LocalDate birthDate,AgeStage stage){family(familyId);return store.save(ChildProfile.create(familyId,name,birthDate,stage,clock.instant()));}
 public GrowthPlan createPlan(UUID familyId,UUID childId,String title,String description,LocalDate start,LocalDate end){var child=store.findChild(childId).filter(c->c.familyId().equals(familyId)).orElseThrow(NotFoundException::new);return store.save(new GrowthPlan(UUID.randomUUID(),familyId,child.id(),title,description,start,end,true,clock.instant()));}
 public GrowthGoal createGoal(UUID familyId,UUID planId,String title,String description){var plan=store.findPlan(planId).filter(p->p.familyId().equals(familyId)).orElseThrow(NotFoundException::new);return store.save(new GrowthGoal(UUID.randomUUID(),familyId,plan.id(),title,description,clock.instant()));}
 public GrowthTask createTask(UUID familyId,UUID goalId,String title,String description,TaskCategory category,TaskDifficulty difficulty,int minutes){var goal=store.findGoal(goalId).filter(g->g.familyId().equals(familyId)).orElseThrow(NotFoundException::new);return store.save(new GrowthTask(UUID.randomUUID(),familyId,goal.id(),title,description,category,difficulty,minutes,true,clock.instant()));}
 private Family family(UUID id){return store.findFamily(id).orElseThrow(NotFoundException::new);}
 public static final class NotFoundException extends RuntimeException {}
}
