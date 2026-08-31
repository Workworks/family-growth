package com.familygrowth.android.core

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalFamilyEngineTest {
    @Test
    fun approvingSubmittedTaskPostsOneTraceableRewardEntry() {
        val created = LocalFamilyEngine.addTask(
            state = FamilyLocalState(),
            title = "阅读二十分钟",
            minutes = 20,
            moneyReward = BigDecimal("2.50"),
            coin = 3,
            xp = 8,
        )
        val submitted = LocalFamilyEngine.submitTask(created, created.tasks.single().id)
        val approved = LocalFamilyEngine.approveTask(submitted, submitted.tasks.single().id)

        assertEquals(TaskStatus.APPROVED, approved.tasks.single().status)
        assertEquals(BigDecimal("2.50"), approved.wallet.money)
        assertEquals(3, approved.wallet.coin)
        assertEquals(8, approved.wallet.xp)
        assertEquals("TASK_REWARD", approved.ledger.single().type)
    }

    @Test
    fun withdrawalWaitsForParentApprovalThenRecordsTransparentFee() {
        val funded = LocalFamilyEngine.depositGiftMoney(FamilyLocalState(), BigDecimal("100.00"))
        val requested = LocalFamilyEngine.requestWithdrawal(funded, BigDecimal("25.00"))

        assertEquals(BigDecimal("100.00"), requested.wallet.money)
        assertEquals(WithdrawalStatus.PENDING, requested.withdrawals.single().status)

        val withdrawn = LocalFamilyEngine.approveWithdrawal(requested, requested.withdrawals.single().id)

        assertEquals(BigDecimal("75.00"), withdrawn.wallet.money)
        assertEquals(WithdrawalStatus.PAID, withdrawn.withdrawals.single().status)
        assertTrue(withdrawn.ledger.first().description.contains("线下到账 ¥24.50"))
        assertTrue(withdrawn.ledger.first().description.contains("手续费 ¥0.50"))
    }

    @Test
    fun exchangeAndRewardRedemptionKeepEveryBalanceChangeInLedger() {
        val funded = LocalFamilyEngine.depositGiftMoney(FamilyLocalState(), BigDecimal("10.00"))
        val exchanged = LocalFamilyEngine.exchangeMoneyToCoin(funded, BigDecimal("4.00"))
        val withReward = LocalFamilyEngine.addReward(exchanged, "周末选电影", 3)
        val redeemed = LocalFamilyEngine.redeemReward(withReward, withReward.rewards.single().id)

        assertEquals(BigDecimal("6.00"), redeemed.wallet.money)
        assertEquals(1, redeemed.wallet.coin)
        assertEquals(listOf("REWARD_REDEEM", "MONEY_TO_COIN", "GIFT_MONEY"), redeemed.ledger.map { it.type })
    }

    @Test
    fun simulatedFundUsesWalletAndReflectsParentUpdatedNav() {
        val funded = LocalFamilyEngine.depositGiftMoney(FamilyLocalState(), BigDecimal("20.00"))
        val bought = LocalFamilyEngine.buyFund(funded, BigDecimal("10.00"))
        val repriced = LocalFamilyEngine.updateFundNav(bought, BigDecimal("1.1000"))
        val sold = LocalFamilyEngine.sellAllFund(repriced)

        assertEquals(BigDecimal("21.00"), sold.wallet.money)
        assertEquals(BigDecimal("0.0000"), sold.fund.shares)
        assertEquals(listOf("FUND_SELL", "FUND_BUY", "GIFT_MONEY"), sold.ledger.map { it.type })
    }

    @Test
    fun insufficientWalletBalanceIsRejectedWithoutCreatingAnEntry() {
        val empty = FamilyLocalState()

        val error = assertThrows(FamilyRuleException::class.java) {
            LocalFamilyEngine.buyFund(empty, BigDecimal("1.00"))
        }

        assertEquals("Money 余额不足", error.message)
        assertTrue(empty.ledger.isEmpty())
    }

    @Test
    fun rewardInterestIsReversibleAndNeverChangesTheWalletLedger() {
        val withReward = LocalFamilyEngine.addReward(FamilyLocalState(), "周末一起去公园", 4)
        val rewardId = withReward.rewards.single().id

        val interested = LocalFamilyEngine.toggleRewardInterest(withReward, rewardId)
        val removed = LocalFamilyEngine.toggleRewardInterest(interested, rewardId)

        assertEquals(listOf(rewardId), interested.rewardInterestIds)
        assertTrue(removed.rewardInterestIds.isEmpty())
        assertEquals(withReward.wallet, interested.wallet)
        assertTrue(interested.ledger.isEmpty())
    }

    @Test
    fun learningVideoSubmitsOneTaskOnlyAfterNinetyPercentActualPlayback() {
        val lesson = LearningCatalog.lessons.first()
        var state = FamilyLocalState()

        repeat(16) { state = LocalFamilyEngine.recordLearningPlayback(state, lesson.id, 1) }
        assertTrue(state.tasks.isEmpty())
        assertEquals(false, state.learningProgress.single().completed)

        state = LocalFamilyEngine.recordLearningPlayback(state, lesson.id, 1)
        val task = state.tasks.single()
        assertEquals(TaskStatus.SUBMITTED, task.status)
        assertEquals(TaskSource.LEARNING_VIDEO, task.source)
        assertEquals(2, task.coinReward)
        assertTrue(state.ledger.isEmpty())

        val replayed = LocalFamilyEngine.recordLearningPlayback(state, lesson.id, 1)
        assertEquals(1, replayed.tasks.size)
        assertEquals(state.learningProgress, replayed.learningProgress)

        val approved = LocalFamilyEngine.approveTask(replayed, task.id)
        assertEquals(2, approved.wallet.coin)
        assertEquals(5, approved.wallet.xp)
        assertEquals("TASK_REWARD", approved.ledger.single().type)
    }

    @Test
    fun stageCatalogAndParentRewardPolicyAreAppliedWhenCompletionIsCreated() {
        assertEquals(3, LearningCatalog.forStage(SchoolStage.KINDERGARTEN).size)
        assertEquals(3, LearningCatalog.forStage(SchoolStage.PRIMARY).size)
        assertTrue(LearningCatalog.forStage(SchoolStage.PRIMARY).all { it.schoolStage == SchoolStage.PRIMARY })
        val lesson = LearningCatalog.forStage(SchoolStage.PRIMARY).first()
        var state = LocalFamilyEngine.updateLearningRewardPolicy(FamilyLocalState(), BigDecimal("1.50"), 7, 12)
        repeat(17) { state = LocalFamilyEngine.recordLearningPlayback(state, lesson.id, 1) }
        val task = state.tasks.single()
        assertEquals(BigDecimal("1.50"), task.moneyReward)
        assertEquals(7, task.coinReward)
        assertEquals(12, task.xpReward)
        assertTrue(state.ledger.isEmpty())
    }
}
