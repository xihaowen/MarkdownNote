package com.mdnote.markdownnote.ui.todo.components

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mdnote.markdownnote.domain.model.Todo
import android.util.Log
import com.mdnote.markdownnote.ui.theme.LocalFontSize

// 日志标签
private const val TAG = "TodoItem"

/**
 * 单个待办事项组件
 */
@Composable
fun TodoItem(
    todo: Todo,
    isOverdue: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val fontSize = LocalFontSize.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = todo.isCompleted,
            onCheckedChange = {
                // 完成时播放音效
                if (!todo.isCompleted) {
                    playCompletionSound(context)
                }
                onToggle()
            },
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = todo.content,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSize.sp),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            color = when {
                todo.isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                isOverdue -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            },
            textDecoration = if (todo.isCompleted)
                TextDecoration.LineThrough
            else
                null
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * 播放完成音效 - 系统默认通知音
 */
private fun playCompletionSound(context: Context) {
    try {
        val notificationUri = Settings.System.DEFAULT_NOTIFICATION_URI
        val ringtone = android.media.RingtoneManager.getRingtone(context, notificationUri)
        ringtone?.play()
    } catch (e: Exception) {
        Log.e(TAG, "播放完成音效失败", e)
    }
}
