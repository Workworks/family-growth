package com.familygrowth.android.core

import java.net.URI

data class OfficialLearningSelection(
    val stage: SchoolStage,
    val grade: String,
    val subject: String,
    val edition: String,
    val volume: String,
)

object SmartEduOfficialSource {
    const val BASE_URL = "https://basic.smartedu.cn/syncClassroom"
    const val VERIFIED_PRIMARY_ONE_CHINESE_URL =
        "https://basic.smartedu.cn/syncClassroom?defaultTag=e7bbb2de-0590-11ed-9c79-92fc3b3249d5%2Fe7bbd296-0590-11ed-9c79-92fc3b3249d5%2F6a749654-0772-11ed-ac74-092ab92074e6%2F44bee8bc-54e6-11ed-9c34-850ba61fa9f4%2Fff8080814371757b014390f883db0453%2F5136342961"

    fun grades(stage: SchoolStage): List<String> = when (stage) {
        SchoolStage.PRIMARY -> listOf("一年级", "二年级", "三年级", "四年级", "五年级", "六年级")
        SchoolStage.JUNIOR_MIDDLE -> listOf("七年级", "八年级", "九年级")
        SchoolStage.SENIOR_HIGH -> listOf("高一", "高二", "高三")
        SchoolStage.KINDERGARTEN -> listOf("幼儿园")
        SchoolStage.PARENT_ONLY -> emptyList()
    }

    fun subjects(stage: SchoolStage): List<String> = when (stage) {
        SchoolStage.PRIMARY -> listOf("语文", "数学", "英语", "道德与法治", "科学", "艺术音乐", "艺术美术", "体育与健康", "劳动与技术")
        SchoolStage.JUNIOR_MIDDLE -> listOf("语文", "数学", "英语", "道德与法治", "物理", "化学", "生物学", "历史", "地理", "体育与健康")
        SchoolStage.SENIOR_HIGH -> listOf("语文", "数学", "英语", "思想政治", "物理", "化学", "生物学", "历史", "地理", "信息技术")
        else -> emptyList()
    }

    fun defaultSelection(stage: SchoolStage): OfficialLearningSelection = OfficialLearningSelection(
        stage = stage,
        grade = grades(stage).firstOrNull().orEmpty(),
        subject = subjects(stage).firstOrNull().orEmpty(),
        edition = "统编版",
        volume = "上册",
    )

    fun launchUrl(selection: OfficialLearningSelection): String =
        if (selection.stage == SchoolStage.PRIMARY && selection.grade == "一年级" &&
            selection.subject == "语文" && selection.edition == "统编版" && selection.volume == "上册"
        ) VERIFIED_PRIMARY_ONE_CHINESE_URL else BASE_URL

    fun hasVerifiedDeepLink(selection: OfficialLearningSelection): Boolean = launchUrl(selection) != BASE_URL

    fun isAllowedNavigation(rawUrl: String): Boolean = runCatching {
        val uri = URI(rawUrl)
        val path = uri.path.orEmpty()
        uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals("basic.smartedu.cn", ignoreCase = true) &&
            (path == "/syncClassroom" || path.startsWith("/syncClassroom/") ||
                path.startsWith("/resource-detail") || path.startsWith("/courseDetail") ||
                path.startsWith("/tResource/"))
    }.getOrDefault(false)
}
