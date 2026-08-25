package com.familygrowth.application;
import com.familygrowth.domain.*; import java.util.*;
public interface FamilyGrowthStore {
 Family save(Family value); ParentProfile save(ParentProfile value); ChildProfile save(ChildProfile value); GrowthPlan save(GrowthPlan value); GrowthGoal save(GrowthGoal value); GrowthTask save(GrowthTask value);
 Optional<Family> findFamily(UUID id); Optional<ChildProfile> findChild(UUID id); Optional<GrowthPlan> findPlan(UUID id); Optional<GrowthGoal> findGoal(UUID id);
}
