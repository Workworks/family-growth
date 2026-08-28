package com.familygrowth.android.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ChildExperiencePolicyTest {
    @Test
    fun startsAtThreeAndLimitsChildNavigationToThreeCalmDestinations() {
        assertEquals(3, ChildExperiencePolicy.MINIMUM_AGE)
        assertEquals(
            listOf(AppSection.TODAY, AppSection.TASKS, AppSection.GROWTH),
            ChildExperiencePolicy.sectionsFor(AppMode.CHILD),
        )
        assertEquals(3, ChildExperiencePolicy.sectionsFor(AppMode.CHILD).size)
    }

    @Test
    fun advancedFinanceIsParentOnly() {
        assertFalse(ChildExperiencePolicy.allowsAdvancedFinance(AppMode.CHILD))
        assertTrue(ChildExperiencePolicy.allowsAdvancedFinance(AppMode.PARENT))
        assertEquals(AppSection.entries, ChildExperiencePolicy.sectionsFor(AppMode.PARENT))
    }

    @Test
    fun preschoolDefaultsFavorShortSessions() {
        val policy = UsagePolicy()
        assertEquals(20, policy.dailyLimitMinutes)
        assertEquals(10, policy.sessionLimitMinutes)
    }

    @Test
    fun ageBoundariesMapToFourSchoolStagesAndParentOnly() {
        val today = LocalDate.of(2026, 8, 26)
        assertEquals(SchoolStage.PARENT_ONLY, ChildExperiencePolicy.recommendedStage(LocalDate.of(2024, 8, 26), today))
        assertEquals(SchoolStage.KINDERGARTEN, ChildExperiencePolicy.recommendedStage(LocalDate.of(2023, 8, 26), today))
        assertEquals(SchoolStage.PRIMARY, ChildExperiencePolicy.recommendedStage(LocalDate.of(2020, 8, 26), today))
        assertEquals(SchoolStage.JUNIOR_MIDDLE, ChildExperiencePolicy.recommendedStage(LocalDate.of(2014, 8, 26), today))
        assertEquals(SchoolStage.SENIOR_HIGH, ChildExperiencePolicy.recommendedStage(LocalDate.of(2011, 8, 26), today))
    }

    @Test
    fun parentOverrideAndReducedMotionRemainSafe() {
        val settings = ChildExperiencePolicy.localSettings(
            LocalDate.of(2020, 8, 26), SchoolStage.JUNIOR_MIDDLE, "按实际入学阶段", true,
            LocalDate.of(2026, 8, 26),
        )
        assertEquals(SchoolStage.PRIMARY, settings.recommendedStage)
        assertEquals(SchoolStage.JUNIOR_MIDDLE, settings.effectiveStage)
        val normal = ChildExperiencePolicy.feedbackFor(settings)
        assertEquals(160, normal.maxAnimationMs)
        assertEquals(1, normal.hapticPulseCount)
        val reduced = ChildExperiencePolicy.feedbackFor(settings, systemReducedMotion = true)
        assertEquals(0, reduced.maxAnimationMs)
        assertEquals(1f, reduced.primaryPressScale)
    }
}
