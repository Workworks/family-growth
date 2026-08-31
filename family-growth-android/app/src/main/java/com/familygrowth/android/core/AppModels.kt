package com.familygrowth.android.core

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

enum class AppMode { CHILD, PARENT }
enum class AppSection { TODAY, TASKS, WALLET, GROWTH, PARENT }
enum class TaskStatus { TODO, SUBMITTED, APPROVED }
enum class TaskSource { FAMILY, LEARNING_VIDEO }
enum class WithdrawalStatus { PENDING, APPROVED, PAID, REJECTED, CANCELLED }
enum class SchoolStage { PARENT_ONLY, KINDERGARTEN, PRIMARY, JUNIOR_MIDDLE, SENIOR_HIGH }
enum class PrimaryGradeBand(val label: String, val guide: String) {
    LOWER_PRIMARY("低年级", "读懂 · 试一试 · 说发现"),
    UPPER_PRIMARY("高年级", "先猜 · 验证 · 解释"),
}
enum class ExperienceSource { LOCAL, SERVER }

data class ChildFeedbackProfile(
    val visualStyle: String,
    val maxAnimationMs: Int,
    val hapticPulseCount: Int,
    val primaryPressScale: Float,
    val hapticsEnabled: Boolean,
)

data class ChildExperienceSettings(
    val birthDate: String = LocalDate.now().minusYears(4).toString(),
    val recommendedStage: SchoolStage = SchoolStage.KINDERGARTEN,
    val stageOverride: SchoolStage? = null,
    val effectiveStage: SchoolStage = SchoolStage.KINDERGARTEN,
    val recommendedPrimaryBand: PrimaryGradeBand? = null,
    val primaryBandOverride: PrimaryGradeBand? = null,
    val effectivePrimaryBand: PrimaryGradeBand? = null,
    val overrideReason: String = "",
    val hapticsEnabled: Boolean = true,
    val version: Long = 0,
    val source: ExperienceSource = ExperienceSource.LOCAL,
)

object ChildExperiencePolicy {
    const val MINIMUM_AGE = 3
    val childSections = listOf(AppSection.TODAY, AppSection.TASKS, AppSection.GROWTH)
    val parentSections = AppSection.entries

    fun sectionsFor(mode: AppMode): List<AppSection> =
        if (mode == AppMode.CHILD) childSections else parentSections

    fun allowsAdvancedFinance(mode: AppMode): Boolean = mode == AppMode.PARENT

    fun recommendedStage(birthDate: LocalDate, today: LocalDate = LocalDate.now()): SchoolStage {
        require(!birthDate.isAfter(today)) { "出生日期不能晚于今天" }
        val age = java.time.Period.between(birthDate, today).years
        return when {
            age < 3 -> SchoolStage.PARENT_ONLY
            age < 6 -> SchoolStage.KINDERGARTEN
            age < 12 -> SchoolStage.PRIMARY
            age < 15 -> SchoolStage.JUNIOR_MIDDLE
            else -> SchoolStage.SENIOR_HIGH
        }
    }

    fun feedbackFor(settings: ChildExperienceSettings, systemReducedMotion: Boolean = false): ChildFeedbackProfile {
        val enabled = settings.hapticsEnabled
        val base = when (settings.effectiveStage) {
            SchoolStage.PARENT_ONLY -> ChildFeedbackProfile("parent-records", 0, 0, 1f, false)
            SchoolStage.KINDERGARTEN -> ChildFeedbackProfile("storybook-stage", 320, if (enabled) 2 else 0, 1.10f, enabled)
            SchoolStage.PRIMARY -> ChildFeedbackProfile("exploration-notebook", 220, if (enabled) 1 else 0, 1.04f, enabled)
            SchoolStage.JUNIOR_MIDDLE -> ChildFeedbackProfile("subject-lab", 160, if (enabled) 1 else 0, 1.02f, enabled)
            SchoolStage.SENIOR_HIGH -> ChildFeedbackProfile("study-studio", 120, if (enabled) 1 else 0, 1.01f, enabled)
        }
        return if (systemReducedMotion) base.copy(maxAnimationMs = 0, primaryPressScale = 1f) else base
    }

    fun localSettings(
        birthDate: LocalDate,
        override: SchoolStage?,
        primaryBandOverride: PrimaryGradeBand? = null,
        overrideReason: String,
        hapticsEnabled: Boolean,
        today: LocalDate = LocalDate.now(),
        version: Long = 0,
    ): ChildExperienceSettings {
        require(override != SchoolStage.PARENT_ONLY) { "家长记录模式不能作为学段覆盖" }
        require(override == null || overrideReason.isNotBlank()) { "覆盖学段时需要填写原因" }
        val recommended = recommendedStage(birthDate, today)
        val effective = override ?: recommended
        require(primaryBandOverride == null || effective == SchoolStage.PRIMARY) { "小学分段只能用于小学阶段" }
        val recommendedBand = if (effective == SchoolStage.PRIMARY) primaryBandFor(birthDate, today) else null
        return ChildExperienceSettings(
            birthDate = birthDate.toString(), recommendedStage = recommended, stageOverride = override,
            effectiveStage = effective, recommendedPrimaryBand = recommendedBand,
            primaryBandOverride = primaryBandOverride,
            effectivePrimaryBand = if (effective == SchoolStage.PRIMARY) primaryBandOverride ?: recommendedBand else null,
            overrideReason = overrideReason.trim(),
            hapticsEnabled = hapticsEnabled, version = version, source = ExperienceSource.LOCAL,
        )
    }

    fun primaryBandFor(birthDate: LocalDate, today: LocalDate = LocalDate.now()): PrimaryGradeBand {
        require(!birthDate.isAfter(today)) { "出生日期不能晚于今天" }
        return if (java.time.Period.between(birthDate, today).years <= 8) {
            PrimaryGradeBand.LOWER_PRIMARY
        } else PrimaryGradeBand.UPPER_PRIMARY
    }
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

data class LearningRewardPolicy(
    val money: BigDecimal = BigDecimal.ZERO.setScale(2),
    val coin: Int = 2,
    val xp: Int = 5,
)

data class LearningLesson(
    val id: String,
    val schoolStage: SchoolStage,
    val title: String,
    val prompt: String,
    val resourceName: String,
    val durationSeconds: Int,
    val symbol: String,
)

object LearningCatalog {
    val lessons = listOf(
        LearningLesson("color-garden", SchoolStage.KINDERGARTEN, "认识三种颜色", "跟着颜色慢慢看", "lesson_color_garden", 18, "●"),
        LearningLesson("count-to-five", SchoolStage.KINDERGARTEN, "一起数到五", "看看圆点一个个出现", "lesson_count_to_five", 18, "1·2·3"),
        LearningLesson("shape-home", SchoolStage.KINDERGARTEN, "形状找到家", "看看圆形、方形和三角形", "lesson_shape_home", 18, "△○□"),
        LearningLesson("primary-color-observe", SchoolStage.PRIMARY, "颜色观察笔记", "看完说出颜色出现的顺序", "lesson_color_garden", 18, "色"),
        LearningLesson("primary-number-pattern", SchoolStage.PRIMARY, "数字规律小实验", "暂停一次，猜猜下一个数量", "lesson_count_to_five", 18, "1→5"),
        LearningLesson("primary-shape-explain", SchoolStage.PRIMARY, "用一句话说形状", "看完解释一种形状的特点", "lesson_shape_home", 18, "△○□"),
        LearningLesson("junior-visual-sequence", SchoolStage.JUNIOR_MIDDLE, "视觉序列与表达", "记录画面顺序，并用三句话复述", "lesson_color_garden", 18, "序列"),
        LearningLesson("junior-discrete-model", SchoolStage.JUNIOR_MIDDLE, "离散数量表示", "比较画面中的数量和符号表达", "lesson_count_to_five", 18, "n"),
        LearningLesson("junior-geometry-feature", SchoolStage.JUNIOR_MIDDLE, "几何特征观察", "找出边、角和曲线的区别", "lesson_shape_home", 18, "∠"),
        LearningLesson("senior-visual-communication", SchoolStage.SENIOR_HIGH, "视觉信息如何传达", "分析颜色变化如何帮助读者理解", "lesson_color_garden", 18, "信息"),
        LearningLesson("senior-sequence-model", SchoolStage.SENIOR_HIGH, "序列模型入门", "把画面数量写成一个简单序列", "lesson_count_to_five", 18, "aₙ"),
        LearningLesson("senior-abstraction", SchoolStage.SENIOR_HIGH, "从图形到抽象", "说明图形特征如何被抽象为规则", "lesson_shape_home", 18, "模型"),
    )

    fun forStage(stage: SchoolStage): List<LearningLesson> = lessons.filter { it.schoolStage == stage }

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
    val learningRewardPolicy: LearningRewardPolicy = LearningRewardPolicy(),
    val savings: List<LocalSavingGoal> = emptyList(),
    val wishes: List<LocalWish> = emptyList(),
    val fund: LocalFundPosition = LocalFundPosition(),
    val usage: UsagePolicy = UsagePolicy(),
    val experience: ChildExperienceSettings = ChildExperienceSettings(),
)

fun money(value: String): BigDecimal =
    value.trim().toBigDecimalOrNull()?.setScale(2, RoundingMode.HALF_UP)
        ?: throw FamilyRuleException("请输入有效金额")

class FamilyRuleException(message: String) : IllegalArgumentException(message)
