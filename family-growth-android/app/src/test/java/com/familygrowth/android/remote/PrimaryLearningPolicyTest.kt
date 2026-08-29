package com.familygrowth.android.remote

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrimaryLearningPolicyTest {
    private val today = LocalDate.of(2026, 8, 29)

    @Test fun `age bands are lower through eight and upper from nine`() {
        assertEquals(PrimaryLearningBand.LOWER_PRIMARY, PrimaryLearningPolicy.bandFor("2020-08-29", today))
        assertEquals(PrimaryLearningBand.LOWER_PRIMARY, PrimaryLearningPolicy.bandFor("2018-08-29", today))
        assertEquals(PrimaryLearningBand.UPPER_PRIMARY, PrimaryLearningPolicy.bandFor("2017-08-29", today))
        assertEquals(PrimaryLearningBand.UPPER_PRIMARY, PrimaryLearningPolicy.bandFor("2015-08-29", today))
    }

    @Test fun `invalid date fails safe to lower density view`() {
        assertEquals(PrimaryLearningBand.LOWER_PRIMARY, PrimaryLearningPolicy.bandFor("not-a-date", today))
        assertEquals(PrimaryLearningBand.LOWER_PRIMARY, PrimaryLearningPolicy.bandFor("2030-01-01", today))
    }

    @Test fun `facts come from assignment evidence and correction state`() {
        val done = activity("done", setOf("CHECKED"), true)
        val wrong = activity("wrong", setOf("CHECKED"), false)
        val waiting = activity("waiting", emptySet(), null)
        val facts = PrimaryLearningPolicy.facts(assignment("IN_PROGRESS", listOf(done, wrong, waiting)))
        assertEquals(1, facts.completed)
        assertEquals(1, facts.inProgress)
        assertEquals(1, facts.toDo)
        assertEquals(1, facts.rework)
    }

    @Test fun `help text guides pause without claiming completion`() {
        val text = PrimaryLearningPolicy.helpText(activity("a", emptySet(), null).copy(hint="先画一条线"))
        assertTrue(text.contains("先停一下"))
        assertTrue(text.contains("请家长"))
    }

    private fun activity(id:String,evidence:Set<String>,correct:Boolean?) = RemoteLearningActivity(
        id,"SINGLE_CHOICE","想一想","先读题","",5,"选一个","",listOf(
            RemoteQuestionOption("a","A"),RemoteQuestionOption("b","B")),"CHECKED",evidence,correct)

    private fun assignment(status:String,activities:List<RemoteLearningActivity>) = RemoteLearningAssignment(
        "assignment","探索课","第一单元","找规律","先猜再验证","PRIMARY","MATH",status,1,activities,"")
}
