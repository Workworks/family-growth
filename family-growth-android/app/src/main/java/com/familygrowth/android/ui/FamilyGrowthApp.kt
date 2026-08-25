package com.familygrowth.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familygrowth.android.core.*
import com.familygrowth.android.update.UpdateViewModel
import kotlinx.coroutines.delay

private data class NavItem(val section: AppSection, val childLabel: String, val parentLabel: String, val icon: ImageVector)

private val navItems = listOf(
    NavItem(AppSection.TODAY, "今天", "总览", Icons.Rounded.Home),
    NavItem(AppSection.TASKS, "小任务", "任务", Icons.AutoMirrored.Rounded.Assignment),
    NavItem(AppSection.WALLET, "钱包", "钱包", Icons.Rounded.AccountBalanceWallet),
    NavItem(AppSection.GROWTH, "我的", "成长", Icons.Rounded.EmojiEvents),
    NavItem(AppSection.PARENT, "家长", "管理", Icons.Rounded.AdminPanelSettings),
)

private fun navigationItemsFor(mode: AppMode) =
    navItems.filter { it.section in ChildExperiencePolicy.sectionsFor(mode) }

@Composable
fun FamilyGrowthApp(appViewModel: FamilyAppViewModel = viewModel(), updateViewModel: UpdateViewModel = viewModel()) {
    var showParentAccess by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(appViewModel.message) {
        appViewModel.message?.let { snackbar.showSnackbar(it); appViewModel.clearMessage() }
    }
    LaunchedEffect(appViewModel.mode) {
        while (appViewModel.mode == AppMode.CHILD) {
            delay(60_000)
            appViewModel.recordUsageMinute()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val tablet = maxWidth >= 840.dp
        if (tablet) {
            Row(Modifier.fillMaxSize()) {
                TabletNavigation(appViewModel, Modifier.width(216.dp).fillMaxHeight(), onParentRequest = { showParentAccess = true })
                AppContent(appViewModel, updateViewModel, snackbar, Modifier.weight(1f).fillMaxHeight()) { showParentAccess = true }
            }
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = { BottomNavigation(appViewModel) { showParentAccess = true } },
            ) { padding -> AppContent(appViewModel, updateViewModel, snackbar, Modifier.padding(padding).fillMaxSize()) { showParentAccess = true } }
        }
    }

    if (showParentAccess) ParentAccessDialog(appViewModel) { showParentAccess = false }
}

@Composable
private fun TabletNavigation(viewModel: FamilyAppViewModel, modifier: Modifier, onParentRequest: () -> Unit) {
    Surface(modifier, color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (viewModel.mode == AppMode.CHILD) "一起成长" else "Family", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(if (viewModel.mode == AppMode.CHILD) "看看 · 去做 · 给家长看" else "GROWTH · 本机基础版", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            navigationItemsFor(viewModel.mode).forEach { item ->
                val selected = viewModel.section == item.section
                NavigationDrawerItem(
                    label = { Text(if (viewModel.mode == AppMode.PARENT) item.parentLabel else item.childLabel) },
                    selected = selected,
                    onClick = { selectNav(viewModel, item.section, onParentRequest) },
                    icon = { Icon(item.icon, null) },
                    badge = { if (viewModel.mode == AppMode.PARENT && item.section == AppSection.TASKS && viewModel.state.tasks.count { it.status == TaskStatus.SUBMITTED } > 0) Badge { Text(viewModel.state.tasks.count { it.status == TaskStatus.SUBMITTED }.toString()) } },
                    colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer),
                )
            }
            Spacer(Modifier.weight(1f))
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusDot(if (viewModel.mode == AppMode.PARENT) "家长模式" else "孩子模式", if (viewModel.mode == AppMode.PARENT) GrowthColors.Amber else GrowthColors.Emerald)
                    Text(if (viewModel.mode == AppMode.CHILD) "做完就去玩一会儿" else "今日 ${viewModel.state.usage.usedMinutes}/${viewModel.state.usage.dailyLimitMinutes} 分钟", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { if (viewModel.mode == AppMode.PARENT) viewModel.enterChild() else onParentRequest() }, contentPadding = PaddingValues(0.dp)) {
                        Text(if (viewModel.mode == AppMode.PARENT) "切换孩子模式" else "家长验证")
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavigation(viewModel: FamilyAppViewModel, onParentRequest: () -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        navigationItemsFor(viewModel.mode).forEach { item ->
            NavigationBarItem(
                selected = viewModel.section == item.section,
                onClick = { selectNav(viewModel, item.section, onParentRequest) },
                icon = { BadgedBox(badge = { if (viewModel.mode == AppMode.PARENT && item.section == AppSection.TASKS && viewModel.state.tasks.count { it.status == TaskStatus.SUBMITTED } > 0) Badge() }) { Icon(item.icon, null) } },
                label = { Text(if (viewModel.mode == AppMode.PARENT) item.parentLabel else item.childLabel) },
            )
        }
    }
}

private fun selectNav(viewModel: FamilyAppViewModel, section: AppSection, onParentRequest: () -> Unit) {
    if (section == AppSection.PARENT && viewModel.mode == AppMode.CHILD) onParentRequest() else viewModel.selectSection(section)
}

@Composable
private fun AppContent(
    viewModel: FamilyAppViewModel,
    updateViewModel: UpdateViewModel,
    snackbar: SnackbarHostState,
    modifier: Modifier,
    onParentRequest: () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { GrowthTopBar(viewModel, onParentRequest) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (viewModel.isChildLocked) {
                ChildLimitScreen(viewModel, onParentRequest)
            } else when (viewModel.section) {
                AppSection.TODAY -> TodayScreen(viewModel)
                AppSection.TASKS -> TasksScreen(viewModel)
                AppSection.WALLET -> WalletScreen(viewModel)
                AppSection.GROWTH -> GrowthScreen(viewModel)
                AppSection.PARENT -> ParentScreen(viewModel, updateViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GrowthTopBar(viewModel: FamilyAppViewModel, onParentRequest: () -> Unit) {
    TopAppBar(
        title = {
            Column {
                Text(sectionTitle(viewModel.section, viewModel.mode), style = MaterialTheme.typography.titleLarge)
                Text(if (viewModel.mode == AppMode.PARENT) "家长视角 · 本机基础版" else "一次做一件小事", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        actions = {
            AssistChip(
                onClick = { if (viewModel.mode == AppMode.PARENT) viewModel.enterChild() else onParentRequest() },
                label = { Text(if (viewModel.mode == AppMode.PARENT) "切换孩子" else "家长验证") },
                leadingIcon = { Icon(if (viewModel.mode == AppMode.PARENT) Icons.Rounded.SupervisorAccount else Icons.Rounded.ChildCare, null, Modifier.size(18.dp)) },
            )
            Spacer(Modifier.width(12.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

private fun sectionTitle(section: AppSection, mode: AppMode) = when (section) {
    AppSection.TODAY -> if (mode == AppMode.PARENT) "家庭成长总览" else "今天，一起成长"
    AppSection.TASKS -> if (mode == AppMode.PARENT) "成长任务" else "我的小任务"
    AppSection.WALLET -> "成长钱包"
    AppSection.GROWTH -> if (mode == AppMode.PARENT) "奖励与财商" else "我的成长"
    AppSection.PARENT -> "家长管理"
}

@Composable
private fun ChildLimitScreen(viewModel: FamilyAppViewModel, onParentRequest: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("🌙", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(12.dp))
        Text("现在休息一下", style = MaterialTheme.typography.headlineMedium)
        Text("小任务已经帮你保存好了。放下平板，动一动，或者找家长聊聊天。", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onParentRequest, modifier = Modifier.heightIn(min = 60.dp)) { Text("请家长帮忙") }
    }
}

@Composable
private fun ParentAccessDialog(viewModel: FamilyAppViewModel, dismiss: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = dismiss,
        icon = { Icon(Icons.Rounded.Lock, null) },
        title = { Text(if (viewModel.hasParentPin) "家长验证" else "设置本机家长 PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (viewModel.hasParentPin) "输入 PIN 进入家长模式。" else "首次使用请设置 4–8 位数字 PIN。它只保护本机基础版，服务端认证将在后续接入。")
                LabeledField(pin, { pin = it.filter(Char::isDigit).take(8) }, "PIN")
                if (!viewModel.hasParentPin) LabeledField(confirm, { confirm = it.filter(Char::isDigit).take(8) }, "再次输入 PIN")
            }
        },
        confirmButton = {
            Button(onClick = {
                val success = if (viewModel.hasParentPin) viewModel.verifyParentPin(pin) else viewModel.setParentPin(pin, confirm)
                if (success) dismiss()
            }) { Text(if (viewModel.hasParentPin) "进入家长模式" else "保存并进入") }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("取消") } },
    )
}
