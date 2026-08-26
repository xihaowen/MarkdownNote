package com.mdnote.markdownnote.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdnote.markdownnote.data.repository.NoteRepository
import com.mdnote.markdownnote.domain.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrashUiState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    init {
        loadDeletedNotes()
    }

    private fun loadDeletedNotes() {
        viewModelScope.launch {
            noteRepository.getDeletedNotes().collect { notes ->
                _uiState.update { it.copy(notes = notes, isLoading = false) }
            }
        }
    }

    fun restoreNote(noteId: Long) {
        viewModelScope.launch {
            noteRepository.restoreFromTrash(noteId)
        }
    }

    fun permanentlyDelete(noteId: Long) {
        viewModelScope.launch {
            noteRepository.permanentlyDelete(noteId)
        }
    }

    fun clearTrash() {
        viewModelScope.launch {
            noteRepository.clearTrashBin()
        }
    }
}
