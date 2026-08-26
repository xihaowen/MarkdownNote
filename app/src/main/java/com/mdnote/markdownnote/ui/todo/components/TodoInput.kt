package com.mdnote.markdownnote.ui.todo.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mdnote.markdownnote.ui.theme.LocalFontSize

/**
 * 待办事项输入组件
 */
@Composable
fun TodoInput(
    onAddTodo: (String) -> Unit
) {
    var newTodoText by remember { mutableStateOf("") }
    val fontSize = LocalFontSize.current

    OutlinedTextField(
        value = newTodoText,
        onValueChange = { newTodoText = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                "添加待办...",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontSize = fontSize.sp
            )
        },
        trailingIcon = {
            if (newTodoText.isNotBlank()) {
                TextButton(
                    onClick = {
                        onAddTodo(newTodoText)
                        newTodoText = ""
                    }
                ) {
                    Text("添加", fontSize = fontSize.sp)
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
    )
}
