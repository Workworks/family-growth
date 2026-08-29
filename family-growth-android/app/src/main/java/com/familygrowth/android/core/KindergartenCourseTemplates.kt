package com.familygrowth.android.core

enum class KindergartenAgeBand(val apiValue: String, val label: String) {
    SHARED_3_4("SHARED_3_4", "3–4 岁 · 一起体验"),
    TRANSITION_5_6("TRANSITION_5_6", "5–6 岁 · 准备过渡"),
}

enum class KindergartenDomain(val apiValue: String, val label: String) {
    HEALTH("HEALTH", "健康"),
    LANGUAGE("LANGUAGE", "语言"),
    SOCIAL("SOCIAL", "社会"),
    SCIENCE("SCIENCE", "科学"),
    ARTS("ARTS", "艺术"),
}

data class KindergartenCourseTemplate(
    val id: String,
    val ageBand: KindergartenAgeBand,
    val domain: KindergartenDomain,
    val courseTitle: String,
    val lessonTitle: String,
    val lessonSummary: String,
    val activityType: String,
    val activityTitle: String,
    val adultGuide: String,
    val childAction: String,
    val expectedMinutes: Int,
) {
    val rightsBasis = "Family Growth 原创亲子活动 · KG-PACK-1.0.0"
}

object KindergartenCourseTemplates {
    val all: List<KindergartenCourseTemplate> = listOf(
        template("kg34-health-stretch", KindergartenAgeBand.SHARED_3_4, KindergartenDomain.HEALTH,
            "动物伸伸腰", "像小动物一样动一动", "在安全空地模仿三个温和动作。", "OFFLINE_PRACTICE", "一起伸伸腰",
            "清出一块不滑的空地，和孩子各选一个动物动作；不比较动作标准。", "像小猫伸腰，再像小鸟张开手臂。做完告诉家长哪里暖暖的。", 5),
        template("kg34-language-picture", KindergartenAgeBand.SHARED_3_4, KindergartenDomain.LANGUAGE,
            "一页图画说给我听", "看看这一页", "亲子共看一页熟悉的图画书并说出看到的事物。", "PARENT_CHILD_READING", "一起看一页",
            "让孩子选家里的一本图画书，只看一页；等待孩子先指或说，再用短句回应。", "选一页你喜欢的图，指给家长看，再说一个你看见的东西。", 5),
        template("kg34-social-blocks", KindergartenAgeBand.SHARED_3_4, KindergartenDomain.SOCIAL,
            "轮流放三块积木", "你一块，我一块", "用三块安全积木体验等待和轮流。", "OFFLINE_PRACTICE", "轮流搭一搭",
            "准备三块大积木，先示范“轮到你了”，孩子等待困难时用手势提示，不催促。", "和家长轮流放积木。家长一块，你一块，一起放完三块。", 5),
        template("kg34-science-leaves", KindergartenAgeBand.SHARED_3_4, KindergartenDomain.SCIENCE,
            "找两片不一样的叶子", "看看哪里不一样", "在现实环境观察两片叶子的颜色、大小或形状。", "OFFLINE_PRACTICE", "找两片叶子",
            "陪孩子在安全地点找落叶，不采摘植物；孩子只要指出一个不同就回应他的发现。", "和家长找两片落叶。看看它们哪里不一样，再指给家长看。", 6),
        template("kg34-arts-sound-line", KindergartenAgeBand.SHARED_3_4, KindergartenDomain.ARTS,
            "听声音画一条线", "声音变成线", "把生活中的一个声音用线条自由表现。", "OFFLINE_PRACTICE", "听一听，画一画",
            "准备纸和粗蜡笔，发出轻柔的拍手或哼唱声；不示范唯一画法。", "听家长发出一个声音，让蜡笔跟着声音在纸上走。", 6),
        template("kg56-health-route", KindergartenAgeBand.TRANSITION_5_6, KindergartenDomain.HEALTH,
            "设计三步小路线", "安全走过三站", "用家中安全物品设计包含跨、绕、停的短路线。", "OFFLINE_PRACTICE", "走一条小路线",
            "确认地面防滑、物品柔软且路线无尖角；请孩子决定三站顺序，累了立即停止。", "选三个安全地点，设计“跨过去、绕一圈、停一下”的路线，再走给家长看。", 7),
        template("kg56-language-ending", KindergartenAgeBand.TRANSITION_5_6, KindergartenDomain.LANGUAGE,
            "给故事换一个结尾", "如果后来不一样", "亲子共读后用自己的话补充一个简短结尾。", "PARENT_CHILD_READING", "换个故事结尾",
            "读一本熟悉故事的最后一页前停下，问“后来还可能发生什么”，接纳一句或动作表达。", "和家长读一个熟悉的故事。到最后一页前停下，说说你想要的新结尾。", 7),
        template("kg56-social-tidy", KindergartenAgeBand.TRANSITION_5_6, KindergartenDomain.SOCIAL,
            "一起商量收纳规则", "给玩具找个家", "共同商量一条可执行的玩具收纳规则。", "ORAL_RESPONSE", "商量一条规则",
            "提供两个现实可行的收纳位置，让孩子表达选择；规则由双方同意，不用惩罚威胁。", "看看两种收玩具的方法，选一种，再告诉家长为什么这样放。", 6),
        template("kg56-science-roll", KindergartenAgeBand.TRANSITION_5_6, KindergartenDomain.SCIENCE,
            "哪个物体更容易滚", "先猜，再试试看", "比较两个安全物体在缓坡上的滚动表现。", "OFFLINE_PRACTICE", "猜猜谁会滚",
            "用书和纸板做低缓坡，准备球和方盒；先问孩子的猜想，再一起试，不强调答对。", "先猜球和方盒谁更容易滚，再和家长放到小斜坡上试一试。", 8),
        template("kg56-arts-home", KindergartenAgeBand.TRANSITION_5_6, KindergartenDomain.ARTS,
            "用三种材料拼个小家", "材料变成小房子", "使用家中安全材料进行开放式拼贴或搭建。", "OFFLINE_PRACTICE", "拼一个小家",
            "准备纸、布和大积木等无尖角材料，让孩子决定用途；作品没有标准答案。", "选三种安全材料，拼一个你想象的小家，再带家长参观。", 8),
    )

    fun find(ageBand: KindergartenAgeBand, domain: KindergartenDomain): KindergartenCourseTemplate =
        all.single { it.ageBand == ageBand && it.domain == domain }

    private fun template(
        id: String,
        ageBand: KindergartenAgeBand,
        domain: KindergartenDomain,
        courseTitle: String,
        lessonTitle: String,
        lessonSummary: String,
        activityType: String,
        activityTitle: String,
        adultGuide: String,
        childAction: String,
        expectedMinutes: Int,
    ) = KindergartenCourseTemplate(id, ageBand, domain, courseTitle, lessonTitle, lessonSummary,
        activityType, activityTitle, adultGuide, childAction, expectedMinutes)
}
