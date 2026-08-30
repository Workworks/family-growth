package com.familygrowth.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.familygrowth.android.core.*
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun TodayScreen(viewModel: FamilyAppViewModel) {
    if (viewModel.mode == AppMode.CHILD) {
        AgeStageChildTodayScreen(viewModel)
        return
    }
    val state = viewModel.state
    val approved = state.tasks.count { it.status == TaskStatus.APPROVED }
    val submitted = state.tasks.count { it.status == TaskStatus.SUBMITTED }
    val taskProgress = if (state.tasks.isEmpty()) 0f else approved.toFloat() / state.tasks.size
    val usageProgress = state.usage.usedMinutes.toFloat() / state.usage.dailyLimitMinutes
    val rewardProgress = (state.wallet.xp % 100) / 100f
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 760.dp
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (viewModel.mode == AppMode.PARENT) "把成长看清楚，也把节奏放轻松。" else "先完成最重要的一件事。", style = MaterialTheme.typography.headlineMedium)
                    Text("任务、使用时长和奖励都来自本机真实记录。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                if (wide) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    GrowthOverviewCard(taskProgress, usageProgress, rewardProgress, approved, state.tasks.size, Modifier.weight(1.15f))
                    TodayTasksCard(viewModel, submitted, Modifier.weight(0.85f))
                } else Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    GrowthOverviewCard(taskProgress, usageProgress, rewardProgress, approved, state.tasks.size)
                    TodayTasksCard(viewModel, submitted)
                }
            }
            item {
                if (wide) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    WalletSnapshotCard(state, Modifier.weight(1f))
                    LatestLedgerCard(state, Modifier.weight(1f))
                } else Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    WalletSnapshotCard(state)
                    LatestLedgerCard(state)
                }
            }
        }
    }
}

@Composable
private fun ChildTodayScreen(viewModel: FamilyAppViewModel) {
    val nextTask = viewModel.state.tasks.firstOrNull { it.status == TaskStatus.TODO }
    val waiting = viewModel.state.tasks.firstOrNull { it.status == TaskStatus.SUBMITTED }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("今天好。", style = MaterialTheme.typography.displaySmall, color = ChildColors.Ink)
                Text("我们一次做一件小事。", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { ChildGrowthPath(if (nextTask != null) 0 else if (waiting != null) 2 else 1) }
        item {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = ChildColors.Paper,
                border = androidx.compose.foundation.BorderStroke(2.dp, ChildColors.Mist),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Icon(
                        when { nextTask != null -> Icons.Rounded.Spa; waiting != null -> Icons.Rounded.SupervisorAccount; else -> Icons.Rounded.WbSunny },
                        contentDescription = null,
                        tint = ChildColors.Moss,
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        when {
                            nextTask != null -> "现在做这件事"
                            waiting != null -> "已经给家长看啦"
                            else -> "今天的小任务完成了"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = ChildColors.Moss,
                    )
                    Text(
                        nextTask?.title ?: waiting?.title ?: "放下平板，去玩一会儿吧。",
                        style = MaterialTheme.typography.headlineMedium,
                        color = ChildColors.Ink,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Text(
                        when {
                            nextTask != null -> "先去做。做好以后，再回来点下面的按钮。"
                            waiting != null -> "家长看过以后，会告诉你下一步。"
                            else -> "运动、阅读，或者和家人说说话。"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    if (nextTask != null) {
                        Button(
                            onClick = { viewModel.submitTask(nextTask.id) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ChildColors.Moss),
                        ) {
                            Icon(Icons.Rounded.CheckCircle, null, Modifier.size(26.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("我做好了，给家长看", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }
        item {
            Text(
                "完成不是比赛。需要帮助时，随时请家长一起。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ChildGrowthPath(activeStep: Int) {
    val labels = listOf("看看", "去做", "给家长看")
    val icons = listOf(Icons.Rounded.Visibility, Icons.AutoMirrored.Rounded.DirectionsRun, Icons.Rounded.SupervisorAccount)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        labels.forEachIndexed { index, label ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = if (index == activeStep) ChildColors.Sun else ChildColors.Mist,
                    modifier = Modifier.size(48.dp),
                ) { Box(contentAlignment = Alignment.Center) { Icon(icons[index], null, tint = ChildColors.Ink, modifier = Modifier.size(24.dp)) } }
                Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = if (index == activeStep) FontWeight.Bold else FontWeight.Normal)
            }
            if (index < labels.lastIndex) androidx.compose.material3.HorizontalDivider(Modifier.weight(.4f), color = ChildColors.Mist, thickness = 2.dp)
        }
    }
}

@Composable
private fun GrowthOverviewCard(taskProgress: Float, usageProgress: Float, rewardProgress: Float, approved: Int, total: Int, modifier: Modifier = Modifier) {
    GrowthCard(modifier) {
        SectionTitle("今日成长环", "外环任务 · 中环用时 · 内环 XP")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GrowthRings(taskProgress, usageProgress, rewardProgress, if (total == 0) "—" else "$approved/$total", if (total == 0) "先创建任务" else "任务已完成", Modifier.weight(1f).widthIn(max = 260.dp))
            Column(Modifier.weight(0.9f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusDot("任务完成", GrowthColors.Emerald)
                StatusDot("孩子模式用时", GrowthColors.Amber)
                StatusDot("每 100 XP 一圈", Color(0xFF55A6C8))
                Text("环不是分数，而是今天三种节奏的并列提示。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TodayTasksCard(viewModel: FamilyAppViewModel, submitted: Int, modifier: Modifier = Modifier) {
    val tasks = viewModel.state.tasks.filter { it.status != TaskStatus.APPROVED }.take(3)
    GrowthCard(modifier) {
        SectionTitle("接下来做什么", if (submitted > 0) "$submitted 项等待家长审核" else "从一个小任务开始")
        if (tasks.isEmpty()) {
            EmptyInvitation("✦", "今天还没有待办", if (viewModel.mode == AppMode.PARENT) "前往任务页创建一个清晰、可完成的小目标。" else "请家长先创建今天的任务。")
        } else tasks.forEach { task ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = MaterialTheme.shapes.small, color = if (task.status == TaskStatus.SUBMITTED) MaterialTheme.colorScheme.secondary.copy(alpha = .16f) else MaterialTheme.colorScheme.primaryContainer) {
                    Text(if (task.status == TaskStatus.SUBMITTED) "待审" else "${task.minutes}m", Modifier.padding(horizontal = 9.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall)
                }
                Column(Modifier.weight(1f)) {
                    Text(task.title, style = MaterialTheme.typography.titleMedium)
                    Text("+¥${task.moneyReward} · +${task.coinReward} Coin · +${task.xpReward} XP", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WalletSnapshotCard(state: FamilyLocalState, modifier: Modifier = Modifier) {
    GrowthCard(modifier) {
        SectionTitle("成长钱包", "家庭内部教育账本")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DataPill("Money", "¥${state.wallet.money}", GrowthColors.Emerald, Modifier.weight(1f))
            DataPill("Coin", state.wallet.coin.toString(), GrowthColors.Amber, Modifier.weight(1f))
            DataPill("XP", state.wallet.xp.toString(), Color(0xFF55A6C8), Modifier.weight(1f))
        }
        Text("纯模拟基金市值 ¥${state.fund.marketValue}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LatestLedgerCard(state: FamilyLocalState, modifier: Modifier = Modifier) {
    GrowthCard(modifier) {
        SectionTitle("最近流水", "每次余额变化都有原因")
        if (state.ledger.isEmpty()) {
            EmptyInvitation("≋", "还没有流水", "完成任务、存入压岁钱或进行模拟交易后会出现在这里。")
        } else state.ledger.take(3).forEach { entry ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(entry.description, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    Text(entry.type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(formatDelta(entry), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = if (entry.moneyDelta.signum() < 0 || entry.coinDelta < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun TasksScreen(viewModel: FamilyAppViewModel) {
    if (viewModel.mode == AppMode.CHILD) {
        ChildTasksScreen(viewModel)
        return
    }
    var showAdd by remember { mutableStateOf(false) }
    val tasks = viewModel.state.tasks
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            SectionTitle("任务板", when (viewModel.mode) { AppMode.PARENT -> "创建任务、审核提交并发放奖励"; AppMode.CHILD -> "完成后提交给家长审核" }) {
                if (viewModel.mode == AppMode.PARENT) Button(onClick = { showAdd = true }) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text("创建任务") }
            }
        }
        item { ParentLearningReviews(viewModel) }
        if (tasks.isEmpty()) {
            item { GrowthCard { EmptyInvitation("✓", "任务板还是空的", if (viewModel.mode == AppMode.PARENT) "创建第一个任务，写清时间和三类奖励。" else "请家长创建任务后再回来。", if (viewModel.mode == AppMode.PARENT) "创建任务" else null) { showAdd = true } } }
        } else {
            items(tasks, key = { it.id }) { task -> TaskCard(task, viewModel) }
        }
    }
    if (showAdd) AddTaskDialog({ showAdd = false }) { title, minutes, rewardMoney, coin, xp ->
        viewModel.addTask(title, minutes, rewardMoney, coin, xp)
        showAdd = false
    }
}

@Composable
private fun ChildTasksScreen(viewModel: FamilyAppViewModel) {
    val feedback = rememberChildControlFeedback(viewModel.state.experience)
    val supportsSelfLearning = viewModel.state.experience.effectiveStage in setOf(SchoolStage.PRIMARY, SchoolStage.JUNIOR_MIDDLE, SchoolStage.SENIOR_HIGH)
    var selfLearning by remember(viewModel.state.experience.effectiveStage) { mutableStateOf(false) }
    val todo = viewModel.state.tasks.filter { it.status == TaskStatus.TODO }.take(3)
    val hasActiveLearning = viewModel.learningAssignments.any { it.status != "COMPLETED" }
    val waitingCount = viewModel.state.tasks.count { it.status == TaskStatus.SUBMITTED }
    val doneCount = viewModel.state.tasks.count { it.status == TaskStatus.APPROVED }
    Column(Modifier.fillMaxSize()) {
        if (supportsSelfLearning) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!selfLearning) Button(onClick = {}, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("我的任务") }
                else OutlinedButton(onClick = { feedback { selfLearning = false } }, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("我的任务") }
                if (selfLearning) Button(onClick = {}, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("自主学习") }
                else OutlinedButton(onClick = { feedback { selfLearning = true } }, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("自主学习") }
            }
        }
        if (selfLearning) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (viewModel.learningAssignments.isNotEmpty()) {
                    Column(Modifier.padding(horizontal = 24.dp)) {
                        SectionTitle("适合当前阶段的家庭课程", "学段由家长配置；视频不会自动播放")
                        ChildLearningPath(viewModel)
                    }
                }
                Box(Modifier.weight(1f)) {
                    OfficialSelfLearningScreen(viewModel.state.experience, viewModel.childEducationCatalog, viewModel::requestParentForResource)
                }
            }
            return@Column
        }
        LazyColumn(
        Modifier.fillMaxWidth().weight(1f),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("小任务", style = MaterialTheme.typography.headlineMedium)
                Text("先做第一件。后面的事情不着急。", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { ChildLearningSyncStatus(viewModel) }
        item { ChildLearningPath(viewModel) }
        if (todo.isEmpty() && !hasActiveLearning) {
            item {
                GrowthCard {
                    EmptyInvitation("✓", if (waitingCount > 0) "正在等家长看看" else "现在没有小任务", if (waitingCount > 0) "你已经完成这一步了。" else "去玩、去读书，或者请家长一起想一件小事。")
                }
            }
        } else if (todo.isNotEmpty() && !hasActiveLearning) {
            item { ChildTaskCard(todo.first(), primary = true) { feedback { viewModel.submitTask(todo.first().id) } } }
            if (todo.size > 1) {
                item {
                    GrowthCard {
                        Text("后来再做", style = MaterialTheme.typography.titleLarge)
                        todo.drop(1).forEach { task ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.RadioButtonUnchecked, null, tint = ChildColors.Moss)
                                Text(task.title, Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        } else if (todo.isNotEmpty()) {
            item {
                GrowthCard {
                    Text("后来再做", style = MaterialTheme.typography.titleLarge)
                    Text("先完成上面的学习。这里的小任务会替你留着。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    todo.forEach { task ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.RadioButtonUnchecked, null, tint = ChildColors.Moss)
                            Text(task.title, Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
        if (waitingCount > 0 || doneCount > 0) {
            item {
                Surface(shape = MaterialTheme.shapes.large, color = ChildColors.Mist.copy(alpha = .6f)) {
                    Text(
                        when {
                            waitingCount > 0 && doneCount > 0 -> "有小任务正等家长看，也有小任务已经做好了。"
                            waitingCount > 0 -> "有小任务正等家长看。"
                            else -> "有小任务已经做好了。"
                        },
                        Modifier.fillMaxWidth().padding(20.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = ChildColors.Ink,
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun ChildTaskCard(task: LocalGrowthTask, primary: Boolean, submit: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = ChildColors.Paper,
        border = androidx.compose.foundation.BorderStroke(if (primary) 2.dp else 1.dp, if (primary) ChildColors.Sun else ChildColors.Mist),
    ) {
        Column(Modifier.fillMaxWidth().padding(26.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("现在做", style = MaterialTheme.typography.titleMedium, color = ChildColors.Moss)
            Text(task.title, style = MaterialTheme.typography.headlineMedium, color = ChildColors.Ink)
            Text("做好后给家长看。", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = submit, Modifier.fillMaxWidth().heightIn(min = 64.dp), colors = ButtonDefaults.buttonColors(containerColor = ChildColors.Moss)) {
                Text("我做好了", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun TaskCard(task: LocalGrowthTask, viewModel: FamilyAppViewModel) {
    GrowthCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(task.title, style = MaterialTheme.typography.titleLarge)
                Text("预计 ${task.minutes} 分钟", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val (status, color) = when (task.status) {
                TaskStatus.TODO -> "待完成" to GrowthColors.Slate
                TaskStatus.SUBMITTED -> "待审核" to GrowthColors.Amber
                TaskStatus.APPROVED -> "已完成" to GrowthColors.Emerald
            }
            Surface(shape = MaterialTheme.shapes.small, color = color.copy(alpha = .12f)) { Text(status, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = color, style = MaterialTheme.typography.labelLarge) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DataPill("Money", "+¥${task.moneyReward}", GrowthColors.Emerald, Modifier.weight(1f))
            DataPill("Coin", "+${task.coinReward}", GrowthColors.Amber, Modifier.weight(1f))
            DataPill("XP", "+${task.xpReward}", Color(0xFF55A6C8), Modifier.weight(1f))
        }
        when {
            viewModel.mode == AppMode.CHILD && task.status == TaskStatus.TODO -> Button(onClick = { viewModel.submitTask(task.id) }, Modifier.fillMaxWidth()) { Text("提交任务") }
            viewModel.mode == AppMode.PARENT && task.status == TaskStatus.SUBMITTED -> Button(onClick = { viewModel.approveTask(task.id) }, Modifier.fillMaxWidth()) { Text("审核并发放奖励") }
        }
    }
}

@Composable
private fun AddTaskDialog(dismiss: () -> Unit, confirm: (String, Int, BigDecimal, Int, Int) -> Unit) {
    var title by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("20") }
    var reward by remember { mutableStateOf("1.00") }
    var coin by remember { mutableStateOf("1") }
    var xp by remember { mutableStateOf("10") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("创建成长任务") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            LabeledField(title, { title = it }, "任务名称")
            LabeledField(minutes, { minutes = it.filter(Char::isDigit).take(3) }, "预计分钟")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(reward, { reward = it }, Modifier.weight(1f), label = { Text("Money") }, singleLine = true)
                OutlinedTextField(coin, { coin = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("Coin") }, singleLine = true)
                OutlinedTextField(xp, { xp = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("XP") }, singleLine = true)
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { Button(onClick = {
            val parsedMoney = reward.toBigDecimalOrNull()?.setScale(2, RoundingMode.HALF_UP)
            val parsedMinutes = minutes.toIntOrNull(); val parsedCoin = coin.toIntOrNull(); val parsedXp = xp.toIntOrNull()
            if (title.isBlank() || parsedMoney == null || parsedMinutes == null || parsedCoin == null || parsedXp == null) error = "请完整填写有效内容"
            else confirm(title, parsedMinutes, parsedMoney, parsedCoin, parsedXp)
        }) { Text("创建任务") } },
        dismissButton = { TextButton(onClick = dismiss) { Text("取消") } },
    )
}

private fun formatDelta(entry: LocalLedgerEntry): String {
    val parts = mutableListOf<String>()
    if (entry.moneyDelta.signum() != 0) parts += (if (entry.moneyDelta.signum() > 0) "+¥${entry.moneyDelta}" else "-¥${entry.moneyDelta.abs()}")
    if (entry.coinDelta != 0) parts += (if (entry.coinDelta > 0) "+${entry.coinDelta}C" else "${entry.coinDelta}C")
    if (entry.xpDelta != 0) parts += "+${entry.xpDelta}XP"
    return parts.joinToString(" · ").ifEmpty { "记录" }
}
