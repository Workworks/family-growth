package com.familygrowth.android.update

import android.content.Context
import com.familygrowth.android.BuildConfig
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.io.IOException

class GitHubReleaseClient(private val repository: String) {
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

    fun download(context: Context, update: UpdateInfo, onProgress: (Int) -> Unit): File {
        val updateDirectory = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(updateDirectory, update.asset.name)
        val partial = File(updateDirectory, "${update.asset.name}.part")
        target.delete()
        partial.delete()

        try {
            return retryIo(MAX_DOWNLOAD_ATTEMPTS, onRetry = { onProgress(0) }) {
                partial.delete()
                downloadOnce(partial, target, update, onProgress)
            }
        } catch (exception: UpdateException) {
            partial.delete()
            target.delete()
            throw exception
        } catch (exception: IOException) {
            partial.delete()
            target.delete()
            throw UpdateException(downloadFailureMessage(exception, MAX_DOWNLOAD_ATTEMPTS), exception)
        } catch (exception: Exception) {
            partial.delete()
            target.delete()
            throw UpdateException(downloadFailureMessage(exception, MAX_DOWNLOAD_ATTEMPTS), exception)
        }
    }

    @Throws(IOException::class)
    private fun downloadOnce(partial: File, target: File, update: UpdateInfo, onProgress: (Int) -> Unit): File {
        val connection = open(update.asset.downloadUrl).apply {
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/octet-stream")
            setRequestProperty("Accept-Encoding", "identity")
            setRequestProperty("User-Agent", "family-growth-android/${BuildConfig.VERSION_NAME}")
        }
        try {
            val status = connection.responseCode
            if (status != HttpURLConnection.HTTP_OK) throw UpdateException("APK 下载失败（HTTP $status）")
            val finalUri = runCatching { connection.url.toURI() }
                .getOrElse { throw UpdateException("GitHub 下载地址无效", it) }
            if (!isTrustedGitHubAssetRedirect(finalUri)) {
                throw UpdateException("GitHub 下载重定向到了不受信任的地址")
            }
            val declaredLength = connection.contentLengthLong
            if (declaredLength > MAX_APK_BYTES || update.asset.size > MAX_APK_BYTES) {
                throw UpdateException("APK 超过允许的大小")
            }
            if (declaredLength > 0L && declaredLength != update.asset.size) {
                throw UpdateException("APK 响应大小与 Release 元数据不一致")
            }

            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            var lastProgress = -1
            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(partial).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_APK_BYTES) throw UpdateException("APK 超过允许的大小")
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        val progress = ((total * 100L) / update.asset.size).coerceIn(0L, 100L).toInt()
                        if (progress != lastProgress) {
                            lastProgress = progress
                            onProgress(progress)
                        }
                    }
                }
            }
            if (total != update.asset.size) {
                throw UpdateException("APK 文件大小与 Release 元数据不一致")
            }
            val actualDigest = digest.digest().joinToString("") { "%02x".format(it) }
            val expectedDigest = update.asset.digest.orEmpty().substringAfter(':').lowercase()
            if (actualDigest != expectedDigest) throw UpdateException("APK SHA-256 校验失败")

            Files.move(partial.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            onProgress(100)
            return target
        } finally {
            connection.disconnect()
        }
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
        private const val MAX_DOWNLOAD_ATTEMPTS = 3
    }
}
