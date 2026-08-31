package com.familygrowth.android.core

enum class JuniorSubject(val apiValue:String,val label:String) {
    CHINESE("CHINESE","语文"), MATH("MATH","数学"), ENGLISH("ENGLISH","英语"),
    PHYSICS("PHYSICS","物理"), BIOLOGY("BIOLOGY","生物"), HISTORY("HISTORY","历史")
}

data class JuniorCourseTemplate(
    val id:String,
    val subject:JuniorSubject,
    val courseTitle:String,
    val chapterTitle:String,
    val lessonTitle:String,
    val knowledgePoints:List<String>,
    val learningGoal:String,
    val activityTitle:String,
    val instruction:String,
    val safetyNote:String,
    val expectedMinutes:Int,
) { val rightsBasis="Family Growth 原创初中活动 · JUNIOR-PACK-1.0.0 · $id" }

object JuniorCourseTemplates {
    val all=listOf(
        item("jr-cn-evidence",JuniorSubject.CHINESE,"文本证据实验台","说明文的证据","观点怎样站得住",
            listOf("观点","文本证据","解释"),"用两处原文证据支持一个可讨论的观点","建立证据卡",
            "从家里的适龄读物选一小段，写下一个观点、两处原文依据，并说明每处依据怎样支持观点。","只使用家庭已有的适龄纸质或已审核数字读物。",20),
        item("jr-math-linear",JuniorSubject.MATH,"变量关系实验台","一次函数","从表格到图像",
            listOf("变量","对应值","变化趋势"),"用表格和图像解释两个量的变化关系","记录并解释关系",
            "任选一个可安全测量的日常关系，例如时间与步数；记录至少四组数据，在纸上画图并写一句趋势解释。","在平整安全区域测量；不边走边看屏幕。",22),
        item("jr-en-summary",JuniorSubject.ENGLISH,"信息重组实验台","段落结构","Main idea and evidence",
            listOf("main idea","supporting detail","summary"),"用自己的英文短句概括主旨和两条信息","制作三句摘要",
            "选一段家中适龄英文材料，写一句 main idea 和两句 supporting details，再遮住原文复述。","不进入开放网页；只使用家长审核的材料。",18),
        item("jr-physics-motion",JuniorSubject.PHYSICS,"运动证据实验台","运动和速度","怎样描述运动变化",
            listOf("路程","时间","平均速度"),"用实测数据解释同一路程下时间与速度的关系","安全测量运动",
            "在家长知情的空旷平整区域，用相同步行路程记录三次时间，计算平均值并解释误差来源。","只步行，不奔跑；远离道路、楼梯和湿滑地面。",20),
        item("jr-bio-leaf",JuniorSubject.BIOLOGY,"分类证据实验台","生物的共同特征","用特征完成分类",
            listOf("观察特征","分类标准","差异"),"用可观察特征建立并检验一个分类标准","比较安全自然物",
            "收集三种已经掉落且确认安全的叶片，只观察形状、叶脉和边缘；先提出分类标准，再检查是否适用。","不采摘、不品尝、不接触不认识或可能引起过敏的植物。",18),
        item("jr-history-source",JuniorSubject.HISTORY,"史料判断实验台","材料与结论","区分材料和推断",
            listOf("史料","事实陈述","推断"),"区分材料直接说明的事实与基于材料作出的推断","做材料双栏卡",
            "从教材或家中适龄历史读物选一小段，左栏写材料直接说明的三点，右栏写一个推断并标明依据。","只使用教材、家庭藏书或家长审核的来源。",20),
    )
    fun find(subject:JuniorSubject)=all.single{it.subject==subject}
    private fun item(id:String,subject:JuniorSubject,courseTitle:String,chapterTitle:String,lessonTitle:String,
        knowledgePoints:List<String>,learningGoal:String,activityTitle:String,instruction:String,safetyNote:String,
        expectedMinutes:Int)=JuniorCourseTemplate(id,subject,courseTitle,chapterTitle,lessonTitle,knowledgePoints,
        learningGoal,activityTitle,instruction,safetyNote,expectedMinutes)
}
