package com.familygrowth.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import com.familygrowth.android.remote.ConnectionState
import com.familygrowth.android.update.UpdateUiState
import com.familygrowth.android.update.UpdateViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class WalletAction { GIFT, EXCHANGE, WITHDRAW }
private enum class GrowthDialog { REWARD, SAVING, WISH, FUND_BUY, FUND_NAV, SAVING_DEPOSIT }

@Composable
fun WalletScreen(viewModel: FamilyAppViewModel) {
    var action by remember { mutableStateOf<WalletAction?>(null) }
    val state = viewModel.state
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            GrowthCard {
                SectionTitle("钱包总览", "Money 是家庭内部教育账本，不代表银行余额")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DataPill("Money", "¥${state.wallet.money}", GrowthColors.Emerald, Modifier.weight(1f))
                    DataPill("Coin", state.wallet.coin.toString(), GrowthColors.Amber, Modifier.weight(1f))
                    DataPill("XP", state.wallet.xp.toString(), Color(0xFF55A6C8), Modifier.weight(1f))
                }
                if (viewModel.mode == AppMode.PARENT) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { action = WalletAction.GIFT }, Modifier.weight(1f)) { Icon(Icons.Rounded.CardGiftcard, null); Spacer(Modifier.width(6.dp)); Text("存入压岁钱") }
                        OutlinedButton(onClick = { action = WalletAction.EXCHANGE }, Modifier.weight(1f)) { Text("Money → Coin") }
                        OutlinedButton(onClick = { action = WalletAction.WITHDRAW }, Modifier.weight(1f)) { Text("零钱回收") }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { action = WalletAction.EXCHANGE }, Modifier.weight(1f)) { Text("兑换 Coin") }
                        OutlinedButton(onClick = { action = WalletAction.WITHDRAW }, Modifier.weight(1f)) { Text("申请零钱回收") }
                    }
                }
            }
        }
        if (state.withdrawals.isNotEmpty()) {
            item {
                GrowthCard {
                    SectionTitle("零钱回收申请", if (viewModel.mode == AppMode.PARENT) "家长确认后才扣款并形成流水" else "待家长确认；提交申请不会直接扣款")
                    state.withdrawals.take(5).forEach { request ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("申请 ¥${request.gross} · 预计到账 ¥${request.net}", style = MaterialTheme.typography.titleMedium)
                                Text("手续费 ¥${request.fee} · ${if (request.status == WithdrawalStatus.PENDING) "待家长确认" else "已确认"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (viewModel.mode == AppMode.PARENT && request.status == WithdrawalStatus.PENDING) {
                                Button(onClick = { viewModel.approveWithdrawal(request.id) }) { Text("确认") }
                            } else {
                                StatusDot(if (request.status == WithdrawalStatus.PENDING) "待确认" else "已处理", if (request.status == WithdrawalStatus.PENDING) GrowthColors.Amber else GrowthColors.Emerald)
                            }
                        }
                    }
                }
            }
        }
        item { SectionTitle("账本流水", "最新记录在前，余额变化不能被静默修改") }
        if (state.ledger.isEmpty()) {
            item { GrowthCard { EmptyInvitation("≋", "账本还没有记录", "任务审核、压岁钱、兑换、消费和模拟交易都会形成流水。") } }
        } else items(state.ledger, key = { it.id }) { entry -> LedgerCard(entry) }
    }
    action?.let { selected ->
        AmountDialog(
            title = when (selected) { WalletAction.GIFT -> "存入压岁钱"; WalletAction.EXCHANGE -> "兑换 Coin"; WalletAction.WITHDRAW -> "申请零钱回收" },
            label = "金额",
            supporting = when (selected) {
                WalletAction.GIFT -> "按基础比例 1:1 存入 Money。"
                WalletAction.EXCHANGE -> "基础体验比例：1 Money = 1 Coin。"
                WalletAction.WITHDRAW -> "家庭线下处理；基础手续费 2%，确认前展示预计到账。"
            },
            showWithdrawalPreview = selected == WalletAction.WITHDRAW,
            dismiss = { action = null },
        ) { amount ->
            when (selected) { WalletAction.GIFT -> viewModel.depositGift(amount); WalletAction.EXCHANGE -> viewModel.exchange(amount); WalletAction.WITHDRAW -> viewModel.withdraw(amount) }
            action = null
        }
    }
}

@Composable
private fun LedgerCard(entry: LocalLedgerEntry) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
                Text(ledgerIcon(entry.type), Modifier.padding(9.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(entry.description, style = MaterialTheme.typography.titleMedium)
                Text("${entry.type} · ${SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(entry.createdAt))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (entry.moneyDelta.signum() != 0) Text(signedMoney(entry.moneyDelta), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = deltaColor(entry.moneyDelta.signum()))
                if (entry.coinDelta != 0) Text("${if (entry.coinDelta > 0) "+" else ""}${entry.coinDelta} C", fontFamily = FontFamily.Monospace, color = deltaColor(entry.coinDelta))
                if (entry.xpDelta != 0) Text("+${entry.xpDelta} XP", fontFamily = FontFamily.Monospace, color = GrowthColors.Emerald)
            }
        }
    }
}

@Composable
fun GrowthScreen(viewModel: FamilyAppViewModel) {
    if (viewModel.mode == AppMode.CHILD) {
        ChildGrowthScreen(viewModel)
        return
    }
    var dialog by remember { mutableStateOf<GrowthDialog?>(null) }
    var selectedSaving by remember { mutableStateOf<String?>(null) }
    val state = viewModel.state
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionTitle("把奖励变成选择", "消费、储蓄、愿望和模拟投资都服务于成长复盘") }
        item {
            GrowthCard {
                SectionTitle("奖励商店", if (viewModel.mode == AppMode.PARENT) "家长定义家庭奖励" else "用 Coin 兑换，不产生真实支付") {
                    if (viewModel.mode == AppMode.PARENT) TextButton(onClick = { dialog = GrowthDialog.REWARD }) { Icon(Icons.Rounded.Add, null); Text("添加") }
                }
                if (state.rewards.isEmpty()) EmptyInvitation("🎁", "还没有家庭奖励", if (viewModel.mode == AppMode.PARENT) "添加一个可兑现的家庭奖励。" else "请家长先设置奖励项目。")
                else state.rewards.forEach { reward ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(reward.title, style = MaterialTheme.typography.titleMedium); Text("${reward.coinPrice} Coin", color = GrowthColors.Amber, fontFamily = FontFamily.Monospace) }
                        Button(onClick = { viewModel.redeemReward(reward.id) }, enabled = state.wallet.coin >= reward.coinPrice) { Text("兑换给孩子") }
                    }
                }
            }
        }
        item {
            BoxWithConstraints {
                val wide = maxWidth >= 720.dp
                if (wide) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SavingCard(viewModel, Modifier.weight(1f), { dialog = GrowthDialog.SAVING }) { id -> selectedSaving = id; dialog = GrowthDialog.SAVING_DEPOSIT }
                    WishCard(viewModel, Modifier.weight(1f)) { dialog = GrowthDialog.WISH }
                } else Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SavingCard(viewModel, onAdd = { dialog = GrowthDialog.SAVING }) { id -> selectedSaving = id; dialog = GrowthDialog.SAVING_DEPOSIT }
                    WishCard(viewModel) { dialog = GrowthDialog.WISH }
                }
            }
        }
        item { FundCard(viewModel, onBuy = { dialog = GrowthDialog.FUND_BUY }, onNav = { dialog = GrowthDialog.FUND_NAV }) }
    }

    when (dialog) {
        GrowthDialog.REWARD -> TitleNumberDialog("添加家庭奖励", "奖励名称", "Coin 价格", { dialog = null }) { title, value -> viewModel.addReward(title, value.toInt()); dialog = null }
        GrowthDialog.SAVING -> TitleMoneyDialog("创建储蓄目标", "目标名称", { dialog = null }) { title, value -> viewModel.addSaving(title, value); dialog = null }
        GrowthDialog.WISH -> TitleMoneyDialog("记录愿望", "愿望名称", { dialog = null }) { title, value -> viewModel.addWish(title, value); dialog = null }
        GrowthDialog.FUND_BUY -> AmountDialog("购买纯模拟基金", "投入 Money", "按当前教学 NAV 计算份额，不接真实行情。", dismiss = { dialog = null }) { viewModel.buyFund(it); dialog = null }
        GrowthDialog.FUND_NAV -> AmountDialog("调整教学 NAV", "新 NAV", "只影响模拟持仓市值，可涨也可跌。", dismiss = { dialog = null }, scale = 4) { viewModel.updateNav(it); dialog = null }
        GrowthDialog.SAVING_DEPOSIT -> AmountDialog("存入储蓄目标", "金额", "Money 将转入目标并形成流水。", dismiss = { dialog = null }) { amount -> selectedSaving?.let { viewModel.saveToGoal(it, amount) }; dialog = null }
        null -> Unit
    }
}

@Composable
private fun ChildGrowthScreen(viewModel: FamilyAppViewModel) {
    val state = viewModel.state
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("我的成长", style = MaterialTheme.typography.headlineMedium)
                Text("看一看就好。要选择时，请家长一起。", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            GrowthCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Surface(shape = androidx.compose.foundation.shape.CircleShape, color = ChildColors.Sun, modifier = Modifier.size(64.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text("★", style = MaterialTheme.typography.headlineMedium, color = ChildColors.Ink) }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("我的小星星", style = MaterialTheme.typography.titleLarge)
                        Text("${state.wallet.coin} 颗", style = MaterialTheme.typography.headlineMedium, color = ChildColors.Moss)
                        Text("完成小任务后，家长会告诉你得到多少。", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            GrowthCard {
                SectionTitle("家长帮我存起来", "这是家庭里的成长记录")
                Text("¥${state.wallet.money}", style = MaterialTheme.typography.headlineMedium, color = ChildColors.Moss)
                Text("想用、想换或想了解时，先和家长说一说。", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            GrowthCard {
                SectionTitle("可以期待的家庭奖励", "和家长一起选择")
                if (state.rewards.isEmpty()) {
                    EmptyInvitation("☆", "还没有家庭奖励", "家长可以先放进一个一起约定的小奖励。")
                } else {
                    state.rewards.take(3).forEach { reward ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("☆", style = MaterialTheme.typography.headlineSmall, color = ChildColors.Sun)
                            Text(reward.title, Modifier.padding(start = 12.dp).weight(1f), style = MaterialTheme.typography.bodyLarge)
                            Text("${reward.coinPrice} 颗星", style = MaterialTheme.typography.bodyLarge, color = ChildColors.Moss)
                        }
                    }
                }
            }
        }
        if (state.wishes.isNotEmpty()) {
            item {
                GrowthCard {
                    SectionTitle("我的小愿望", "慢慢来，不着急")
                    state.wishes.take(2).forEach { wish ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("♡", style = MaterialTheme.typography.headlineSmall, color = ChildColors.Coral)
                            Text(wish.title, Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
        item {
            Text(
                "更多事情由家长管理。你可以随时请家长一起看看。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun SavingCard(viewModel: FamilyAppViewModel, modifier: Modifier = Modifier, onAdd: () -> Unit, onDeposit: (String) -> Unit) {
    GrowthCard(modifier) {
        SectionTitle("储蓄目标", "从钱包分配到长期目标") { TextButton(onClick = onAdd) { Text("新增") } }
        if (viewModel.state.savings.isEmpty()) EmptyInvitation("◎", "还没有储蓄目标", "给一个想实现的目标定下金额。")
        else viewModel.state.savings.forEach { goal ->
            val progress = if (goal.target.signum() == 0) 0f else goal.saved.divide(goal.target, 4, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row { Text(goal.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium); Text("¥${goal.saved} / ¥${goal.target}", fontFamily = FontFamily.Monospace) }
                LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth())
                TextButton(onClick = { onDeposit(goal.id) }, enabled = viewModel.state.wallet.money > BigDecimal.ZERO) { Text("存入") }
            }
        }
    }
}

@Composable
private fun WishCard(viewModel: FamilyAppViewModel, modifier: Modifier = Modifier, onAdd: () -> Unit) {
    GrowthCard(modifier) {
        SectionTitle("愿望清单", "先说清想要什么，再决定如何实现") { TextButton(onClick = onAdd) { Text("记录") } }
        if (viewModel.state.wishes.isEmpty()) EmptyInvitation("☆", "还没有愿望", "记录一个愿望和大致目标金额。")
        else viewModel.state.wishes.forEach { wish ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("☆", color = GrowthColors.Amber, style = MaterialTheme.typography.headlineSmall)
                Column(Modifier.padding(start = 10.dp).weight(1f)) { Text(wish.title, style = MaterialTheme.typography.titleMedium); Text("目标 ¥${wish.target}", fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun FundCard(viewModel: FamilyAppViewModel, onBuy: () -> Unit, onNav: () -> Unit) {
    val fund = viewModel.state.fund
    GrowthCard {
        SectionTitle("纯模拟成长基金", "教育模拟 · NAV 可涨可跌 · 不接真实行情")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DataPill("教学 NAV", fund.nav.toPlainString(), GrowthColors.Emerald, Modifier.weight(1f))
            DataPill("持有份额", fund.shares.toPlainString(), Color(0xFF55A6C8), Modifier.weight(1f))
            DataPill("模拟市值", "¥${fund.marketValue}", GrowthColors.Amber, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onBuy, enabled = viewModel.state.wallet.money > BigDecimal.ZERO, modifier = Modifier.weight(1f)) { Text("购买模拟份额") }
            OutlinedButton(onClick = { viewModel.sellFund() }, enabled = fund.shares > BigDecimal.ZERO, modifier = Modifier.weight(1f)) { Text("全部赎回") }
            if (viewModel.mode == AppMode.PARENT) OutlinedButton(onClick = onNav, modifier = Modifier.weight(1f)) { Text("调整 NAV") }
        }
    }
}

@Composable
fun ParentScreen(viewModel: FamilyAppViewModel, updateViewModel: UpdateViewModel) {
    if (viewModel.mode != AppMode.PARENT) return
    var showUsage by remember { mutableStateOf(false) }
    var showConnection by remember { mutableStateOf(false) }
    val state = viewModel.state
    val approved = state.tasks.count { it.status == TaskStatus.APPROVED }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            SectionTitle("家庭家长中心", "可连接自有服务端；Token 仅保留在本次 App 进程内")
        }
        item { ServiceConnectionCard(viewModel) { showConnection = true } }
        item {
            BoxWithConstraints {
                val wide = maxWidth >= 720.dp
                if (wide) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ParentReportCard(state, approved, Modifier.weight(1f))
                    UsagePolicyCard(state, Modifier.weight(1f)) { showUsage = true }
                } else Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ParentReportCard(state, approved)
                    UsagePolicyCard(state) { showUsage = true }
                }
            }
        }
        item { UpdatePanel(updateViewModel) }
        item {
            GrowthCard {
                SectionTitle("生产接入边界", "服务端授权是最终边界")
                Text("连接后同步任务、钱包和今日审核数据；孩子提交与家长确认写入真实服务端账本。")
                Text("离线本机数据仍可使用，但明确标为本机状态；不把断线操作伪装成已同步。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (showUsage) UsageDialog(state.usage, { showUsage = false }) { daily, session -> viewModel.updateUsage(daily, session); showUsage = false }
    if (showConnection) ServiceConnectionDialog(viewModel) { showConnection = false }
}

@Composable
private fun ServiceConnectionCard(viewModel: FamilyAppViewModel, configure: () -> Unit) {
    GrowthCard {
        SectionTitle("家庭服务", "HTTPS；开发版可连接 loopback/私网 HTTP")
        when (val connection = viewModel.connectionState) {
            ConnectionState.Disconnected -> { Text("未连接，当前页面显示本机状态。", color = MaterialTheme.colorScheme.onSurfaceVariant); Button(onClick = configure) { Text("连接服务") } }
            ConnectionState.Connecting -> { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("正在安全登录并同步…") }
            ConnectionState.Expired -> { Text("会话已过期，Token 已从内存清除。", color = MaterialTheme.colorScheme.error); Button(onClick = configure) { Text("重新登录") } }
            is ConnectionState.Error -> { Text(connection.message, color = MaterialTheme.colorScheme.error); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = configure) { Text("检查连接") }; TextButton(onClick = viewModel::disconnectService) { Text("清除") } } }
            is ConnectionState.Connected -> {
                val snapshot = connection.snapshot
                StatusDot("已同步 · ${snapshot.childName}", GrowthColors.Emerald)
                Text("服务端任务 ${snapshot.tasks.size} · 待审核 ${snapshot.pendingReviews} · 今日完成 ${snapshot.approvedToday}")
                Text("Money ¥${snapshot.money} · Coin ${snapshot.coin}", fontFamily = FontFamily.Monospace)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = viewModel::refreshService) { Text("同步") }; TextButton(onClick = viewModel::disconnectService) { Text("断开并清除 Token") } }
            }
        }
    }
}

@Composable
private fun ServiceConnectionDialog(viewModel: FamilyAppViewModel, dismiss: () -> Unit) {
    var base by remember { mutableStateOf("") }; var family by remember { mutableStateOf("") }; var parent by remember { mutableStateOf("") }; var child by remember { mutableStateOf("") }; var pin by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = dismiss, title = { Text("连接家庭服务") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("这些标识只用于本次连接；PIN 与 Token 不写入本地文件或日志。", style = MaterialTheme.typography.bodySmall)
            LabeledField(base,{base=it},"服务地址（例如 https://family.example）")
            LabeledField(family,{family=it.trim()},"Family ID")
            LabeledField(parent,{parent=it.trim()},"Parent ID")
            LabeledField(child,{child=it.trim()},"Child ID")
            LabeledField(pin,{pin=it.filter(Char::isDigit).take(6)},"6 位服务端 PIN")
        }
    }, confirmButton = { Button(onClick = { viewModel.connectService(base,family,parent,child,pin);dismiss() }, enabled = base.isNotBlank()&&family.isNotBlank()&&parent.isNotBlank()&&child.isNotBlank()&&pin.length==6) { Text("登录并同步") } }, dismissButton = { TextButton(onClick = dismiss) { Text("取消") } })
}

@Composable
private fun ParentReportCard(state: FamilyLocalState, approved: Int, modifier: Modifier = Modifier) {
    GrowthCard(modifier) {
        SectionTitle("今日报告", "来自本机实际记录")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DataPill("完成任务", "$approved/${state.tasks.size}", GrowthColors.Emerald, Modifier.weight(1f))
            DataPill("待审核", state.tasks.count { it.status == TaskStatus.SUBMITTED }.toString(), GrowthColors.Amber, Modifier.weight(1f))
            DataPill("使用", "${state.usage.usedMinutes}m", Color(0xFF55A6C8), Modifier.weight(1f))
        }
        Text("零钱回收待确认 ${state.withdrawals.count { it.status == WithdrawalStatus.PENDING }} 笔", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("累计 XP ${state.wallet.xp} · Money ¥${state.wallet.money} · Coin ${state.wallet.coin}", fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun UsagePolicyCard(state: FamilyLocalState, modifier: Modifier = Modifier, edit: () -> Unit) {
    GrowthCard(modifier) {
        SectionTitle("本 App 防沉迷", "只统计孩子模式，不控制其他应用") { TextButton(onClick = edit) { Text("调整") } }
        Text("每日 ${state.usage.dailyLimitMinutes} 分钟", style = MaterialTheme.typography.titleLarge)
        Text("单次 ${state.usage.sessionLimitMinutes} 分钟 · 今日已用 ${state.usage.usedMinutes} 分钟", color = MaterialTheme.colorScheme.onSurfaceVariant)
        LinearProgressIndicator(progress = { (state.usage.usedMinutes.toFloat() / state.usage.dailyLimitMinutes).coerceIn(0f, 1f) }, Modifier.fillMaxWidth())
    }
}

@Composable
private fun UpdatePanel(viewModel: UpdateViewModel) {
    val state = viewModel.state
    GrowthCard {
        SectionTitle("应用更新", "公开 GitHub Release · SHA-256 校验 · 系统确认")
        when (state) {
            UpdateUiState.Unconfigured -> { Text("当前构建未配置 GitHub 更新源。使用 GITHUB_REPOSITORY=owner/repo 构建正式包。", color = MaterialTheme.colorScheme.onSurfaceVariant); Button({}, enabled = false) { Text("检查更新") } }
            UpdateUiState.Idle -> { Text("检查项目公开 Release 的更高语义版本。"); Button(onClick = viewModel::check) { Text("检查更新") } }
            UpdateUiState.Checking -> { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("正在检查…") }
            UpdateUiState.UpToDate -> { Text("当前已是最新版本。"); TextButton(onClick = viewModel::check) { Text("重新检查") } }
            is UpdateUiState.Available -> { Text("发现 ${state.update.version}，下载后先校验再交给系统安装。"); Button(onClick = { viewModel.download(state.update) }) { Text("下载更新") } }
            is UpdateUiState.Downloading -> { LinearProgressIndicator(progress = { state.percent / 100f }, Modifier.fillMaxWidth()); Text("下载并校验 ${state.percent}%") }
            is UpdateUiState.Ready -> { Text("APK 已通过 SHA-256 校验。"); Button(onClick = { viewModel.install(state.update, state.file) }) { Text("打开系统安装界面") } }
            is UpdateUiState.PermissionRequired -> { Text("请允许本应用安装未知来源应用，返回后继续。"); Button(onClick = { viewModel.install(state.update, state.file) }) { Text("继续安装") } }
            is UpdateUiState.Error -> { Text(state.message, color = MaterialTheme.colorScheme.error); TextButton(onClick = viewModel::check) { Text("重试") } }
        }
    }
}

@Composable
private fun AmountDialog(
    title: String,
    label: String,
    supporting: String,
    dismiss: () -> Unit,
    showWithdrawalPreview: Boolean = false,
    scale: Int = 2,
    confirm: (BigDecimal) -> Unit,
) {
    var value by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val amount = value.toBigDecimalOrNull()?.setScale(scale, RoundingMode.HALF_UP)
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = { Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        LabeledField(value, { value = it }, label)
        Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (showWithdrawalPreview && amount != null && amount > BigDecimal.ZERO) {
            val fee = amount.multiply(BigDecimal("0.02")).setScale(2, RoundingMode.HALF_UP)
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondary.copy(alpha = .12f)) {
                Column(Modifier.padding(12.dp)) { Text("手续费 ¥$fee"); Text("预计线下到账 ¥${amount.subtract(fee)}", fontWeight = FontWeight.Bold) }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    } }, confirmButton = { Button(onClick = { if (amount == null || amount <= BigDecimal.ZERO) error = "请输入大于 0 的有效数值" else confirm(amount) }) { Text("确认") } }, dismissButton = { TextButton(onClick = dismiss) { Text("取消") } })
}

@Composable
private fun TitleMoneyDialog(title: String, field: String, dismiss: () -> Unit, confirm: (String, BigDecimal) -> Unit) {
    var text by remember { mutableStateOf("") }; var amount by remember { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = { Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        LabeledField(text, { text = it }, field); LabeledField(amount, { amount = it }, "目标金额")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    } }, confirmButton = { Button(onClick = { val parsed = amount.toBigDecimalOrNull()?.setScale(2, RoundingMode.HALF_UP); if (text.isBlank() || parsed == null || parsed <= BigDecimal.ZERO) error = "请填写有效名称和金额" else confirm(text, parsed) }) { Text("保存") } }, dismissButton = { TextButton(onClick = dismiss) { Text("取消") } })
}

@Composable
private fun TitleNumberDialog(title: String, field: String, numberField: String, dismiss: () -> Unit, confirm: (String, BigDecimal) -> Unit) {
    var text by remember { mutableStateOf("") }; var number by remember { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = { Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        LabeledField(text, { text = it }, field); LabeledField(number, { number = it.filter(Char::isDigit) }, numberField)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    } }, confirmButton = { Button(onClick = { val parsed = number.toBigDecimalOrNull(); if (text.isBlank() || parsed == null || parsed <= BigDecimal.ZERO) error = "请填写有效内容" else confirm(text, parsed) }) { Text("保存") } }, dismissButton = { TextButton(onClick = dismiss) { Text("取消") } })
}

@Composable
private fun UsageDialog(policy: UsagePolicy, dismiss: () -> Unit, confirm: (Int, Int) -> Unit) {
    var daily by remember { mutableStateOf(policy.dailyLimitMinutes.toString()) }; var session by remember { mutableStateOf(policy.sessionLimitMinutes.toString()) }; var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = dismiss, title = { Text("调整本 App 使用时长") }, text = { Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        LabeledField(daily, { daily = it.filter(Char::isDigit) }, "每日分钟（10–480）"); LabeledField(session, { session = it.filter(Char::isDigit) }, "单次分钟（5–240）")
        Text("达到限制后阻断孩子工作区，家长 PIN 可进入管理。", style = MaterialTheme.typography.bodySmall)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    } }, confirmButton = { Button(onClick = { val d = daily.toIntOrNull(); val s = session.toIntOrNull(); if (d == null || s == null || d !in 10..480 || s !in 5..240 || s > d) error = "请设置有效时长" else confirm(d, s) }) { Text("保存规则") } }, dismissButton = { TextButton(onClick = dismiss) { Text("取消") } })
}

private fun ledgerIcon(type: String) = when {
    "REWARD" in type -> "✓"
    "FUND" in type -> "↗"
    "GIFT" in type -> "礼"
    "WITHDRAW" in type -> "¥"
    else -> "≋"
}

private fun signedMoney(value: BigDecimal) = if (value.signum() >= 0) "+¥$value" else "-¥${value.abs()}"

@Composable
private fun deltaColor(sign: Int) = if (sign < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
