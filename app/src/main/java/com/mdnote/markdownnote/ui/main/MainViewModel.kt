package com.mdnote.markdownnote.ui.main

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdnote.markdownnote.data.repository.FolderRepository
import com.mdnote.markdownnote.data.repository.RecentExternalFile
import com.mdnote.markdownnote.util.AppSettings
import com.mdnote.markdownnote.util.ExternalFileManager
import com.mdnote.markdownnote.util.ExternalFileState
import com.mdnote.markdownnote.util.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 主页面 UI 状态
 */
data class MainUiState(
    val isInitialized: Boolean = false,
    val pendingNavigation: PendingNavigation? = null
)

/**
 * 待处理的导航
 */
sealed class PendingNavigation {
    data class ToExternalFile(val fileUri: Uri, val folderUri: Uri) : PendingNavigation()
    data class ToNote(val noteId: Long) : PendingNavigation()
}

/**
 * 主页面 ViewModel
 * 管理 MainActivity 的状态
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val folderRepository: FolderRepository,
    private val externalFileManager: ExternalFileManager,
    val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // 应用设置
    val settings: StateFlow<AppSettings> = settingsManager.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    // 当前文件夹 URI
    val currentFolderUri: StateFlow<Uri?> = folderRepository.currentFolderUri

    // 最近文件
    val recentFiles: StateFlow<List<RecentExternalFile>> = folderRepository.recentFiles

    // 当前外部文件
    val currentExternalFile: StateFlow<ExternalFileState> = externalFileManager.currentExternalFile

    // 初始化状态
    val isInitialized: StateFlow<Boolean> = folderRepository.isInitialized

    init {
        // 初始化 FolderRepository
        viewModelScope.launch {
            folderRepository.initialize()
            _uiState.update { it.copy(isInitialized = true) }
        }
    }

    /**
     * 设置当前文件夹
     */
    fun setCurrentFolder(uri: Uri) {
        folderRepository.setCurrentFolderUri(uri)
    }

    /**
     * 清除当前文件夹
     */
    fun clearCurrentFolder() {
        folderRepository.setCurrentFolderUri(null)
    }

    /**
     * 从最近文件打开
     */
    fun openRecentFile(fileUri: Uri, folderUri: Uri) {
        viewModelScope.launch {
            externalFileManager.loadExternalFileWithFolder(fileUri, folderUri)
            _uiState.update { it.copy(pendingNavigation = PendingNavigation.ToExternalFile(fileUri, folderUri)) }
        }
    }

    /**
     * 从文件夹浏览器打开文件
     */
    fun openFileFromFolder(fileUri: Uri, folderUri: Uri) {
        viewModelScope.launch {
            externalFileManager.loadExternalFileWithFolder(fileUri, folderUri)
            _uiState.update { it.copy(pendingNavigation = PendingNavigation.ToExternalFile(fileUri, folderUri)) }
        }
    }

    /**
     * 打开应用内笔记
     */
    fun openNote(noteId: Long) {
        externalFileManager.clearExternalFile()
        _uiState.update { it.copy(pendingNavigation = PendingNavigation.ToNote(noteId)) }
    }

    /**
     * 清除待处理的导航
     */
    fun clearPendingNavigation() {
        _uiState.update { it.copy(pendingNavigation = null) }
    }

    /**
     * 添加最近文件
     */
    fun addRecentFile(file: RecentExternalFile) {
        folderRepository.addRecentFile(file)
    }

    /**
     * 移除最近文件
     */
    fun removeRecentFile(fileUri: Uri) {
        folderRepository.removeRecentFile(fileUri)
    }
}
