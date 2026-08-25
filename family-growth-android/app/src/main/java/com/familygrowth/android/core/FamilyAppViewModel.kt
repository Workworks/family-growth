package com.familygrowth.android.core

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import org.mindrot.jbcrypt.BCrypt
import java.math.BigDecimal

class FamilyAppViewModel(application: Application) : AndroidViewModel(application) {
    private val store = LocalFamilyStore(application)

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
    }

    fun addTask(title: String, minutes: Int, rewardMoney: BigDecimal, coin: Int, xp: Int) =
        mutate { LocalFamilyEngine.addTask(it, title, minutes, rewardMoney, coin, xp) }
    fun submitTask(id: String) = mutate("任务已提交，等待家长审核") { LocalFamilyEngine.submitTask(it, id) }
    fun approveTask(id: String) = mutate("审核通过，奖励已进入钱包流水") { LocalFamilyEngine.approveTask(it, id) }
    fun depositGift(amount: BigDecimal) = mutate("压岁钱已按 1:1 存入") { LocalFamilyEngine.depositGiftMoney(it, amount) }
    fun exchange(amount: BigDecimal) = mutate("兑换完成") { LocalFamilyEngine.exchangeMoneyToCoin(it, amount) }
    fun withdraw(amount: BigDecimal) = mutate("零钱回收申请已提交，家长确认后才会扣款") { LocalFamilyEngine.requestWithdrawal(it, amount) }
    fun approveWithdrawal(id: String) = mutate("申请已确认，扣款和手续费已进入流水") { LocalFamilyEngine.approveWithdrawal(it, id) }
    fun addReward(title: String, price: Int) = mutate { LocalFamilyEngine.addReward(it, title, price) }
    fun redeemReward(id: String) = mutate("奖励兑换已记录") { LocalFamilyEngine.redeemReward(it, id) }
    fun addSaving(title: String, target: BigDecimal) = mutate { LocalFamilyEngine.addSavingGoal(it, title, target) }
    fun saveToGoal(id: String, amount: BigDecimal) = mutate("已存入目标") { LocalFamilyEngine.saveToGoal(it, id, amount) }
    fun addWish(title: String, target: BigDecimal) = mutate { LocalFamilyEngine.addWish(it, title, target) }
    fun buyFund(amount: BigDecimal) = mutate("已购买纯模拟份额") { LocalFamilyEngine.buyFund(it, amount) }
    fun sellFund() = mutate("已赎回全部纯模拟份额") { LocalFamilyEngine.sellAllFund(it) }
    fun updateNav(nav: BigDecimal) = mutate("教学 NAV 已更新") { LocalFamilyEngine.updateFundNav(it, nav) }
    fun updateUsage(daily: Int, session: Int) = mutate("本机防沉迷规则已更新") { LocalFamilyEngine.updateUsagePolicy(it, daily, session) }
    fun clearMessage() { message = null }

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
}
