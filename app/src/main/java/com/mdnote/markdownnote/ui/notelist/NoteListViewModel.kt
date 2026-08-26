package com.mdnote.markdownnote.ui.notelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdnote.markdownnote.data.repository.NoteRepository
import com.mdnote.markdownnote.domain.model.Note
import com.mdnote.markdownnote.util.AppSettings
import com.mdnote.markdownnote.util.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteListUiState(
    val notes: List<Note> = emptyList(),
    val deletedNotesCount: Int = 0,
    val searchQuery: String = "",
    val showStarredOnly: Boolean = false,
    val sidebarExpanded: Boolean = true
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteListUiState())
    val uiState: StateFlow<NoteListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _showStarredOnly = MutableStateFlow(false)

    val settings: StateFlow<AppSettings> = settingsManager.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    init {
        viewModelScope.launch {
            settingsManager.settings.collect { appSettings ->
                _uiState.update { it.copy(sidebarExpanded = appSettings.sidebarExpanded) }
            }
        }

        viewModelScope.launch {
            noteRepository.getDeletedNotesCount().collect { count ->
                _uiState.update { it.copy(deletedNotesCount = count) }
            }
        }

        // 合并搜索和过滤条件
        viewModelScope.launch {
            combine(_searchQuery.debounce(300), _showStarredOnly) { query, starredOnly ->
                Pair(query, starredOnly)
            }
                .flatMapLatest { (query, starredOnly) ->
                    observeNotes(query, starredOnly)
                }
                .collect { notes ->
                    _uiState.update { state ->
                        state.copy(notes = notes)
                    }
                }
        }
    }

    private fun observeNotes(query: String, starredOnly: Boolean): Flow<List<Note>> {
        return when {
            query.isNotEmpty() -> noteRepository.searchNotes(query)
            starredOnly -> noteRepository.getStarredNotes()
            else -> noteRepository.getAllNoteSummaries()
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleStarredFilter() {
        _showStarredOnly.update { !it }
        _uiState.update { it.copy(showStarredOnly = !_uiState.value.showStarredOnly) }
    }

    fun toggleSidebar() {
        viewModelScope.launch {
            settingsManager.setSidebarExpanded(!_uiState.value.sidebarExpanded)
        }
    }

    suspend fun createNote(): Long = noteRepository.createNote()

    fun deleteNote(note: Note) {
        viewModelScope.launch { noteRepository.moveToTrash(note.id) }
    }

    fun toggleStar(noteId: Long) {
        viewModelScope.launch { noteRepository.toggleStar(noteId) }
    }

    fun togglePin(noteId: Long) {
        viewModelScope.launch { noteRepository.togglePin(noteId) }
    }

    fun renameNote(noteId: Long, newTitle: String) {
        viewModelScope.launch { noteRepository.renameNote(noteId, newTitle) }
    }

    suspend fun importNote(title: String, content: String): Long {
        return noteRepository.createNote(title, content)
    }
}
