package com.familygrowth.domain;

import com.familygrowth.domain.Stage23LearningModels.SubjectLearningFacts;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class Stage25SeniorModels {
    private Stage25SeniorModels() { }
    public enum ModuleType { REQUIRED, SELECTIVE_REQUIRED, ELECTIVE }
    public enum GoalStatus { ACTIVE, ARCHIVED }
    public enum ReflectionStrategy { CONTINUE, REVIEW_FOUNDATION, TRY_ANOTHER_METHOD, ASK_FOR_SUPPORT, PAUSE_AND_REPLAN }

    public record ModuleSelection(String subjectCode, ModuleType moduleType) {
        public ModuleSelection {
            subjectCode=required(subjectCode,"subjectCode",40).toUpperCase();
            if(!subjectCode.matches("[A-Z0-9_-]+")||moduleType==null) throw new IllegalArgumentException("Valid senior module is required");
        }
    }
    public record ModuleConfiguration(UUID childId,long revision,List<ModuleSelection> selections,Instant updatedAt) {
        public ModuleConfiguration {
            selections=selections==null?List.of():selections.stream().distinct().toList();
            if(childId==null||revision<0||updatedAt==null||selections.size()>12) throw new IllegalArgumentException("Senior module configuration is invalid");
        }
    }
    public record WeeklyGoal(UUID id,UUID childId,UUID assignmentId,ModuleSelection module,LocalDate weekStart,
                             String title,String evidenceTarget,String nextAction,GoalStatus status,long revision,
                             Instant createdAt,Instant updatedAt) {
        public WeeklyGoal {
            if(id==null||childId==null||module==null||weekStart==null||status==null||revision<0||createdAt==null||updatedAt==null)
                throw new IllegalArgumentException("Senior weekly goal facts are required");
            title=required(title,"title",160);evidenceTarget=required(evidenceTarget,"evidenceTarget",500);nextAction=required(nextAction,"nextAction",500);
        }
    }
    public record Reflection(UUID id,UUID childId,UUID goalId,UUID assignmentId,String evidenceSummary,
                             ReflectionStrategy strategy,String nextAction,boolean supportRequested,Instant createdAt) {
        public Reflection {
            if(id==null||childId==null||strategy==null||createdAt==null) throw new IllegalArgumentException("Senior reflection facts are required");
            evidenceSummary=required(evidenceSummary,"evidenceSummary",1000);nextAction=required(nextAction,"nextAction",500);
        }
    }
    public record SeniorLearningReport(UUID childId,Instant windowStart,Instant windowEnd,long recordedLearningMinutes,
                                       List<SubjectLearningFacts> subjects,long activeGoals,long archivedGoals,
                                       long reflections,long supportRequests,Instant generatedAt) {
        public SeniorLearningReport {
            subjects=subjects==null?List.of():List.copyOf(subjects);
            if(childId==null||windowStart==null||windowEnd==null||generatedAt==null||windowEnd.isBefore(windowStart)
                ||recordedLearningMinutes<0||activeGoals<0||archivedGoals<0||reflections<0||supportRequests<0)
                throw new IllegalArgumentException("Senior report facts are invalid");
        }
    }
    private static String required(String value,String name,int max){String text=value==null?"":value.trim();if(text.isBlank()||text.length()>max)throw new IllegalArgumentException(name+" is required and must be at most "+max);return text;}
}
