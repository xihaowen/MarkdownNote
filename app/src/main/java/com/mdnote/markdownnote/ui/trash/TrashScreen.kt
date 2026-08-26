package com.mdnote.markdownnote.ui.trash

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
// 回收站不可见时暂停业务 Flow 收集
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mdnote.markdownnote.domain.model.Note
import com.mdnote.markdownnote.ui.components.glass.GlassDialog
import com.mdnote.markdownnote.ui.components.glass.GlassSheet
import com.mdnote.markdownnote.util.TimeUtils

/**
 * 回收站页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashSheetContent(
    onDismiss: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel()
) {
    // 回收站状态按宿主生命周期收集
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var noteToRestore by remember { mutableStateOf<Note?>(null) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    GlassSheet(
        onDismissRequest = onDismiss,
        title = "回收站",
        onClose = onDismiss,
        trailingAction = {
            if (uiState.notes.isNotEmpty()) {
                TextButton(
                    onClick = { showClearConfirm = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("清空")
                }
            }
        }
    ) {
        if (uiState.notes.isEmpty()) {
            EmptyTrash(modifier = Modifier.fillMaxSize())
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // 提示信息
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "笔记将在回收站保留30天后自动删除",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // 笔记列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(items = uiState.notes, key = { it.id }) { note ->
                        TrashNoteItem(
                            note = note,
                            onRestore = { noteToRestore = note },
                            onDelete = { noteToDelete = note }
                        )
                    }
                }
            }
        }
    }

    // 恢复确认
    noteToRestore?.let { note ->
        GlassDialog(
            onDismissRequest = { noteToRestore = null },
            title = { Text("恢复笔记") },
            text = { Text("确定要恢复「${note.title.ifEmpty { "无标题笔记" }}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.restoreNote(note.id)
                    noteToRestore = null
                }) { Text("恢复") }
            },
            dismissButton = {
                TextButton(onClick = { noteToRestore = null }) { Text("取消") }
            }
        )
    }

    // 永久删除确认
    noteToDelete?.let { note ->
        GlassDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("永久删除") },
            text = { Text("确定要永久删除「${note.title.ifEmpty { "无标题笔记" }}」吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.permanentlyDelete(note.id)
                        noteToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) { Text("取消") }
            }
        )
    }

    // 清空确认
    if (showClearConfirm) {
        GlassDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空回收站") },
            text = { Text("确定要清空回收站吗？所有笔记将被永久删除，此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearTrash()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("清空") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun TrashNoteItem(
    note: Note,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title.ifEmpty { "无标题笔记" },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "删除于 ${TimeUtils.formatTime(note.deletedAt ?: 0)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onRestore) {
                Icon(Icons.Default.Restore, "恢复")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteForever, "永久删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun EmptyTrash(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.DeleteOutline,
                null,
                Modifier.size(64.dp),
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(16.dp))
            Text("回收站是空的", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
