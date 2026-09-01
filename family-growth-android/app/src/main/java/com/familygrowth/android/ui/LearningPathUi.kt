package com.familygrowth.android.ui

import androidx.core.net.toUri
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SupervisorAccount
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.familygrowth.android.R
import com.familygrowth.android.core.FamilyAppViewModel
import com.familygrowth.android.core.SchoolStage
import com.familygrowth.android.remote.KindergartenActivityPolicy
import com.familygrowth.android.core.PrimaryGradeBand
import com.familygrowth.android.remote.PrimaryLearningPolicy
import com.familygrowth.android.remote.RemoteLearningActivity
import com.familygrowth.android.remote.RemoteLearningAssignment
import com.familygrowth.android.remote.RemoteSeniorGoal
import com.familygrowth.android.remote.RemoteSeniorModule
import kotlinx.coroutines.delay
import kotlin.math.ceil

@Composable
fun ChildLearningPath(viewModel: FamilyAppViewModel) {
    val assignment = viewModel.learningAssignments.firstOrNull { it.status != "COMPLETED" }
        ?: viewModel.learningAssignments.firstOrNull()
        ?: return
    var video by remember { mutableStateOf<RemoteLearningActivity?>(null) }
    val active = if (assignment.status == "REWORK_REQUIRED") {
        assignment.activities.firstOrNull { it.requiredEvidence == "PARENT_CONFIRMED" }
            ?: assignment.activities.firstOrNull()
    } else assignment.activities.firstOrNull { !it.childReady() }
    if (viewModel.state.experience.effectiveStage == SchoolStage.KINDERGARTEN) {
        KindergartenLearningPath(viewModel, assignment, active) { video = it }
        video?.let { activity ->
            DynamicLearningVideoDialog(activity, { video = null }) { played, duration ->
                viewModel.completeLearningVideo(assignment.id, activity.id, played, duration)
            }
        }
        return
    }
    if (viewModel.state.experience.effectiveStage == SchoolStage.PRIMARY) {
        PrimaryLearningPath(viewModel, assignment, active) { video = it }
        video?.let { activity ->
            DynamicLearningVideoDialog(activity, { video = null }) { played, duration ->
                viewModel.completeLearningVideo(assignment.id, activity.id, played, duration)
            }
        }
        return
    }
    if (viewModel.state.experience.effectiveStage == SchoolStage.JUNIOR_MIDDLE) {
        JuniorLearningPath(viewModel, assignment, active) { video = it }
        video?.let { activity -> DynamicLearningVideoDialog(activity, { video=null }) { played,duration ->
            viewModel.completeLearningVideo(assignment.id,activity.id,played,duration)
        } }
        return
    }
    if(viewModel.state.experience.effectiveStage==SchoolStage.SENIOR_HIGH) {
        SeniorLearningRoom(viewModel,assignment,active){video=it}
        video?.let{activity->DynamicLearningVideoDialog(activity,{video=null}){played,duration->viewModel.completeLearningVideo(assignment.id,activity.id,played,duration)}}
        return
    }
    GrowthCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("学习路径", style = MaterialTheme.typography.labelLarge, color = ChildColors.Moss)
                Text(assignment.lessonTitle, style = MaterialTheme.typography.headlineSmall, color = ChildColors.Ink)
                Text("${assignment.courseTitle} · ${assignment.unitTitle}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LearningStatus(assignment.status)
        }
        Text(assignment.lessonSummary, style = MaterialTheme.typography.bodyLarge)
        LearningRewardPromise(assignment)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(assignment.activities, key = { _, it -> it.id }) { index, activity ->
                val done = activity.childReady()
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = if (done) ChildColors.Mist else if (activity.id == active?.id) ChildColors.Sun.copy(alpha = .3f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(if (activity.id == active?.id) 2.dp else 1.dp, if (done) ChildColors.Moss else MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (done) Icon(Icons.Rounded.Check, null, tint = ChildColors.Moss, modifier = Modifier.size(18.dp))
                        else Text("${index + 1}", color = ChildColors.Ink)
                        Spacer(Modifier.width(6.dp)); Text(activity.title, maxLines = 1)
                    }
                }
            }
        }
        when (assignment.status) {
            "SUBMITTED" -> GentleNotice("已经交给家长看了。现在可以去做别的事。")
            "COMPLETED" -> GentleNotice("这一课完成了。你认真做了每一步。")
            else -> active?.let { activity ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("现在做：${activity.title}", style = MaterialTheme.typography.titleLarge)
                    Text(activity.instruction, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    when {
                        activity.type == "SHORT_VIDEO" -> Button(onClick = { video = activity }, Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                            Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("在这里播放")
                        }
                        activity.options.isNotEmpty() -> activity.options.forEach { option ->
                            OutlinedButton(onClick = { viewModel.attemptLearningActivity(assignment.id, activity.id, option.value) }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(option.label) }
                        }
                        else -> Button(onClick = { viewModel.attemptLearningActivity(assignment.id, activity.id, "我完成了这一步") }, Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                            Text("我做过了")
                        }
                    }
                    if (activity.checkedCorrect == false) Text("还没对上。看看提示，再慢慢试一次。${activity.hint}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (assignment.canSubmit()) Button(onClick = { viewModel.submitLearningAssignment(assignment.id, assignment.version) }, Modifier.fillMaxWidth().heightIn(min = 58.dp)) {
            Text("请家长看看")
        }
        if (assignment.status == "REWORK_REQUIRED" && assignment.reviewNote.isNotBlank()) {
            GentleNotice("家长说：${assignment.reviewNote}")
        }
    }
    video?.let { activity ->
        DynamicLearningVideoDialog(activity, { video = null }) { played, duration ->
            viewModel.completeLearningVideo(assignment.id, activity.id, played, duration)
        }
    }
}

private object SeniorRoomColors {
    val Carbon=Color(0xFF263238);val Pine=Color(0xFF3E6B62);val Paper=Color(0xFFFAFAF7);val Archive=Color(0xFF8A9491);val Evidence=Color(0xFFC58B2B);val Annotation=Color(0xFF356C8C)
}

@Composable
private fun SeniorLearningRoom(viewModel:FamilyAppViewModel,assignment:RemoteLearningAssignment,active:RemoteLearningActivity?,playVideo:(RemoteLearningActivity)->Unit) {
    var showGoal by remember{mutableStateOf(false)};var showReflection by remember{mutableStateOf(false)}
    val module=viewModel.seniorModuleConfiguration?.selections?.firstOrNull{it.subjectCode==assignment.subjectCode}
        ?: RemoteSeniorModule(assignment.subjectCode,assignment.seniorMetadata?.moduleType?:"REQUIRED")
    val goal=viewModel.seniorGoals.firstOrNull{it.status=="ACTIVE"&&it.module.subjectCode==assignment.subjectCode}
    Surface(shape=MaterialTheme.shapes.extraLarge,color=SeniorRoomColors.Paper,border=BorderStroke(1.dp,SeniorRoomColors.Pine.copy(alpha=.45f))){
        Column(Modifier.fillMaxWidth().padding(22.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("自主学习室",style=MaterialTheme.typography.labelLarge,color=SeniorRoomColors.Pine,fontFamily=FontFamily.Monospace);Text(assignment.lessonTitle,style=MaterialTheme.typography.headlineSmall,color=SeniorRoomColors.Carbon,fontWeight=FontWeight.Bold);Text("${PrimaryLearningPolicy.subjectLabel(assignment.subjectCode)} · ${module.moduleType}",color=SeniorRoomColors.Archive)};LearningStatus(assignment.status)}
            assignment.seniorMetadata?.let{m->Surface(shape=MaterialTheme.shapes.medium,color=Color.White,border=BorderStroke(1.dp,SeniorRoomColors.Annotation.copy(alpha=.35f))){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text(m.topicTitle,fontWeight=FontWeight.Bold,color=SeniorRoomColors.Annotation);Text("研究问题：${m.inquiryQuestion}");Text("证据要求：${m.expectedEvidence}",color=SeniorRoomColors.Evidence);Text("边界：${m.safetyNote}",style=MaterialTheme.typography.bodySmall,color=SeniorRoomColors.Archive)}}}
            goal?.let{g->Surface(shape=MaterialTheme.shapes.medium,color=SeniorRoomColors.Pine.copy(alpha=.08f)){Column(Modifier.padding(14.dp)){Text("本周目标",style=MaterialTheme.typography.labelLarge,color=SeniorRoomColors.Pine);Text(g.title,fontWeight=FontWeight.Bold);Text("证据：${g.evidenceTarget}");Text("下一步：${g.nextAction}",color=SeniorRoomColors.Annotation)}}} ?: OutlinedButton(onClick={showGoal=true},Modifier.fillMaxWidth()){Text("为这门课写本周目标")}
            when(assignment.status){"SUBMITTED"->GentleNotice("证据已提交，等家长回应。你可以先离开屏幕。 ");"COMPLETED"->GentleNotice("这段学习已完成。记录保留，不形成排名或能力标签。")
                else->active?.let{a->Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Text("当前行动",style=MaterialTheme.typography.labelLarge,color=SeniorRoomColors.Evidence);Text(a.title,style=MaterialTheme.typography.titleLarge);Text(a.instruction,color=MaterialTheme.colorScheme.onSurfaceVariant);if(a.type=="SHORT_VIDEO")Button(onClick={playVideo(a)},Modifier.fillMaxWidth()){Text("播放并核对证据")}else if(a.options.isNotEmpty())a.options.forEach{o->OutlinedButton(onClick={viewModel.attemptLearningActivity(assignment.id,a.id,o.value)},Modifier.fillMaxWidth()){Text(o.label)}}else Button(onClick={viewModel.attemptLearningActivity(assignment.id,a.id,"已形成证据")},Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=SeniorRoomColors.Pine)){Text("记录已形成证据")};OutlinedButton(onClick={viewModel.requestLearningHelp(assignment.id,a.id,"我需要一起厘清研究问题或证据边界。")},Modifier.fillMaxWidth()){Text("请求支持")}}}
            }
            if(assignment.canSubmit())Button(onClick={viewModel.submitLearningAssignment(assignment.id,assignment.version)},Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=SeniorRoomColors.Carbon)){Text("提交证据给家长")}
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={showReflection=true},Modifier.weight(1f)){Text("写复盘")};goal?.let{OutlinedButton(onClick={viewModel.archiveSeniorGoal(it)},Modifier.weight(1f)){Text("归档目标")}}}
            Text("自主安排不等于延长在线时间。建议单段不超过 45 分钟，证据可在线下形成；随时可以暂停和重排。",style=MaterialTheme.typography.bodySmall,color=SeniorRoomColors.Archive)
        }
    }
    if(showGoal)SeniorGoalDialog(module,assignment,{showGoal=false}){title,evidence,next->viewModel.createSeniorGoal(module,assignment.id,title,evidence,next);showGoal=false}
    if(showReflection)SeniorReflectionDialog(goal,assignment,{showReflection=false}){evidence,strategy,next,support->viewModel.createSeniorReflection(goal,assignment.id,evidence,strategy,next,support);showReflection=false}
}

@Composable private fun SeniorGoalDialog(module:RemoteSeniorModule,assignment:RemoteLearningAssignment,dismiss:()->Unit,save:(String,String,String)->Unit){var title by remember{mutableStateOf(assignment.lessonTitle)};var evidence by remember{mutableStateOf(assignment.seniorMetadata?.expectedEvidence?:"一份可复查的学习证据")};var next by remember{mutableStateOf("先完成第一段材料或实验记录")};AlertDialog(onDismissRequest=dismiss,title={Text("本周目标 · ${PrimaryLearningPolicy.subjectLabel(module.subjectCode)}")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(title,{title=it.take(160)},label={Text("目标")});OutlinedTextField(evidence,{evidence=it.take(500)},label={Text("预期证据")});OutlinedTextField(next,{next=it.take(500)},label={Text("下一步")})}},confirmButton={Button(onClick={save(title,evidence,next)},enabled=title.isNotBlank()&&evidence.isNotBlank()&&next.isNotBlank()){Text("保存")}},dismissButton={TextButton(onClick=dismiss){Text("取消")}})}

@Composable private fun SeniorReflectionDialog(goal:RemoteSeniorGoal?,assignment:RemoteLearningAssignment,dismiss:()->Unit,save:(String,String,String,Boolean)->Unit){var evidence by remember{mutableStateOf("")};var strategy by remember{mutableStateOf("CONTINUE")};var next by remember{mutableStateOf("")};var support by remember{mutableStateOf(false)};val choices=listOf("CONTINUE" to "继续当前方法","REVIEW_FOUNDATION" to "回看基础","TRY_ANOTHER_METHOD" to "换一种方法","PAUSE_AND_REPLAN" to "暂停并重排");AlertDialog(onDismissRequest=dismiss,title={Text("证据复盘 · ${assignment.lessonTitle}")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(evidence,{evidence=it.take(1000)},label={Text("我形成了什么证据")},minLines=2);choices.forEach{(value,label)->FilterChip(selected=strategy==value,onClick={strategy=value},label={Text(label)})};OutlinedTextField(next,{next=it.take(500)},label={Text("下一步")});Row(verticalAlignment=Alignment.CenterVertically){Checkbox(support,{support=it;if(it)strategy="ASK_FOR_SUPPORT"});Text("需要家长或老师支持")}}},confirmButton={Button(onClick={save(evidence,strategy,next,support)},enabled=evidence.isNotBlank()&&next.isNotBlank()){Text("保存复盘")}},dismissButton={TextButton(onClick=dismiss){Text("取消")}})}

private object JuniorLabColors {
    val Navy=Color(0xFF17324D); val Measure=Color(0xFF4E8799); val Evidence=Color(0xFFE2B44A)
    val Paper=Color(0xFFF5F7F4); val Graphite=Color(0xFF27343D); val Correct=Color(0xFFC66A3D)
}

@Composable
private fun JuniorLearningPath(viewModel:FamilyAppViewModel,assignment:RemoteLearningAssignment,
                               active:RemoteLearningActivity?,playVideo:(RemoteLearningActivity)->Unit) {
    val subjects=viewModel.learningAssignments.filter{it.schoolStage=="JUNIOR_MIDDLE"}.map{it.subjectCode}.distinct()
    Surface(shape=MaterialTheme.shapes.extraLarge,color=JuniorLabColors.Paper,
        border=BorderStroke(1.dp,JuniorLabColors.Measure.copy(alpha=.45f))) {
        Column(Modifier.fillMaxWidth().padding(22.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text("学科实验台",style=MaterialTheme.typography.labelLarge,color=JuniorLabColors.Measure,
                fontFamily=FontFamily.Monospace)
            LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { items(subjects,key={it}) { subject ->
                val selected=subject==assignment.subjectCode
                Surface(shape=MaterialTheme.shapes.small,color=if(selected)JuniorLabColors.Navy else Color.Transparent,
                    border=BorderStroke(1.dp,JuniorLabColors.Measure)) {
                    Text(PrimaryLearningPolicy.subjectLabel(subject),Modifier.padding(horizontal=14.dp,vertical=9.dp),
                        color=if(selected)Color.White else JuniorLabColors.Graphite)
                }
            } }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(assignment.unitTitle,style=MaterialTheme.typography.labelLarge,color=JuniorLabColors.Measure)
                    Text(assignment.lessonTitle,style=MaterialTheme.typography.headlineSmall,
                        color=JuniorLabColors.Graphite,fontWeight=FontWeight.Bold)
                }
                LearningStatus(assignment.status)
            }
            Text(assignment.lessonSummary,color=MaterialTheme.colorScheme.onSurfaceVariant)
            assignment.juniorMetadata?.let { metadata ->
                Surface(shape=MaterialTheme.shapes.medium,color=JuniorLabColors.Measure.copy(alpha=.09f)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                        Text(metadata.chapterTitle,style=MaterialTheme.typography.labelLarge,color=JuniorLabColors.Measure)
                        Text("本段目标：${metadata.learningGoal}",color=JuniorLabColors.Graphite)
                        Text(metadata.knowledgePoints.joinToString(" · "),style=MaterialTheme.typography.bodySmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("安全边界：${metadata.safetyNote}",style=MaterialTheme.typography.bodySmall,
                            color=JuniorLabColors.Correct)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                assignment.activities.forEachIndexed { index,item ->
                    val current=item.id==active?.id
                    Surface(Modifier.weight(1f),shape=MaterialTheme.shapes.small,
                        color=if(current)JuniorLabColors.Evidence.copy(alpha=.38f) else JuniorLabColors.Measure.copy(alpha=.10f)) {
                        Column(Modifier.padding(9.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                            Text(if(item.childReady())"✓" else "${index+1}",fontFamily=FontFamily.Monospace,
                                fontWeight=FontWeight.Bold,color=JuniorLabColors.Navy)
                            Text(if(index==0)"问题" else if(index==assignment.activities.lastIndex)"解释" else "证据",
                                style=MaterialTheme.typography.labelSmall,color=JuniorLabColors.Graphite)
                        }
                    }
                }
            }
            LearningRewardPromise(assignment)
            when(assignment.status) {
                "SUBMITTED"->GentleNotice("已经交给家长回应。先停下来，稍后再看计划。")
                "COMPLETED"->GentleNotice("这一段已经完成。这里记录行动和证据，不和别人比较。")
                else->active?.let { activity -> Column(verticalArrangement=Arrangement.spacedBy(11.dp)) {
                    Text("当前证据",style=MaterialTheme.typography.labelLarge,color=JuniorLabColors.Correct)
                    Text(activity.title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                    Text(activity.instruction,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    when {
                        activity.type=="SHORT_VIDEO"->Button(onClick={playVideo(activity)},Modifier.fillMaxWidth().heightIn(min=54.dp),
                            colors=ButtonDefaults.buttonColors(containerColor=JuniorLabColors.Navy)){Text("播放并记录证据")}
                        activity.options.isNotEmpty()->activity.options.forEach { option -> OutlinedButton(
                            onClick={viewModel.attemptLearningActivity(assignment.id,activity.id,option.value)},
                            modifier=Modifier.fillMaxWidth().heightIn(min=50.dp)){Text(option.label)} }
                        else->Button(onClick={viewModel.attemptLearningActivity(assignment.id,activity.id,"我完成了这一步")},
                            Modifier.fillMaxWidth().heightIn(min=54.dp),colors=ButtonDefaults.buttonColors(containerColor=JuniorLabColors.Navy)){Text("记录这一步")}
                    }
                    OutlinedButton(onClick={viewModel.requestLearningHelp(assignment.id,activity.id,"这条证据我还没理清，请和我一起看看。")},
                        Modifier.fillMaxWidth().heightIn(min=50.dp)){Text("这一步需要帮助")}
                } } ?: GentleNotice("当前没有可继续的证据步骤，请家长检查课程。")
            }
            if(assignment.canSubmit()) Button(onClick={viewModel.submitLearningAssignment(assignment.id,assignment.version)},
                Modifier.fillMaxWidth().heightIn(min=56.dp),colors=ButtonDefaults.buttonColors(containerColor=JuniorLabColors.Navy)){Text("提交这段解释")}
            viewModel.juniorLearningPlan?.items?.takeIf { it.isNotEmpty() }?.let { planned ->
                HorizontalDivider(color=JuniorLabColors.Measure.copy(alpha=.28f))
                Text("待学顺序（第 1 项是当前）",style=MaterialTheme.typography.titleMedium,color=JuniorLabColors.Navy,fontWeight=FontWeight.Bold)
                planned.forEachIndexed { index,item ->
                    Surface(shape=MaterialTheme.shapes.medium,color=Color.White,
                        border=BorderStroke(1.dp,JuniorLabColors.Measure.copy(alpha=.32f))) {
                        Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically,
                            horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text("${index+1}",fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Bold,color=JuniorLabColors.Measure)
                            Column(Modifier.weight(1f)) {
                                Text(item.lessonTitle,fontWeight=FontWeight.SemiBold,color=JuniorLabColors.Graphite)
                                Text("${PrimaryLearningPolicy.subjectLabel(item.subjectCode)} · ${item.courseTitle}",
                                    style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick={viewModel.moveJuniorLearning(item.assignmentId,"UP")},enabled=index>0) { Text("上移") }
                            TextButton(onClick={viewModel.moveJuniorLearning(item.assignmentId,"DOWN")},enabled=index<planned.lastIndex) { Text("下移") }
                        }
                    }
                }
            }
            Text("一次只完成一段，建议不超过 25 分钟；需要时直接休息。上移或下移会保存到家庭服务，进行中课程不参与排序。",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private object PrimaryNotebookColors {
    val Paper = Color(0xFFFBFCF7)
    val Ink = Color(0xFF263A45)
    val Lake = Color(0xFF3C7891)
    val Grid = Color(0xFFCFE0E7)
    val Note = Color(0xFFD98245)
}

@Composable
private fun PrimaryLearningPath(
    viewModel: FamilyAppViewModel,
    assignment: RemoteLearningAssignment,
    active: RemoteLearningActivity?,
    playVideo: (RemoteLearningActivity) -> Unit,
) {
    val band = remember(viewModel.state.experience.effectivePrimaryBand, viewModel.state.experience.birthDate) {
        PrimaryLearningPolicy.bandFor(viewModel.state.experience.effectivePrimaryBand, viewModel.state.experience.birthDate)
    }
    val facts = remember(assignment) { PrimaryLearningPolicy.facts(assignment) }
    var needsHelp by remember(assignment.id, active?.id) { mutableStateOf(false) }
    Surface(
        shape=MaterialTheme.shapes.extraLarge,
        color=PrimaryNotebookColors.Paper,
        border=BorderStroke(1.dp, PrimaryNotebookColors.Grid),
    ) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(Modifier.width(10.dp).fillMaxHeight().background(PrimaryNotebookColors.Lake))
            Column(Modifier.weight(1f).padding(22.dp), verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement=Arrangement.spacedBy(3.dp)) {
                        Text("${band.label}探索夹页", style=MaterialTheme.typography.labelLarge, color=PrimaryNotebookColors.Lake)
                        Text(assignment.lessonTitle, style=MaterialTheme.typography.headlineSmall,
                            color=PrimaryNotebookColors.Ink, fontWeight=FontWeight.Bold)
                        Text("${PrimaryLearningPolicy.subjectLabel(assignment.subjectCode)} · ${assignment.courseTitle}",
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    LearningStatus(assignment.status)
                }
                if (band == PrimaryGradeBand.LOWER_PRIMARY) {
                    PrimaryLowerSteps(assignment, active)
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                        PrimaryFact("要做", facts.toDo, PrimaryNotebookColors.Lake, Modifier.weight(1f))
                        PrimaryFact("在做", facts.inProgress, PrimaryNotebookColors.Note, Modifier.weight(1f))
                        PrimaryFact("再练", facts.rework, PrimaryNotebookColors.Ink, Modifier.weight(1f))
                    }
                    Text(band.guide, style=MaterialTheme.typography.labelLarge, color=PrimaryNotebookColors.Lake)
                }
                HorizontalDivider(color=PrimaryNotebookColors.Grid)
                LearningRewardPromise(assignment)
                when (assignment.status) {
                    "SUBMITTED" -> GentleNotice("已经交给家长看。现在可以休息或去做别的事。")
                    "COMPLETED" -> GentleNotice("这次探索完成了。记录的是你做过的步骤，不是和别人比较。")
                    else -> active?.let { activity ->
                        Column(verticalArrangement=Arrangement.spacedBy(11.dp)) {
                            Text(if(band==PrimaryGradeBand.LOWER_PRIMARY) "这页只做这一项" else "当前探索",
                                style=MaterialTheme.typography.labelLarge, color=PrimaryNotebookColors.Note)
                            Text(activity.title, style=MaterialTheme.typography.titleLarge, color=PrimaryNotebookColors.Ink,
                                fontWeight=FontWeight.Bold)
                            Text(activity.instruction, style=MaterialTheme.typography.bodyLarge,
                                color=MaterialTheme.colorScheme.onSurfaceVariant)
                            activity.prompt.takeIf(String::isNotBlank)?.let {
                                Surface(shape=MaterialTheme.shapes.medium, color=PrimaryNotebookColors.Grid.copy(alpha=.42f)) {
                                    Text(it, Modifier.fillMaxWidth().padding(14.dp), style=MaterialTheme.typography.bodyLarge,
                                        color=PrimaryNotebookColors.Ink)
                                }
                            }
                            when {
                                activity.type == "SHORT_VIDEO" -> Button(
                                    onClick={ playVideo(activity) },
                                    modifier=Modifier.fillMaxWidth().heightIn(min=56.dp),
                                    colors=ButtonDefaults.buttonColors(containerColor=PrimaryNotebookColors.Lake),
                                ) { Icon(Icons.Rounded.PlayArrow,null); Spacer(Modifier.width(8.dp)); Text("播放这一小段") }
                                activity.options.isNotEmpty() -> activity.options.forEach { option ->
                                    OutlinedButton(
                                        onClick={ viewModel.attemptLearningActivity(assignment.id,activity.id,option.value) },
                                        modifier=Modifier.fillMaxWidth().heightIn(min=52.dp),
                                    ) { Text(option.label) }
                                }
                                else -> Button(
                                    onClick={ viewModel.attemptLearningActivity(assignment.id,activity.id,"我完成了这一步") },
                                    modifier=Modifier.fillMaxWidth().heightIn(min=56.dp),
                                    colors=ButtonDefaults.buttonColors(containerColor=PrimaryNotebookColors.Lake),
                                ) { Text(if(band==PrimaryGradeBand.LOWER_PRIMARY) "我试过了" else "记录这次尝试") }
                            }
                            if (activity.checkedCorrect == false) {
                                GentleNotice("这次还没对上。先找一找哪里不同，再试一次。${activity.hint}")
                            }
                            OutlinedButton(
                                onClick={ needsHelp=true; viewModel.requestLearningHelp(assignment.id,activity.id,"这里我没看懂，请和我一起看看。") },
                                modifier=Modifier.fillMaxWidth().heightIn(min=50.dp),
                            ) { Icon(Icons.Rounded.SupervisorAccount,null); Spacer(Modifier.width(7.dp)); Text("我没看懂") }
                            if (needsHelp) GentleNotice(PrimaryLearningPolicy.helpText(activity))
                        }
                    } ?: GentleNotice("这页暂时没有可做的活动，请家长检查课程内容。")
                }
                if (assignment.canSubmit()) Button(
                    onClick={ viewModel.submitLearningAssignment(assignment.id,assignment.version) },
                    modifier=Modifier.fillMaxWidth().heightIn(min=58.dp),
                    colors=ButtonDefaults.buttonColors(containerColor=PrimaryNotebookColors.Lake),
                ) { Text("交给家长看看") }
                if (assignment.status=="REWORK_REQUIRED" && assignment.reviewNote.isNotBlank()) {
                    GentleNotice("家长建议：${assignment.reviewNote}")
                }
                Text("“我没看懂”会安全告诉家长，但不会把这一步记成尝试或完成。",
                    style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LearningRewardPromise(assignment: RemoteLearningAssignment) {
    val reward = assignment.reward
    val hasReward = reward.money.toBigDecimalOrNull()?.signum() == 1 || reward.coin > 0 || reward.xp > 0
    if (!hasReward) return
    val text = when {
        assignment.schoolStage == "KINDERGARTEN" -> "做完后，请家长一起看看小星星。"
        reward.settledAt != null -> "家长已经确认并记录了这次奖励。"
        else -> "家长预设奖励：${reward.money} Money · ${reward.coin} Coin · ${reward.xp} XP；完成并由家长确认后记录。"
    }
    GentleNotice(text)
}

@Composable
private fun PrimaryLowerSteps(assignment:RemoteLearningAssignment, active:RemoteLearningActivity?) {
    val activeIndex = when {
        assignment.status in setOf("SUBMITTED","COMPLETED") || assignment.canSubmit() -> 2
        active?.evidence?.isNotEmpty() == true || active?.checkedCorrect == false -> 1
        else -> 0
    }
    val labels=listOf("读懂","试一试","说发现")
    Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
        labels.forEachIndexed { index,label ->
            Column(Modifier.weight(1f), horizontalAlignment=Alignment.CenterHorizontally) {
                Surface(shape=androidx.compose.foundation.shape.CircleShape,
                    color=if(index==activeIndex) PrimaryNotebookColors.Note else PrimaryNotebookColors.Grid) {
                    Text("${index+1}", Modifier.padding(horizontal=13.dp,vertical=8.dp), color=PrimaryNotebookColors.Ink,
                        fontWeight=FontWeight.Bold)
                }
                Spacer(Modifier.height(5.dp)); Text(label, style=MaterialTheme.typography.bodyMedium)
            }
            if(index<labels.lastIndex) HorizontalDivider(Modifier.weight(.28f),color=PrimaryNotebookColors.Grid)
        }
    }
}

@Composable
private fun PrimaryFact(label:String,value:Int,color:Color,modifier:Modifier=Modifier) {
    Surface(modifier,shape=MaterialTheme.shapes.medium,color=color.copy(alpha=.10f),
        border=BorderStroke(1.dp,color.copy(alpha=.28f))) {
        Column(Modifier.padding(horizontal=12.dp,vertical=10.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Text(value.toString(),fontFamily=FontFamily.Monospace,style=MaterialTheme.typography.titleLarge,
                color=color,fontWeight=FontWeight.Bold)
            Text(label,style=MaterialTheme.typography.labelMedium,color=PrimaryNotebookColors.Ink)
        }
    }
}

@Composable
private fun KindergartenLearningPath(
    viewModel: FamilyAppViewModel,
    assignment: RemoteLearningAssignment,
    active: RemoteLearningActivity?,
    playVideo: (RemoteLearningActivity) -> Unit,
) {
    val activeStep = when {
        assignment.status in setOf("SUBMITTED", "COMPLETED") || assignment.canSubmit() -> 2
        active?.requiredEvidence == "PARENT_CONFIRMED" -> 1
        else -> 0
    }
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = ChildColors.Paper,
        border = BorderStroke(2.dp, ChildColors.Mist),
    ) {
        Column(Modifier.fillMaxWidth().padding(26.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("成长舞台", style = MaterialTheme.typography.labelLarge, color = ChildColors.Moss)
            Text(assignment.lessonTitle, style = MaterialTheme.typography.headlineMedium, color = ChildColors.Ink)
            KindergartenLearningSteps(activeStep)
            when (assignment.status) {
                "SUBMITTED" -> GentleNotice("已经给家长看啦。现在去玩一会儿吧。")
                "COMPLETED" -> GentleNotice("这一小步做好了。谢谢你认真去做。")
                else -> active?.let { activity ->
                    val issue = KindergartenActivityPolicy.renderIssue(activity)
                    Text("现在做", style = MaterialTheme.typography.titleMedium, color = ChildColors.Moss)
                    Text(activity.title, style = MaterialTheme.typography.headlineSmall, color = ChildColors.Ink)
                    Text(activity.instruction, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (issue != null) {
                        GentleNotice(issue)
                    } else when {
                        activity.type == "SHORT_VIDEO" -> StagePrimaryActionButton(viewModel.state.experience, "和家长一起看", Icons.Rounded.PlayArrow) { playVideo(activity) }
                        activity.options.isNotEmpty() -> activity.options.forEach { option ->
                            OutlinedButton(
                                onClick = { viewModel.attemptLearningActivity(assignment.id, activity.id, option.value) },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                            ) { Text(option.label, style = MaterialTheme.typography.titleMedium) }
                        }
                        else -> StagePrimaryActionButton(viewModel.state.experience, "我去做一做", Icons.Rounded.Check) {
                            viewModel.attemptLearningActivity(assignment.id, activity.id, "我完成了这一步")
                        }
                    }
                    if (activity.checkedCorrect == false) GentleNotice("还没对上。慢慢再试一次，也可以请家长帮忙。")
                }
            }
            if (assignment.canSubmit()) {
                StagePrimaryActionButton(viewModel.state.experience, "给家长看", Icons.Rounded.SupervisorAccount) {
                    viewModel.submitLearningAssignment(assignment.id, assignment.version)
                }
            }
            if (assignment.status == "REWORK_REQUIRED" && assignment.reviewNote.isNotBlank()) {
                GentleNotice("家长说：${assignment.reviewNote}")
            }
            Text("看完屏幕要去现实中做一做。需要帮助时，请家长一起。", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun KindergartenLearningSteps(active: Int) {
    val labels = listOf("看看", "去做", "给家长看")
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        labels.forEachIndexed { index, label ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = if (index == active) ChildColors.Sun else ChildColors.Mist,
                ) { Box(contentAlignment = Alignment.Center) { Text("${index + 1}", color = ChildColors.Ink) } }
                Spacer(Modifier.height(5.dp))
                Text(label, style = MaterialTheme.typography.bodyLarge)
            }
            if (index < labels.lastIndex) HorizontalDivider(Modifier.weight(.35f), thickness = 2.dp, color = ChildColors.Mist)
        }
    }
}

@Composable
fun ParentLearningReviews(viewModel: FamilyAppViewModel) {
    val submitted = viewModel.learningAssignments.filter { it.status == "SUBMITTED" }
    val support = viewModel.learningSupportByAssignment
    viewModel.primaryLearningReport?.let { report ->
        GrowthCard {
            SectionTitle("最近 7 天学习事实", "只记录发生过的行动，不是成绩或能力评价")
            Text("已记录学习 ${report.recordedLearningMinutes} 分钟", style=MaterialTheme.typography.titleMedium)
            if(report.subjects.isEmpty()) Text("还没有小学课程记录。",color=MaterialTheme.colorScheme.onSurfaceVariant)
            report.subjects.forEach { fact ->
                val subject=when(fact.subjectCode){"CHINESE"->"语文";"MATH"->"数学";"ENGLISH"->"英语";"SCIENCE"->"科学";else->fact.subjectCode}
                Text("$subject · 完成 ${fact.completed} · 待回应 ${fact.submitted} · 求助 ${fact.openSupport} · 到期再练 ${fact.dueRevisits}",
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if(report.effectiveStage!="PRIMARY") Text("当前已切换学段；这里仍保留以前的小学学习事实。",style=MaterialTheme.typography.bodySmall)
        }
    }
    viewModel.juniorLearningReport?.let { report ->
        GrowthCard {
            SectionTitle("最近 7 天初中学习事实","计划、行动、求助和再练；不生成分数或能力判断")
            Text("已记录学习 ${report.recordedLearningMinutes} 分钟 · 计划版本 ${report.planRevision}",style=MaterialTheme.typography.titleMedium)
            if(report.subjects.isEmpty()) Text("还没有初中课程记录。",color=MaterialTheme.colorScheme.onSurfaceVariant)
            report.subjects.forEach { fact ->
                Text("${PrimaryLearningPolicy.subjectLabel(fact.subjectCode)} · 计划 ${fact.assigned} · 进行中 ${fact.inProgress} · 待回应 ${fact.submitted} · 完成 ${fact.completed} · 求助 ${fact.openSupport} · 到期再练 ${fact.dueRevisits}",
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    viewModel.seniorLearningReport?.let{report->GrowthCard{SectionTitle("最近 7 天高中学习事实","只汇总课程、目标、复盘和求助，不生成分数或能力预测");Text("已记录学习 ${report.recordedLearningMinutes} 分钟 · 进行中目标 ${report.activeGoals} · 已归档 ${report.archivedGoals}",style=MaterialTheme.typography.titleMedium);Text("复盘 ${report.reflections} · 主动求助 ${report.supportRequests}",color=MaterialTheme.colorScheme.onSurfaceVariant);report.subjects.forEach{fact->Text("${PrimaryLearningPolicy.subjectLabel(fact.subjectCode)} · 完成 ${fact.completed} · 待回应 ${fact.submitted} · 求助 ${fact.openSupport}",color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
    support.forEach { (assignmentId,events) ->
        val assignment=viewModel.learningAssignments.firstOrNull { it.id==assignmentId } ?: return@forEach
        val classified=events.filter { it.type=="MISCONCEPTION_CLASSIFIED" }.mapNotNull { it.parentEventId }.toSet()
        val open=events.firstOrNull { it.type in setOf("HELP_REQUESTED","INCORRECT_OBSERVED") && it.id !in classified } ?: return@forEach
        GrowthCard {
            SectionTitle("孩子需要一起看看", "${assignment.courseTitle} · ${assignment.lessonTitle}")
            Text(open.childMessage,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text("这不是成绩或能力判断；先理解卡在哪里，再安排一次短复习。",style=MaterialTheme.typography.bodySmall)
            Button(onClick={viewModel.scheduleLearningRevisit(assignmentId,open.id)},Modifier.fillMaxWidth().heightIn(min=52.dp)) { Text("安排两天后再练") }
        }
    }
    if (submitted.isEmpty()) return
    submitted.forEach { assignment ->
        var observation by remember(assignment.id) { mutableStateOf("") }
        val kindergarten = assignment.schoolStage == "KINDERGARTEN"
        GrowthCard {
            SectionTitle("课节待回应", "${assignment.courseTitle} · ${assignment.lessonTitle}")
            Text(if (kindergarten) "写一句你看到的行动或表达，不评价聪明、落后或输赢。"
                else "孩子已完成客户端要求的步骤；亲子、口头和线下活动仍由你确认。",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (kindergarten) OutlinedTextField(
                value=observation,
                onValueChange={ observation=it.take(240) },
                label={ Text("我看到孩子……") },
                supportingText={ Text("例如：他先猜球会滚，再自己把方盒放上斜坡。") },
                modifier=Modifier.fillMaxWidth(),
                minLines=2,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { viewModel.reviewLearningAssignment(assignment.id, false,
                    if(kindergarten) observation.trim() else "请再慢慢完成一次需要回应的步骤", assignment.version) },
                    Modifier.weight(1f).heightIn(min = 52.dp), enabled=!kindergarten||observation.isNotBlank()) {
                    Icon(Icons.Rounded.Refresh, null); Spacer(Modifier.width(6.dp)); Text(if(kindergarten) "再陪一次" else "请再试")
                }
                Button(onClick = { viewModel.reviewLearningAssignment(assignment.id, true,
                    if(kindergarten) observation.trim() else "看见你认真完成了每一步", assignment.version) },
                    Modifier.weight(1f).heightIn(min = 52.dp), enabled=!kindergarten||observation.isNotBlank()) {
                    Icon(Icons.Rounded.Check, null); Spacer(Modifier.width(6.dp)); Text("确认完成")
                }
            }
        }
    }
}

@Composable
private fun DynamicLearningVideoDialog(activity: RemoteLearningActivity, dismiss: () -> Unit, completed: (Int, Int) -> Unit) {
    val context = LocalContext.current
    val resource = remember(activity.contentRef) { bundledVideo(activity.contentRef) }
    var view by remember(activity.id) { mutableStateOf<VideoView?>(null) }
    var playing by remember(activity.id) { mutableStateOf(false) }
    var watched by remember(activity.id) { mutableIntStateOf(0) }
    var duration by remember(activity.id) { mutableIntStateOf(0) }
    var reported by remember(activity.id) { mutableStateOf("VIEWED" in activity.evidence) }
    var error by remember(activity.id) { mutableStateOf(resource == null) }
    LaunchedEffect(playing, duration) {
        while (playing) {
            delay(1_000)
            if (view?.isPlaying == true) {
                watched += 1
                if (!reported && duration > 0 && watched * 10 >= duration * 9) {
                    reported = true
                    completed(watched, duration)
                }
            } else playing = false
        }
    }
    DisposableEffect(activity.id) { onDispose { view?.stopPlayback() } }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(activity.title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (resource != null) AndroidView(factory = { videoContext ->
                VideoView(videoContext).also { video ->
                    view = video
                    video.setVideoURI("android.resource://${context.packageName}/$resource".toUri())
                    video.setOnPreparedListener { duration = ceil(it.duration / 1000.0).toInt().coerceAtLeast(1) }
                    video.setOnCompletionListener {
                        playing = false
                        watched = duration
                        if (!reported && duration > 0) { reported = true; completed(duration, duration) }
                    }
                    video.setOnErrorListener { _, _, _ -> error = true; playing = false; true }
                }
            }, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            LinearProgressIndicator(progress = { if (duration == 0) 0f else (watched.toFloat() / duration).coerceIn(0f, 1f) }, Modifier.fillMaxWidth())
            Text(if (reported) "已记录观看。掌握情况还要看后面的活动。" else "只累计实际播放时间，达到 90% 才记录观看。", color = if (reported) ChildColors.Moss else MaterialTheme.colorScheme.onSurfaceVariant)
            if (error) Text("这段内容在当前安装包中不可用，请家长检查课程版本。", color = MaterialTheme.colorScheme.error)
        } },
        confirmButton = { Button(enabled = !error, onClick = {
            view?.let { video -> if (playing) video.pause() else video.start(); playing = !playing }
        }) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text(if (playing) "暂停" else "播放") } },
        dismissButton = { TextButton(onClick = dismiss) { Text("关闭") } },
    )
}

@Composable private fun LearningStatus(status: String) {
    val (label, color) = when(status) { "ASSIGNED" -> "待开始" to Color(0xFF64748B); "IN_PROGRESS" -> "进行中" to ChildColors.Moss; "SUBMITTED" -> "等家长" to Color(0xFFB7791F); "REWORK_REQUIRED" -> "再试一次" to Color(0xFFC05621); else -> "已完成" to ChildColors.Moss }
    Surface(shape = MaterialTheme.shapes.small, color = color.copy(alpha=.12f)) { Text(label, Modifier.padding(horizontal=10.dp, vertical=6.dp), color=color) }
}

@Composable private fun GentleNotice(text: String) { Surface(shape=MaterialTheme.shapes.large, color=ChildColors.Mist.copy(alpha=.55f)) { Text(text, Modifier.fillMaxWidth().padding(16.dp), style=MaterialTheme.typography.bodyLarge, color=ChildColors.Ink) } }

private fun bundledVideo(contentRef: String): Int? = when(contentRef) {
    "lesson_color_garden" -> R.raw.lesson_color_garden
    "lesson_count_to_five" -> R.raw.lesson_count_to_five
    "lesson_shape_home" -> R.raw.lesson_shape_home
    else -> null
}
