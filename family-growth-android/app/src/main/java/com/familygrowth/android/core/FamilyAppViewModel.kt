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
    private var remoteCompletionByTask: Map<String, String> = emptyMap()
    private val pendingUsage = mutableListOf<PendingUsage>()
    private var usageFlushRunning = false

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

    val isChildLocked: Boolean get() = mode == AppMode.CHILD &&
        (state.usage.usedMinutes >= state.usage.dailyLimitMinutes || sessionUsedMinutes >= state.usage.sessionLimitMinutes)

    init {
        store.load().onSuccess { state = it }.onFailure { message = "本地数据读取失败，已进入空白安全状态" }
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
    fun clearMessage() { message = null }

    fun connectService(baseUrl: String, familyId: String, parentId: String, childId: String, pin: String) {
        if (connectionState == ConnectionState.Connecting) return
        connectionState = ConnectionState.Connecting
        viewModelScope.launch { handleRemote(remote.connect(baseUrl, familyId, parentId, childId, pin), "已连接并同步家庭服务") }
    }
    fun refreshService() {
        if (connectionState !is ConnectionState.Connected) connectionState = ConnectionState.Connecting
        viewModelScope.launch {
            val result = remote.refresh()
            handleRemote(result, "已同步最新数据")
            if (result is RemoteResult.Ok) flushUsageNow()
        }
    }
    fun disconnectService() { remote.disconnect(); remoteCompletionByTask = emptyMap(); connectionState = ConnectionState.Disconnected; message = "已断开；服务端 Token 已从内存清除" }

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
