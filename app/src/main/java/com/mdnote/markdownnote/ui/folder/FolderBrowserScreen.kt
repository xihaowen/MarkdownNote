package com.mdnote.markdownnote.ui.folder

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mdnote.markdownnote.data.repository.FolderFile
import com.mdnote.markdownnote.data.repository.FolderRepository
import com.mdnote.markdownnote.ui.components.glass.GlassSheet
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 文件夹浏览器状态
 */
data class FolderBrowserState(
    val folderUri: Uri? = null,
    val folderName: String = "",
    val files: List<FolderFile> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * 文件夹浏览器屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderBrowserSheetContent(
    onDismiss: () -> Unit,
    onOpenFile: (Uri, String) -> Unit,
    folderRepository: FolderRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf(FolderBrowserState()) }

    // 文件夹选择器
    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            // 获取持久化权限
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            // 持久化保存文件夹 URI
            folderRepository.setCurrentFolderUri(uri)

            state = state.copy(
                folderUri = uri,
                folderName = folderRepository.getFolderName(uri),
                isLoading = true,
                error = null
            )

            scope.launch {
                val files = folderRepository.getMarkdownFiles(uri)
                state = state.copy(
                    files = files,
                    isLoading = false
                )
            }
        }
    }

    // 加载已保存的文件夹
    LaunchedEffect(Unit) {
        val savedUri = folderRepository.getCurrentFolderUri()
        if (savedUri != null) {
            state = state.copy(
                folderUri = savedUri,
                folderName = folderRepository.getFolderName(savedUri),
                isLoading = true
            )

            val files = folderRepository.getMarkdownFiles(savedUri)
            state = state.copy(
                files = files,
                isLoading = false
            )
        }
    }

    GlassSheet(
        onDismissRequest = onDismiss,
        title = if (state.folderUri != null) state.folderName else "打开文件夹",
        onClose = onDismiss,
        trailingAction = {
            IconButton(onClick = {
                folderPickerLauncher.launch(null)
            }) {
                Icon(Icons.Filled.FolderOpen, "选择文件夹")
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (state.folderUri == null) {
                // 未选择文件夹
                EmptyFolderState(
                    onOpenFolder = { folderPickerLauncher.launch(null) }
                )
            } else if (state.isLoading) {
                // 加载中
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.files.isEmpty()) {
                // 文件夹为空
                EmptyFilesState(
                    folderName = state.folderName,
                    onChangeFolder = { folderPickerLauncher.launch(null) }
                )
            } else {
                // 文件列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(state.files, key = { it.uri.toString() }) { file ->
                        FileItem(
                            file = file,
                            onClick = {
                                onOpenFile(file.uri, state.folderUri.toString())
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFolderState(
    onOpenFolder: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Folder,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "选择一个文件夹",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "选择包含 Markdown 文件的文件夹来浏览和编辑",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onOpenFolder,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Filled.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("选择文件夹")
        }
    }
}

@Composable
private fun EmptyFilesState(
    folderName: String,
    onChangeFolder: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "「$folderName」中没有 Markdown 文件",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "支持的格式: .md, .markdown",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onChangeFolder) {
            Text("选择其他文件夹")
        }
    }
}

@Composable
private fun FileItem(
    file: FolderFile,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (file.lastModified > 0) {
                    Text(
                        text = dateFormat.format(Date(file.lastModified)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(180f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}
