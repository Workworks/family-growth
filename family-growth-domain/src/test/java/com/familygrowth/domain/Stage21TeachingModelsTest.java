package com.familygrowth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.familygrowth.domain.Stage21TeachingModels.ActivityDraft;
import com.familygrowth.domain.Stage21TeachingModels.ActivityType;
import com.familygrowth.domain.Stage21TeachingModels.EvidenceType;
import com.familygrowth.domain.Stage21TeachingModels.ActivityContent;
import com.familygrowth.domain.Stage21TeachingModels.CourseVersion;
import com.familygrowth.domain.Stage21TeachingModels.CourseVersionStatus;
import com.familygrowth.domain.Stage21TeachingModels.KindergartenAgeBand;
import com.familygrowth.domain.Stage21TeachingModels.KindergartenDomain;
import com.familygrowth.domain.Stage21TeachingModels.LessonContent;
import com.familygrowth.domain.Stage21TeachingModels.QuestionOption;
import com.familygrowth.domain.Stage21TeachingModels.UnitContent;
import com.familygrowth.domain.Stage20Models.SchoolStage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class Stage21TeachingModelsTest {
    @Test
    void nineActivitiesHaveExplicitNonMasteryCompletionRules() {
        assertThat(ActivityType.values()).hasSize(9);
        assertThat(ActivityType.SHORT_VIDEO.requiredEvidence()).isEqualTo(EvidenceType.VIEWED);
        assertThat(List.of(ActivityType.LISTEN_CHOOSE, ActivityType.SINGLE_CHOICE,
            ActivityType.MATCHING, ActivityType.SORTING)).allMatch(ActivityType::objective);
        assertThat(List.of(ActivityType.PARENT_CHILD_READING, ActivityType.ORAL_RESPONSE,
            ActivityType.OFFLINE_PRACTICE, ActivityType.PARENT_CONFIRMATION))
            .allMatch(ActivityType::parentConfirmed);
        assertThat(List.of(ActivityType.values())).noneMatch(type -> type.requiredEvidence() == EvidenceType.MASTERED);
    }

    @Test
    void objectiveAnswersStayRequiredAndNonObjectiveActivitiesCannotSmuggleKeys() {
        assertThatThrownBy(() -> new ActivityDraft(ActivityType.SINGLE_CHOICE, "选一选", "选出正确答案", "", 3,
            "天空是什么颜色？", "", List.of(new QuestionOption("blue", "蓝色")), "blue"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ActivityDraft(ActivityType.SHORT_VIDEO, "看一看", "和家长一起看", "lesson_color_garden", 3,
            "", "", List.of(), "secret"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void kindergartenPublicationRequiresAgeDomainShortLessonsAndOfflineParentSupport() {
        CourseVersion valid = kindergarten(List.of(
            content(ActivityType.SHORT_VIDEO, 3, List.of()),
            content(ActivityType.LISTEN_CHOOSE, 2, List.of(new QuestionOption("a", "雨声"), new QuestionOption("b", "鸟声"))),
            content(ActivityType.OFFLINE_PRACTICE, 5, List.of())
        ));
        Stage21TeachingModels.validateForPublish(valid);

        assertThatThrownBy(() -> Stage21TeachingModels.validateForPublish(validWithoutBand(valid)))
            .hasMessageContaining("age band");
        assertThatThrownBy(() -> Stage21TeachingModels.validateForPublish(validWithoutDomains(valid)))
            .hasMessageContaining("learning domain");
        assertThatThrownBy(() -> Stage21TeachingModels.validateForPublish(kindergarten(List.of(
            content(ActivityType.SHORT_VIDEO, 5, List.of()), content(ActivityType.LISTEN_CHOOSE, 4, twoOptions()),
            content(ActivityType.OFFLINE_PRACTICE, 5, List.of())))))
            .hasMessageContaining("screen activities");
        assertThatThrownBy(() -> Stage21TeachingModels.validateForPublish(kindergarten(List.of(
            content(ActivityType.LISTEN_CHOOSE, 2, List.of(new QuestionOption("a", "一"), new QuestionOption("b", "二"), new QuestionOption("c", "三"))),
            content(ActivityType.OFFLINE_PRACTICE, 4, List.of())))))
            .hasMessageContaining("two choices");
        assertThatThrownBy(() -> Stage21TeachingModels.validateForPublish(kindergarten(List.of(
            content(ActivityType.SHORT_VIDEO, 3, List.of()), content(ActivityType.LISTEN_CHOOSE, 2, twoOptions())))))
            .hasMessageContaining("parent-child or offline");
    }

    private static CourseVersion kindergarten(List<ActivityContent> activities) {
        return new CourseVersion(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), SchoolStage.KINDERGARTEN,
            "SCIENCE", "自然小发现", 1, "一起看看再去做", "家庭原创",
            KindergartenAgeBand.SHARED_3_4, List.of(KindergartenDomain.SCIENCE, KindergartenDomain.LANGUAGE),
            CourseVersionStatus.DRAFT,
            List.of(new UnitContent(UUID.randomUUID(), "自然", List.of(new LessonContent(UUID.randomUUID(), "听雨", "听听再找找", activities)))),
            null);
    }

    private static ActivityContent content(ActivityType type, int minutes, List<QuestionOption> options) {
        return new ActivityContent(UUID.randomUUID(), type, "一步", "慢慢做", type == ActivityType.SHORT_VIDEO ? "lesson_color_garden" : "",
            minutes, type.objective() ? "选一选" : "", "", options, type.objective() ? options.get(0).value() : "");
    }

    private static List<QuestionOption> twoOptions() {
        return List.of(new QuestionOption("a", "一"), new QuestionOption("b", "二"));
    }

    private static CourseVersion validWithoutBand(CourseVersion value) {
        return new CourseVersion(value.courseId(), value.versionId(), value.familyId(), value.schoolStage(), value.subjectCode(), value.title(),
            value.versionNumber(), value.summary(), value.rightsBasis(), null, value.kindergartenDomains(), value.status(), value.units(), value.publishedAt());
    }

    private static CourseVersion validWithoutDomains(CourseVersion value) {
        return new CourseVersion(value.courseId(), value.versionId(), value.familyId(), value.schoolStage(), value.subjectCode(), value.title(),
            value.versionNumber(), value.summary(), value.rightsBasis(), value.kindergartenAgeBand(), List.of(), value.status(), value.units(), value.publishedAt());
    }
}
