package com.familygrowth.android.core

enum class SeniorSubject(val apiValue:String,val label:String,val defaultModule:String) {
    CHINESE("CHINESE","语文","REQUIRED"), MATH("MATH","数学","REQUIRED"), ENGLISH("ENGLISH","英语","REQUIRED"),
    PHYSICS("PHYSICS","物理","SELECTIVE_REQUIRED"), HISTORY("HISTORY","历史","SELECTIVE_REQUIRED"),
    INFORMATION_TECHNOLOGY("INFORMATION_TECHNOLOGY","信息技术","ELECTIVE")
}

data class SeniorCourseTemplate(
    val id:String,val subject:SeniorSubject,val courseTitle:String,val lessonTitle:String,
    val topicTitle:String,val inquiryQuestion:String,val expectedEvidence:String,
    val instruction:String,val safetyNote:String,val expectedMinutes:Int,
) { val rightsBasis="Family Growth 原创高中研究活动 · SENIOR-PACK-1.0.0 · $id" }

object SeniorCourseTemplates {
    val all=listOf(
        item("sr-cn-argument",SeniorSubject.CHINESE,"论证研究室","证据怎样支撑判断","论证与证据",
            "同一材料能支持哪些不同判断？","一页论证图：判断、两条依据、一个限制条件",
            "从教材或家长审核读物选一段材料，画出判断与证据的连接，并写明这份证据不能说明什么。","只使用教材、家庭藏书或家长审核来源；引用注明出处。",30),
        item("sr-math-model",SeniorSubject.MATH,"数学建模桌","用函数描述现实变化","函数模型",
            "一个简单模型在哪些条件下才成立？","数据表、函数表达或图像，以及对适用边界的说明",
            "选择一个安全可测的日常变量关系，记录至少五组数据，提出模型并用一组新数据检查。","不以健康、体重或同伴数据为研究对象；测量不妨碍现实活动。",35),
        item("sr-en-source",SeniorSubject.ENGLISH,"英文信息核验室","Compare two explanations","Source comparison",
            "How do two approved sources frame the same topic differently?","A short comparison with two cited details and one unanswered question",
            "Read two parent-approved short texts on the same topic. Note each main claim, one supporting detail, and one question left open.","Do not open comments, recommendations, advertisements, or unapproved links.",30),
        item("sr-physics-error",SeniorSubject.PHYSICS,"物理测量台","测量为何总有差异","测量与误差",
            "重复测量的差异可能从哪里来？","至少五次测量、平均值、误差来源和改进方案",
            "选择低风险静态对象进行长度或周期测量，重复记录并解释差异；先写安全边界再开始。","不用电源、明火、高处、道路或高速运动物体；不确定时停止并请家长。",35),
        item("sr-history-context",SeniorSubject.HISTORY,"历史材料档案室","把材料放回时代语境","史料与语境",
            "材料的作者、对象和时代怎样影响它能说明的问题？","材料信息卡、可确认事实、合理推断与仍未知之处",
            "从教材选一则史料，分别记录来源信息、直接事实、推断和无法仅凭该材料回答的问题。","只使用教材和家长审核资料；不把单一材料当作完整结论。",30),
        item("sr-it-data",SeniorSubject.INFORMATION_TECHNOLOGY,"数据表达工作台","图表会怎样影响理解","数据与可视化",
            "同一组数据用不同图表呈现时，读者感受为何不同？","两种图表草图、选择理由和可能误导点",
            "使用不含个人信息的自建小数据集，画两种图表并说明坐标、比例和取舍。","不采集同伴资料、位置或账号信息；不上传家庭数据。",30),
    )
    fun find(subject:SeniorSubject)=all.single{it.subject==subject}
    private fun item(id:String,subject:SeniorSubject,courseTitle:String,lessonTitle:String,topicTitle:String,
        inquiryQuestion:String,expectedEvidence:String,instruction:String,safetyNote:String,expectedMinutes:Int)=
        SeniorCourseTemplate(id,subject,courseTitle,lessonTitle,topicTitle,inquiryQuestion,expectedEvidence,instruction,safetyNote,expectedMinutes)
}
