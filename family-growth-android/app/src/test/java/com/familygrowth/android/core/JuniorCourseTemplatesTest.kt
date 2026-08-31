package com.familygrowth.android.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JuniorCourseTemplatesTest {
    @Test fun packCoversEverySubjectWithSafeBoundedOriginalActivities() {
        assertEquals(JuniorSubject.entries.toSet(),JuniorCourseTemplates.all.map{it.subject}.toSet())
        assertEquals(JuniorCourseTemplates.all.size,JuniorCourseTemplates.all.map{it.id}.distinct().size)
        JuniorCourseTemplates.all.forEach { item ->
            assertTrue(item.expectedMinutes in 1..25)
            assertTrue(item.knowledgePoints.isNotEmpty())
            assertTrue(item.rightsBasis.contains("JUNIOR-PACK-1.0.0"))
            assertTrue(listOf("明火","高压","强酸","强碱","药品配制","锋利刀具").none {
                (item.instruction+item.safetyNote).contains(it)
            })
        }
    }
}
