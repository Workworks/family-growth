package com.familygrowth.domain;

import com.familygrowth.domain.Stage20Models.SchoolStage;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class Stage21TeachingModels {
    private static final Set<String> BUNDLED_VIDEO_REFS = Set.of(
        "lesson_color_garden", "lesson_count_to_five", "lesson_shape_home");
    private Stage21TeachingModels() { }

    public enum CourseVersionStatus { DRAFT, PUBLISHED }
    public enum AssignmentStatus { ASSIGNED, IN_PROGRESS, SUBMITTED, COMPLETED, REWORK_REQUIRED }
    public enum EvidenceType { VIEWED, ATTEMPTED, CHECKED, PARENT_CONFIRMED, MASTERED }
    public enum ReviewDecision { APPROVE, REWORK }
    public enum KindergartenAgeBand { SHARED_3_4, TRANSITION_5_6 }
    public enum KindergartenDomain { HEALTH, LANGUAGE, SOCIAL, SCIENCE, ARTS }

    public enum ActivityType {
        SHORT_VIDEO(EvidenceType.VIEWED, false),
        PARENT_CHILD_READING(EvidenceType.PARENT_CONFIRMED, false),
        LISTEN_CHOOSE(EvidenceType.CHECKED, true),
        SINGLE_CHOICE(EvidenceType.CHECKED, true),
        MATCHING(EvidenceType.CHECKED, true),
        SORTING(EvidenceType.CHECKED, true),
        ORAL_RESPONSE(EvidenceType.PARENT_CONFIRMED, false),
        OFFLINE_PRACTICE(EvidenceType.PARENT_CONFIRMED, false),
        PARENT_CONFIRMATION(EvidenceType.PARENT_CONFIRMED, false);

        private final EvidenceType requiredEvidence;
        private final boolean objective;
        ActivityType(EvidenceType requiredEvidence, boolean objective) {
            this.requiredEvidence = requiredEvidence;
            this.objective = objective;
        }
        public EvidenceType requiredEvidence() { return requiredEvidence; }
        public boolean objective() { return objective; }
        public boolean parentConfirmed() { return requiredEvidence == EvidenceType.PARENT_CONFIRMED; }
    }

    public record QuestionOption(String value, String label) {
        public QuestionOption {
            value = text(value, "option value", 160);
            label = text(label, "option label", 240);
        }
    }

    public record ActivityDraft(ActivityType type, String title, String instruction, String contentRef, int expectedMinutes,
                                String prompt, String hint, List<QuestionOption> options, String answerKey) {
        public ActivityDraft {
            Objects.requireNonNull(type);
            title = text(title, "activity title", 160);
            instruction = text(instruction, "activity instruction", 500);
            contentRef = normalize(contentRef, 160);
            if (expectedMinutes < 1 || expectedMinutes > 60) throw new IllegalArgumentException("expectedMinutes must be 1 to 60");
            prompt = normalize(prompt, 500);
            hint = normalize(hint, 300);
            answerKey = normalize(answerKey, 1000);
            options = options == null ? List.of() : List.copyOf(options);
            if (options.size() > 20 || options.stream().map(QuestionOption::value).distinct().count() != options.size()) {
                throw new IllegalArgumentException("Activity options must be unique and no more than 20");
            }
            if (type.objective() && (prompt.isBlank() || answerKey.isBlank() || options.size() < 2)) {
                throw new IllegalArgumentException("Objective activity requires prompt, answer key and at least two options");
            }
            boolean answerDeclared = false;
            for (QuestionOption option : options) if (option.value().equals(answerKey)) answerDeclared = true;
            if (type.objective() && !answerDeclared) throw new IllegalArgumentException("Objective answer key must reference a declared option");
            if (!type.objective() && (!answerKey.isBlank() || !options.isEmpty())) {
                throw new IllegalArgumentException("Non-objective activity cannot contain an answer key or options");
            }
            if (type == ActivityType.SHORT_VIDEO && !BUNDLED_VIDEO_REFS.contains(contentRef)) {
                throw new IllegalArgumentException("Short video requires a reviewed bundled contentRef");
            }
            if (type != ActivityType.SHORT_VIDEO && !contentRef.isBlank()) {
                throw new IllegalArgumentException("Only short video activities can contain contentRef");
            }
        }
    }

    public record LessonDraft(String title, String summary, List<ActivityDraft> activities) {
        public LessonDraft {
            title = text(title, "lesson title", 160);
            summary = text(summary, "lesson summary", 500);
            activities = activities == null ? List.of() : List.copyOf(activities);
            if (activities.isEmpty() || activities.size() > 20) throw new IllegalArgumentException("Lesson requires 1 to 20 activities");
        }
    }

    public record UnitDraft(String title, List<LessonDraft> lessons) {
        public UnitDraft {
            title = text(title, "unit title", 160);
            lessons = lessons == null ? List.of() : List.copyOf(lessons);
            if (lessons.isEmpty() || lessons.size() > 30) throw new IllegalArgumentException("Unit requires 1 to 30 lessons");
        }
    }

    public record VersionDraft(String summary, String rightsBasis, KindergartenAgeBand kindergartenAgeBand,
                               List<KindergartenDomain> kindergartenDomains, List<UnitDraft> units) {
        public VersionDraft {
            summary = text(summary, "version summary", 500);
            rightsBasis = text(rightsBasis, "rightsBasis", 500);
            kindergartenDomains = kindergartenDomains == null ? List.of() : kindergartenDomains.stream().distinct().toList();
            units = units == null ? List.of() : List.copyOf(units);
            if (units.isEmpty() || units.size() > 12) throw new IllegalArgumentException("Version requires 1 to 12 units");
        }
        public VersionDraft(String summary, String rightsBasis, List<UnitDraft> units) {
            this(summary, rightsBasis, null, List.of(), units);
        }
    }

    public record ActivityContent(UUID id, ActivityType type, String title, String instruction, String contentRef, int expectedMinutes,
                                  String prompt, String hint, List<QuestionOption> options, String answerKey) { }
    public record LessonContent(UUID id, String title, String summary, List<ActivityContent> activities) { }
    public record UnitContent(UUID id, String title, List<LessonContent> lessons) { }
    public record CourseVersion(UUID courseId, UUID versionId, UUID familyId, SchoolStage schoolStage,
                                String subjectCode, String title, int versionNumber, String summary,
                                String rightsBasis, KindergartenAgeBand kindergartenAgeBand,
                                List<KindergartenDomain> kindergartenDomains, CourseVersionStatus status, List<UnitContent> units,
                                Instant publishedAt) {
        public CourseVersion {
            kindergartenDomains = kindergartenDomains == null ? List.of() : List.copyOf(kindergartenDomains);
            units = units == null ? List.of() : List.copyOf(units);
        }
    }
    public record ParentCourseSummary(UUID courseId, String title, SchoolStage schoolStage, String subjectCode,
                                      UUID versionId, int versionNumber, CourseVersionStatus status, int lessonCount,
                                      Instant publishedAt) { }

    public record ActivityProgress(UUID id, ActivityType type, String title, String instruction, String contentRef, int expectedMinutes,
                                   String prompt, String hint, List<QuestionOption> options,
                                   EvidenceType requiredEvidence, Set<EvidenceType> evidence, Boolean checkedCorrect) { }
    public record LearningAssignment(UUID id, UUID childId, UUID courseVersionId, UUID lessonId,
                                     String courseTitle, String unitTitle, String lessonTitle, String lessonSummary,
                                     SchoolStage schoolStage, String subjectCode, AssignmentStatus status,
                                     long version, List<ActivityProgress> activities, String reviewNote,
                                     Instant updatedAt) { }
    public record AssignmentFacts(LearningAssignment projection, Map<UUID, ActivityContent> contentByActivity) { }

    public static boolean childEvidenceSatisfied(ActivityProgress activity) {
        if (activity.requiredEvidence() == EvidenceType.PARENT_CONFIRMED) {
            return activity.evidence().contains(EvidenceType.ATTEMPTED);
        }
        return activity.evidence().contains(activity.requiredEvidence())
            && (activity.requiredEvidence() != EvidenceType.CHECKED || Boolean.TRUE.equals(activity.checkedCorrect()));
    }

    public static void validateForPublish(CourseVersion version) {
        if (version.schoolStage() != SchoolStage.KINDERGARTEN) {
            if (version.kindergartenAgeBand() != null || !version.kindergartenDomains().isEmpty()) {
                throw new IllegalArgumentException("Kindergarten metadata is only valid for kindergarten courses");
            }
            return;
        }
        if (version.kindergartenAgeBand() == null) {
            throw new IllegalArgumentException("Kindergarten course requires an age band");
        }
        if (version.kindergartenDomains() == null || version.kindergartenDomains().isEmpty()) {
            throw new IllegalArgumentException("Kindergarten course requires at least one learning domain");
        }
        for (UnitContent unit : version.units()) {
            for (LessonContent lesson : unit.lessons()) validateKindergartenLesson(lesson);
        }
    }

    private static void validateKindergartenLesson(LessonContent lesson) {
        if (lesson.activities().size() > 3) {
            throw new IllegalArgumentException("Kindergarten lesson supports at most three activities");
        }
        int totalMinutes = lesson.activities().stream().mapToInt(ActivityContent::expectedMinutes).sum();
        if (totalMinutes > 15) throw new IllegalArgumentException("Kindergarten lesson must not exceed 15 minutes");
        int screenMinutes = lesson.activities().stream().filter(activity -> !activity.type().parentConfirmed())
            .mapToInt(ActivityContent::expectedMinutes).sum();
        if (screenMinutes > 8) throw new IllegalArgumentException("Kindergarten screen activities must not exceed 8 minutes");
        if (lesson.activities().stream().anyMatch(activity -> activity.expectedMinutes() > 8)) {
            throw new IllegalArgumentException("Kindergarten activity must not exceed 8 minutes");
        }
        if (lesson.activities().stream().anyMatch(activity -> activity.options().size() > 2)) {
            throw new IllegalArgumentException("Kindergarten activity supports at most two choices");
        }
        if (lesson.activities().stream().noneMatch(activity -> activity.type().parentConfirmed())) {
            throw new IllegalArgumentException("Kindergarten lesson requires a parent-child or offline activity");
        }
    }

    private static String text(String value, String field, int max) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        String normalized = value.trim();
        if (normalized.length() > max) throw new IllegalArgumentException(field + " is too long");
        return normalized;
    }

    private static String normalize(String value, int max) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > max) throw new IllegalArgumentException("Text is too long");
        return normalized;
    }
}
