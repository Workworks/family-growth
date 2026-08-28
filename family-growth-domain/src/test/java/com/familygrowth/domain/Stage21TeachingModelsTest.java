package com.familygrowth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.familygrowth.domain.Stage21TeachingModels.ActivityDraft;
import com.familygrowth.domain.Stage21TeachingModels.ActivityType;
import com.familygrowth.domain.Stage21TeachingModels.EvidenceType;
import com.familygrowth.domain.Stage21TeachingModels.QuestionOption;
import java.util.List;
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
}
