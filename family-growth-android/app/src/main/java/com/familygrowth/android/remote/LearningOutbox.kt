package com.familygrowth.android.remote

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.KeyStore
import java.time.Instant
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

enum class LearningActionType { ATTEMPT, SUBMIT, REVIEW }
enum class LearningActionState { PENDING, NEEDS_REVIEW }

data class PendingLearningAction(
    val idempotencyKey: String = UUID.randomUUID().toString(),
    val familyId: String,
    val childId: String,
    val type: LearningActionType,
    val assignmentId: String,
    val activityId: String = "",
    val responseText: String = "",
    val playedSeconds: Int? = null,
    val durationSeconds: Int? = null,
    val expectedVersion: Long? = null,
    val decision: String = "",
    val note: String = "",
    val createdAtEpochMillis: Long = Instant.now().toEpochMilli(),
    val state: LearningActionState = LearningActionState.PENDING,
    val attempts: Int = 0,
    val lastError: String = "",
)

data class LearningReconcileResult(val ready: Int, val achieved: Int, val unresolved: Int)

interface LearningOutboxStore {
    fun load(): Result<List<PendingLearningAction>>
    fun save(actions: List<PendingLearningAction>): Result<Unit>
}

class LearningOutbox(private val store: LearningOutboxStore, private val limit: Int = 100) {
    private var actions: List<PendingLearningAction>
    val initializationError: String?
    private val writable: Boolean

    init {
        val loaded = store.load()
        actions = loaded.getOrDefault(emptyList()).sortedBy(PendingLearningAction::createdAtEpochMillis)
        initializationError = loaded.exceptionOrNull()?.message
        writable = initializationError == null
    }

    @Synchronized fun snapshot(): List<PendingLearningAction> = actions.toList()

    @Synchronized fun enqueue(action: PendingLearningAction): Result<PendingLearningAction> {
        if (!writable) return Result.failure(IllegalStateException("加密学习队列无法读取，请由家长检查应用数据"))
        actions.firstOrNull { it.state == LearningActionState.PENDING && it.samePayload(action) }?.let {
            return Result.success(it)
        }
        if (actions.size >= limit) return Result.failure(IllegalStateException("待同步学习记录已达 $limit 项，请家长先处理"))
        return persist(actions + action).map { action }
    }

    @Synchronized fun remove(idempotencyKey: String): Result<Unit> =
        if (!writable) Result.failure(IllegalStateException("加密学习队列不可用")) else persist(actions.filterNot { it.idempotencyKey == idempotencyKey })

    @Synchronized fun markFailure(idempotencyKey: String, message: String, needsReview: Boolean): Result<Unit> =
        if (!writable) Result.failure(IllegalStateException("加密学习队列不可用")) else persist(actions.map { action ->
            if (action.idempotencyKey != idempotencyKey) action else action.copy(
                state = if (needsReview) LearningActionState.NEEDS_REVIEW else LearningActionState.PENDING,
                attempts = action.attempts + 1,
                lastError = message.take(240),
            )
        })

    @Synchronized fun reconcile(assignments: List<RemoteLearningAssignment>): Result<LearningReconcileResult> {
        if (!writable) return Result.failure(IllegalStateException("加密学习队列不可用"))
        val facts = assignments.associateBy(RemoteLearningAssignment::id)
        var achieved = 0
        var ready = 0
        val next = buildList {
            actions.forEach { action ->
                if (action.state != LearningActionState.NEEDS_REVIEW) {
                    add(action)
                    return@forEach
                }
                val assignment = facts[action.assignmentId]
                val resolved = assignment?.let { reconcileAction(action, it) }
                when {
                    resolved == null -> add(action)
                    resolved.first -> achieved += 1
                    else -> { ready += 1; add(resolved.second) }
                }
            }
        }
        return persist(next).map { LearningReconcileResult(ready, achieved, next.count { it.state == LearningActionState.NEEDS_REVIEW }) }
    }

    private fun reconcileAction(action: PendingLearningAction, assignment: RemoteLearningAssignment): Pair<Boolean, PendingLearningAction>? = when (action.type) {
        LearningActionType.ATTEMPT -> {
            val progress = assignment.activities.firstOrNull { it.id == action.activityId }
            when {
                assignment.status in setOf("ASSIGNED", "IN_PROGRESS", "REWORK_REQUIRED") -> false to action.copy(state = LearningActionState.PENDING, lastError = "")
                progress?.childReady() == true -> true to action
                else -> null
            }
        }
        LearningActionType.SUBMIT -> when {
            assignment.status in setOf("SUBMITTED", "COMPLETED") -> true to action
            assignment.status == "IN_PROGRESS" && assignment.canSubmit() -> false to action.copy(
                expectedVersion = assignment.version, state = LearningActionState.PENDING, lastError = "")
            else -> null
        }
        LearningActionType.REVIEW -> when {
            action.decision == "APPROVE" && assignment.status == "COMPLETED" -> true to action
            action.decision == "REWORK" && assignment.status in setOf("REWORK_REQUIRED", "IN_PROGRESS") -> true to action
            assignment.status == "SUBMITTED" -> false to action.copy(
                expectedVersion = assignment.version, state = LearningActionState.PENDING, lastError = "")
            else -> null
        }
    }

    private fun persist(next: List<PendingLearningAction>): Result<Unit> = store.save(next).onSuccess {
        actions = next.sortedBy(PendingLearningAction::createdAtEpochMillis)
    }

    private fun PendingLearningAction.samePayload(other: PendingLearningAction): Boolean =
        familyId == other.familyId && childId == other.childId && type == other.type && assignmentId == other.assignmentId &&
            activityId == other.activityId && responseText == other.responseText && playedSeconds == other.playedSeconds &&
            durationSeconds == other.durationSeconds && expectedVersion == other.expectedVersion && decision == other.decision && note == other.note
}

object LearningOutboxCodec {
    private const val VERSION = 1

    fun encode(actions: List<PendingLearningAction>): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { out ->
            out.writeInt(VERSION)
            out.writeInt(actions.size)
            actions.forEach { action ->
                out.writeUTF(action.idempotencyKey); out.writeUTF(action.familyId); out.writeUTF(action.childId)
                out.writeUTF(action.type.name); out.writeUTF(action.assignmentId); out.writeUTF(action.activityId)
                out.writeUTF(action.responseText); out.writeNullableInt(action.playedSeconds); out.writeNullableInt(action.durationSeconds)
                out.writeNullableLong(action.expectedVersion); out.writeUTF(action.decision); out.writeUTF(action.note)
                out.writeLong(action.createdAtEpochMillis); out.writeUTF(action.state.name); out.writeInt(action.attempts); out.writeUTF(action.lastError)
            }
        }
        bytes.toByteArray()
    }

    fun decode(bytes: ByteArray): List<PendingLearningAction> = DataInputStream(ByteArrayInputStream(bytes)).use { input ->
        require(input.readInt() == VERSION) { "不支持的学习队列版本" }
        val count = input.readInt()
        require(count in 0..100) { "学习队列数量无效" }
        List(count) {
            PendingLearningAction(
                idempotencyKey = input.readUTF(), familyId = input.readUTF(), childId = input.readUTF(),
                type = LearningActionType.valueOf(input.readUTF()), assignmentId = input.readUTF(), activityId = input.readUTF(),
                responseText = input.readUTF(), playedSeconds = input.readNullableInt(), durationSeconds = input.readNullableInt(),
                expectedVersion = input.readNullableLong(), decision = input.readUTF(), note = input.readUTF(),
                createdAtEpochMillis = input.readLong(), state = LearningActionState.valueOf(input.readUTF()),
                attempts = input.readInt(), lastError = input.readUTF(),
            )
        }.also { require(input.available() == 0) { "学习队列包含多余数据" } }
    }

    private fun DataOutputStream.writeNullableInt(value: Int?) { writeBoolean(value != null); if (value != null) writeInt(value) }
    private fun DataOutputStream.writeNullableLong(value: Long?) { writeBoolean(value != null); if (value != null) writeLong(value) }
    private fun DataInputStream.readNullableInt(): Int? = if (readBoolean()) readInt() else null
    private fun DataInputStream.readNullableLong(): Long? = if (readBoolean()) readLong() else null
}

class EncryptedLearningOutboxStore(context: Context) : LearningOutboxStore {
    private val preferences = context.getSharedPreferences("family_growth_learning_outbox_v1", Context.MODE_PRIVATE)

    override fun load(): Result<List<PendingLearningAction>> = runCatching {
        val encoded = preferences.getString(DATA_KEY, null) ?: return@runCatching emptyList()
        LearningOutboxCodec.decode(decrypt(Base64.getDecoder().decode(encoded)))
    }

    override fun save(actions: List<PendingLearningAction>): Result<Unit> = runCatching {
        val editor = preferences.edit()
        if (actions.isEmpty()) editor.remove(DATA_KEY)
        else editor.putString(DATA_KEY, Base64.getEncoder().encodeToString(encrypt(LearningOutboxCodec.encode(actions))))
        check(editor.commit()) { "学习队列写入失败" }
    }

    private fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(plain)
        return ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { out -> out.writeInt(cipher.iv.size); out.write(cipher.iv); out.write(encrypted) }
            bytes.toByteArray()
        }
    }

    private fun decrypt(blob: ByteArray): ByteArray = DataInputStream(ByteArrayInputStream(blob)).use { input ->
        val ivSize = input.readInt()
        require(ivSize in 12..32) { "学习队列密文无效" }
        val iv = ByteArray(ivSize).also(input::readFully)
        val encrypted = ByteArray(input.available()).also(input::readFully)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        cipher.doFinal(encrypted)
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build())
        return generator.generateKey()
    }

    private companion object {
        const val DATA_KEY = "encrypted_actions"
        const val KEY_ALIAS = "family_growth_learning_outbox_key_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
