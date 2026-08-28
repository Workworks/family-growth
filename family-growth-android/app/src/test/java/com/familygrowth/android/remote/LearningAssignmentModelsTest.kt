package com.familygrowth.android.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningAssignmentModelsTest {
    @Test fun childReadinessSeparatesViewedCheckedAndParentConfirmation() {
        assertTrue(activity("SHORT_VIDEO", "VIEWED", setOf("VIEWED"), null).childReady())
        assertFalse(activity("SINGLE_CHOICE", "CHECKED", setOf("CHECKED"), false).childReady())
        assertTrue(activity("SINGLE_CHOICE", "CHECKED", setOf("CHECKED"), true).childReady())
        assertTrue(activity("OFFLINE_PRACTICE", "PARENT_CONFIRMED", setOf("ATTEMPTED"), null).childReady())
        assertFalse(activity("OFFLINE_PRACTICE", "PARENT_CONFIRMED", emptySet(), null).childReady())
    }

    @Test fun assignmentCanSubmitOnlyFromRecoverableStatesWithEveryActivityReady() {
        val ready = activity("SINGLE_CHOICE", "CHECKED", setOf("CHECKED"), true)
        val pending = activity("SHORT_VIDEO", "VIEWED", emptySet(), null)
        assertTrue(assignment("IN_PROGRESS", listOf(ready)).canSubmit())
        assertFalse(assignment("REWORK_REQUIRED", listOf(ready)).canSubmit())
        assertFalse(assignment("SUBMITTED", listOf(ready)).canSubmit())
        assertFalse(assignment("IN_PROGRESS", listOf(ready, pending)).canSubmit())
    }

    @Test fun kindergartenProjectionRejectsLongOrChoiceHeavyActivities() {
        assertNull(KindergartenActivityPolicy.renderIssue(activity("OFFLINE_PRACTICE", "PARENT_CONFIRMED", emptySet(), null)))
        val long = activity("OFFLINE_PRACTICE", "PARENT_CONFIRMED", emptySet(), null).copy(expectedMinutes = 9)
        assertEquals("这一步有点长，请家长换成短一点的活动。", KindergartenActivityPolicy.renderIssue(long))
        val crowded = activity("SINGLE_CHOICE", "CHECKED", emptySet(), null).copy(options = listOf(
            RemoteQuestionOption("a", "A"), RemoteQuestionOption("b", "B"), RemoteQuestionOption("c", "C"),
        ))
        assertEquals("这里的选择太多了，请家长帮忙。", KindergartenActivityPolicy.renderIssue(crowded))
    }

    private fun activity(type:String, required:String, evidence:Set<String>, correct:Boolean?) =
        RemoteLearningActivity("activity", type, "一步", "慢慢做", "", 3, "", "", emptyList(), required, evidence, correct)
    private fun assignment(status:String, activities:List<RemoteLearningActivity>) =
        RemoteLearningAssignment("assignment", "课程", "单元", "课节", "说明", "PRIMARY", "SCIENCE", status, 1, activities, "")
}
