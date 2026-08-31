package com.familygrowth.domain;

import com.familygrowth.domain.Stage21TeachingModels.AssignmentStatus;
import com.familygrowth.domain.Stage23LearningModels.SubjectLearningFacts;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class Stage24JuniorModels {
    private Stage24JuniorModels() { }
    public enum MoveDirection { UP, DOWN }
    public record PlanItem(UUID assignmentId, String subjectCode, String courseTitle, String lessonTitle,
                           AssignmentStatus status, int position) { }
    public record JuniorLearningPlan(UUID childId, long revision, List<PlanItem> items, Instant updatedAt) {
        public JuniorLearningPlan { items = items == null ? List.of() : List.copyOf(items); }
    }
    public record JuniorLearningReport(UUID childId, Instant windowStart, Instant windowEnd,
                                       long recordedLearningMinutes, List<SubjectLearningFacts> subjects,
                                       long planRevision, Instant generatedAt) {
        public JuniorLearningReport {
            if(childId==null||windowStart==null||windowEnd==null||generatedAt==null||windowEnd.isBefore(windowStart)
                ||recordedLearningMinutes<0||planRevision<0) throw new IllegalArgumentException("Junior report facts are invalid");
            subjects=subjects==null?List.of():List.copyOf(subjects);
        }
    }
}
