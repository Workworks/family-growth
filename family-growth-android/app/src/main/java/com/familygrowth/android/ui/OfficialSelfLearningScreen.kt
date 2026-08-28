package com.familygrowth.android.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.familygrowth.android.core.*
import com.familygrowth.android.remote.RemoteChildEducationSource

private val SmartEduBlue = Color(0xFF355C7D)
private val SmartEduPaper = Color(0xFFF7F9F5)

@Composable
fun OfficialSelfLearningScreen(
    settings: ChildExperienceSettings,
    familyResources: List<RemoteChildEducationSource>,
    requestParent: () -> Unit,
) {
    val stage = settings.effectiveStage
    var selection by remember(stage) { mutableStateOf(SmartEduOfficialSource.defaultSelection(stage)) }
    var browserUrl by remember { mutableStateOf<String?>(null) }
    if (browserUrl != null) {
        SmartEduBrowser(browserUrl!!, close = { browserUrl = null })
        return
    }

    val grades = SmartEduOfficialSource.grades(stage)
    val subjects = SmartEduOfficialSource.subjects(stage)
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.School, null, tint = SmartEduBlue, modifier = Modifier.size(38.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("自主学习", style = MaterialTheme.typography.headlineMedium, color = SmartEduBlue)
                    Text("自己选教材，手动打开官方课程。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = SmartEduBlue,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("当前教材", color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.labelLarge)
                    Text(
                        listOf(stage.displayName(), selection.grade, selection.subject, selection.edition, selection.volume)
                            .filter(String::isNotBlank).joinToString(" · "),
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }
        item { FilterRow("学段", listOf(stage.displayName()), stage.displayName(), enabled = false) {} }
        item { FilterRow("年级", grades, selection.grade) { selection = selection.copy(grade = it) } }
        item { FilterRow("学科", subjects, selection.subject) { selection = selection.copy(subject = it) } }
        item { FilterRow("版本", listOf("统编版", "人教版"), selection.edition) { selection = selection.copy(edition = it) } }
        item { FilterRow("册次", listOf("上册", "下册"), selection.volume) { selection = selection.copy(volume = it) } }
        item {
            GrowthCard {
                Text(
                    if (SmartEduOfficialSource.hasVerifiedDeepLink(selection))
                        "这个教材路径已核验，会直接带到对应官方同步课堂。"
                    else "该组合会打开官方同步课堂，请在官方页面继续确认教材。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { browserUrl = SmartEduOfficialSource.launchUrl(selection) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SmartEduBlue),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, null)
                    Spacer(Modifier.width(8.dp))
                    Text("去官方平台学习", style = MaterialTheme.typography.titleMedium)
                }
                Text("课程由国家中小学智慧教育平台提供；不会自动播放，也不会自动发放奖励。", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (familyResources.isNotEmpty()) {
            item {
                HorizontalDivider()
                Text("家长添加的资源", style = MaterialTheme.typography.titleLarge, color = SmartEduBlue)
                Text("栏目会随家长最近一次读取结果更新。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(familyResources, key = { it.id }) { source ->
                GrowthCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = MaterialTheme.shapes.small, color = SmartEduBlue.copy(alpha = .12f)) {
                            Text("资源站", Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = SmartEduBlue)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(source.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        source.categories.forEach { category ->
                            SuggestionChip(onClick = requestParent, label = { Text(category.title) })
                        }
                    }
                    Text("点栏目后请家长一起打开，App 不会把你带到陌生网页。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FilterRow(label: String, options: List<String>, selected: String, enabled: Boolean = true, change: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { if (enabled) change(option) },
                    enabled = enabled,
                    label = { Text(option) },
                    border = BorderStroke(1.dp, if (selected == option) SmartEduBlue else MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SmartEduBrowser(initialUrl: String, close: () -> Unit) {
    var title by remember { mutableStateOf("国家智慧教育平台") }
    var pageError by remember { mutableStateOf<String?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    BackHandler {
        val view = webView
        if (view != null && view.canGoBack()) view.goBack() else close()
    }
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Surface(color = SmartEduPaper, tonalElevation = 2.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = close) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回自主学习") }
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text("仅允许国家中小学智慧教育平台课程页面", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        pageError?.let { message ->
            Surface(color = MaterialTheme.colorScheme.errorContainer) {
                Text(message, Modifier.fillMaxWidth().padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        AndroidView(
            modifier = Modifier.fillMaxWidth().weight(1f),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    settings.setSupportMultipleWindows(false)
                    settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val allowed = SmartEduOfficialSource.isAllowedNavigation(request.url.toString())
                            if (!allowed) pageError = "已阻止离开官方课程区域的链接。"
                            return !allowed
                        }
                        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                            pageError = null
                            title = "官方同步课堂"
                        }
                        override fun onPageFinished(view: WebView, url: String) {
                            title = view.title?.takeIf(String::isNotBlank) ?: "官方同步课堂"
                        }
                    }
                    webView = this
                    loadUrl(initialUrl)
                }
            },
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            webView?.apply { stopLoading(); clearHistory(); clearCache(true); destroy() }
            webView = null
        }
    }
}
