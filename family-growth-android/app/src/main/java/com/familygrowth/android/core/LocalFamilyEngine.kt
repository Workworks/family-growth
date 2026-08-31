package com.familygrowth.android.core

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import kotlin.math.ceil

object LocalFamilyEngine {
    fun addTask(state: FamilyLocalState, title: String, minutes: Int, moneyReward: BigDecimal, coin: Int, xp: Int): FamilyLocalState {
        requireText(title, "任务名称")
        if (minutes !in 1..480) throw FamilyRuleException("任务时长需在 1–480 分钟之间")
        requireNonNegative(moneyReward)
        if (coin < 0 || xp < 0) throw FamilyRuleException("奖励不能为负数")
        return state.copy(tasks = state.tasks + LocalGrowthTask(title = title.trim(), minutes = minutes, moneyReward = moneyReward, coinReward = coin, xpReward = xp))
    }

    fun submitTask(state: FamilyLocalState, id: String): FamilyLocalState = state.copy(
        tasks = state.tasks.map { if (it.id == id && it.status == TaskStatus.TODO) it.copy(status = TaskStatus.SUBMITTED) else it },
    )

    fun approveTask(state: FamilyLocalState, id: String): FamilyLocalState {
        val task = state.tasks.singleOrNull { it.id == id } ?: throw FamilyRuleException("任务不存在")
        if (task.status != TaskStatus.SUBMITTED) throw FamilyRuleException("只有已提交任务可以审核")
        val entry = LocalLedgerEntry(
            type = "TASK_REWARD",
            description = "完成任务：${task.title}",
            moneyDelta = task.moneyReward,
            coinDelta = task.coinReward,
            xpDelta = task.xpReward,
        )
        return state.copy(
            tasks = state.tasks.map { if (it.id == id) it.copy(status = TaskStatus.APPROVED) else it },
            wallet = state.wallet.apply(entry),
            ledger = listOf(entry) + state.ledger,
        )
    }

    fun depositGiftMoney(state: FamilyLocalState, amount: BigDecimal): FamilyLocalState {
        requirePositive(amount)
        val entry = LocalLedgerEntry(type = "GIFT_MONEY", description = "压岁钱按 1:1 存入", moneyDelta = amount)
        return post(state, entry)
    }

    fun exchangeMoneyToCoin(state: FamilyLocalState, amount: BigDecimal): FamilyLocalState {
        requirePositive(amount)
        ensureMoney(state, amount)
        val coin = amount.setScale(0, RoundingMode.DOWN).intValueExact()
        if (coin <= 0) throw FamilyRuleException("兑换金额至少为 1.00")
        val entry = LocalLedgerEntry(type = "MONEY_TO_COIN", description = "Money 兑换 Coin（基础比例 1:1）", moneyDelta = amount.negate(), coinDelta = coin)
        return post(state, entry)
    }

    fun requestWithdrawal(state: FamilyLocalState, gross: BigDecimal, feeRate: BigDecimal = BigDecimal("0.02")): FamilyLocalState {
        requirePositive(gross)
        ensureMoney(state, gross)
        val fee = gross.multiply(feeRate).setScale(2, RoundingMode.HALF_UP)
        val net = gross.subtract(fee)
        return state.copy(withdrawals = listOf(LocalWithdrawalRequest(gross = gross, fee = fee, net = net)) + state.withdrawals)
    }

    fun approveWithdrawal(state: FamilyLocalState, id: String): FamilyLocalState {
        val request = state.withdrawals.singleOrNull { it.id == id } ?: throw FamilyRuleException("零钱回收申请不存在")
        if (request.status != WithdrawalStatus.PENDING) throw FamilyRuleException("该申请已处理")
        ensureMoney(state, request.gross)
        val entry = LocalLedgerEntry(
            type = "WITHDRAWAL_APPROVED",
            description = "零钱回收：预计线下到账 ¥${request.net}，手续费 ¥${request.fee}",
            moneyDelta = request.gross.negate(),
        )
        return post(state, entry).copy(
            withdrawals = state.withdrawals.map {
                if (it.id == id) it.copy(status = WithdrawalStatus.PAID) else it
            },
        )
    }

    fun addReward(state: FamilyLocalState, title: String, price: Int): FamilyLocalState {
        requireText(title, "奖励名称")
        if (price <= 0) throw FamilyRuleException("兑换价格必须大于 0")
        return state.copy(rewards = state.rewards + LocalRewardItem(title = title.trim(), coinPrice = price))
    }

    fun redeemReward(state: FamilyLocalState, id: String): FamilyLocalState {
        val reward = state.rewards.singleOrNull { it.id == id } ?: throw FamilyRuleException("奖励不存在")
        if (state.wallet.coin < reward.coinPrice) throw FamilyRuleException("Coin 余额不足")
        val entry = LocalLedgerEntry(type = "REWARD_REDEEM", description = "兑换奖励：${reward.title}", coinDelta = -reward.coinPrice)
        return post(state, entry)
    }

    fun toggleRewardInterest(state: FamilyLocalState, id: String): FamilyLocalState {
        if (state.rewards.none { it.id == id }) throw FamilyRuleException("奖励不存在")
        val interests = state.rewardInterestIds.toMutableSet()
        if (!interests.add(id)) interests.remove(id)
        return state.copy(rewardInterestIds = interests.toList())
    }

    fun recordLearningPlayback(state: FamilyLocalState, videoId: String, playedSeconds: Int): FamilyLocalState {
        if (playedSeconds !in 1..5) throw FamilyRuleException("播放进度无效")
        val lesson = LearningCatalog.byId(videoId)
        val current = state.learningProgress.singleOrNull { it.videoId == videoId }
            ?: LocalLearningProgress(videoId)
        if (current.completed) return state

        val watched = (current.watchedSeconds + playedSeconds).coerceAtMost(lesson.durationSeconds)
        val completed = watched >= ceil(lesson.durationSeconds * 0.9).toInt()
        val updatedProgress = current.copy(watchedSeconds = watched, completed = completed)
        val progress = state.learningProgress.filterNot { it.videoId == videoId } + updatedProgress
        if (!completed) return state.copy(learningProgress = progress)

        val taskId = "video:$videoId"
        val existing = state.tasks.singleOrNull { it.id == taskId }
        val tasks = when {
            existing == null -> state.tasks + LocalGrowthTask(
                id = taskId,
                title = "看完：${lesson.title}",
                minutes = 1,
                moneyReward = state.learningRewardPolicy.money,
                coinReward = state.learningRewardPolicy.coin,
                xpReward = state.learningRewardPolicy.xp,
                status = TaskStatus.SUBMITTED,
                source = TaskSource.LEARNING_VIDEO,
                sourceVideoId = videoId,
            )
            existing.status == TaskStatus.TODO -> state.tasks.map {
                if (it.id == taskId) it.copy(status = TaskStatus.SUBMITTED) else it
            }
            else -> state.tasks
        }
        return state.copy(tasks = tasks, learningProgress = progress)
    }

    fun updateLearningRewardPolicy(state: FamilyLocalState, money: BigDecimal, coin: Int, xp: Int): FamilyLocalState {
        requireNonNegative(money)
        if (coin !in 0..10_000 || xp !in 0..100_000) throw FamilyRuleException("学习奖励超出允许范围")
        return state.copy(learningRewardPolicy = LearningRewardPolicy(money.setScale(2, RoundingMode.HALF_UP), coin, xp))
    }

    fun addSavingGoal(state: FamilyLocalState, title: String, target: BigDecimal): FamilyLocalState {
        requireText(title, "储蓄目标")
        requirePositive(target)
        return state.copy(savings = state.savings + LocalSavingGoal(title = title.trim(), target = target))
    }

    fun saveToGoal(state: FamilyLocalState, id: String, amount: BigDecimal): FamilyLocalState {
        requirePositive(amount)
        ensureMoney(state, amount)
        val goal = state.savings.singleOrNull { it.id == id } ?: throw FamilyRuleException("储蓄目标不存在")
        val entry = LocalLedgerEntry(type = "SAVING_DEPOSIT", description = "存入储蓄目标：${goal.title}", moneyDelta = amount.negate())
        return post(state, entry).copy(savings = state.savings.map { if (it.id == id) it.copy(saved = it.saved.add(amount)) else it })
    }

    fun addWish(state: FamilyLocalState, title: String, target: BigDecimal): FamilyLocalState {
        requireText(title, "愿望名称")
        requirePositive(target)
        return state.copy(wishes = state.wishes + LocalWish(title = title.trim(), target = target))
    }

    fun buyFund(state: FamilyLocalState, amount: BigDecimal): FamilyLocalState {
        requirePositive(amount)
        ensureMoney(state, amount)
        val shares = amount.divide(state.fund.nav, 4, RoundingMode.DOWN)
        if (shares <= BigDecimal.ZERO) throw FamilyRuleException("购买金额过小")
        val entry = LocalLedgerEntry(type = "FUND_BUY", description = "购买纯模拟成长基金 $shares 份，NAV ${state.fund.nav}", moneyDelta = amount.negate())
        return post(state, entry).copy(fund = state.fund.copy(shares = state.fund.shares.add(shares)))
    }

    fun sellAllFund(state: FamilyLocalState): FamilyLocalState {
        if (state.fund.shares <= BigDecimal.ZERO) throw FamilyRuleException("当前没有模拟持仓")
        val proceeds = state.fund.marketValue
        val entry = LocalLedgerEntry(type = "FUND_SELL", description = "赎回全部纯模拟基金，NAV ${state.fund.nav}", moneyDelta = proceeds)
        return post(state, entry).copy(fund = state.fund.copy(shares = BigDecimal.ZERO.setScale(4)))
    }

    fun updateFundNav(state: FamilyLocalState, nav: BigDecimal): FamilyLocalState {
        if (nav <= BigDecimal.ZERO || nav > BigDecimal("1000")) throw FamilyRuleException("NAV 必须在 0–1000 之间")
        return state.copy(fund = state.fund.copy(nav = nav.setScale(4, RoundingMode.HALF_UP)))
    }

    fun updateUsagePolicy(state: FamilyLocalState, daily: Int, session: Int): FamilyLocalState {
        if (daily !in 10..480 || session !in 5..240 || session > daily) throw FamilyRuleException("请设置有效的每日/单次时长")
        return state.copy(usage = state.usage.copy(dailyLimitMinutes = daily, sessionLimitMinutes = session))
    }

    fun recordUsageMinute(state: FamilyLocalState): FamilyLocalState {
        val today = LocalDate.now().toString()
        val usage = if (state.usage.usageDate == today) state.usage else state.usage.copy(usedMinutes = 0, usageDate = today)
        return state.copy(usage = usage.copy(usedMinutes = usage.usedMinutes + 1))
    }

    private fun post(state: FamilyLocalState, entry: LocalLedgerEntry): FamilyLocalState = state.copy(
        wallet = state.wallet.apply(entry),
        ledger = listOf(entry) + state.ledger,
    )

    private fun WalletSnapshot.apply(entry: LocalLedgerEntry) = copy(
        money = money.add(entry.moneyDelta).setScale(2, RoundingMode.HALF_UP),
        coin = coin + entry.coinDelta,
        xp = xp + entry.xpDelta,
    )

    private fun ensureMoney(state: FamilyLocalState, amount: BigDecimal) {
        if (state.wallet.money < amount) throw FamilyRuleException("Money 余额不足")
    }

    private fun requirePositive(value: BigDecimal) {
        if (value <= BigDecimal.ZERO) throw FamilyRuleException("金额必须大于 0")
    }

    private fun requireNonNegative(value: BigDecimal) {
        if (value < BigDecimal.ZERO) throw FamilyRuleException("金额不能为负数")
    }

    private fun requireText(value: String, field: String) {
        if (value.isBlank()) throw FamilyRuleException("$field 不能为空")
    }
}
