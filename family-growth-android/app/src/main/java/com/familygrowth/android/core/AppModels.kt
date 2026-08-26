package com.familygrowth.android.core

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

enum class AppMode { CHILD, PARENT }
enum class AppSection { TODAY, TASKS, WALLET, GROWTH, PARENT }
enum class TaskStatus { TODO, SUBMITTED, APPROVED }
enum class TaskSource { FAMILY, LEARNING_VIDEO }
enum class WithdrawalStatus { PENDING, APPROVED }

object ChildExperiencePolicy {
    const val MINIMUM_AGE = 3
    val childSections = listOf(AppSection.TODAY, AppSection.TASKS, AppSection.GROWTH)
    val parentSections = AppSection.entries

    fun sectionsFor(mode: AppMode): List<AppSection> =
        if (mode == AppMode.CHILD) childSections else parentSections

    fun allowsAdvancedFinance(mode: AppMode): Boolean = mode == AppMode.PARENT
}

data class LocalGrowthTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val minutes: Int,
    val moneyReward: BigDecimal,
    val coinReward: Int,
    val xpReward: Int,
    val status: TaskStatus = TaskStatus.TODO,
    val source: TaskSource = TaskSource.FAMILY,
    val sourceVideoId: String? = null,
)

data class WalletSnapshot(
    val money: BigDecimal = BigDecimal.ZERO.setScale(2),
    val coin: Int = 0,
    val xp: Int = 0,
)

data class LocalLedgerEntry(
    val id: String = UUID.randomUUID().toString(),
    val type: String,
    val description: String,
    val moneyDelta: BigDecimal = BigDecimal.ZERO.setScale(2),
    val coinDelta: Int = 0,
    val xpDelta: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

data class LocalWithdrawalRequest(
    val id: String = UUID.randomUUID().toString(),
    val gross: BigDecimal,
    val fee: BigDecimal,
    val net: BigDecimal,
    val status: WithdrawalStatus = WithdrawalStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
)

data class LocalRewardItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val coinPrice: Int,
)

data class LocalLearningProgress(
    val videoId: String,
    val watchedSeconds: Int = 0,
    val completed: Boolean = false,
)

data class LearningLesson(
    val id: String,
    val title: String,
    val prompt: String,
    val resourceName: String,
    val durationSeconds: Int,
    val symbol: String,
)

object LearningCatalog {
    val lessons = listOf(
        LearningLesson("color-garden", "认识三种颜色", "跟着颜色慢慢看", "lesson_color_garden", 18, "●"),
        LearningLesson("count-to-five", "一起数到五", "看看圆点一个个出现", "lesson_count_to_five", 18, "1·2·3"),
        LearningLesson("shape-home", "形状找到家", "看看圆形、方形和三角形", "lesson_shape_home", 18, "△○□"),
    )

    fun byId(id: String): LearningLesson =
        lessons.singleOrNull { it.id == id } ?: throw FamilyRuleException("教学视频不存在")
}

data class LocalSavingGoal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val target: BigDecimal,
    val saved: BigDecimal = BigDecimal.ZERO.setScale(2),
)

data class LocalWish(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val target: BigDecimal,
)

data class LocalFundPosition(
    val nav: BigDecimal = BigDecimal.ONE.setScale(4),
    val shares: BigDecimal = BigDecimal.ZERO.setScale(4),
) {
    val marketValue: BigDecimal get() = shares.multiply(nav).setScale(2, RoundingMode.HALF_UP)
}

data class UsagePolicy(
    val dailyLimitMinutes: Int = 20,
    val sessionLimitMinutes: Int = 10,
    val usedMinutes: Int = 0,
    val usageDate: String = LocalDate.now().toString(),
)

data class FamilyLocalState(
    val tasks: List<LocalGrowthTask> = emptyList(),
    val wallet: WalletSnapshot = WalletSnapshot(),
    val ledger: List<LocalLedgerEntry> = emptyList(),
    val withdrawals: List<LocalWithdrawalRequest> = emptyList(),
    val rewards: List<LocalRewardItem> = emptyList(),
    val rewardInterestIds: List<String> = emptyList(),
    val learningProgress: List<LocalLearningProgress> = emptyList(),
    val savings: List<LocalSavingGoal> = emptyList(),
    val wishes: List<LocalWish> = emptyList(),
    val fund: LocalFundPosition = LocalFundPosition(),
    val usage: UsagePolicy = UsagePolicy(),
)

fun money(value: String): BigDecimal =
    value.trim().toBigDecimalOrNull()?.setScale(2, RoundingMode.HALF_UP)
        ?: throw FamilyRuleException("请输入有效金额")

class FamilyRuleException(message: String) : IllegalArgumentException(message)
