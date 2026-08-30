package com.familygrowth.android.core

enum class PrimarySubject(val apiValue: String, val label: String) {
    CHINESE("CHINESE", "语文"), MATH("MATH", "数学"), ENGLISH("ENGLISH", "英语"), SCIENCE("SCIENCE", "科学")
}

data class PrimaryCourseTemplate(
    val id: String,
    val band: PrimaryGradeBand,
    val subject: PrimarySubject,
    val courseTitle: String,
    val lessonTitle: String,
    val goal: String,
    val activityType: String,
    val activityTitle: String,
    val instruction: String,
    val realityExit: String,
    val expectedMinutes: Int,
) { val rightsBasis = "Family Growth 原创小学活动 · PRIMARY-PACK-1.0.0 · $id" }

object PrimaryCourseTemplates {
    val all = listOf(
        item("lp-cn-read", PrimaryGradeBand.LOWER_PRIMARY, PrimarySubject.CHINESE, "读一句，找线索", "谁在做什么", "从短句中找到人物和行动", "PARENT_CHILD_READING", "读给家长听", "读两遍短句，用手指分别指出‘谁’和‘做什么’，再用自己的话说一次。", "合上屏幕，读一行家里的儿童读物给家长听。", 8),
        item("lp-math-share", PrimaryGradeBand.LOWER_PRIMARY, PrimarySubject.MATH, "分一分，说方法", "把十二颗豆子分组", "用实物理解等分与余数", "OFFLINE_PRACTICE", "动手分一分", "准备 12 颗豆子或积木，分别每 2 个、3 个、5 个分一组，说说有没有剩下。", "把材料收好，并告诉家长哪种分法没有剩余。", 10),
        item("lp-en-say", PrimaryGradeBand.LOWER_PRIMARY, PrimarySubject.ENGLISH, "看一看，说一句", "This is my…", "用一个完整短句描述熟悉物品", "ORAL_RESPONSE", "说给家长听", "选身边一个安全物品，用 ‘This is my …’ 说一句；不追求口音完全一致。", "离开屏幕，再找一个物品说给家长听。", 7),
        item("lp-sci-shadow", PrimaryGradeBand.LOWER_PRIMARY, PrimarySubject.SCIENCE, "影子会去哪儿", "观察光和影", "通过安全观察比较影子方向", "OFFLINE_PRACTICE", "画下影子方向", "白天和家长在安全位置观察一个固定物体的影子，用箭头画下方向；不要直视太阳。", "一小时后再看一次，告诉家长哪里变了。", 10),
        item("up-cn-view", PrimaryGradeBand.UPPER_PRIMARY, PrimarySubject.CHINESE, "观点要有证据", "从一段话找依据", "区分观点和支持它的文本证据", "PARENT_CHILD_READING", "标出依据", "读家里一本适龄读物的一小段，写下一句自己的观点，再抄出一句支持它的原文。", "合上屏幕，把观点和依据讲给家长听。", 12),
        item("up-math-estimate", PrimaryGradeBand.UPPER_PRIMARY, PrimarySubject.MATH, "先估再验证", "测量桌面周长", "用估算、测量和误差解释解决问题", "OFFLINE_PRACTICE", "估一估再量", "先估计桌面周长，再用尺或软线测量；记录估计值、实测值和差值。", "收好工具，用一句话解释误差可能来自哪里。", 12),
        item("up-en-summary", PrimaryGradeBand.UPPER_PRIMARY, PrimarySubject.ENGLISH, "三句小摘要", "First, Then, Finally", "按顺序组织三个英文短句", "ORAL_RESPONSE", "说三句摘要", "选今天做过的一件事，用 First、Then、Finally 各说一个短句；可以先写关键词。", "不看屏幕，再完整讲给家长听一次。", 10),
        item("up-sci-filter", PrimaryGradeBand.UPPER_PRIMARY, PrimarySubject.SCIENCE, "比较过滤材料", "哪一种过滤更清楚", "控制变量并用观察证据解释结果", "OFFLINE_PRACTICE", "做安全过滤观察", "在家长陪同下，用清水混少量泥土，分别通过纸巾和干净棉布；不饮用实验液体，只比较清澈程度。", "清理材料并洗手，再说明哪些条件保持相同。", 15),
    )

    fun find(band: PrimaryGradeBand, subject: PrimarySubject) = all.single { it.band == band && it.subject == subject }
    private fun item(id:String, band:PrimaryGradeBand, subject:PrimarySubject, courseTitle:String, lessonTitle:String,
                     goal:String, activityType:String, activityTitle:String, instruction:String, realityExit:String,
                     expectedMinutes:Int) = PrimaryCourseTemplate(id,band,subject,courseTitle,lessonTitle,goal,activityType,
        activityTitle,instruction,realityExit,expectedMinutes)
}
