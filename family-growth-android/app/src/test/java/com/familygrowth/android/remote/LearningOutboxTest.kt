package com.familygrowth.android.remote

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningOutboxTest {
    @Test fun helpKeepsStableEncryptedQueuePayloadAndNeverNeedsCompletionEvidence() {
        val store = MemoryOutboxStore()
        val queue = LearningOutbox(store)
        val help = PendingLearningAction(idempotencyKey="help-key", familyId="family", childId="child",
            type=LearningActionType.HELP, assignmentId="assignment", activityId="activity", note="这里我没看懂")
        queue.enqueue(help).getOrThrow()
        val restored = LearningOutboxCodec.decode(LearningOutboxCodec.encode(queue.snapshot())).single()
        assertEquals(LearningActionType.HELP, restored.type)
        assertEquals("这里我没看懂", restored.note)
        assertNull(restored.expectedVersion)
    }
    @Test fun codecRoundTripKeepsMinimalActionAndIdempotencyKey() {
        val action = attempt("key-1", 10).copy(playedSeconds = 9, durationSeconds = 10, responseText = "VIEWED")
        val encoded = LearningOutboxCodec.encode(listOf(action))
        assertArrayEquals(encoded, LearningOutboxCodec.encode(LearningOutboxCodec.decode(encoded)))
        assertEquals(action, LearningOutboxCodec.decode(encoded).single())
    }

    @Test fun writeBeforeSendSurvivesQueueReconstructionAndDeduplicatesPayload() {
        val store = MemoryOutboxStore()
        val first = LearningOutbox(store)
        val action = attempt("stable-key", 1)
        assertTrue(first.enqueue(action).isSuccess)
        assertEquals("stable-key", LearningOutbox(store).snapshot().single().idempotencyKey)
        assertEquals("stable-key", LearningOutbox(store).enqueue(action.copy(idempotencyKey = "new-key")).getOrThrow().idempotencyKey)
        assertEquals(1, LearningOutbox(store).snapshot().size)
    }

    @Test fun queueLimitFailsClosedWithoutDroppingExistingActions() {
        val store = MemoryOutboxStore()
        val queue = LearningOutbox(store, limit = 2)
        queue.enqueue(attempt("one", 1)).getOrThrow()
        queue.enqueue(attempt("two", 2)).getOrThrow()
        assertTrue(queue.enqueue(attempt("three", 3)).isFailure)
        assertEquals(listOf("one", "two"), queue.snapshot().map(PendingLearningAction::idempotencyKey))
    }

    @Test fun conflictRefreshUpdatesVersionOrRecognizesAlreadyAchievedState() {
        val store = MemoryOutboxStore()
        val queue = LearningOutbox(store)
        val submit = PendingLearningAction(familyId="family", childId="child", type=LearningActionType.SUBMIT,
            assignmentId="a", expectedVersion=2, idempotencyKey="submit")
        queue.enqueue(submit).getOrThrow()
        queue.markFailure("submit", "版本冲突", true).getOrThrow()
        val ready = assignment("a", "IN_PROGRESS", version=7)
        val result = queue.reconcile(listOf(ready)).getOrThrow()
        assertEquals(1, result.ready)
        assertEquals(7L, queue.snapshot().single().expectedVersion)
        assertEquals(LearningActionState.PENDING, queue.snapshot().single().state)

        queue.markFailure("submit", "版本冲突", true).getOrThrow()
        val achieved = queue.reconcile(listOf(assignment("a", "SUBMITTED", version=8))).getOrThrow()
        assertEquals(1, achieved.achieved)
        assertTrue(queue.snapshot().isEmpty())
    }

    @Test fun retryableFailureStaysPendingWhileConflictNeedsParent() {
        val queue = LearningOutbox(MemoryOutboxStore())
        queue.enqueue(attempt("retry", 1)).getOrThrow()
        queue.markFailure("retry", "网络不可用", false).getOrThrow()
        assertEquals(LearningActionState.PENDING, queue.snapshot().single().state)
        queue.markFailure("retry", "状态冲突", true).getOrThrow()
        assertEquals(LearningActionState.NEEDS_REVIEW, queue.snapshot().single().state)
        assertFalse(queue.snapshot().single().lastError.isBlank())
    }

    @Test fun unreadableCiphertextDisablesWritesInsteadOfReplacingTheQueue() {
        val store = object : LearningOutboxStore {
            override fun load(): Result<List<PendingLearningAction>> = Result.failure(IllegalStateException("密文损坏"))
            override fun save(actions: List<PendingLearningAction>): Result<Unit> = error("不可覆盖损坏数据")
        }
        val queue = LearningOutbox(store)
        assertEquals("密文损坏", queue.initializationError)
        assertTrue(queue.enqueue(attempt("blocked", 1)).isFailure)
        assertTrue(queue.snapshot().isEmpty())
    }

    @Test fun failedDurableWriteDoesNotMutateTheInMemoryQueue() {
        val store = MemoryOutboxStore().apply { failWrites = true }
        val queue = LearningOutbox(store)
        assertTrue(queue.enqueue(attempt("not-saved", 1)).isFailure)
        assertTrue(queue.snapshot().isEmpty())
    }

    private fun attempt(key:String, order:Int) = PendingLearningAction(
        idempotencyKey=key, familyId="family", childId="child", type=LearningActionType.ATTEMPT,
        assignmentId="assignment-$order", activityId="activity-$order", responseText="完成", createdAtEpochMillis=order.toLong())

    private fun assignment(id:String,status:String,version:Long) = RemoteLearningAssignment(
        id,"课程","单元","课节","说明","PRIMARY","SCIENCE",status,version,
        listOf(RemoteLearningActivity("activity","OFFLINE_PRACTICE","去做","做一做","",3,"","",emptyList(),"PARENT_CONFIRMED",setOf("ATTEMPTED"),null)),"")

    private class MemoryOutboxStore : LearningOutboxStore {
        var encoded: ByteArray? = null
        var failWrites = false
        override fun load(): Result<List<PendingLearningAction>> = runCatching { encoded?.let(LearningOutboxCodec::decode) ?: emptyList() }
        override fun save(actions: List<PendingLearningAction>): Result<Unit> = runCatching {
            check(!failWrites) { "磁盘写入失败" }
            encoded = LearningOutboxCodec.encode(actions)
        }
    }
}
