package com.mdnote.markdownnote.ui.todo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
// 待办页不可见时暂停业务 Flow 收集
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mdnote.markdownnote.ui.theme.LocalFontSize
import com.mdnote.markdownnote.ui.todo.components.TodoInput
import com.mdnote.markdownnote.ui.todo.components.TodoItem

/**
 * 待办事项主界面
 */
@Composable
fun TodoScreen(
    viewModel: TodoViewModel = hiltViewModel()
) {
    // 待办状态按宿主生命周期收集
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val fontSize = LocalFontSize.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 24.dp)
    ) {
        // 标题区域
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "待办",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = (fontSize + 4).sp
                )
            )
            if (uiState.todayTodos.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${uiState.todayTodos.size}",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = fontSize.sp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 添加待办输入框
        TodoInput(
            onAddTodo = { viewModel.addTodo(it) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 待办列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 过期待办
            if (uiState.overdueTodos.isNotEmpty()) {
                item {
                    Text(
                        text = "过期",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = (fontSize - 2).sp),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(items = uiState.overdueTodos, key = { it.id }) { todo ->
                    TodoItem(
                        todo = todo,
                        isOverdue = true,
                        onToggle = { viewModel.toggleTodoCompleted(todo) },
                        onDelete = { viewModel.deleteTodo(todo) }
                    )
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            // 今日待办标题
            item {
                Text(
                    text = "今天",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = (fontSize - 2).sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // 今日待办列表
            if (uiState.todayTodos.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.CheckBox,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            )
                            Text(
                                text = "暂无待办",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSize.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            } else {
                items(items = uiState.todayTodos, key = { it.id }) { todo ->
                    TodoItem(
                        todo = todo,
                        isOverdue = false,
                        onToggle = { viewModel.toggleTodoCompleted(todo) },
                        onDelete = { viewModel.deleteTodo(todo) }
                    )
                }
            }
        }
    }
}
