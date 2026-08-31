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
import com.familygrowth.domain.Stage21TeachingModels.JuniorLessonMetadata;
import com.familygrowth.domain.Stage21TeachingModels.SeniorLessonMetadata;
import com.familygrowth.domain.Stage21TeachingModels.LessonContent;
import com.familygrowth.domain.Stage21TeachingModels.QuestionOption;
import com.familygrowth.domain.Stage21TeachingModels.UnitContent;
import com.familygrowth.domain.Stage20Models.SchoolStage;
import com.familygrowth.domain.Stage25SeniorModels.ModuleType;
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

    @Test
    void juniorPublicationRequiresKnowledgeMetadataAndSafeShortBlocks() {
        JuniorLessonMetadata metadata = new JuniorLessonMetadata("一次函数", List.of("变量关系", "图像证据"),
            "用图像证据解释变量变化", "只使用纸笔和直尺");
        CourseVersion valid = junior(List.of(content(ActivityType.OFFLINE_PRACTICE, 20, List.of())), metadata);
        Stage21TeachingModels.validateForPublish(valid);
        assertThatThrownBy(() -> Stage21TeachingModels.validateForPublish(junior(
            List.of(content(ActivityType.OFFLINE_PRACTICE, 26, List.of())), metadata))).hasMessageContaining("25 minutes");
        assertThatThrownBy(() -> Stage21TeachingModels.validateForPublish(junior(
            List.of(content(ActivityType.OFFLINE_PRACTICE, 10, List.of())), null))).hasMessageContaining("metadata");
        assertThatThrownBy(() -> new JuniorLessonMetadata("实验", List.of("观察"), "观察变化", "使用明火加热"))
            .hasMessageContaining("high-risk");
        assertThatThrownBy(() -> new JuniorLessonMetadata("实验", List.of("观察\n解释"), "观察变化", "只用纸笔"))
            .hasMessageContaining("single-line");
        ActivityContent unsafeActivity = new ActivityContent(UUID.randomUUID(), ActivityType.OFFLINE_PRACTICE,
            "加热观察", "点燃明火后记录变化", "", 10, "", "", List.of(), "");
        assertThatThrownBy(() -> Stage21TeachingModels.validateForPublish(junior(List.of(unsafeActivity), metadata)))
            .hasMessageContaining("high-risk");
    }

    @Test
    void seniorPublicationRequiresModuleInquiryEvidenceAndSafeBoundedBlocks() {
        SeniorLessonMetadata metadata = new SeniorLessonMetadata(ModuleType.REQUIRED, "变化率专题", "怎样解释变化快慢",
            "数据表、图像和文字解释", "只使用纸笔或安全日常测量");
        Stage21TeachingModels.validateForPublish(senior(List.of(content(ActivityType.OFFLINE_PRACTICE, 40, List.of())), metadata));
        assertThatThrownBy(() -> Stage21TeachingModels.validateForPublish(senior(
            List.of(content(ActivityType.OFFLINE_PRACTICE, 46, List.of())), metadata))).hasMessageContaining("45 minutes");
        assertThatThrownBy(() -> Stage21TeachingModels.validateForPublish(senior(
            List.of(content(ActivityType.OFFLINE_PRACTICE, 20, List.of())), null))).hasMessageContaining("Senior lesson metadata");
        ActivityContent unsafe = new ActivityContent(UUID.randomUUID(), ActivityType.OFFLINE_PRACTICE,
            "观察", "使用高压设备记录", "", 20, "", "", List.of(), "");
        assertThatThrownBy(() -> Stage21TeachingModels.validateForPublish(senior(List.of(unsafe), metadata)))
            .hasMessageContaining("high-risk");
    }

    private static CourseVersion junior(List<ActivityContent> activities, JuniorLessonMetadata metadata) {
        return new CourseVersion(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),SchoolStage.JUNIOR_MIDDLE,
            "MATH","变量实验台",1,"用证据解释","家庭原创",null,List.of(),CourseVersionStatus.DRAFT,
            List.of(new UnitContent(UUID.randomUUID(),"函数",List.of(new LessonContent(UUID.randomUUID(),"一次函数","观察关系",activities,metadata)))),null);
    }

    private static CourseVersion senior(List<ActivityContent> activities, SeniorLessonMetadata metadata) {
        return new CourseVersion(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), SchoolStage.SENIOR_HIGH,
            "MATH", "研究工作室", 1, "专题证据", "家庭原创", null, List.of(), CourseVersionStatus.DRAFT,
            List.of(new UnitContent(UUID.randomUUID(), "函数", List.of(new LessonContent(UUID.randomUUID(),
                "变化率", "用数据解释", activities, null, metadata)))), null);
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
