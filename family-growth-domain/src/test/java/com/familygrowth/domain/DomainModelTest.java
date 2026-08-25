package com.familygrowth.domain;
import static org.junit.jupiter.api.Assertions.*; import java.time.*; import java.util.UUID; import org.junit.jupiter.api.Test;
class DomainModelTest {
 @Test void learningTaskRequiresSaneDuration(){assertThrows(IllegalArgumentException.class,()->new GrowthTask(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"数学","",TaskCategory.LEARNING,TaskDifficulty.NORMAL,0,true,Instant.now()));}
 @Test void planEndCannotPrecedeStart(){var day=LocalDate.of(2026,8,25);assertThrows(IllegalArgumentException.class,()->new GrowthPlan(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"阅读","",day,day.minusDays(1),true,Instant.now()));}
 @Test void familyNameIsTrimmed(){assertEquals("成长家庭",Family.create("  成长家庭  ",Instant.EPOCH).name());}
}
