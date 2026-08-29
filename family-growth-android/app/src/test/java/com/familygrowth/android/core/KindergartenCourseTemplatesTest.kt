package com.familygrowth.android.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KindergartenCourseTemplatesTest {
    @Test
    fun `pack covers every age band and domain exactly once`() {
        val templates = KindergartenCourseTemplates.all
        assertEquals(10, templates.size)
        assertEquals(10, templates.map { it.id }.toSet().size)
        KindergartenAgeBand.entries.forEach { ageBand ->
            KindergartenDomain.entries.forEach { domain ->
                assertEquals(1, templates.count { it.ageBand == ageBand && it.domain == domain })
            }
        }
    }

    @Test
    fun `pack is short original and offline`() {
        KindergartenCourseTemplates.all.forEach { template ->
            assertTrue(template.expectedMinutes in 3..8)
            assertTrue(template.activityType in setOf("PARENT_CHILD_READING", "ORAL_RESPONSE", "OFFLINE_PRACTICE"))
            assertTrue(template.adultGuide.isNotBlank())
            assertTrue(template.childAction.isNotBlank())
            assertTrue(template.rightsBasis.contains("KG-PACK-1.0.0"))
            listOf(template.lessonSummary, template.adultGuide, template.childAction).forEach { text ->
                assertFalse(text.contains("http://") || text.contains("https://"))
            }
        }
    }
}
