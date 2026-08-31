package com.familygrowth.android.remote

import java.time.LocalDate
import java.time.Period
import com.familygrowth.android.core.PrimaryGradeBand

data class PrimaryLearningFacts(
    val toDo: Int,
    val inProgress: Int,
    val rework: Int,
    val completed: Int,
)

object PrimaryLearningPolicy {
    fun bandFor(effectiveBand: PrimaryGradeBand?, birthDate: String, today: LocalDate = LocalDate.now()): PrimaryGradeBand =
        effectiveBand ?: bandFor(birthDate, today)

    fun bandFor(birthDate: String, today: LocalDate = LocalDate.now()): PrimaryGradeBand =
        runCatching {
            val date = LocalDate.parse(birthDate)
            require(!date.isAfter(today))
            if (Period.between(date, today).years <= 8) PrimaryGradeBand.LOWER_PRIMARY
            else PrimaryGradeBand.UPPER_PRIMARY
        }.getOrDefault(PrimaryGradeBand.LOWER_PRIMARY)

    fun facts(assignment: RemoteLearningAssignment): PrimaryLearningFacts {
        val completed = assignment.activities.count(RemoteLearningActivity::childReady)
        val unfinished = (assignment.activities.size - completed).coerceAtLeast(0)
        val actionable = assignment.status in setOf("ASSIGNED", "IN_PROGRESS", "REWORK_REQUIRED")
        val inProgress = if (actionable && unfinished > 0) 1 else 0
        val wrong = assignment.activities.count { it.checkedCorrect == false }
        val rework = if (assignment.status == "REWORK_REQUIRED" && wrong == 0) 1 else wrong
        return PrimaryLearningFacts(
            toDo = (unfinished - inProgress).coerceAtLeast(0),
            inProgress = inProgress,
            rework = rework,
            completed = completed,
        )
    }

    fun subjectLabel(subjectCode: String): String = when (subjectCode.uppercase()) {
        "CHINESE", "LANGUAGE" -> "语文"
        "MATH", "MATHEMATICS" -> "数学"
        "ENGLISH" -> "英语"
        "SCIENCE" -> "科学"
        "PHYSICS" -> "物理"
        "CHEMISTRY" -> "化学"
        "BIOLOGY" -> "生物"
        "HISTORY" -> "历史"
        "GEOGRAPHY" -> "地理"
        "MORAL", "ETHICS" -> "道德与法治"
        "ART", "ARTS", "MUSIC" -> "艺术"
        "PE", "HEALTH" -> "体育与健康"
        "LABOR", "TECHNOLOGY" -> "劳动与技术"
        "FAMILY" -> "家庭探索"
        else -> "综合学习"
    }

    fun helpText(activity: RemoteLearningActivity): String = when {
        activity.hint.isNotBlank() -> "先停一下，看看提示：${activity.hint}。还是不明白就请家长一起读题。"
        activity.type == "SHORT_VIDEO" -> "先暂停播放，说出哪一段没看懂，再请家长一起回看这一小段。"
        else -> "先停一下，用自己的话说出卡住的地方，再请家长一起读一遍。"
    }
}
