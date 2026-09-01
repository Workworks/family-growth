package com.familygrowth.android.update

import android.app.Application
import android.content.Intent
import androidx.core.net.toUri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.familygrowth.android.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val repository = BuildConfig.GITHUB_REPOSITORY.trim()
    private val client = repository.takeIf(::isValidGitHubRepository)?.let {
        runCatching { GitHubReleaseClient(it) }.getOrNull()
    }
    private var downloadJob: Job? = null

    var state: UpdateUiState by mutableStateOf(if (client == null) UpdateUiState.Unconfigured else UpdateUiState.Idle)
        private set

    fun check() {
        val updateClient = client ?: run {
            state = UpdateUiState.Unconfigured
            return
        }
        state = UpdateUiState.Checking
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { updateClient.latestUpdate() } }
                .onSuccess { update ->
                    val current = SemanticVersion.parse(BuildConfig.VERSION_NAME)
                    state = if (current != null && update.version <= current) {
                        UpdateUiState.UpToDate
                    } else {
                        UpdateUiState.Available(update)
                    }
                }
                .onFailure { state = UpdateUiState.Error(it.message ?: "检查更新失败") }
        }
    }

    fun download(update: UpdateInfo) {
        val updateClient = client ?: return
        if (downloadJob?.isActive == true) return
        state = UpdateUiState.Downloading(update, DownloadProgress(DownloadPhase.QUEUED))
        downloadJob = viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    updateClient.download(getApplication(), update) { percent ->
                        mainHandler.post {
                            if (state is UpdateUiState.Downloading) {
                                state = UpdateUiState.Downloading(update, percent)
                            }
                        }
                    }
                }
                state = UpdateUiState.Ready(update, file)
            } catch (_: CancellationException) {
                state = UpdateUiState.Available(update)
            } catch (_: InterruptedException) {
                state = UpdateUiState.Available(update)
            } catch (exception: Exception) {
                state = UpdateUiState.Error(exception.message ?: "下载更新失败", update)
            } finally {
                downloadJob = null
            }
        }
    }

    fun cancelDownload() {
        val downloading = state as? UpdateUiState.Downloading ?: return
        client?.cancel(getApplication())
        downloadJob?.cancel()
        state = UpdateUiState.Available(downloading.update)
    }

    fun openReleasePage(update: UpdateInfo) {
        runCatching {
            getApplication<Application>().startActivity(
                Intent(Intent.ACTION_VIEW, update.releasePageUrl.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure { state = UpdateUiState.Error("无法打开 GitHub Release 页面", update) }
    }

    fun install(update: UpdateInfo, file: java.io.File) {
        runCatching { UpdateInstaller.open(getApplication(), file) }
            .onSuccess { result ->
                if (result == UpdateInstaller.Result.PERMISSION_REQUIRED) {
                    state = UpdateUiState.PermissionRequired(update, file)
                }
            }
            .onFailure { state = UpdateUiState.Error(it.message ?: "无法打开系统安装界面") }
    }
}
