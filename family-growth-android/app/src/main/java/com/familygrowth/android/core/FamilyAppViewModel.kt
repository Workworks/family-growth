package com.familygrowth.android.core

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.familygrowth.android.BuildConfig
import com.familygrowth.android.remote.*
import kotlinx.coroutines.launch
import org.mindrot.jbcrypt.BCrypt
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class FamilyAppViewModel(application: Application) : AndroidViewModel(application) {
    private val store = LocalFamilyStore(application)
    private val remote = RemoteFamilyRepository(HttpFamilyApiTransport(), MemorySessionStore(), BuildConfig.DEBUG)
    private val learningOutbox = LearningOutbox(EncryptedLearningOutboxStore(application))
    private var remoteCompletionByTask: Map<String, String> = emptyMap()
    private val pendingUsage = mutableListOf<PendingUsage>()
    private var usageFlushRunning = false
    private var learningFlushRunning = false
    private var teachingActionRunningInternal = false

    var state by mutableStateOf(FamilyLocalState())
        private set
    var mode by mutableStateOf(AppMode.CHILD)
        private set
    var section by mutableStateOf(AppSection.TODAY)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var hasParentPin by mutableStateOf(store.hasPin())
        private set
    var sessionUsedMinutes by mutableIntStateOf(0)
        private set
    var connectionState by mutableStateOf<ConnectionState>(ConnectionState.Disconnected)
        private set
    var educationSources by mutableStateOf<List<RemoteEducationSource>>(emptyList())
        private set
    var childEducationCatalog by mutableStateOf<List<RemoteChildEducationSource>>(emptyList())
        private set
    var learningAssignments by mutableStateOf<List<RemoteLearningAssignment>>(emptyList())
        private set
    var teachingCourses by mutableStateOf<List<RemoteCourseSummary>>(emptyList())
        private set
    var pendingLearningActions by mutableStateOf(learningOutbox.snapshot())
        private set
    var teachingActionRunning by mutableStateOf(false)
        private set

    val isChildLocked: Boolean get() = mode == AppMode.CHILD &&
        (state.usage.usedMinutes >= state.usage.dailyLimitMinutes || sessionUsedMinutes >= state.usage.sessionLimitMinutes)

    init {
        store.load().onSuccess { state = it }.onFailure { message = "本地数据读取失败，已进入空白安全状态" }
        learningOutbox.initializationError?.let { message = "加密学习记录无法读取，请由家长检查应用数据" }
    }

    fun selectSection(value: AppSection) {
        section = if (value in ChildExperiencePolicy.sectionsFor(mode)) value else AppSection.TODAY
    }
    fun enterChild() { mode = AppMode.CHILD; section = AppSection.TODAY; sessionUsedMinutes = 0 }

    fun setParentPin(pin: String, confirm: String): Boolean {
        if (!pin.matches(Regex("^\\d{4,8}$"))) return fail("PIN 需为 4–8 位数字")
        if (pin != confirm) return fail("两次 PIN 不一致")
        val saved = store.savePin(BCrypt.hashpw(pin, BCrypt.gensalt(10)))
        if (!saved) return fail("PIN 保存失败")
        hasParentPin = true
        mode = AppMode.PARENT
        section = AppSection.TODAY
        message = "家长 PIN 已设置，仅保护本机基础版"
        return true
    }

    fun verifyParentPin(pin: String): Boolean {
        val hash = store.pinHash() ?: return fail("请先设置家长 PIN")
        val valid = runCatching { BCrypt.checkpw(pin, hash) }.getOrDefault(false)
        if (!valid) return fail("PIN 不正确")
        mode = AppMode.PARENT
        section = AppSection.TODAY
        message = "已进入家长模式"
        return true
    }

    fun recordUsageMinute() {
        if (mode != AppMode.CHILD || isChildLocked) return
        sessionUsedMinutes += 1
        mutate { LocalFamilyEngine.recordUsageMinute(it) }
        if (remote.hasSession()) {
            pendingUsage += PendingUsage(UUID.randomUUID().toString(), Instant.now())
            flushUsage()
        }
    }

    fun addTask(title: String, minutes: Int, rewardMoney: BigDecimal, coin: Int, xp: Int) =
        mutate { LocalFamilyEngine.addTask(it, title, minutes, rewardMoney, coin, xp) }
    fun submitTask(id: String) {
        if (!remote.hasSession()) return mutate("任务已提交，等待家长审核") { LocalFamilyEngine.submitTask(it, id) }
        viewModelScope.launch { handleRemote(remote.submitTask(id), "任务已提交，等待家长回应") }
    }
    fun approveTask(id: String) {
        val completion = remoteCompletionByTask[id]
        if (!remote.hasSession() || completion == null) return mutate("审核通过，奖励已进入钱包流水") { LocalFamilyEngine.approveTask(it, id) }
        viewModelScope.launch { handleRemote(remote.approveTask(completion), "家长已确认，稳定奖励已进入服务端账本") }
    }
    fun depositGift(amount: BigDecimal) = mutate("压岁钱已按 1:1 存入") { LocalFamilyEngine.depositGiftMoney(it, amount) }
    fun exchange(amount: BigDecimal) = mutate("兑换完成") { LocalFamilyEngine.exchangeMoneyToCoin(it, amount) }
    fun withdraw(amount: BigDecimal) = mutate("零钱回收申请已提交，家长确认后才会扣款") { LocalFamilyEngine.requestWithdrawal(it, amount) }
    fun approveWithdrawal(id: String) = mutate("申请已确认，扣款和手续费已进入流水") { LocalFamilyEngine.approveWithdrawal(it, id) }
    fun addReward(title: String, price: Int) = mutate { LocalFamilyEngine.addReward(it, title, price) }
    fun redeemReward(id: String) = mutate("奖励兑换已记录") { LocalFamilyEngine.redeemReward(it, id) }
    fun toggleRewardInterest(id: String) {
        val selecting = id !in state.rewardInterestIds
        mutate(if (selecting) "已经记下你想要它，可以和家长说一说" else "已经先不选这个奖励") {
            LocalFamilyEngine.toggleRewardInterest(it, id)
        }
    }
    fun recordLearningSecond(videoId: String) {
        val wasCompleted = state.learningProgress.any { it.videoId == videoId && it.completed }
        mutate { LocalFamilyEngine.recordLearningPlayback(it, videoId, 1) }
        val isCompleted = state.learningProgress.any { it.videoId == videoId && it.completed }
        if (!wasCompleted && isCompleted) message = "这节看完了，等家长确认"
    }
    fun addSaving(title: String, target: BigDecimal) = mutate { LocalFamilyEngine.addSavingGoal(it, title, target) }
    fun saveToGoal(id: String, amount: BigDecimal) = mutate("已存入目标") { LocalFamilyEngine.saveToGoal(it, id, amount) }
    fun addWish(title: String, target: BigDecimal) = mutate { LocalFamilyEngine.addWish(it, title, target) }
    fun buyFund(amount: BigDecimal) = mutate("已购买纯模拟份额") { LocalFamilyEngine.buyFund(it, amount) }
    fun sellFund() = mutate("已赎回全部纯模拟份额") { LocalFamilyEngine.sellAllFund(it) }
    fun updateNav(nav: BigDecimal) = mutate("教学 NAV 已更新") { LocalFamilyEngine.updateFundNav(it, nav) }
    fun updateUsage(daily: Int, session: Int) = mutate("本机防沉迷规则已更新") { LocalFamilyEngine.updateUsagePolicy(it, daily, session) }
    fun updateExperience(birthDate: String, stageOverride: SchoolStage?, overrideReason: String, hapticsEnabled: Boolean) {
        val parsed = runCatching { java.time.LocalDate.parse(birthDate) }.getOrElse { return failUnit("请填写 YYYY-MM-DD 格式的出生日期") }
        if (remote.hasSession()) {
            viewModelScope.launch {
                handleRemote(remote.updateExperience(parsed, stageOverride, overrideReason, hapticsEnabled,
                    state.experience.version), "学习阶段已保存到家庭服务")
            }
        } else {
            mutate("本机学习阶段已更新；连接服务后以服务端配置为准") {
                it.copy(experience = ChildExperiencePolicy.localSettings(parsed, stageOverride, overrideReason,
                    hapticsEnabled, version = it.experience.version + 1))
            }
        }
    }
    fun addEducationSource(title: String, url: String, stages: List<SchoolStage>, usageNote: String) {
        if (title.isBlank() || url.isBlank() || stages.isEmpty() || usageNote.isBlank()) return failUnit("请完整填写来源、网址、学段和免费使用说明")
        viewModelScope.launch {
            when (val result = remote.createEducationSource(title.trim(), url.trim(), stages, usageNote.trim())) {
                is RemoteResult.Ok -> { educationSources = result.value; message = "来源已保存，请读取栏目后再批准" }
                RemoteResult.Unauthorized -> handleRemote(RemoteResult.Unauthorized, "")
                is RemoteResult.Failure -> message = result.message
            }
        }
    }
    fun refreshEducationSource(id: String) = changeEducationSource(id, "refresh", "栏目读取完成；请确认后批准")
    fun approveEducationSource(id: String) = changeEducationSource(id, "approve", "来源已批准，孩子目录会动态更新")
    fun withdrawEducationSource(id: String) = changeEducationSource(id, "withdraw", "来源已撤回，不再显示给孩子")
    fun requestParentForResource() { message = "这个栏目需要家长一起打开" }
    fun attemptLearningActivity(assignmentId: String, activityId: String, response: String) {
        enqueueLearning(LearningActionType.ATTEMPT, assignmentId, activityId=activityId, response=response)
    }
    fun completeLearningVideo(assignmentId: String, activityId: String, playedSeconds: Int, durationSeconds: Int) {
        enqueueLearning(LearningActionType.ATTEMPT, assignmentId, activityId=activityId, response="VIEWED",
            playedSeconds=playedSeconds, durationSeconds=durationSeconds)
    }
    fun submitLearningAssignment(assignmentId: String, version: Long) {
        enqueueLearning(LearningActionType.SUBMIT, assignmentId, expectedVersion=version)
    }
    fun reviewLearningAssignment(assignmentId: String, approve: Boolean, note: String, version: Long) {
        enqueueLearning(LearningActionType.REVIEW, assignmentId, expectedVersion=version,
            decision=if (approve) "APPROVE" else "REWORK", note=note)
    }
    fun retryLearningSync() { viewModelScope.launch { reconcileAndFlushLearning() } }
    fun discardLearningAction(key: String) {
        learningOutbox.remove(key).onSuccess { updateLearningOutbox(); message = "这条本地操作已由家长移除" }
            .onFailure { message = it.message ?: "本地学习记录无法更新" }
    }
    fun createTeachingCourse(courseTitle:String, lessonTitle:String, lessonSummary:String, activityType:String,
                             activityTitle:String, instruction:String, contentRef:String?) {
        val stage = state.experience.effectiveStage
        if (stage == SchoolStage.PARENT_ONLY) return failUnit("0–2 岁仅由家长记录成长，不创建儿童课程")
        if (teachingActionRunningInternal) return
        if (courseTitle.isBlank() || lessonTitle.isBlank() || lessonSummary.isBlank() || activityTitle.isBlank() || instruction.isBlank())
            return failUnit("请完整填写课程、课节和活动")
        updateTeachingActionState(true)
        viewModelScope.launch {
            try {
                when (val result = remote.createTeachingCourse(stage.name, "FAMILY", courseTitle.trim(), lessonTitle.trim(),
                    lessonSummary.trim(), activityType, activityTitle.trim(), instruction.trim(), contentRef)) {
                    is RemoteResult.Ok -> { message = "课程草稿已保存"; syncTeachingCourses() }
                    RemoteResult.Unauthorized -> handleRemote(RemoteResult.Unauthorized, "")
                    is RemoteResult.Failure -> message = result.message
                }
            } finally {
                updateTeachingActionState(false)
            }
        }
    }
    fun publishTeachingCourse(versionId:String) = runTeachingAction {
        when (val result=remote.publishTeachingVersion(versionId)) {
            is RemoteResult.Ok -> { message="课程已发布，可以布置给孩子"; syncTeachingCourses() }
            RemoteResult.Unauthorized -> handleRemote(RemoteResult.Unauthorized, "")
            is RemoteResult.Failure -> message=result.message
        }
    }
    fun assignTeachingCourse(versionId:String) = runTeachingAction {
        when (val result=remote.assignTeachingVersion(versionId)) {
            is RemoteResult.Ok -> { replaceLearning(result.value); message="课节已布置给孩子"; syncTeachingCourses() }
            RemoteResult.Unauthorized -> handleRemote(RemoteResult.Unauthorized, "")
            is RemoteResult.Failure -> message=result.message
        }
    }
    fun clearMessage() { message = null }

    fun connectService(baseUrl: String, familyId: String, parentId: String, childId: String, pin: String) {
        if (connectionState == ConnectionState.Connecting) return
        connectionState = ConnectionState.Connecting
        viewModelScope.launch {
            val result = remote.connect(baseUrl, familyId, parentId, childId, pin)
            handleRemote(result, "已连接并同步家庭服务")
            if (result is RemoteResult.Ok) { syncEducationResources(); reconcileAndFlushLearning(refreshFirst=false) }
        }
    }
    fun refreshService() {
        if (connectionState !is ConnectionState.Connected) connectionState = ConnectionState.Connecting
        viewModelScope.launch {
            val result = remote.refresh()
            handleRemote(result, "已同步最新数据")
            if (result is RemoteResult.Ok) { syncEducationResources(); reconcileAndFlushLearning(refreshFirst=false); flushUsageNow() }
        }
    }
    fun disconnectService() { remote.disconnect(); remoteCompletionByTask = emptyMap(); educationSources = emptyList(); childEducationCatalog = emptyList(); learningAssignments = emptyList(); teachingCourses = emptyList(); connectionState = ConnectionState.Disconnected; message = if(pendingLearningActions.isEmpty()) "已断开；服务端 Token 已从内存清除" else "已断开；Token 已清除，待同步学习记录仍加密保留" }

    private fun changeEducationSource(id: String, action: String, success: String) {
        viewModelScope.launch {
            when (val result = remote.educationSourceAction(id, action)) {
                is RemoteResult.Ok -> {
                    educationSources = result.value
                    syncEducationResources()
                    val changed = result.value.firstOrNull { it.id == id }
                    message = if (action == "refresh" && changed?.refreshStatus == "FAILED")
                        "栏目读取失败，已保留上一次成功结果：${changed.refreshError}" else success
                }
                RemoteResult.Unauthorized -> handleRemote(RemoteResult.Unauthorized, "")
                is RemoteResult.Failure -> message = result.message
            }
        }
    }

    private fun enqueueLearning(type:LearningActionType, assignmentId:String, activityId:String="", response:String="",
                                playedSeconds:Int?=null, durationSeconds:Int?=null, expectedVersion:Long?=null,
                                decision:String="", note:String="") {
        val scope = remote.learningScope() ?: return failUnit("请家长先连接家庭服务")
        val action = PendingLearningAction(familyId=scope.first, childId=scope.second, type=type,
            assignmentId=assignmentId, activityId=activityId, responseText=response,
            playedSeconds=playedSeconds, durationSeconds=durationSeconds, expectedVersion=expectedVersion,
            decision=decision, note=note)
        learningOutbox.enqueue(action).onSuccess {
            updateLearningOutbox()
            message = "这一步已安全保存，正在同步"
            flushLearningOutbox()
        }.onFailure { message = it.message ?: "学习记录无法安全保存" }
    }

    private fun updateLearningOutbox() { pendingLearningActions = learningOutbox.snapshot() }

    private fun flushLearningOutbox() {
        if (learningFlushRunning) return
        viewModelScope.launch { flushLearningOutboxNow() }
    }

    private suspend fun flushLearningOutboxNow() {
        if (learningFlushRunning) return
        learningFlushRunning = true
        try {
            while (remote.hasSession()) {
                val scope = remote.learningScope() ?: return
                val action = learningOutbox.snapshot().firstOrNull {
                    it.state == LearningActionState.PENDING && it.familyId == scope.first && it.childId == scope.second
                } ?: return
                when (val result = remote.executeLearning(action)) {
                    is RemoteResult.Ok -> {
                        val removed = learningOutbox.remove(action.idempotencyKey)
                        if (removed.isFailure) {
                            message = removed.exceptionOrNull()?.message ?: "同步成功，但本地队列无法安全更新"
                            updateLearningOutbox()
                            return
                        }
                        updateLearningOutbox()
                        replaceLearning(result.value)
                        message = when {
                            action.type == LearningActionType.REVIEW && action.decision == "APPROVE" -> "已确认孩子的努力"
                            action.type == LearningActionType.REVIEW -> "已温和地告诉孩子再试哪一步"
                            action.type == LearningActionType.SUBMIT -> "已经交给家长看了"
                            result.value.activities.firstOrNull { it.id == action.activityId }?.checkedCorrect == false -> "再看一看，可以慢慢试"
                            action.responseText == "VIEWED" -> "已记录观看；这不代表全部学会"
                            else -> "这一步已经同步"
                        }
                    }
                    RemoteResult.Unauthorized -> {
                        handleRemote(RemoteResult.Unauthorized, "")
                        updateLearningOutbox()
                        return
                    }
                    is RemoteResult.Failure -> {
                        val needsReview = result.kind != RemoteFailureKind.RETRYABLE
                        val marked = learningOutbox.markFailure(action.idempotencyKey, result.message, needsReview)
                        if (marked.isFailure) {
                            message = marked.exceptionOrNull()?.message ?: "本地学习记录无法安全更新"
                            updateLearningOutbox()
                            return
                        }
                        updateLearningOutbox()
                        if (result.kind == RemoteFailureKind.CONFLICT && syncLearningAssignments()) {
                            val reconciliation = learningOutbox.reconcile(learningAssignments)
                            if (reconciliation.isFailure) {
                                message = reconciliation.exceptionOrNull()?.message ?: "本地学习记录无法安全合并"
                                updateLearningOutbox()
                                return
                            }
                            updateLearningOutbox()
                            val reconciled = learningOutbox.snapshot().firstOrNull { it.idempotencyKey == action.idempotencyKey }
                            if (reconciled == null || reconciled.state == LearningActionState.PENDING) continue
                        }
                        message = if (needsReview) "有一条学习记录需要家长刷新处理" else "网络暂不可用，这一步已加密保留"
                        return
                    }
                }
            }
        } finally { learningFlushRunning = false }
    }

    private suspend fun reconcileAndFlushLearning(refreshFirst:Boolean=true) {
        if (refreshFirst && !syncLearningAssignments()) return
        val reconciliation = learningOutbox.reconcile(learningAssignments)
        if (reconciliation.isFailure) {
            message = reconciliation.exceptionOrNull()?.message ?: "本地学习记录无法安全合并"
            updateLearningOutbox()
            return
        }
        updateLearningOutbox()
        flushLearningOutboxNow()
    }

    private suspend fun syncEducationResources() {
        when (val sources = remote.educationSources()) {
            is RemoteResult.Ok -> educationSources = sources.value
            RemoteResult.Unauthorized -> { handleRemote(RemoteResult.Unauthorized, ""); return }
            is RemoteResult.Failure -> message = sources.message
        }
        when (val catalog = remote.childEducationCatalog()) {
            is RemoteResult.Ok -> childEducationCatalog = catalog.value
            RemoteResult.Unauthorized -> handleRemote(RemoteResult.Unauthorized, "")
            is RemoteResult.Failure -> message = catalog.message
        }
        syncLearningAssignments()
        syncTeachingCourses()
    }

    private suspend fun syncLearningAssignments():Boolean {
        return when (val result = remote.learningAssignments()) {
            is RemoteResult.Ok -> { learningAssignments = result.value; true }
            RemoteResult.Unauthorized -> { handleRemote(RemoteResult.Unauthorized, ""); false }
            is RemoteResult.Failure -> { message = result.message; false }
        }
    }

    private suspend fun syncTeachingCourses() {
        when (val result = remote.teachingCourses()) {
            is RemoteResult.Ok -> teachingCourses = result.value
            RemoteResult.Unauthorized -> handleRemote(RemoteResult.Unauthorized, "")
            is RemoteResult.Failure -> message = result.message
        }
    }

    private fun runTeachingAction(block: suspend () -> Unit) {
        if (teachingActionRunningInternal) return
        updateTeachingActionState(true)
        viewModelScope.launch {
            try { block() } finally { updateTeachingActionState(false) }
        }
    }

    private fun updateTeachingActionState(running:Boolean) {
        teachingActionRunningInternal = running
        teachingActionRunning = running
    }

    private fun replaceLearning(updated: RemoteLearningAssignment) {
        learningAssignments = (learningAssignments.filterNot { it.id == updated.id } + updated)
            .sortedBy { if (it.status == "COMPLETED") 1 else 0 }
    }

    private fun handleRemote(result: RemoteResult<RemoteSnapshot>, success: String) {
        when (result) {
            is RemoteResult.Ok -> {
                val snapshot = result.value
                remoteCompletionByTask = snapshot.tasks.mapNotNull { task -> task.completionId?.let { task.id to it } }.toMap()
                state = state.copy(
                    tasks = snapshot.tasks.map { task -> LocalGrowthTask(
                        id = task.id, title = task.title, minutes = task.minutes,
                        moneyReward = BigDecimal.ZERO.setScale(2), coinReward = 10, xpReward = 10,
                        status = when (task.status) { "SUBMITTED" -> TaskStatus.SUBMITTED; "APPROVED" -> TaskStatus.APPROVED; else -> TaskStatus.TODO },
                    ) },
                    wallet = state.wallet.copy(money = snapshot.money, coin = snapshot.coin),
                    experience = snapshot.experience?.toLocal() ?: state.experience,
                )
                connectionState = ConnectionState.Connected(snapshot)
                message = success
            }
            RemoteResult.Unauthorized -> { remoteCompletionByTask = emptyMap(); connectionState = ConnectionState.Expired; message = "登录已过期，请由家长重新连接" }
            is RemoteResult.Failure -> { if (connectionState !is ConnectionState.Connected) connectionState = ConnectionState.Error(result.message); message = result.message }
        }
    }

    private fun flushUsage() {
        if (usageFlushRunning) return
        viewModelScope.launch { flushUsageNow() }
    }

    private suspend fun flushUsageNow() {
        if (usageFlushRunning) return
        usageFlushRunning = true
        try {
            while (pendingUsage.isNotEmpty() && remote.hasSession()) {
                val event = pendingUsage.first()
                when (val result = remote.recordUsage(event.key, event.occurredAt)) {
                    is RemoteResult.Ok -> pendingUsage.removeAt(0)
                    RemoteResult.Unauthorized -> { handleRemote(RemoteResult.Unauthorized, ""); return }
                    is RemoteResult.Failure -> { message = result.message; return }
                }
            }
        } finally {
            usageFlushRunning = false
        }
    }

    private fun mutate(success: String? = null, operation: (FamilyLocalState) -> FamilyLocalState) {
        runCatching { operation(state) }
            .onSuccess { updated ->
                store.save(updated).onSuccess {
                    state = updated
                    if (success != null) message = success
                }.onFailure { message = it.message ?: "本地保存失败" }
            }
            .onFailure { message = it.message ?: "操作失败" }
    }

    private fun fail(text: String): Boolean { message = text; return false }
    private fun failUnit(text: String) { message = text }
    private data class PendingUsage(val key: String, val occurredAt: Instant)
}

private fun RemoteExperienceProfile.toLocal() = ChildExperienceSettings(
    birthDate = birthDate,
    recommendedStage = SchoolStage.valueOf(recommendedStage),
    stageOverride = stageOverride?.let(SchoolStage::valueOf),
    effectiveStage = SchoolStage.valueOf(effectiveStage),
    overrideReason = overrideReason,
    hapticsEnabled = hapticsEnabled,
    version = version,
    source = ExperienceSource.SERVER,
)
