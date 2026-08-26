package com.familygrowth.android.update

import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException
import javax.net.ssl.SSLException

data class SemanticVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<SemanticVersion> {
    override fun compareTo(other: SemanticVersion): Int =
        compareValuesBy(this, other, SemanticVersion::major, SemanticVersion::minor, SemanticVersion::patch)

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        private val pattern = Regex("^v?(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)$")

        fun parse(value: String): SemanticVersion? {
            val match = pattern.matchEntire(value.trim()) ?: return null
            val parts = match.groupValues.drop(1).map { it.toIntOrNull() ?: return null }
            return SemanticVersion(parts[0], parts[1], parts[2])
        }
    }
}

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val digest: String?,
    val size: Long,
    val apiUrl: String? = null,
)

data class UpdateInfo(
    val version: SemanticVersion,
    val releasePageUrl: String,
    val asset: ReleaseAsset,
)

sealed interface UpdateUiState {
    data object Unconfigured : UpdateUiState
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val update: UpdateInfo) : UpdateUiState
    data class Downloading(val update: UpdateInfo, val progress: DownloadProgress) : UpdateUiState
    data class Ready(val update: UpdateInfo, val file: File) : UpdateUiState
    data class PermissionRequired(val update: UpdateInfo, val file: File) : UpdateUiState
    data class Error(val message: String, val update: UpdateInfo? = null) : UpdateUiState
}

enum class DownloadPhase { QUEUED, CONNECTING, DOWNLOADING, PAUSED, SWITCHING_SOURCE, VERIFYING }

data class DownloadProgress(
    val phase: DownloadPhase,
    val percent: Int? = null,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val sourceAttempt: Int = 1,
    val detail: String? = null,
)

private val sha256DigestPattern = Regex("^sha256:[0-9a-fA-F]{64}$")
private val githubRepositoryPattern = Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")

fun isValidGitHubRepository(value: String): Boolean = githubRepositoryPattern.matches(value)

fun isTrustedGitHubAssetRedirect(uri: URI): Boolean {
    val host = uri.host.orEmpty().lowercase()
    val trustedHost = host == "github.com" || host.endsWith(".githubusercontent.com")
    return uri.scheme.equals("https", ignoreCase = true) && trustedHost
}

fun isTrustedGitHubAssetApi(uri: URI, repository: String): Boolean {
    val expectedPrefix = "/repos/$repository/releases/assets/"
    val assetId = uri.path.orEmpty().removePrefix(expectedPrefix)
    return uri.scheme.equals("https", ignoreCase = true) &&
        uri.host.equals("api.github.com", ignoreCase = true) &&
        uri.path.orEmpty().startsWith(expectedPrefix) &&
        assetId.matches(Regex("^[1-9]\\d*$"))
}

fun downloadFailureMessage(exception: Throwable, attempts: Int): String = when (exception) {
    is SocketTimeoutException -> "GitHub APK 下载超时，已自动重试 $attempts 次，请检查网络后重试"
    is UnknownHostException, is ConnectException -> "无法连接 GitHub APK 下载服务，已自动重试 $attempts 次，请检查网络或 DNS"
    is SSLException -> "无法建立安全的 GitHub APK 下载连接，请检查设备时间、证书和网络"
    else -> "GitHub APK 下载中断，已自动重试 $attempts 次，请检查存储空间和网络"
}

@Throws(IOException::class)
fun <T> retryIo(attempts: Int, onRetry: (Int) -> Unit = {}, block: (Int) -> T): T {
    require(attempts > 0) { "attempts must be positive" }
    var lastFailure: IOException? = null
    repeat(attempts) { attempt ->
        try {
            return block(attempt + 1)
        } catch (exception: IOException) {
            lastFailure = exception
            if (attempt < attempts - 1) onRetry(attempt + 2)
        }
    }
    throw lastFailure ?: IOException("download failed")
}

fun selectReleaseAsset(version: SemanticVersion, repository: String, assets: List<ReleaseAsset>): ReleaseAsset {
    val expectedName = "family-growth-$version.apk"
    val asset = assets.singleOrNull { it.name == expectedName }
        ?: throw UpdateException("Release 必须且只能包含一个 $expectedName")
    if (!sha256DigestPattern.matches(asset.digest.orEmpty())) {
        throw UpdateException("Release asset 缺少有效的 SHA-256 digest")
    }
    val expectedPathPrefix = "/$repository/releases/download/"
    val uri = runCatching { java.net.URI(asset.downloadUrl) }.getOrNull()
    if (uri?.scheme != "https" || uri.host != "github.com" || !uri.path.startsWith(expectedPathPrefix)) {
        throw UpdateException("Release asset 下载地址不是配置仓库的 GitHub HTTPS 地址")
    }
    if (asset.size <= 0L) throw UpdateException("Release asset 文件大小无效")
    val apiUri = asset.apiUrl?.let { runCatching { URI(it) }.getOrNull() }
    if (apiUri == null || !isTrustedGitHubAssetApi(apiUri, repository)) {
        throw UpdateException("Release asset API 地址不是配置仓库的 GitHub HTTPS 地址")
    }
    return asset
}

class UpdateException(message: String, cause: Throwable? = null) : Exception(message, cause)
