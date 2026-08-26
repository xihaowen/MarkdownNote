package com.mdnote.markdownnote.ui.noteedit

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdnote.markdownnote.data.repository.NoteRepository
import com.mdnote.markdownnote.domain.model.Note
import com.mdnote.markdownnote.util.AppSettings
import com.mdnote.markdownnote.util.ExportManager
import com.mdnote.markdownnote.util.ImageManager
import com.mdnote.markdownnote.util.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 编辑器 UI 状态
 */
data class NoteEditUiState(
    val note: Note? = null,
    val title: String = "",
    val content: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = true,
    val isNewNote: Boolean = false,
    val lastSavedContent: String = "",  // 用于比较是否需要保存
    val isExternalFile: Boolean = false,  // 是否为外部文件
    val externalFileUri: Uri? = null,     // 外部文件 URI
    val externalFolderUri: Uri? = null    // 外部文件所在的文件夹 URI
)

/**
 * 笔记编辑 ViewModel
 * 优化：防抖自动保存、无提示
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class NoteEditViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    val settingsManager: SettingsManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val noteId: StateFlow<Long> = savedStateHandle.getStateFlow("noteId", 0L)

    private val _uiState = MutableStateFlow(NoteEditUiState())
    val uiState: StateFlow<NoteEditUiState> = _uiState.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsManager.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    // 自动保存 Job
    private var autoSaveJob: Job? = null

    // 内容流（用于防抖保存）
    private val _contentFlow = MutableStateFlow("")

    // 撤销栈
    private val undoStack = mutableListOf<String>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    // 标记：是否正在加载外部文件
    private var isLoadingExternalFile = false

    init {
        // 监听 noteId 变化，重新加载笔记
        viewModelScope.launch {
            noteId.collect { id ->
                // 如果正在加载外部文件，跳过
                if (isLoadingExternalFile) {
                    isLoadingExternalFile = false
                    return@collect
                }
                loadNote(id)
            }
        }

        // 监听内容变化，防抖自动保存
        viewModelScope.launch {
            _contentFlow
                .debounce(AUTO_SAVE_DELAY)
                .distinctUntilChanged()
                .collect { content ->
                    if (content.isNotEmpty() && content != _uiState.value.lastSavedContent) {
                        performAutoSave(content)
                    }
                }
        }
    }

    private fun loadNote(id: Long) {
        viewModelScope.launch {
            if (id == 0L) {
                _uiState.update {
                    NoteEditUiState(isNewNote = true)
                }
            } else {
                val note = noteRepository.getNoteById(id)
                if (note != null) {
                    // 解析外部文件夹 URI
                    val externalFolderUri = note.externalFolderUri?.let { Uri.parse(it) }

                    _uiState.update { state ->
                        state.copy(
                            note = note,
                            title = note.title,
                            content = note.content,
                            lastSavedContent = note.content,
                            isNewNote = false,
                            externalFolderUri = externalFolderUri
                        )
                    }
                }
            }
        }
    }

    // ==================== 内容更新 ====================

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title, isSaved = false) }
        scheduleAutoSave()
    }

    fun updateContent(content: String) {
        val oldContent = _uiState.value.content
        if (oldContent != content) {
            undoStack.add(oldContent)
            if (undoStack.size > 50) undoStack.removeAt(0)
            _canUndo.value = true
        }
        _uiState.update { it.copy(content = content, isSaved = false) }
        _contentFlow.value = content
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val previousContent = undoStack.removeAt(undoStack.size - 1)
        _canUndo.value = undoStack.isNotEmpty()
        _uiState.update { it.copy(content = previousContent, isSaved = false) }
        _contentFlow.value = previousContent
    }

    /**
     * 加载外部文件（不保存到数据库）
     * 调用此方法前，应先调用 prepareForExternalFile()
     */
    fun loadExternalFile(title: String, content: String, fileUri: Uri, folderUri: Uri?) {
        _uiState.update {
            it.copy(
                title = title,
                content = content,
                lastSavedContent = content,
                isSaved = true,
                isNewNote = false,
                isExternalFile = true,
                externalFileUri = fileUri,
                externalFolderUri = folderUri
            )
        }
    }

    /**
     * 准备加载外部文件（在导航之前调用）
     */
    fun prepareForExternalFile() {
        isLoadingExternalFile = true
        _uiState.update {
            NoteEditUiState(
                isExternalFile = true,
                isSaved = true
            )
        }
    }

    // ==================== 自动保存 ====================

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(AUTO_SAVE_DELAY)
            performAutoSave(_uiState.value.content)
        }
    }

    /**
     * 执行自动保存（静默，无提示）
     */
    private suspend fun performAutoSave(content: String) {
        val currentTitle = _uiState.value.title.ifEmpty {
            noteRepository.extractTitle(content)
        }

        // 获取当前笔记 ID（如果是外部文件编辑后保存，使用新 ID）
        val currentNoteId = if (_uiState.value.isExternalFile) {
            0L  // 外部文件保存为新笔记
        } else {
            noteId.value
        }

        // 获取外部文件夹 URI（用于保存到数据库，以便后续打开时能正确显示图片）
        val externalFolderUriStr = _uiState.value.externalFolderUri?.toString()
            ?: _uiState.value.note?.externalFolderUri

        val note = Note(
            id = if (_uiState.value.isNewNote || _uiState.value.isExternalFile) 0L else currentNoteId,
            title = currentTitle,
            content = content,
            lastModifiedTime = System.currentTimeMillis(),
            isStarred = _uiState.value.note?.isStarred ?: false,
            isPinned = _uiState.value.note?.isPinned ?: false,
            externalFolderUri = externalFolderUriStr
        )

        val savedId = noteRepository.saveNote(note)

        // 更新 noteId（用于后续保存）
        if (savedId > 0) {
            savedStateHandle["noteId"] = savedId
        }

        _uiState.update { state ->
            state.copy(
                isSaved = true,
                isNewNote = false,
                isExternalFile = false,  // 保存后不再是外部文件
                lastSavedContent = content,
                note = note.copy(id = savedId)
            )
        }
    }

    // ==================== 手动保存 ====================

    fun saveNote() {
        viewModelScope.launch {
            performAutoSave(_uiState.value.content)
        }
    }

    // ==================== 其他操作 ====================

    fun toggleStar() {
        viewModelScope.launch {
            val currentNote = _uiState.value.note
            if (currentNote != null) {
                noteRepository.toggleStar(currentNote.id)
                _uiState.update { state ->
                    state.copy(note = currentNote.copy(isStarred = !currentNote.isStarred))
                }
            }
        }
    }

    fun insertImage(context: Context, uri: Uri): String? {
        val relativePath = ImageManager.compressAndSaveImage(context, uri) ?: return null
        return ImageManager.generateMarkdownImage("图片", relativePath)
    }

    fun exportNote(context: Context): Boolean {
        val title = _uiState.value.title.ifEmpty { "未命名笔记" }
        return ExportManager.exportToPrivateStorage(context, title, _uiState.value.content) != null
    }

    fun createShareIntent(context: Context) = ExportManager.createShareIntent(
        context,
        _uiState.value.title.ifEmpty { "未命名笔记" },
        _uiState.value.content
    )

    override fun onCleared() {
        super.onCleared()
        // ViewModel 清除时不再用 runBlocking 保存（会阻塞主线程导致 ANR）
        // 保存由 Composable 的 DisposableEffect 提前触发
    }

    companion object {
        private const val AUTO_SAVE_DELAY = 1000L // 1秒
    }
}
