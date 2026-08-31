package com.familygrowth.android.ui

import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.familygrowth.android.R
import com.familygrowth.android.core.*
import com.familygrowth.android.remote.ConnectionState
import com.familygrowth.android.update.DownloadPhase
import com.familygrowth.android.update.DownloadProgress
import com.familygrowth.android.update.UpdateUiState
import com.familygrowth.android.update.UpdateViewModel
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class WalletAction { GIFT, EXCHANGE, WITHDRAW }
private enum class GrowthDialog { PLAN, MILESTONE, REWARD, SAVING, WISH, WISH_ALLOCATE, FUND_BUY, FUND_NAV, SAVING_DEPOSIT }
private enum class ChildGrowthArea { LESSONS, REWARDS }

@Composable
fun WalletScreen(viewModel: FamilyAppViewModel) {
    var action by remember { mutableStateOf<WalletAction?>(null) }
    var showGovernance by remember { mutableStateOf(false) }
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
        if(viewModel.mode==AppMode.PARENT) item { GrowthCard { SectionTitle("家庭奖励约定","额度是家长的护栏，不会作为压力显示给孩子");val b=viewModel.rewardBudget;if(b==null)Text("还没有设置奖励预算",color=MaterialTheme.colorScheme.onSurfaceVariant)else{Text("今天还可安排 ¥${b.remaining}",style=MaterialTheme.typography.titleLarge,fontFamily=FontFamily.Monospace);Text("日 / 周 / 月：¥${b.dailyLimit} / ¥${b.weeklyLimit} / ¥${b.monthlyLimit}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};val c=viewModel.exchangeControl;Text(if(c==null)"兑换护栏尚未设置" else "Money→Coin：每日 ¥${c.moneyDailyLimit}，每月 ¥${c.moneyMonthlyLimit} · ${if(c.childRequiresApproval)"孩子先请家长确认" else "可直接确认"}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Button(onClick={showGovernance=true}){Text(if(b==null||c==null)"设置家庭约定" else "调整家庭约定")};viewModel.exchangeApprovals.filter{it.status=="PENDING"}.forEach{request->HorizontalDivider();Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("孩子想进行一次兑换");Text("等待你的回应",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};TextButton(onClick={viewModel.reviewExchangeApproval(request.id,false)}){Text("这次先不换")};Button(onClick={viewModel.reviewExchangeApproval(request.id,true)}){Text("同意")}}} } }
        if(viewModel.mode==AppMode.CHILD&&viewModel.exchangeApprovals.isNotEmpty()) item { GrowthCard { SectionTitle("我的兑换请求","提交后不会马上扣除 Money");viewModel.exchangeApprovals.take(3).forEach{Text(when(it.status){"PENDING"->"正在等家长回应";"APPROVED"->"家长已经同意";else->"这次先不换，也没关系"},style=MaterialTheme.typography.titleMedium)} } }
        if(viewModel.rewardOrders.isNotEmpty()) item { GrowthCard { SectionTitle("奖励申请","批准时才扣除 Coin；现实里做到后由家长确认兑现");viewModel.rewardOrders.take(8).forEach{order->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){Column(Modifier.weight(1f)){Text(order.title,style=MaterialTheme.typography.titleMedium);Text("${order.coinCost} Coin · ${when(order.status){"CREATED"->"待审核";"APPROVED"->"等待兑现";"FULFILLED"->"已经兑现";"REJECTED"->"这次先不换";else->"已取消"}}",color=MaterialTheme.colorScheme.onSurfaceVariant)};if(order.status=="CREATED"&&viewModel.mode==AppMode.PARENT)Row{TextButton(onClick={viewModel.reviewRewardOrder(order.id,false)}){Text("拒绝")};Button(onClick={viewModel.reviewRewardOrder(order.id,true)}){Text("批准")}}else if(order.status=="APPROVED"&&viewModel.mode==AppMode.PARENT)Button(onClick={viewModel.fulfillRewardOrder(order.id)}){Text("确认已经做到")}else StatusDot(if(order.status=="FULFILLED")"已兑现" else if(order.status=="APPROVED")"等待家长做到" else "已回应",if(order.status=="FULFILLED")GrowthColors.Emerald else MaterialTheme.colorScheme.outline)}}} }
        if (state.withdrawals.isNotEmpty()) {
            item {
                GrowthCard {
                    SectionTitle("零钱回收申请", if (viewModel.mode == AppMode.PARENT) "家长确认后才扣款并形成流水" else "待家长确认；提交申请不会直接扣款")
                    state.withdrawals.take(5).forEach { request ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("申请 ¥${request.gross} · 预计到账 ¥${request.net}", style = MaterialTheme.typography.titleMedium)
                                Text("手续费 ¥${request.fee} · ${when(request.status){WithdrawalStatus.PENDING->"待家长确认";WithdrawalStatus.APPROVED->"额度已冻结，待线下支付";WithdrawalStatus.PAID->"已确认线下支付";WithdrawalStatus.REJECTED->"已拒绝";WithdrawalStatus.CANCELLED->"已取消"}}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (viewModel.mode == AppMode.PARENT && request.status == WithdrawalStatus.PENDING) {
                                Row{TextButton(onClick={viewModel.rejectWithdrawal(request.id)}){Text("拒绝")};Button(onClick = { viewModel.approveWithdrawal(request.id) }) { Text("批准") }}
                            } else if(viewModel.mode==AppMode.PARENT&&request.status==WithdrawalStatus.APPROVED){
                                Button(onClick={viewModel.markWithdrawalPaid(request.id)}){Text("确认已线下支付")}
                            } else if(viewModel.mode==AppMode.CHILD&&request.status==WithdrawalStatus.PENDING){
                                TextButton(onClick={viewModel.cancelWithdrawalRequest(request.id)}){Text("取消申请")}
                            } else {
                                StatusDot(if(request.status==WithdrawalStatus.PAID)"已支付" else "已处理",if(request.status==WithdrawalStatus.PAID)GrowthColors.Emerald else MaterialTheme.colorScheme.outline)
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
                WalletAction.GIFT -> "按输入金额存入家庭教育账本。"
                WalletAction.EXCHANGE -> "下一步会展示服务端当前比例、费用和到账 Coin。"
                WalletAction.WITHDRAW -> "家庭线下处理；下一步会展示当前费用和预计到账。"
            },
            showWithdrawalPreview = selected == WalletAction.WITHDRAW,
            dismiss = { action = null },
        ) { amount ->
            when (selected) { WalletAction.GIFT -> viewModel.depositGift(amount); WalletAction.EXCHANGE -> viewModel.exchange(amount); WalletAction.WITHDRAW -> viewModel.withdraw(amount) }
            action = null
        }
    }
    viewModel.pendingExchangePreview?.let { preview ->
        AlertDialog(onDismissRequest=viewModel::cancelExchange,title={Text(if(viewModel.mode==AppMode.CHILD&&viewModel.exchangeControl?.childRequiresApproval==true)"请家长看看" else "确认 Money → Coin")},text={Column(verticalArrangement=Arrangement.spacedBy(6.dp)){Text("扣除 Money ¥${preview.sourceAmount}");Text("费用 ¥${preview.fee}");Text("预计到账 ${preview.targetAmount.stripTrailingZeros().toPlainString()} Coin",fontWeight=FontWeight.Bold);Text(preview.notice,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);if(viewModel.mode==AppMode.CHILD&&viewModel.exchangeControl?.childRequiresApproval==true)Text("发送后只是等待家长回应，现在不会扣除 Money。",style=MaterialTheme.typography.bodySmall)}},confirmButton={Button(onClick=viewModel::confirmExchange){Text(if(viewModel.mode==AppMode.CHILD&&viewModel.exchangeControl?.childRequiresApproval==true)"请家长确认" else "确认兑换")}},dismissButton={TextButton(onClick=viewModel::cancelExchange){Text("取消")}})
    }
    if(showGovernance) RewardGovernanceDialog(viewModel){showGovernance=false}
    viewModel.pendingWithdrawalQuote?.let { quote ->
        AlertDialog(onDismissRequest=viewModel::cancelWithdrawal,title={Text("确认零钱回收申请")},text={Column(verticalArrangement=Arrangement.spacedBy(6.dp)){Text("账本金额 ¥${quote.moneyAmount}");Text("线下约定金额 ¥${quote.gross}");Text("费用 ¥${quote.fee}");Text("预计线下到账 ¥${quote.net}",fontWeight=FontWeight.Bold);Text(quote.notice,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text("提交申请不会立即扣款，仍需家长确认。",style=MaterialTheme.typography.bodySmall)}},confirmButton={Button(onClick=viewModel::confirmWithdrawal){Text("提交申请")}},dismissButton={TextButton(onClick=viewModel::cancelWithdrawal){Text("取消")}})
    }
}

@Composable private fun RewardGovernanceDialog(viewModel:FamilyAppViewModel,dismiss:()->Unit){
    var daily by remember{mutableStateOf(viewModel.rewardBudget?.dailyLimit?.toPlainString()?:"10.00")};var weekly by remember{mutableStateOf(viewModel.rewardBudget?.weeklyLimit?.toPlainString()?:"50.00")};var monthly by remember{mutableStateOf(viewModel.rewardBudget?.monthlyLimit?.toPlainString()?:"150.00")};var exchangeDaily by remember{mutableStateOf(viewModel.exchangeControl?.moneyDailyLimit?.toPlainString()?:"20.00")};var exchangeMonthly by remember{mutableStateOf(viewModel.exchangeControl?.moneyMonthlyLimit?.toPlainString()?:"200.00")};val values=listOf(daily,weekly,monthly,exchangeDaily,exchangeMonthly).map{it.toBigDecimalOrNull()};val valid=values.all{it!=null&&it.signum()>=0}&&values[1]!!>=values[0]!!&&values[2]!!>=values[1]!!&&values[4]!!>=values[3]!!
    AlertDialog(onDismissRequest=dismiss,title={Text("调整家庭奖励约定")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("奖励 Money 预算",fontWeight=FontWeight.Bold);OutlinedTextField(daily,{daily=it},label={Text("每天上限")},singleLine=true);OutlinedTextField(weekly,{weekly=it},label={Text("每周上限")},singleLine=true);OutlinedTextField(monthly,{monthly=it},label={Text("每月上限")},singleLine=true);Text("超出预算的 Money 按固定 1:1 转为 Coin，不隐藏减少。",style=MaterialTheme.typography.bodySmall);HorizontalDivider();Text("单向兑换额度",fontWeight=FontWeight.Bold);OutlinedTextField(exchangeDaily,{exchangeDaily=it},label={Text("每天上限")},singleLine=true);OutlinedTextField(exchangeMonthly,{exchangeMonthly=it},label={Text("每月上限")},singleLine=true);Text("Money↔Coin 使用同一组额度；孩子发起后必须由家长确认。",style=MaterialTheme.typography.bodySmall)}},confirmButton={Button(enabled=valid,onClick={viewModel.configureRewardGovernance(values[0]!!,values[1]!!,values[2]!!,values[3]!!,values[4]!!);dismiss()}){Text("保存约定")}},dismissButton={TextButton(onClick=dismiss){Text("取消")}})
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
    var selectedWish by remember { mutableStateOf<String?>(null) }
    var photoMilestone by remember { mutableStateOf<String?>(null) }
    val context=LocalContext.current
    val photoPicker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->uri?.let{selected->val mime=context.contentResolver.getType(selected).orEmpty();val bytes=runCatching{context.contentResolver.openInputStream(selected)?.use{it.readBytes()}}.getOrNull();val milestoneId=photoMilestone;photoMilestone=null;if(bytes!=null&&milestoneId!=null)viewModel.uploadGrowthPhoto(milestoneId,bytes,mime,"家长记录的成长照片")}}
    val state = viewModel.state
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionTitle("家庭成长年轮", "记录真实变化，不给孩子打分，也不比较快慢") }
        item {
            GrowthCard {
                SectionTitle("正在生长的枝条", "计划只是方向，随时可以暂停；完成后历史仍会保留") { TextButton(onClick={dialog=GrowthDialog.PLAN}){Icon(Icons.Rounded.Add,null);Text("新计划")} }
                viewModel.growthReport?.let{report->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){DataPill("进行中",report.activePlans.toString(),GrowthColors.Emerald,Modifier.weight(1f));DataPill("真实记录",report.milestones.toString(),GrowthColors.Amber,Modifier.weight(1f));DataPill("照片",report.artifacts.toString(),Color(0xFF5E7D8C),Modifier.weight(1f))}}
                if(viewModel.growthPlans.isEmpty()) EmptyInvitation("🌱","还没有成长计划","从一个现实中能观察的小行动开始。")
                else viewModel.growthPlans.take(6).forEach{plan->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){Column(Modifier.weight(1f)){Text(plan.title,style=MaterialTheme.typography.titleMedium);Text(plan.target.ifBlank{"关注真实变化"},color=MaterialTheme.colorScheme.onSurfaceVariant);StatusDot(when(plan.status){"ACTIVE"->"正在生长";"PAUSED"->"先休息";"COMPLETED"->"已经走过";else->"准备中"},if(plan.status=="ACTIVE")GrowthColors.Emerald else MaterialTheme.colorScheme.outline)};if(plan.status=="ACTIVE")TextButton(onClick={viewModel.completeGrowthPlan(plan)}){Text("完成")}}}
            }
        }
        item {
            GrowthCard {
                SectionTitle("今天看见了什么", "写具体动作和回应，不写“聪明”“落后”等标签") { TextButton(onClick={dialog=GrowthDialog.MILESTONE}){Icon(Icons.Rounded.Add,null);Text("记录")}}
                if(viewModel.growthMilestones.isEmpty()) EmptyInvitation("🍃","还没有成长记录","例如：今天自己把绘本放回书架。")
                else viewModel.growthMilestones.take(10).forEach{m->Column(Modifier.fillMaxWidth().padding(vertical=8.dp)){Text(m.title,style=MaterialTheme.typography.titleMedium);Text(m.observation,color=MaterialTheme.colorScheme.onSurfaceVariant);Row(verticalAlignment=Alignment.CenterVertically){Text(m.occurredOn,style=MaterialTheme.typography.bodySmall,color=Color(0xFF5E7D8C));Spacer(Modifier.weight(1f));if(m.artifacts.isNotEmpty())Text("${m.artifacts.size} 张照片",style=MaterialTheme.typography.bodySmall);TextButton(onClick={photoMilestone=m.id;photoPicker.launch("image/*")}){Text("加照片")}}}}
            }
        }
        item { SectionTitle("把奖励变成选择", "消费、储蓄、愿望和模拟投资都服务于成长复盘") }
        item {
            GrowthCard {
                SectionTitle("奖励商店", if (viewModel.mode == AppMode.PARENT) "家长定义家庭奖励" else "用 Coin 兑换，不产生真实支付") {
                    if (viewModel.mode == AppMode.PARENT) TextButton(onClick = { dialog = GrowthDialog.REWARD }) { Icon(Icons.Rounded.Add, null); Text("添加") }
                }
                if (state.rewards.isEmpty()) EmptyInvitation("🎁", "还没有家庭奖励", if (viewModel.mode == AppMode.PARENT) "添加一个可兑现的家庭奖励。" else "请家长先设置奖励项目。")
                else state.rewards.forEach { reward ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(reward.title, style = MaterialTheme.typography.titleMedium)
                            Text("${reward.coinPrice} Coin", color = GrowthColors.Amber, fontFamily = FontFamily.Monospace)
                        }
                        if (reward.id in state.rewardInterestIds) {
                            StatusDot("孩子想要", ChildColors.Coral)
                            Spacer(Modifier.width(8.dp))
                        }
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
                    WishCard(viewModel, Modifier.weight(1f),{dialog=GrowthDialog.WISH}){id->selectedWish=id;dialog=GrowthDialog.WISH_ALLOCATE}
                } else Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SavingCard(viewModel, onAdd = { dialog = GrowthDialog.SAVING }) { id -> selectedSaving = id; dialog = GrowthDialog.SAVING_DEPOSIT }
                    WishCard(viewModel,onAdd={dialog=GrowthDialog.WISH}){id->selectedWish=id;dialog=GrowthDialog.WISH_ALLOCATE}
                }
            }
        }
        item { FundCard(viewModel, onBuy = { dialog = GrowthDialog.FUND_BUY }, onNav = { dialog = GrowthDialog.FUND_NAV }) }
    }

    when (dialog) {
        GrowthDialog.PLAN -> GrowthTextPairDialog("建立成长计划","计划名称","希望观察到的具体变化",{dialog=null}){title,target->viewModel.addGrowthPlan(title,target);dialog=null}
        GrowthDialog.MILESTONE -> GrowthTextPairDialog("记录真实变化","今天发生了什么","描述一个可观察的动作或回应",{dialog=null}){title,observation->viewModel.addGrowthMilestone(title,observation);dialog=null}
        GrowthDialog.REWARD -> TitleNumberDialog("添加家庭奖励", "奖励名称", "Coin 价格", { dialog = null }) { title, value -> viewModel.addReward(title, value.toInt()); dialog = null }
        GrowthDialog.SAVING -> TitleMoneyDialog("创建储蓄目标", "目标名称", { dialog = null }) { title, value -> viewModel.addSaving(title, value); dialog = null }
        GrowthDialog.WISH -> TitleMoneyDialog("记录愿望", "愿望名称", { dialog = null }) { title, value -> viewModel.addWish(title, value); dialog = null }
        GrowthDialog.WISH_ALLOCATE -> AmountDialog("为愿望分配储蓄","金额","从服务端储蓄账户分配，不会凭空增加 Money。",dismiss={dialog=null}){amount->selectedWish?.let{viewModel.allocateWish(it,amount)};dialog=null}
        GrowthDialog.FUND_BUY -> AmountDialog("购买纯模拟基金", "投入 Money", "按当前教学 NAV 计算份额，不接真实行情。", dismiss = { dialog = null }) { viewModel.buyFund(it); dialog = null }
        GrowthDialog.FUND_NAV -> AmountDialog("调整教学 NAV", "新 NAV", "只影响模拟持仓市值，可涨也可跌。", dismiss = { dialog = null }, scale = 4) { viewModel.updateNav(it); dialog = null }
        GrowthDialog.SAVING_DEPOSIT -> AmountDialog("存入储蓄目标", "金额", "Money 将转入目标并形成流水。", dismiss = { dialog = null }) { amount -> selectedSaving?.let { viewModel.saveToGoal(it, amount) }; dialog = null }
        null -> Unit
    }
    viewModel.pendingFundTrade?.let{preview->AlertDialog(onDismissRequest=viewModel::cancelFundTrade,title={Text(if(preview.side=="BUY")"确认模拟买入" else "确认模拟赎回")},text={Column(verticalArrangement=Arrangement.spacedBy(6.dp)){Text("教学 NAV ${preview.nav}");Text("交易金额 ¥${preview.gross}");Text("费用 ¥${preview.fee}");Text("净额 ¥${preview.net}");Text("模拟份额 ${preview.shares}",fontWeight=FontWeight.Bold);Text(preview.notice,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}},confirmButton={Button(onClick=viewModel::confirmFundTrade){Text("确认模拟交易")}},dismissButton={TextButton(onClick=viewModel::cancelFundTrade){Text("取消")}})}
}

@Composable private fun GrowthTextPairDialog(title:String,firstLabel:String,secondLabel:String,dismiss:()->Unit,confirm:(String,String)->Unit){var first by remember{mutableStateOf("")};var second by remember{mutableStateOf("")};AlertDialog(onDismissRequest=dismiss,title={Text(title)},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){OutlinedTextField(first,{first=it.take(120)},label={Text(firstLabel)},singleLine=true);OutlinedTextField(second,{second=it.take(500)},label={Text(secondLabel)},minLines=3)}},confirmButton={Button(onClick={confirm(first.trim(),second.trim())},enabled=first.isNotBlank()&&second.isNotBlank()){Text("保存")}},dismissButton={TextButton(onClick=dismiss){Text("取消")}})}

@Composable
private fun ChildGrowthScreen(viewModel: FamilyAppViewModel) {
    val state = viewModel.state
    val feedback = rememberChildControlFeedback(state.experience)
    var area by remember { mutableStateOf(ChildGrowthArea.LESSONS) }
    var selectedReward by remember { mutableStateOf<LocalRewardItem?>(null) }
    var selectedLesson by remember { mutableStateOf<LearningLesson?>(null) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("我的成长", style = MaterialTheme.typography.headlineMedium)
                Text("选一个小课堂，或者看看想要的奖励。", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ChildAreaButton("安静小课堂", area == ChildGrowthArea.LESSONS, Modifier.weight(1f)) { feedback { area = ChildGrowthArea.LESSONS } }
                    ChildAreaButton("奖励商店", area == ChildGrowthArea.REWARDS, Modifier.weight(1f)) { feedback { area = ChildGrowthArea.REWARDS } }
                }
            }
        }
        if (area == ChildGrowthArea.LESSONS) {
            item { SectionTitle("今天看一个就好", "视频不会自动播放，看完记为待家长确认的任务") }
            items(LearningCatalog.forStage(state.experience.effectiveStage), key = { it.id }) { lesson ->
                val progress = state.learningProgress.singleOrNull { it.videoId == lesson.id }
                ChildLessonCard(lesson, progress) { feedback { selectedLesson = lesson } }
            }
        } else {
            item { SectionTitle("奖励商店", "点开看看；喜欢就告诉家长") }
            if (state.rewards.isEmpty()) {
                item { GrowthCard { EmptyInvitation("☆", "还没有家庭奖励", "请家长先添加一个一起约定的奖励。") } }
            } else {
                items(state.rewards, key = { it.id }) { reward ->
                    ChildRewardBrowseCard(reward, reward.id in state.rewardInterestIds) { feedback { selectedReward = reward } }
                }
            }
        }
        item {
            Text(
                "看累了就停下来。更多事情请家长一起看看。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        }
    }

    selectedReward?.let { reward ->
        ChildRewardDetailDialog(
            reward = reward,
            interested = reward.id in state.rewardInterestIds,
            dismiss = { feedback { selectedReward = null } },
            toggleInterest = { feedback { viewModel.toggleRewardInterest(reward.id) } },
        )
    }
    selectedLesson?.let { lesson ->
        LearningVideoDialog(
            lesson = lesson,
            progress = state.learningProgress.singleOrNull { it.videoId == lesson.id },
            dismiss = { selectedLesson = null },
            watchedSecond = { viewModel.recordLearningSecond(lesson.id) },
            feedback = feedback,
        )
    }
}

@Composable
private fun ChildAreaButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier.heightIn(min = 56.dp)) { Text(label) }
    } else {
        TextButton(onClick = onClick, modifier = modifier.heightIn(min = 56.dp)) { Text(label) }
    }
}

@Composable
private fun ChildLessonCard(lesson: LearningLesson, progress: LocalLearningProgress?, open: () -> Unit) {
    val completed = progress?.completed == true
    val fraction = (progress?.watchedSeconds ?: 0).toFloat().div(lesson.durationSeconds).coerceIn(0f, 1f)
    GrowthCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = MaterialTheme.shapes.large, color = if (completed) ChildColors.Mist else ChildColors.Sun.copy(alpha = .38f), modifier = Modifier.size(72.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(lesson.symbol, style = MaterialTheme.typography.titleLarge, color = ChildColors.Ink) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(lesson.title, style = MaterialTheme.typography.titleLarge)
                Text(lesson.prompt, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(progress = { fraction }, Modifier.fillMaxWidth())
                Text(if (completed) "已看完 · 等家长确认" else "约 18 秒 · 不会自动播放", style = MaterialTheme.typography.bodySmall, color = if (completed) ChildColors.Moss else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = open, modifier = Modifier.heightIn(min = 56.dp)) { Text(if (completed) "再看" else "打开") }
        }
    }
}

@Composable
private fun ChildRewardBrowseCard(reward: LocalRewardItem, interested: Boolean, open: () -> Unit) {
    GrowthCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = MaterialTheme.shapes.large, color = if (interested) ChildColors.Coral.copy(alpha = .22f) else ChildColors.Sun.copy(alpha = .5f), modifier = Modifier.size(72.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(if (interested) "♥" else "☆", style = MaterialTheme.typography.headlineMedium, color = if (interested) ChildColors.Coral else ChildColors.Ink) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(reward.title, style = MaterialTheme.typography.titleLarge)
                Text("${reward.coinPrice} 颗星", style = MaterialTheme.typography.bodyLarge, color = ChildColors.Moss)
                Text(if (interested) "已经告诉家长：我想要" else "点开看看是什么", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = open, modifier = Modifier.heightIn(min = 56.dp)) { Text("看看") }
        }
    }
}

@Composable
private fun ChildRewardDetailDialog(reward: LocalRewardItem, interested: Boolean, dismiss: () -> Unit, toggleInterest: () -> Unit) {
    AlertDialog(
        onDismissRequest = dismiss,
        icon = { Text(if (interested) "♥" else "☆", style = MaterialTheme.typography.displaySmall, color = if (interested) ChildColors.Coral else ChildColors.Sun) },
        title = { Text(reward.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("需要 ${reward.coinPrice} 颗星", style = MaterialTheme.typography.titleLarge, color = ChildColors.Moss)
                Text("这是家长放进商店的家庭奖励。你可以说‘我想要’，不会现在就扣掉星星。", style = MaterialTheme.typography.bodyLarge)
            }
        },
        confirmButton = {
            Button(onClick = toggleInterest, modifier = Modifier.heightIn(min = 52.dp)) { Text(if (interested) "先不选" else "我想要") }
        },
        dismissButton = { TextButton(onClick = dismiss, modifier = Modifier.heightIn(min = 52.dp)) { Text("返回商店") } },
    )
}

@Composable
private fun LearningVideoDialog(lesson: LearningLesson, progress: LocalLearningProgress?, dismiss: () -> Unit, watchedSecond: () -> Unit, feedback: ((() -> Unit) -> Unit)) {
    val context = LocalContext.current
    var videoView by remember(lesson.id) { mutableStateOf<VideoView?>(null) }
    var playing by remember(lesson.id) { mutableStateOf(false) }
    var videoError by remember(lesson.id) { mutableStateOf(false) }
    val completed = progress?.completed == true
    val fraction = (progress?.watchedSeconds ?: 0).toFloat().div(lesson.durationSeconds).coerceIn(0f, 1f)

    LaunchedEffect(playing, lesson.id) {
        while (playing) {
            delay(1_000)
            if (videoView?.isPlaying == true) watchedSecond() else playing = false
        }
    }
    DisposableEffect(lesson.id) {
        onDispose { videoView?.stopPlayback() }
    }

    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(lesson.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AndroidView(
                    factory = { videoContext ->
                        VideoView(videoContext).also { view ->
                            videoView = view
                            val uri = Uri.parse("android.resource://${context.packageName}/${learningVideoResource(lesson.resourceName)}")
                            view.setVideoURI(uri)
                            view.setOnCompletionListener { playing = false }
                            view.setOnErrorListener { _, _, _ -> playing = false; videoError = true; true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                )
                LinearProgressIndicator(progress = { fraction }, Modifier.fillMaxWidth())
                if (videoError) {
                    Text("视频暂时无法播放。关闭后重新打开；观看进度已经保存。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
                }
                Text(
                    if (completed) "这节看完了，任务正在等家长确认。" else "只计算实际播放时间。可以随时暂停，拖到结尾不会直接完成。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (completed) ChildColors.Moss else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !videoError,
                onClick = { feedback {
                    videoView?.let { view ->
                        if (playing) view.pause() else {
                            if (!view.isPlaying && view.currentPosition >= view.duration - 500) view.seekTo(0)
                            view.start()
                        }
                        playing = !playing
                    }
                } },
                modifier = Modifier.heightIn(min = 52.dp),
            ) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text(if (playing) "暂停" else if (completed) "再看一次" else "开始播放") }
        },
        dismissButton = { TextButton(onClick = { feedback(dismiss) }, modifier = Modifier.heightIn(min = 52.dp)) { Text("关闭") } },
    )
}

private fun learningVideoResource(resourceName: String): Int = when (resourceName) {
    "lesson_color_garden" -> R.raw.lesson_color_garden
    "lesson_count_to_five" -> R.raw.lesson_count_to_five
    "lesson_shape_home" -> R.raw.lesson_shape_home
    else -> throw FamilyRuleException("教学视频资源不存在")
}

@Composable
private fun SavingCard(viewModel: FamilyAppViewModel, modifier: Modifier = Modifier, onAdd: () -> Unit, onDeposit: (String) -> Unit) {
    GrowthCard(modifier) {
        SectionTitle("储蓄目标", "从钱包分配到长期目标") { TextButton(onClick = onAdd) { Text("新增") } }
        viewModel.savingBalance?.let{Text("服务端储蓄账户 ¥$it",fontFamily=FontFamily.Monospace,color=GrowthColors.Emerald)}
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
private fun WishCard(viewModel: FamilyAppViewModel, modifier: Modifier = Modifier, onAdd: () -> Unit,onAllocate:(String)->Unit) {
    GrowthCard(modifier) {
        SectionTitle("愿望清单", "先说清想要什么，再决定如何实现") { TextButton(onClick = onAdd) { Text("记录") } }
        if (viewModel.state.wishes.isEmpty()) EmptyInvitation("☆", "还没有愿望", "记录一个愿望和大致目标金额。")
        else viewModel.state.wishes.forEach { wish ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("☆", color = GrowthColors.Amber, style = MaterialTheme.typography.headlineSmall)
                Column(Modifier.padding(start = 10.dp).weight(1f)) { Text(wish.title, style = MaterialTheme.typography.titleMedium); Text("目标 ¥${wish.target}", fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if(viewModel.savingBalance!=null)TextButton(onClick={onAllocate(wish.id)},enabled=viewModel.savingBalance!!>BigDecimal.ZERO){Text("分配")}
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
    var showExperience by remember { mutableStateOf(false) }
    var showLearningReward by remember { mutableStateOf(false) }
    var showEducationSource by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showAllowance by remember { mutableStateOf(false) }
    val state = viewModel.state
    val approved = state.tasks.count { it.status == TaskStatus.APPROVED }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            SectionTitle("家庭家长中心", "可连接自有服务端；Token 仅保留在本次 App 进程内")
        }
        item { ServiceConnectionCard(viewModel) { showConnection = true } }
        item { ChildExperienceCard(viewModel) { showExperience = true } }
        item { LearningRewardPolicyCard(state.learningRewardPolicy) { showLearningReward = true } }
        item { ParentTeachingStudio(viewModel) }
        item { EducationResourceShelfCard(viewModel) { showEducationSource = true } }
        item { PrivacyCenterCard(viewModel){showPrivacy=true} }
        item {
            BoxWithConstraints {
                val wide = maxWidth >= 720.dp
                if (wide) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ParentReportCard(viewModel, state, approved, Modifier.weight(1f))
                    UsagePolicyCard(viewModel,state, Modifier.weight(1f)) { showUsage = true }
                } else Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ParentReportCard(viewModel, state, approved)
                    UsagePolicyCard(viewModel,state) { showUsage = true }
                }
            }
        }
        item { if(viewModel.connectionState is ConnectionState.Connected) OutlinedButton(onClick={showAllowance=true},Modifier.fillMaxWidth().heightIn(min=52.dp)){Text("临时允许使用（最多 60 分钟）")} }
        item { UpdatePanel(updateViewModel) }
        item {
            GrowthCard {
                SectionTitle("生产接入边界", "服务端授权是最终边界")
                Text("连接后，钱包、兑换、奖励、储蓄、愿望、模拟基金、零钱回收和报表均使用家庭服务 API。")
                Text("离线本机数据仍可使用，但明确标为本机状态；不把断线操作伪装成已同步。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (showUsage) UsageDialog(state.usage, { showUsage = false }) { daily, session -> viewModel.updateUsage(daily, session); showUsage = false }
    if (showConnection) ServiceConnectionDialog(viewModel) { showConnection = false }
    if (showExperience) ChildExperienceDialog(viewModel) { showExperience = false }
    if (showLearningReward) LearningRewardPolicyDialog(state.learningRewardPolicy, { showLearningReward = false }) { money, coin, xp ->
        viewModel.updateLearningReward(money, coin, xp)
        showLearningReward = false
    }
    if (showEducationSource) EducationResourceSourceDialog(viewModel) { showEducationSource = false }
    if(showPrivacy) PrivacyCenterDialog(viewModel){showPrivacy=false}
    if(showAllowance) TemporaryAllowanceDialog({showAllowance=false}){minutes,reason->viewModel.grantTemporaryUsage(minutes,reason);showAllowance=false}
}

@Composable private fun PrivacyCenterCard(viewModel:FamilyAppViewModel,open:()->Unit){GrowthCard{SectionTitle("儿童隐私中心","家长专属 · 导出不含 PIN、Token、答案或私密说明"){TextButton(onClick=open,enabled=viewModel.connectionState is ConnectionState.Connected){Text("管理")}};Text("可导出机器可读 JSON；删除采用预览、十分钟确认和服务端 PIN 再认证。",color=MaterialTheme.colorScheme.onSurfaceVariant);Text("账本、费用、幂等与最小审计会去标识化保留，以维持守恒和安全追责。",style=MaterialTheme.typography.bodySmall);viewModel.lastExportPath?.let{Text("最近导出：$it",style=MaterialTheme.typography.bodySmall,fontFamily=FontFamily.Monospace)}}}

@Composable private fun PrivacyCenterDialog(viewModel:FamilyAppViewModel,dismiss:()->Unit){
    var pin by remember{mutableStateOf("")};var days by remember(viewModel.retentionPolicy){mutableStateOf(viewModel.retentionPolicy?.usageDetailDays?.toString()?:"90")};val preview=viewModel.erasurePreview;val parsedDays=days.toIntOrNull()
    AlertDialog(onDismissRequest=dismiss,title={Text("儿童隐私中心")},text={Column(Modifier.heightIn(max=560.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
        Text("自动保留",style=MaterialTheme.typography.titleMedium);Text("过期的 App 使用明细会自动删除，临时放行原因会脱敏；账本、费用和最小审计始终保留。");OutlinedTextField(days,{days=it.filter(Char::isDigit).take(3)},label={Text("使用明细保留天数（30–365）")},singleLine=true);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={viewModel.updateRetentionPolicy(parsedDays!!)},enabled=parsedDays in 30..365){Text("保存策略")};OutlinedButton(onClick=viewModel::runRetentionNow){Text("立即执行一次")}}
        HorizontalDivider();Text("数据导出",style=MaterialTheme.typography.titleMedium);Text("生成包含资料、学习、使用和账本事实的 JSON；不会包含认证秘密、答案键和家长私密说明。");Button(onClick=viewModel::exportChildData,Modifier.fillMaxWidth()){Text("导出儿童数据")}
        HorizontalDivider();Text("删除儿童数据",style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.error);if(preview==null){Text("先查看将删除/脱敏与必须保留的范围。此步骤不会立即删除。");OutlinedButton(onClick=viewModel::previewChildErasure,Modifier.fillMaxWidth()){Text("生成删除预览")}}else{Text("将删除或脱敏：");preview.deletedOrRedacted.forEach{Text("• $it")};Text("仍会保留：");preview.retained.forEach{Text("• $it")};Text("确认有效至 ${preview.confirmationExpiresAt}",style=MaterialTheme.typography.bodySmall);OutlinedTextField(pin,{pin=it.filter(Char::isDigit).take(6)},label={Text("6 位服务端 PIN")},singleLine=true);Button(onClick={viewModel.confirmChildErasure(pin)},enabled=pin.length==6,colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.error),modifier=Modifier.fillMaxWidth()){Text("确认删除并断开服务")}}
    }},confirmButton={TextButton(onClick=dismiss){Text("关闭")}})
}

@Composable private fun TemporaryAllowanceDialog(dismiss:()->Unit,save:(Int,String)->Unit){var minutes by remember{mutableStateOf("15")};var reason by remember{mutableStateOf("")};val parsed=minutes.toIntOrNull();AlertDialog(onDismissRequest=dismiss,title={Text("临时允许使用")},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Text("只用于具体家庭安排，到时自动结束。孩子不能自行延长。");OutlinedTextField(minutes,{minutes=it.filter(Char::isDigit).take(2)},label={Text("分钟（1–60）")});OutlinedTextField(reason,{reason=it.take(240)},label={Text("具体原因")},minLines=2)}},confirmButton={Button(onClick={save(parsed!!,reason.trim())},enabled=parsed in 1..60&&reason.isNotBlank()){Text("允许一次")}},dismissButton={TextButton(onClick=dismiss){Text("取消")}})}

@Composable
private fun LearningRewardPolicyCard(policy: LearningRewardPolicy, edit: () -> Unit) {
    GrowthCard {
        SectionTitle("自主学习预设奖励", "认真观看达到 90% 后生成待家长审核任务") {
            TextButton(onClick = edit) { Text("配置") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DataPill("Money", "¥${policy.money}", GrowthColors.Emerald, Modifier.weight(1f))
            DataPill("Coin", policy.coin.toString(), GrowthColors.Amber, Modifier.weight(1f))
            DataPill("XP", policy.xp.toString(), Color(0xFF55A6C8), Modifier.weight(1f))
        }
        Text("奖励在课程达标时固化到任务，家长审核后才写入本机账本；已生成任务不被后续改值。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LearningRewardPolicyDialog(policy: LearningRewardPolicy, dismiss: () -> Unit, save: (BigDecimal, Int, Int) -> Unit) {
    var money by remember { mutableStateOf(policy.money.toPlainString()) }
    var coin by remember { mutableStateOf(policy.coin.toString()) }
    var xp by remember { mutableStateOf(policy.xp.toString()) }
    val parsedMoney = money.toBigDecimalOrNull()?.setScale(2, RoundingMode.HALF_UP)
    val parsedCoin = coin.toIntOrNull()
    val parsedXp = xp.toIntOrNull()
    val valid = parsedMoney != null && parsedMoney.signum() >= 0 && parsedCoin != null && parsedCoin in 0..10_000 && parsedXp != null && parsedXp in 0..100_000
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("配置自主学习奖励") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("固定、透明的奖励会在真实观看达到 90% 后形成待审核任务，不使用随机奖励。")
                OutlinedTextField(money, { money = it.take(20) }, label = { Text("Money") }, singleLine = true)
                OutlinedTextField(coin, { coin = it.filter(Char::isDigit).take(5) }, label = { Text("Coin") }, singleLine = true)
                OutlinedTextField(xp, { xp = it.filter(Char::isDigit).take(6) }, label = { Text("XP") }, singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { save(parsedMoney!!, parsedCoin!!, parsedXp!!) }, enabled = valid) { Text("保存奖励") } },
        dismissButton = { TextButton(onClick = dismiss) { Text("取消") } },
    )
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
private fun ParentReportCard(viewModel:FamilyAppViewModel,state: FamilyLocalState, approved: Int, modifier: Modifier = Modifier) {
    GrowthCard(modifier) {
        val today=viewModel.todayUsageReport
        val month=viewModel.monthlyUsageReport
        SectionTitle("使用与学习报告", if(today==null)"未连接时仅显示本机实际记录" else "来自家庭服务端的权威统计")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DataPill("今日完成", (today?.approvedTasks?:approved).toString(), GrowthColors.Emerald, Modifier.weight(1f))
            DataPill("今日学习", "${today?.learningMinutes?:0}m", GrowthColors.Amber, Modifier.weight(1f))
            DataPill("今日使用", "${today?.appMinutes?:state.usage.usedMinutes}m", Color(0xFF55A6C8), Modifier.weight(1f))
        }
        month?.let{Text("本月使用 ${it.appMinutes} 分钟 · 学习 ${it.learningMinutes} 分钟 · 完成 ${it.approvedTasks} 项",color=MaterialTheme.colorScheme.onSurfaceVariant)}
        Text("零钱回收待确认 ${state.withdrawals.count { it.status == WithdrawalStatus.PENDING }} 笔", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("累计 XP ${state.wallet.xp} · Money ¥${state.wallet.money} · Coin ${state.wallet.coin}", fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun UsagePolicyCard(viewModel:FamilyAppViewModel,state: FamilyLocalState, modifier: Modifier = Modifier, edit: () -> Unit) {
    GrowthCard(modifier) {
        SectionTitle("本 App 防沉迷", "只统计孩子模式，不控制其他应用") { TextButton(onClick = edit) { Text("调整") } }
        Text("每日 ${state.usage.dailyLimitMinutes} 分钟", style = MaterialTheme.typography.titleLarge)
        Text("单次 ${state.usage.sessionLimitMinutes} 分钟 · 今日已用 ${state.usage.usedMinutes} 分钟", color = MaterialTheme.colorScheme.onSurfaceVariant)
        viewModel.remoteUsageAccess?.let{access->Text("本段 ${access.sessionUsedMinutes}/${access.sessionLimitMinutes} 分钟 · 每段后休息 ${access.restMinutes} 分钟",color=MaterialTheme.colorScheme.onSurfaceVariant);if(access.reasonCode=="SESSION_REST")Text("孩子端当前处于温和休息页",color=GrowthColors.Amber)}
        Text("连接服务后统一执行休息时段 21:30–06:30；临时放行必须由家长说明原因。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
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
            is UpdateUiState.Downloading -> {
                val progress = state.progress
                if (progress.phase == DownloadPhase.DOWNLOADING && progress.percent != null) {
                    LinearProgressIndicator(progress = { progress.percent / 100f }, Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                Text(downloadStatusText(progress), style = MaterialTheme.typography.titleMedium)
                progress.detail?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                TextButton(onClick = viewModel::cancelDownload) { Text("取消下载") }
            }
            is UpdateUiState.Ready -> { Text("APK 已通过 SHA-256 校验。"); Button(onClick = { viewModel.install(state.update, state.file) }) { Text("打开系统安装界面") } }
            is UpdateUiState.PermissionRequired -> { Text("请允许本应用安装未知来源应用，返回后继续。"); Button(onClick = { viewModel.install(state.update, state.file) }) { Text("继续安装") } }
            is UpdateUiState.Error -> {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = viewModel::check) { Text("重新检查") }
                    state.update?.let { update -> TextButton(onClick = { viewModel.openReleasePage(update) }) { Text("浏览器打开发布页") } }
                }
            }
        }
    }
}

private fun downloadStatusText(progress: DownloadProgress): String = when (progress.phase) {
    DownloadPhase.QUEUED -> "已交给 Android 系统下载，正在排队"
    DownloadPhase.CONNECTING -> "正在连接 GitHub 下载节点（入口 ${progress.sourceAttempt}/2）"
    DownloadPhase.SWITCHING_SOURCE -> "主入口没有收到数据，正在切换官方备用入口"
    DownloadPhase.PAUSED -> progress.detail ?: "系统暂时暂停下载"
    DownloadPhase.VERIFYING -> "下载完成，正在校验大小和 SHA-256"
    DownloadPhase.DOWNLOADING -> progress.percent?.let { percent ->
        "正在下载 $percent% · ${formatMegabytes(progress.downloadedBytes)} / ${formatMegabytes(progress.totalBytes)}"
    } ?: "正在接收 APK 数据"
}

private fun formatMegabytes(bytes: Long): String = String.format(Locale.US, "%.1f MB", bytes.coerceAtLeast(0L) / (1024.0 * 1024.0))

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
