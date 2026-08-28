package com.familygrowth.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.familygrowth.android.core.FamilyAppViewModel
import com.familygrowth.android.core.SchoolStage
import com.familygrowth.android.remote.ConnectionState
import com.familygrowth.android.remote.RemoteEducationSource
import java.net.URI

private val SourceInk = Color(0xFF294C60)
private val SourceMint = Color(0xFFDCEFE6)

@Composable
fun EducationResourceShelfCard(viewModel: FamilyAppViewModel, add: () -> Unit) {
    GrowthCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("免费教育资源书架", style = MaterialTheme.typography.titleLarge, color = SourceInk)
                Text("家长添加网址，服务端只读取公开栏目。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = add, enabled = viewModel.connectionState is ConnectionState.Connected) { Text("添加来源") }
        }
        if (viewModel.connectionState !is ConnectionState.Connected) {
            Text("先连接家庭服务，来源审核和动态栏目才会保存。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (viewModel.educationSources.isEmpty()) {
            Surface(shape = MaterialTheme.shapes.medium, color = SourceMint.copy(alpha = .6f)) {
                Text("还没有来源。添加一个确认可免费浏览的教育网站。", Modifier.fillMaxWidth().padding(16.dp), color = SourceInk)
            }
        } else {
            viewModel.educationSources.forEach { source -> ResourceSourceCard(source, viewModel) }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ResourceSourceCard(source: RemoteEducationSource, viewModel: FamilyAppViewModel) {
    Surface(
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = MaterialTheme.shapes.small, color = SourceInk) {
                    Text(sourceHost(source.sourceUrl), Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(source.title, style = MaterialTheme.typography.titleMedium)
                    Text(source.schoolStages.joinToString(" · ") { stageLabel(it) }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = MaterialTheme.shapes.small, color = SourceMint) {
                    Text(statusLabel(source), Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = SourceInk, style = MaterialTheme.typography.labelMedium)
                }
            }
            Text(source.sourceUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(source.usageNote, style = MaterialTheme.typography.bodySmall)
            if (source.refreshStatus == "FAILED") {
                Text("本次读取失败：${source.refreshError.ifBlank { "来源暂时不可用" }}。已保留上一次栏目。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (source.categories.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    source.categories.forEach { category ->
                        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text(category.title, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            } else Text("尚未读取栏目。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (source.status != "WITHDRAWN") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.refreshEducationSource(source.id) }) { Text("读取栏目") }
                    if (source.status == "DRAFT" && source.refreshStatus == "READY" && source.categories.isNotEmpty()) {
                        OutlinedButton(onClick = { viewModel.approveEducationSource(source.id) }) { Text("批准展示") }
                    }
                    TextButton(onClick = { viewModel.withdrawEducationSource(source.id) }) { Text("撤回") }
                }
            }
        }
    }
}

@Composable
fun EducationResourceSourceDialog(viewModel: FamilyAppViewModel, dismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var stages by remember { mutableStateOf(setOf(SchoolStage.PRIMARY)) }
    val choices = listOf(SchoolStage.PRIMARY, SchoolStage.JUNIOR_MIDDLE, SchoolStage.SENIOR_HIGH)
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("添加免费教育来源") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("只填写无需登录即可浏览的 HTTPS 首页。系统读取栏目后，还要由家长批准才会展示。", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(title, { title = it.take(160) }, label = { Text("来源名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(url, { url = it.take(1000) }, label = { Text("来源网址（https://…）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("适用学段", style = MaterialTheme.typography.labelLarge)
                choices.forEach { stage ->
                    FilterChip(
                        selected = stage in stages,
                        onClick = { stages = if (stage in stages) stages - stage else stages + stage },
                        label = { Text(stage.displayName()) },
                    )
                }
                OutlinedTextField(note, { note = it.take(500) }, label = { Text("免费使用依据或家长说明") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                Text("不会读取登录页、视频文件或跨站链接；孩子端不会收到网址。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.addEducationSource(title, url, stages.toList(), note); dismiss() },
                enabled = title.isNotBlank() && url.startsWith("https://") && note.isNotBlank() && stages.isNotEmpty(),
            ) { Text("保存来源") }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("取消") } },
    )
}

private fun sourceHost(url: String): String = runCatching { URI(url).host }.getOrNull()?.removePrefix("www.") ?: "HTTPS 来源"
private fun stageLabel(stage: String): String = runCatching { SchoolStage.valueOf(stage).displayName() }.getOrDefault(stage)
private fun statusLabel(source: RemoteEducationSource): String = when {
    source.status == "WITHDRAWN" -> "已撤回"
    source.status == "APPROVED" && source.refreshStatus == "FAILED" -> "已批准 · 栏目陈旧"
    source.status == "APPROVED" -> "已批准"
    source.refreshStatus == "READY" -> "待批准"
    source.refreshStatus == "FAILED" -> "读取失败"
    else -> "待读取"
}
