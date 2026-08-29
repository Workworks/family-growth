package com.familygrowth.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AssignmentTurnedIn
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.familygrowth.android.core.FamilyAppViewModel
import com.familygrowth.android.core.KindergartenAgeBand
import com.familygrowth.android.core.KindergartenCourseTemplates
import com.familygrowth.android.core.KindergartenDomain
import com.familygrowth.android.core.SchoolStage
import com.familygrowth.android.remote.ConnectionState
import com.familygrowth.android.remote.LearningActionState

@Composable
fun ParentTeachingStudio(viewModel: FamilyAppViewModel) {
    var showCreate by remember { mutableStateOf(false) }
    val canCreate = viewModel.connectionState is ConnectionState.Connected &&
        viewModel.state.experience.effectiveStage != SchoolStage.PARENT_ONLY && !viewModel.teachingActionRunning
    GrowthCard {
        SectionTitle("家庭备课夹", "写一课 → 发布 → 布置") {
            Button(onClick = { showCreate = true }, enabled = canCreate) {
                Icon(Icons.Rounded.Book, null); Spacer(Modifier.width(6.dp)); Text("写一课")
            }
        }
        LearningOutboxParentStatus(viewModel)
        if (viewModel.teachingActionRunning) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (viewModel.state.experience.effectiveStage == SchoolStage.PARENT_ONLY) {
            Text("0–2 岁只由家长记录现实成长，不向孩子布置屏幕课程。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (viewModel.connectionState !is ConnectionState.Connected) {
            Text("连接家庭服务后，可以把一节真实课程保存为草稿。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (viewModel.teachingCourses.isEmpty()) {
            EmptyInvitation("▤", "备课夹还是空的", "先写一节短课。发布前它只对家长可见。")
        } else {
            viewModel.teachingCourses.takeLast(6).reversed().forEach { course ->
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = if (course.status == "DRAFT") ChildColors.Sun.copy(alpha = .13f) else ChildColors.Mist.copy(alpha = .65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(course.title, style = MaterialTheme.typography.titleLarge, color = ChildColors.Ink)
                                Text("${stageLabel(course.schoolStage)} · ${course.subjectCode} · 第 ${course.versionNumber} 版",
                                    fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            SuggestionChip(onClick = {}, enabled = false, label = { Text(if (course.status == "DRAFT") "草稿" else "已发布") })
                        }
                        if (course.status == "DRAFT") {
                            Button(onClick = { viewModel.publishTeachingCourse(course.versionId) }, Modifier.fillMaxWidth().heightIn(min = 50.dp), enabled = !viewModel.teachingActionRunning) {
                                Icon(Icons.Rounded.Publish, null); Spacer(Modifier.width(7.dp)); Text("发布这一版")
                            }
                        } else {
                            OutlinedButton(onClick = { viewModel.assignTeachingCourse(course.versionId) }, Modifier.fillMaxWidth().heightIn(min = 50.dp), enabled = !viewModel.teachingActionRunning) {
                                Icon(Icons.Rounded.AssignmentTurnedIn, null); Spacer(Modifier.width(7.dp)); Text("布置给当前孩子")
                            }
                        }
                    }
                }
            }
        }
        Text("已发布内容不可直接改写；完整多课节和题库编辑将在后续课程工作台提供。",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (showCreate) CreateTeachingCourseDialog(viewModel) { showCreate = false }
}

@Composable
fun ChildLearningSyncStatus(viewModel: FamilyAppViewModel) {
    val pending = viewModel.pendingLearningActions
    if (pending.isEmpty()) return
    val needsParent = pending.any { it.state == LearningActionState.NEEDS_REVIEW }
    Surface(shape = MaterialTheme.shapes.large, color = if (needsParent) ChildColors.Sun.copy(alpha=.22f) else ChildColors.Mist.copy(alpha=.7f)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(if (needsParent) Icons.Rounded.CloudOff else Icons.Rounded.CloudDone, null, tint = ChildColors.Moss)
            Column {
                Text(if (needsParent) "请家长帮忙保存" else "这一步已经安全记下", fontWeight = FontWeight.Bold, color = ChildColors.Ink)
                Text(if (needsParent) "记录没有丢，家长刷新后会继续。" else "网络回来后会自动同步。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LearningOutboxParentStatus(viewModel: FamilyAppViewModel) {
    val actions = viewModel.pendingLearningActions
    if (actions.isEmpty()) return
    val review = actions.filter { it.state == LearningActionState.NEEDS_REVIEW }
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.55f)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("本机待同步 ${actions.size} 项${if (review.isNotEmpty()) " · ${review.size} 项需要处理" else ""}", fontWeight = FontWeight.Bold)
            Text("内容已用设备密钥加密；PIN 和 Token 不在队列中。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::retryLearningSync, enabled = viewModel.connectionState is ConnectionState.Connected) { Text("刷新并合并") }
                if (review.isNotEmpty()) TextButton(onClick = { viewModel.discardLearningAction(review.first().idempotencyKey) }) { Text("移除首条冲突") }
            }
            review.firstOrNull()?.lastError?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun CreateTeachingCourseDialog(viewModel: FamilyAppViewModel, dismiss: () -> Unit) {
    if (viewModel.state.experience.effectiveStage == SchoolStage.KINDERGARTEN) {
        CreateKindergartenTemplateDialog(viewModel, dismiss)
        return
    }
    CreateGeneralTeachingCourseDialog(viewModel, dismiss)
}

@Composable
private fun CreateKindergartenTemplateDialog(viewModel: FamilyAppViewModel, dismiss: () -> Unit) {
    var ageBand by remember { mutableStateOf(KindergartenAgeBand.SHARED_3_4) }
    var domain by remember { mutableStateOf(KindergartenDomain.HEALTH) }
    val template = KindergartenCourseTemplates.find(ageBand, domain)
    AlertDialog(
        onDismissRequest = dismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("选一个今天的亲子活动")
                Text("幼儿园 · 原创内容包 KG-PACK-1.0.0", style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("年龄带", style=MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp), verticalArrangement=Arrangement.spacedBy(7.dp)) {
                    KindergartenAgeBand.entries.forEach { value ->
                        FilterChip(selected=ageBand==value, onClick={ ageBand=value }, label={ Text(value.label) })
                    }
                }
                Text("发展领域", style=MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp), verticalArrangement=Arrangement.spacedBy(7.dp)) {
                    KindergartenDomain.entries.forEach { value ->
                        FilterChip(selected=domain==value, onClick={ domain=value }, label={ Text(value.label) })
                    }
                }
                Surface(shape=MaterialTheme.shapes.large, color=MaterialTheme.colorScheme.surface,
                    border=BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(template.courseTitle, style=MaterialTheme.typography.titleLarge, fontWeight=FontWeight.Bold)
                                Text("${template.domain.label} · ${template.expectedMinutes} 分钟 · 现实亲子活动",
                                    style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(shape=MaterialTheme.shapes.small, color=MaterialTheme.colorScheme.tertiaryContainer) {
                                Text("陪伴折页", Modifier.padding(horizontal=10.dp, vertical=6.dp),
                                    style=MaterialTheme.typography.labelMedium)
                            }
                        }
                        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            FoldoutSide("给家长", template.adultGuide, Modifier.weight(1f))
                            FoldoutSide("和孩子去做", template.childAction, Modifier.weight(1f))
                        }
                        Text("完成后回来记一句你看到的行动或表达。", style=MaterialTheme.typography.bodySmall,
                            color=MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick={ viewModel.createKindergartenTeachingCourse(template); dismiss() }) {
                Text("使用这个亲子活动")
            }
        },
        dismissButton = { TextButton(onClick=dismiss) { Text("取消") } },
    )
}

@Composable
private fun FoldoutSide(title:String, body:String, modifier:Modifier=Modifier) {
    Column(modifier, verticalArrangement=Arrangement.spacedBy(5.dp)) {
        Text(title, style=MaterialTheme.typography.labelLarge, color=MaterialTheme.colorScheme.primary)
        Text(body, style=MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CreateGeneralTeachingCourseDialog(viewModel: FamilyAppViewModel, dismiss: () -> Unit) {
    var courseTitle by remember { mutableStateOf("") }
    var lessonTitle by remember { mutableStateOf("") }
    var lessonSummary by remember { mutableStateOf("") }
    var activityTitle by remember { mutableStateOf("") }
    var instruction by remember { mutableStateOf("") }
    var template by remember { mutableStateOf("PARENT_CHILD_READING") }
    val templates = listOf(
        "PARENT_CHILD_READING" to "亲子共读",
        "OFFLINE_PRACTICE" to "现实行动",
        "SHORT_VIDEO" to "原创短片",
    )
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Column { Text("写一节家庭短课"); Text("当前学段：${viewModel.state.experience.effectiveStage.displayName()}", style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant) } },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LabeledField(courseTitle, { courseTitle=it.take(160) }, "课程名，例如：周末自然观察")
                LabeledField(lessonTitle, { lessonTitle=it.take(160) }, "课节名，例如：找到三种叶子")
                LabeledField(lessonSummary, { lessonSummary=it.take(500) }, "孩子会做什么", singleLine=false)
                Text("选择一种活动", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    templates.forEach { (value,label) -> FilterChip(selected=template==value,onClick={template=value},label={Text(label)}) }
                }
                LabeledField(activityTitle, { activityTitle=it.take(160) }, "活动标题")
                LabeledField(instruction, { instruction=it.take(500) }, "给孩子的一句短指令", singleLine=false)
                if (template == "SHORT_VIDEO") Text("使用安装包内已审核的《颜色花园》；观看只记 VIEWED。", style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(onClick = {
                viewModel.createTeachingCourse(courseTitle,lessonTitle,lessonSummary,template,activityTitle,instruction,
                    if(template=="SHORT_VIDEO")"lesson_color_garden" else null)
                dismiss()
            }, enabled = listOf(courseTitle,lessonTitle,lessonSummary,activityTitle,instruction).all(String::isNotBlank)) { Text("保存草稿") }
        },
        dismissButton = { TextButton(onClick=dismiss) { Text("取消") } },
    )
}

private fun stageLabel(stage:String):String = runCatching { com.familygrowth.android.core.SchoolStage.valueOf(stage).displayName() }.getOrDefault(stage)
