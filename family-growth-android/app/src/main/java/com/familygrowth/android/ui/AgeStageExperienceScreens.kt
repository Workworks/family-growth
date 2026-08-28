package com.familygrowth.android.ui

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.familygrowth.android.core.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private object StageColors {
    val KindergartenPaper = Color(0xFFFFFDF6)
    val KindergartenLeaf = Color(0xFF4F8A65)
    val KindergartenSun = Color(0xFFF3BB45)
    val PrimaryBlue = Color(0xFF355C7D)
    val PrimaryPaper = Color(0xFFF8FBF4)
    val JuniorInk = Color(0xFF294C53)
    val JuniorMist = Color(0xFFDDE8EA)
    val SeniorInk = Color(0xFF263238)
    val SeniorPine = Color(0xFF3E6B62)
}

fun SchoolStage.displayName(): String = when (this) {
    SchoolStage.PARENT_ONLY -> "家长陪同记录"
    SchoolStage.KINDERGARTEN -> "幼儿园"
    SchoolStage.PRIMARY -> "小学"
    SchoolStage.JUNIOR_MIDDLE -> "初中"
    SchoolStage.SENIOR_HIGH -> "高中"
}

fun childNavigationLabel(section: AppSection, stage: SchoolStage): String = when (stage) {
    SchoolStage.PARENT_ONLY -> when (section) { AppSection.TODAY -> "一起"; AppSection.TASKS -> "记录"; else -> "我的" }
    SchoolStage.KINDERGARTEN -> when (section) { AppSection.TODAY -> "今天"; AppSection.TASKS -> "发现"; else -> "我的" }
    SchoolStage.PRIMARY -> when (section) { AppSection.TODAY -> "今天"; AppSection.TASKS -> "学习"; else -> "我的" }
    SchoolStage.JUNIOR_MIDDLE -> when (section) { AppSection.TODAY -> "计划"; AppSection.TASKS -> "学科"; else -> "复盘" }
    SchoolStage.SENIOR_HIGH -> when (section) { AppSection.TODAY -> "目标"; AppSection.TASKS -> "课程"; else -> "复盘" }
}

@Composable
fun rememberChildControlFeedback(settings: ChildExperienceSettings): ((() -> Unit) -> Unit) {
    val haptic = LocalHapticFeedback.current
    return remember(settings.hapticsEnabled, settings.effectiveStage) {
        { action ->
            if (settings.hapticsEnabled && settings.effectiveStage != SchoolStage.PARENT_ONLY) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            action()
        }
    }
}

@Composable
fun AgeStageChildTodayScreen(viewModel: FamilyAppViewModel) {
    when (viewModel.state.experience.effectiveStage) {
        SchoolStage.PARENT_ONLY -> ParentOnlyChildScreen()
        SchoolStage.KINDERGARTEN -> KindergartenStoryStage(viewModel)
        SchoolStage.PRIMARY -> PrimaryExplorationNotebook(viewModel)
        SchoolStage.JUNIOR_MIDDLE -> JuniorSubjectLab(viewModel)
        SchoolStage.SENIOR_HIGH -> SeniorStudyStudio(viewModel)
    }
}

@Composable
private fun ParentOnlyChildScreen() {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.SupervisorAccount, null, Modifier.size(56.dp), tint = ChildColors.Moss)
        Spacer(Modifier.height(16.dp))
        Text("请家长一起", style = MaterialTheme.typography.headlineMedium)
        Text("这个年龄默认由家长陪同记录，不提供孩子独立操作。", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun KindergartenStoryStage(viewModel: FamilyAppViewModel) {
    val task = viewModel.state.tasks.firstOrNull { it.status == TaskStatus.TODO }
    val waiting = viewModel.state.tasks.firstOrNull { it.status == TaskStatus.SUBMITTED }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("今天好。", style = MaterialTheme.typography.displaySmall, color = ChildColors.Ink)
        Text("打开成长舞台，只做一件小事。", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        KindergartenPath(if (task != null) 0 else if (waiting != null) 2 else 1)
        Surface(
            Modifier.fillMaxWidth().weight(1f),
            shape = MaterialTheme.shapes.extraLarge,
            color = StageColors.KindergartenPaper,
            border = BorderStroke(2.dp, ChildColors.Mist),
        ) {
            Column(
                Modifier.fillMaxSize().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Surface(Modifier.size(88.dp), CircleShape, StageColors.KindergartenSun.copy(alpha = .32f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(if (task != null) Icons.Rounded.Spa else Icons.Rounded.WbSunny, null, Modifier.size(48.dp), tint = StageColors.KindergartenLeaf)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(task?.title ?: waiting?.title ?: "今天完成啦", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (task != null) "先去做，做好再回来。" else if (waiting != null) "已经给家长看啦。" else "放下平板，去玩一会儿吧。",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (task != null) {
                    Spacer(Modifier.height(22.dp))
                    StagePrimaryActionButton(viewModel.state.experience, "我做好了，给家长看", Icons.Rounded.CheckCircle) {
                        viewModel.submitTask(task.id)
                    }
                }
            }
        }
        Text("需要帮助时，请家长一起。", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun KindergartenPath(active: Int) {
    val labels = listOf("看看", "去做", "给家长看")
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        labels.forEachIndexed { index, label ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(Modifier.size(46.dp), CircleShape, if (index == active) StageColors.KindergartenSun else ChildColors.Mist) {
                    Box(contentAlignment = Alignment.Center) { Text((index + 1).toString(), fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(5.dp)); Text(label)
            }
            if (index < labels.lastIndex) HorizontalDivider(Modifier.weight(.35f), thickness = 2.dp, color = ChildColors.Mist)
        }
    }
}

@Composable
private fun PrimaryExplorationNotebook(viewModel: FamilyAppViewModel) {
    val task = viewModel.state.tasks.firstOrNull { it.status == TaskStatus.TODO }
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("今天的探索手册", style = MaterialTheme.typography.headlineMedium, color = StageColors.PrimaryBlue)
                Text("读一读、想一想、动手做。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = CircleShape, color = StageColors.PrimaryBlue.copy(alpha = .10f)) {
                Text("小学", Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = StageColors.PrimaryBlue, fontWeight = FontWeight.Bold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("阅读", "数学", "科学").forEach { label ->
                SuggestionChip(onClick = {}, label = { Text(label) }, enabled = false)
            }
        }
        Surface(Modifier.fillMaxWidth(), MaterialTheme.shapes.large, StageColors.PrimaryPaper, border = BorderStroke(1.dp, StageColors.PrimaryBlue.copy(alpha = .25f))) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("本页任务", style = MaterialTheme.typography.labelLarge, color = StageColors.PrimaryBlue)
                Text(task?.title ?: "今天没有待做任务", style = MaterialTheme.typography.headlineSmall)
                listOf("1  看清楚题目", "2  自己试一试", "3  做完再检查").forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
                if (task != null) StagePrimaryActionButton(viewModel.state.experience, "完成并交给家长", Icons.AutoMirrored.Rounded.ArrowForward) { viewModel.submitTask(task.id) }
            }
        }
        Text("探索印章只记录完成的行动，不计算连续签到。", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun JuniorSubjectLab(viewModel: FamilyAppViewModel) {
    val tasks = viewModel.state.tasks.filter { it.status != TaskStatus.APPROVED }.take(3)
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("学科实验台", style = MaterialTheme.typography.headlineMedium, color = StageColors.JuniorInk)
        Text("先安排负担，再处理知识点。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DataPill("待处理", tasks.size.toString(), StageColors.JuniorInk, Modifier.weight(1f))
            DataPill("今日用时", "${viewModel.state.usage.usedMinutes}m", GrowthColors.Amber, Modifier.weight(1f))
        }
        Surface(Modifier.fillMaxWidth(), MaterialTheme.shapes.large, StageColors.JuniorMist.copy(alpha = .65f)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("今日路径", style = MaterialTheme.typography.titleLarge)
                if (tasks.isEmpty()) Text("没有待处理任务，可以整理一次错因。")
                tasks.forEachIndexed { index, task ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}", Modifier.background(StageColors.JuniorInk, CircleShape).padding(horizontal = 10.dp, vertical = 6.dp), color = Color.White)
                        Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(task.title, fontWeight = FontWeight.SemiBold); Text("预计 ${task.minutes} 分钟", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                tasks.firstOrNull()?.let { task -> StagePrimaryActionButton(viewModel.state.experience, "提交当前任务", Icons.Rounded.Check) { viewModel.submitTask(task.id) } }
            }
        }
        GrowthCard { SectionTitle("复盘入口", "没看懂可以标记并请家长协助"); Text("Stage 21 将接入知识点、错因和复做证据。") }
    }
}

@Composable
private fun SeniorStudyStudio(viewModel: FamilyAppViewModel) {
    val task = viewModel.state.tasks.firstOrNull { it.status == TaskStatus.TODO }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("自主学习室", style = MaterialTheme.typography.headlineMedium, color = StageColors.SeniorInk)
        Text("目标、证据、下一行动。", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(Modifier.fillMaxWidth(), MaterialTheme.shapes.medium, StageColors.SeniorInk) {
            Row(Modifier.padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("本周研究板", color = Color.White.copy(alpha = .7f)); Text(task?.title ?: "暂无待办目标", color = Color.White, style = MaterialTheme.typography.headlineSmall) }
                Icon(Icons.Rounded.Route, null, tint = GrowthColors.Amber, modifier = Modifier.size(40.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GrowthCard(Modifier.weight(1f)) { SectionTitle("投入"); Text("${viewModel.state.usage.usedMinutes} 分钟", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleLarge) }
            GrowthCard(Modifier.weight(1f)) { SectionTitle("证据"); Text("${viewModel.state.tasks.count { it.status == TaskStatus.APPROVED }} 项完成", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleLarge) }
        }
        GrowthCard {
            SectionTitle("下一行动", "可以调整计划，不使用倒计时惩罚")
            Text(task?.let { "先完成：${it.title}" } ?: "复盘本周目标和计划偏差。")
            if (task != null) StagePrimaryActionButton(viewModel.state.experience, "提交学习证据", Icons.Rounded.DoneAll) { viewModel.submitTask(task.id) }
        }
    }
}

@Composable
fun StagePrimaryActionButton(
    settings: ChildExperienceSettings,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    action: () -> Unit,
) {
    val context = LocalContext.current
    val reducedMotion = remember {
        runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false)
    }
    val profile = ChildExperiencePolicy.feedbackFor(settings, reducedMotion)
    val haptic = LocalHapticFeedback.current
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    Button(
        onClick = {
            if (busy) return@Button
            busy = true
            scope.launch {
                if (profile.hapticsEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (profile.hapticPulseCount > 1) { delay(70); haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                }
                if (profile.maxAnimationMs > 0) {
                    scale.animateTo(profile.primaryPressScale, tween(profile.maxAnimationMs / 3))
                    scale.animateTo(.98f, tween(profile.maxAnimationMs / 3))
                    scale.animateTo(1f, tween(profile.maxAnimationMs / 3))
                }
                action()
                busy = false
            }
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        enabled = !busy,
        colors = ButtonDefaults.buttonColors(containerColor = when (settings.effectiveStage) {
            SchoolStage.KINDERGARTEN -> StageColors.KindergartenLeaf
            SchoolStage.PRIMARY -> StageColors.PrimaryBlue
            SchoolStage.JUNIOR_MIDDLE -> StageColors.JuniorInk
            SchoolStage.SENIOR_HIGH -> StageColors.SeniorPine
            SchoolStage.PARENT_ONLY -> ChildColors.Moss
        }),
    ) {
        Icon(icon, null); Spacer(Modifier.width(8.dp)); Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun ChildExperienceCard(viewModel: FamilyAppViewModel, edit: () -> Unit) {
    val experience = viewModel.state.experience
    GrowthCard {
        SectionTitle("孩子学习阶段", if (experience.source == ExperienceSource.SERVER) "家庭服务事实源" else "未连接时的本机配置") {
            TextButton(onClick = edit) { Text("调整") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DataPill("当前页面", experience.effectiveStage.displayName(), GrowthColors.Emerald, Modifier.weight(1f))
            DataPill("系统建议", experience.recommendedStage.displayName(), GrowthColors.Amber, Modifier.weight(1f))
        }
        Text("出生日期 ${experience.birthDate} · ${if (experience.hapticsEnabled) "适龄触觉开启" else "适龄触觉关闭"}")
        if (experience.stageOverride != null) Text("家长覆盖：${experience.overrideReason}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("切换学段不会删除既有任务、奖励或学习记录。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ChildExperienceDialog(viewModel: FamilyAppViewModel, dismiss: () -> Unit) {
    val current = viewModel.state.experience
    var birthDate by remember { mutableStateOf(current.birthDate) }
    var override by remember { mutableStateOf(current.stageOverride) }
    var reason by remember { mutableStateOf(current.overrideReason) }
    var haptics by remember { mutableStateOf(current.hapticsEnabled) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("配置孩子学习阶段") },
        text = {
            Column(Modifier.heightIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LabeledField(birthDate, { birthDate = it.take(10) }, "出生日期（YYYY-MM-DD）")
                Text("系统根据年龄给出建议；家长可按实际入学阶段覆盖。", style = MaterialTheme.typography.bodySmall)
                listOf<Pair<SchoolStage?, String>>(
                    null to "跟随系统建议",
                    SchoolStage.KINDERGARTEN to "幼儿园",
                    SchoolStage.PRIMARY to "小学",
                    SchoolStage.JUNIOR_MIDDLE to "初中",
                    SchoolStage.SENIOR_HIGH to "高中",
                ).forEach { (stage, label) ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = override == stage, onClick = { override = stage })
                        Text(label)
                    }
                }
                if (override != null) LabeledField(reason, { reason = it.take(240) }, "覆盖原因")
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("儿童适龄触觉"); Text("可随时关闭；系统设置优先", style = MaterialTheme.typography.bodySmall) }
                    Switch(checked = haptics, onCheckedChange = { haptics = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = { viewModel.updateExperience(birthDate, override, reason, haptics); dismiss() }, enabled = override == null || reason.isNotBlank()) { Text("保存阶段") }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("取消") } },
    )
}
