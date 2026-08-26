package com.familygrowth.android.update

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.SystemClock
import com.familygrowth.android.BuildConfig
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.io.IOException
import java.net.SocketTimeoutException

class GitHubReleaseClient(private val repository: String) {
    @Volatile private var activeDownloadId: Long? = null
    init {
        if (!isValidGitHubRepository(repository)) {
            throw IllegalArgumentException("GITHUB_REPOSITORY 必须为 owner/repo")
        }
    }

    fun latestUpdate(): UpdateInfo {
        val connection = open("https://api.github.com/repos/$repository/releases/latest")
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("X-GitHub-Api-Version", BuildConfig.GITHUB_API_VERSION)
        connection.setRequestProperty("User-Agent", "family-growth-android/${BuildConfig.VERSION_NAME}")
        try {
            val status = connection.responseCode
            if (status != HttpURLConnection.HTTP_OK) {
                throw UpdateException("GitHub Release 查询失败（HTTP $status）")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return parseLatestRelease(JSONObject(body))
        } catch (exception: UpdateException) {
            throw exception
        } catch (exception: Exception) {
            throw UpdateException("无法读取 GitHub Release", exception)
        } finally {
            connection.disconnect()
        }
    }

    fun download(context: Context, update: UpdateInfo, onProgress: (DownloadProgress) -> Unit): File {
        val updateDirectory = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(updateDirectory, update.asset.name)
        target.delete()

        val sources = listOf(update.asset.downloadUrl, update.asset.apiUrl.orEmpty())
        var lastFailure: IOException? = null
        sources.forEachIndexed { index, source ->
            val attempt = index + 1
            if (index > 0) {
                onProgress(DownloadProgress(DownloadPhase.SWITCHING_SOURCE, sourceAttempt = attempt, detail = "正在切换 GitHub 官方备用入口"))
            }
            try {
                val systemAsset = downloadWithSystem(context, update, source, attempt, onProgress)
                try {
                    onProgress(DownloadProgress(DownloadPhase.VERIFYING, percent = 100, downloadedBytes = update.asset.size, totalBytes = update.asset.size, sourceAttempt = attempt))
                    return verifyAndCopy(systemAsset.file, target, update)
                } finally {
                    systemAsset.manager.remove(systemAsset.id)
                    systemAsset.file.delete()
                }
            } catch (exception: IOException) {
                lastFailure = exception
            }
        }
        target.delete()
        throw UpdateException(
            "GitHub 两个官方下载入口均未完成：${downloadFailureMessage(lastFailure ?: IOException("download failed"), sources.size)}",
            lastFailure,
        )
    }

    fun cancel(context: Context) {
        activeDownloadId?.let { context.getSystemService(DownloadManager::class.java).remove(it) }
        activeDownloadId = null
    }

    @Throws(IOException::class)
    private fun downloadWithSystem(
        context: Context,
        update: UpdateInfo,
        source: String,
        attempt: Int,
        onProgress: (DownloadProgress) -> Unit,
    ): SystemAsset {
        val manager = context.getSystemService(DownloadManager::class.java)
        val externalRoot = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: throw IOException("系统下载目录不可用")
        val relativePath = "updates/${update.asset.name}.$attempt.download"
        val destination = File(externalRoot, relativePath)
        destination.parentFile?.mkdirs()
        destination.delete()

        val request = DownloadManager.Request(Uri.parse(source)).apply {
            setTitle("家庭成长 ${update.version} 更新")
            setDescription("正在从 GitHub Release 下载并等待安全校验")
            setAllowedOverMetered(true)
            setAllowedOverRoaming(false)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, relativePath)
            addRequestHeader("Accept", "application/octet-stream")
            addRequestHeader("Accept-Encoding", "identity")
            if (source.startsWith("https://api.github.com/")) {
                addRequestHeader("X-GitHub-Api-Version", BuildConfig.GITHUB_API_VERSION)
            }
        }
        val id = manager.enqueue(request)
        activeDownloadId = id
        var completed = false
        var lastBytes = 0L
        var lastMovementAt = SystemClock.elapsedRealtime()
        onProgress(DownloadProgress(DownloadPhase.QUEUED, sourceAttempt = attempt))
        try {
            while (true) {
                val cursor = manager.query(DownloadManager.Query().setFilterById(id))
                cursor.use {
                    if (!it.moveToFirst()) throw IOException("系统下载任务不存在")
                    val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val bytes = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)).coerceAtLeast(0L)
                    val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)).takeIf { value -> value > 0L }
                        ?: update.asset.size
                    if (bytes > lastBytes) {
                        lastBytes = bytes
                        lastMovementAt = SystemClock.elapsedRealtime()
                    }
                    val percent = if (bytes > 0L && total > 0L) ((bytes * 100L) / total).coerceIn(0L, 100L).toInt() else null
                    when (status) {
                        DownloadManager.STATUS_PENDING -> onProgress(DownloadProgress(DownloadPhase.QUEUED, percent, bytes, total, attempt))
                        DownloadManager.STATUS_RUNNING -> onProgress(DownloadProgress(if (bytes == 0L) DownloadPhase.CONNECTING else DownloadPhase.DOWNLOADING, percent, bytes, total, attempt))
                        DownloadManager.STATUS_PAUSED -> {
                            val reason = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                            onProgress(DownloadProgress(DownloadPhase.PAUSED, percent, bytes, total, attempt, pausedReason(reason)))
                        }
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            completed = true
                            return SystemAsset(manager, id, destination)
                        }
                        DownloadManager.STATUS_FAILED -> {
                            val reason = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                            throw IOException(failedReason(reason))
                        }
                    }
                }
                if (SystemClock.elapsedRealtime() - lastMovementAt >= STALL_TIMEOUT_MILLIS) {
                    throw SocketTimeoutException(if (lastBytes == 0L) "GitHub 下载未收到首字节" else "GitHub 下载长时间无新数据")
                }
                Thread.sleep(POLL_INTERVAL_MILLIS)
            }
        } finally {
            activeDownloadId = null
            if (!completed) manager.remove(id)
        }
    }

    private fun verifyAndCopy(source: File, target: File, update: UpdateInfo): File {
        if (!source.isFile || source.length() != update.asset.size) {
            throw UpdateException("APK 文件大小与 Release 元数据不一致")
        }
        if (source.length() > MAX_APK_BYTES) throw UpdateException("APK 超过允许的大小")
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(source).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        val actualDigest = digest.digest().joinToString("") { "%02x".format(it) }
        val expectedDigest = update.asset.digest.orEmpty().substringAfter(':').lowercase()
        if (actualDigest != expectedDigest) throw UpdateException("APK SHA-256 校验失败")
        Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        return target
    }

    private fun parseLatestRelease(json: JSONObject): UpdateInfo {
        val version = SemanticVersion.parse(json.getString("tag_name"))
            ?: throw UpdateException("Release tag 必须为 vMAJOR.MINOR.PATCH")
        val releasePageUrl = json.getString("html_url")
        val assetsJson = json.getJSONArray("assets")
        val assets = buildList {
            for (index in 0 until assetsJson.length()) {
                val asset = assetsJson.getJSONObject(index)
                add(
                    ReleaseAsset(
                        name = asset.getString("name"),
                        downloadUrl = asset.getString("browser_download_url"),
                        digest = if (asset.isNull("digest")) null else asset.getString("digest"),
                        size = asset.getLong("size"),
                        apiUrl = asset.getString("url"),
                    ),
                )
            }
        }
        return UpdateInfo(version, releasePageUrl, selectReleaseAsset(version, repository, assets))
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 120_000
            useCaches = false
        }

    companion object {
        private const val MAX_APK_BYTES = 250L * 1024L * 1024L
        private const val STALL_TIMEOUT_MILLIS = 45_000L
        private const val POLL_INTERVAL_MILLIS = 750L
    }

    private data class SystemAsset(val manager: DownloadManager, val id: Long, val file: File)
}

private fun pausedReason(reason: Int): String = when (reason) {
    DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "等待可用网络"
    DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "等待 Wi-Fi"
    DownloadManager.PAUSED_WAITING_TO_RETRY -> "系统准备重试"
    else -> "系统暂时暂停"
}

private fun failedReason(reason: Int): String = when (reason) {
    DownloadManager.ERROR_INSUFFICIENT_SPACE -> "设备存储空间不足"
    DownloadManager.ERROR_DEVICE_NOT_FOUND -> "下载存储不可用"
    DownloadManager.ERROR_HTTP_DATA_ERROR -> "GitHub 下载数据中断"
    DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "GitHub 下载重定向过多"
    DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "GitHub 返回无法处理的 HTTP 状态"
    DownloadManager.ERROR_CANNOT_RESUME -> "系统无法继续该下载"
    else -> "系统下载失败（原因 $reason）"
}
