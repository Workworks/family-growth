package com.familygrowth.domain;
import static com.familygrowth.domain.Stage27GrowthModels.*;import static org.junit.jupiter.api.Assertions.*;import org.junit.jupiter.api.Test;
class Stage27GrowthModelsTest{
 @Test void planTransitionsAreExplicitAndTerminal(){assertDoesNotThrow(()->requireTransition(PlanStatus.DRAFT,PlanStatus.ACTIVE));assertDoesNotThrow(()->requireTransition(PlanStatus.ACTIVE,PlanStatus.PAUSED));assertDoesNotThrow(()->requireTransition(PlanStatus.PAUSED,PlanStatus.COMPLETED));assertThrows(IllegalArgumentException.class,()->requireTransition(PlanStatus.COMPLETED,PlanStatus.ACTIVE));assertThrows(IllegalArgumentException.class,()->requireTransition(PlanStatus.CANCELED,PlanStatus.DRAFT));}
 @Test void goalCannotBeReopened(){assertDoesNotThrow(()->requireTransition(GoalStatus.ACTIVE,GoalStatus.COMPLETED));assertThrows(IllegalArgumentException.class,()->requireTransition(GoalStatus.COMPLETED,GoalStatus.ACTIVE));}
}
