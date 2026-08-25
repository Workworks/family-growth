package com.familygrowth.android.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
