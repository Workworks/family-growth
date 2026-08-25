package com.familygrowth.android.update

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.familygrowth.android.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BuildConfig.GITHUB_REPOSITORY.trim()
    private val client = repository.takeIf(::isValidGitHubRepository)?.let {
        runCatching { GitHubReleaseClient(it) }.getOrNull()
    }

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
        state = UpdateUiState.Downloading(update, 0)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    updateClient.download(getApplication(), update) { percent ->
                        state = UpdateUiState.Downloading(update, percent)
                    }
                }
            }.onSuccess { file ->
                state = UpdateUiState.Ready(update, file)
            }.onFailure {
                state = UpdateUiState.Error(it.message ?: "下载更新失败")
            }
        }
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
