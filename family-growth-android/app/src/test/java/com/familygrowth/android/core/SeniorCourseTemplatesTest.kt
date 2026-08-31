package com.familygrowth.android.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeniorCourseTemplatesTest {
    @Test fun packCoversEveryModuleWithBoundedOriginalEvidenceActivities() {
        assertEquals(SeniorSubject.entries.toSet(),SeniorCourseTemplates.all.map{it.subject}.toSet())
        assertEquals(SeniorCourseTemplates.all.size,SeniorCourseTemplates.all.map{it.id}.distinct().size)
        assertTrue(SeniorCourseTemplates.all.map{it.subject.defaultModule}.toSet().containsAll(setOf("REQUIRED","SELECTIVE_REQUIRED","ELECTIVE")))
        SeniorCourseTemplates.all.forEach { item ->
            assertTrue(item.expectedMinutes in 1..45)
            assertTrue(item.inquiryQuestion.endsWith("？")||item.inquiryQuestion.endsWith("?"))
            assertTrue(item.expectedEvidence.isNotBlank())
            assertTrue(item.rightsBasis.contains("SENIOR-PACK-1.0.0"))
            assertTrue(listOf("排名","倒计时","能力预测","保分","提分承诺").none{(item.instruction+item.expectedEvidence).contains(it)})
        }
    }
}
