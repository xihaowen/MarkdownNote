package com.mdnote.markdownnote.ui.todo

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdnote.markdownnote.data.repository.TodoRepository
import com.mdnote.markdownnote.domain.model.Todo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val todoRepository: TodoRepository
) : ViewModel() {

    companion object {
        private const val TAG = "TodoViewModel"
    }

    private val _uiState = MutableStateFlow(TodoUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // 获取今日待办
        viewModelScope.launch {
            try {
                todoRepository.getTodayTodos().collect { todos ->
                    _uiState.update { it.copy(todayTodos = todos) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "收集今日待办失败", e)
            }
        }

        // 获取过期待办
        viewModelScope.launch {
            try {
                todoRepository.getOverdueTodos().collect { todos ->
                    _uiState.update { it.copy(overdueTodos = todos) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "收集过期待办失败", e)
            }
        }
    }

    fun addTodo(content: String) {
        viewModelScope.launch {
            todoRepository.addTodo(content)
        }
    }

    fun toggleTodoCompleted(todo: Todo) {
        viewModelScope.launch {
            todoRepository.toggleCompleted(todo)
        }
    }

    fun deleteTodo(todo: Todo) {
        viewModelScope.launch {
            todoRepository.deleteTodo(todo)
        }
    }
}
