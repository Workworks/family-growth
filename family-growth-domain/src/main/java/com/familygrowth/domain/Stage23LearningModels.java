package com.familygrowth.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.familygrowth.domain.Stage20Models.PrimaryGradeBand;
import com.familygrowth.domain.Stage20Models.SchoolStage;

public final class Stage23LearningModels {
    private Stage23LearningModels() { }

    public enum AssignmentSource { PARENT, AUTONOMOUS }
    public enum SupportEventType { HELP_REQUESTED, INCORRECT_OBSERVED, MISCONCEPTION_CLASSIFIED, REVISIT_SCHEDULED, REVISIT_COMPLETED }
    public enum MisconceptionCategory { INSTRUCTION, CONCEPT, PROCEDURE, LANGUAGE, ATTENTION, OTHER }

    public record SupportEvent(UUID id, UUID assignmentId, UUID activityId, SupportEventType type,
                               MisconceptionCategory category, String childMessage, String privateNote,
                               Instant revisitAt, UUID parentEventId, Instant createdAt) {
        public SupportEvent childSafe() {
            return new SupportEvent(id, assignmentId, activityId, type, category, childMessage, "",
                revisitAt, parentEventId, createdAt);
        }
    }

    public record SupportTimeline(List<SupportEvent> events) {
        public SupportTimeline { events = events == null ? List.of() : List.copyOf(events); }
    }

    public record SubjectLearningFacts(String subjectCode, long assigned, long inProgress, long submitted,
                                       long completed, long reworkRequired, long openSupport,
                                       long scheduledRevisits, long dueRevisits) {
        public SubjectLearningFacts {
            if (subjectCode == null || subjectCode.isBlank()) throw new IllegalArgumentException("subjectCode is required");
            if (assigned < 0 || inProgress < 0 || submitted < 0 || completed < 0 || reworkRequired < 0
                || openSupport < 0 || scheduledRevisits < 0 || dueRevisits < 0) {
                throw new IllegalArgumentException("Learning fact counts cannot be negative");
            }
        }
    }

    public record PrimaryLearningReport(UUID childId, SchoolStage effectiveStage,
                                        PrimaryGradeBand effectivePrimaryBand, Instant windowStart,
                                        Instant windowEnd, long recordedLearningMinutes,
                                        List<SubjectLearningFacts> subjects, Instant generatedAt) {
        public PrimaryLearningReport {
            if (childId == null || effectiveStage == null || windowStart == null || windowEnd == null || generatedAt == null) {
                throw new IllegalArgumentException("Primary learning report facts are required");
            }
            if (recordedLearningMinutes < 0 || windowEnd.isBefore(windowStart)) {
                throw new IllegalArgumentException("Primary learning report window is invalid");
            }
            subjects = subjects == null ? List.of() : List.copyOf(subjects);
        }
    }

    public record RewardPolicy(UUID familyId, UUID childId, BigDecimal moneyReward,
                               long coinReward, long xpReward, long version, Instant updatedAt) {
        public RewardPolicy {
            moneyReward = normalize(moneyReward);
            if (coinReward < 0 || coinReward > 1_000_000 || xpReward < 0 || xpReward > 1_000_000) {
                throw new IllegalArgumentException("Reward is outside the supported range");
            }
        }
    }

    public record RewardSnapshot(BigDecimal money, long coin, long xp, Instant settledAt) {
        public RewardSnapshot {
            money = normalize(money);
            if (coin < 0 || xp < 0) throw new IllegalArgumentException("Reward cannot be negative");
        }
        public boolean settled() { return settledAt != null; }
    }

    public static BigDecimal normalize(BigDecimal value) {
        BigDecimal result = value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP);
        if (result.signum() < 0 || result.compareTo(new BigDecimal("10000.00")) > 0) {
            throw new IllegalArgumentException("Money reward is outside the supported range");
        }
        return result;
    }
}
