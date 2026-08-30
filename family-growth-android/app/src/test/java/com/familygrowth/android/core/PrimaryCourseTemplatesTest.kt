package com.familygrowth.android.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrimaryCourseTemplatesTest {
    @Test fun packCoversTwoBandsAndFourSubjectsWithAuditableRealityExit() {
        assertEquals(8, PrimaryCourseTemplates.all.size)
        PrimaryGradeBand.entries.forEach { band ->
            PrimarySubject.entries.forEach { subject ->
                val item = PrimaryCourseTemplates.find(band, subject)
                assertTrue(item.id.isNotBlank())
                assertTrue(item.goal.isNotBlank())
                assertTrue(item.expectedMinutes in 1..15)
                assertTrue(item.realityExit.isNotBlank())
                assertTrue(item.rightsBasis.contains("PRIMARY-PACK-1.0.0"))
                assertTrue(item.activityType in setOf("PARENT_CHILD_READING", "ORAL_RESPONSE", "OFFLINE_PRACTICE"))
            }
        }
        assertEquals(8, PrimaryCourseTemplates.all.map { it.id }.distinct().size)
    }
}
