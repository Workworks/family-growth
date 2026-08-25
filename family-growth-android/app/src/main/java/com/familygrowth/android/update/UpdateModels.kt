package com.familygrowth.android.update

import java.io.File

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
    data class Downloading(val update: UpdateInfo, val percent: Int) : UpdateUiState
    data class Ready(val update: UpdateInfo, val file: File) : UpdateUiState
    data class PermissionRequired(val update: UpdateInfo, val file: File) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

private val sha256DigestPattern = Regex("^sha256:[0-9a-fA-F]{64}$")
private val githubRepositoryPattern = Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")

fun isValidGitHubRepository(value: String): Boolean = githubRepositoryPattern.matches(value)

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
    return asset
}

class UpdateException(message: String, cause: Throwable? = null) : Exception(message, cause)
