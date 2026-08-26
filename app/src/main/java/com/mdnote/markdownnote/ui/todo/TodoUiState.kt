package com.mdnote.markdownnote.ui.todo

import com.mdnote.markdownnote.domain.model.Todo

/**
 * 待办事项 UI 状态
 */
data class TodoUiState(
    val todayTodos: List<Todo> = emptyList(),
    val overdueTodos: List<Todo> = emptyList(),
    val isLoading: Boolean = false
)
