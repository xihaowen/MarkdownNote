package com.mdnote.markdownnote.ui.notelist

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import com.mdnote.markdownnote.ui.components.glass.GlassDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
// 首页不可见时暂停业务 Flow 收集
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mdnote.markdownnote.domain.model.Note
import com.mdnote.markdownnote.ui.main.MainViewModel
import com.mdnote.markdownnote.ui.notelist.components.NoteItem
import com.mdnote.markdownnote.ui.theme.LocalFontSize
import com.mdnote.markdownnote.ui.todo.TodoScreen
import com.mdnote.markdownnote.data.repository.RecentExternalFile
import kotlinx.coroutines.launch

/**
 * 笔记列表页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    onNoteClick: (Long) -> Unit,
    onSettingsClick: () -> Unit = {},
    onTrashClick: () -> Unit = {},
    onFolderBrowserClick: () -> Unit = {},
    onExternalFileClick: (Uri, Uri) -> Unit = { _, _ -> },
    viewModel: NoteListViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    // 首页状态按宿主生命周期收集
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var noteToRename by remember { mutableStateOf<Note?>(null) }
    val fontSize = LocalFontSize.current
    val context = LocalContext.current
    val isLargeScreen = LocalConfiguration.current.screenWidthDp >= 600

    // 从 MainViewModel 获取最近文件
    val recentFiles by mainViewModel.recentFiles.collectAsStateWithLifecycle()
    val isInitialized by mainViewModel.isInitialized.collectAsStateWithLifecycle()

    // 删除最近文件
    fun removeRecentFile(fileUri: Uri) {
        mainViewModel.removeRecentFile(fileUri)
    }
    val onCreateNote: () -> Unit = {
        scope.launch {
            val noteId = viewModel.createNote()
            onNoteClick(noteId)
        }
    }

    // 删除确认对话框
    noteToDelete?.let { note ->
        GlassDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("删除笔记") },
            text = { Text("确定要删除「${note.title.ifEmpty { "无标题笔记" }}」吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteNote(note)
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

    // 重命名对话框
    noteToRename?.let { note ->
        var newTitle by remember { mutableStateOf(note.title) }
        GlassDialog(
            onDismissRequest = { noteToRename = null },
            title = { Text("重命名笔记") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入新标题") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameNote(note.id, newTitle)
                    noteToRename = null
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { noteToRename = null }) { Text("取消") }
            }
        )
    }
    if (isLargeScreen) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // 侧边栏
                SidebarContent(
                    uiState = uiState,
                    fontSize = fontSize,
                    recentFiles = recentFiles,
                    isInitialized = isInitialized,
                    isLargeScreen = isLargeScreen,
                    onToggleSidebar = { viewModel.toggleSidebar() },
                    onSettingsClick = onSettingsClick,
                    onCreateNote = onCreateNote,
                    onFolderBrowserClick = onFolderBrowserClick,
                    onExternalFileClick = onExternalFileClick,
                    onRemoveRecentFile = { fileUri -> removeRecentFile(fileUri) },
                    onSearch = { viewModel.search(it) },
                    onToggleStarredFilter = { viewModel.toggleStarredFilter() },
                    onNoteClick = onNoteClick,
                    onDeleteNote = { noteToDelete = it },
                    onToggleStar = { viewModel.toggleStar(it) },
                    onTogglePin = { viewModel.togglePin(it) },
                    onRename = { noteToRename = it },
                    onTrashClick = onTrashClick
                )

                // 分隔线
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // 主内容区 - 待办事项 (使用独立的 TodoScreen)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    TodoScreen()
                }
            }
        }
    } else {
        PhoneHome(
            uiState = uiState,
            recentFiles = recentFiles,
            isInitialized = isInitialized,
            onSettingsClick = onSettingsClick,
            onCreateNote = onCreateNote,
            onFolderBrowserClick = onFolderBrowserClick,
            onExternalFileClick = onExternalFileClick,
            onRemoveRecentFile = { fileUri -> removeRecentFile(fileUri) },
            onSearch = { viewModel.search(it) },
            onToggleStarredFilter = { viewModel.toggleStarredFilter() },
            onNoteClick = onNoteClick,
            onDeleteNote = { noteToDelete = it },
            onToggleStar = { viewModel.toggleStar(it) },
            onTogglePin = { viewModel.togglePin(it) },
            onRename = { noteToRename = it },
            onTrashClick = onTrashClick
        )
    }
}

/**
 * 侧边栏内容
 */
@Composable
private fun SidebarContent(
    uiState: NoteListUiState,
    fontSize: Int,
    recentFiles: List<RecentExternalFile>,
    isInitialized: Boolean,
    isLargeScreen: Boolean,
    onToggleSidebar: () -> Unit,
    onSettingsClick: () -> Unit,
    onCreateNote: () -> Unit,
    onFolderBrowserClick: () -> Unit,
    onExternalFileClick: (Uri, Uri) -> Unit,
    onRemoveRecentFile: (Uri) -> Unit,
    onSearch: (String) -> Unit,
    onToggleStarredFilter: () -> Unit,
    onNoteClick: (Long) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onToggleStar: (Long) -> Unit,
    onTogglePin: (Long) -> Unit,
    onRename: (Note) -> Unit,
    onTrashClick: () -> Unit
) {
    val showExpanded = isLargeScreen || uiState.sidebarExpanded

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(if (showExpanded) 260.dp else 56.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            // 头部
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (!isLargeScreen && !uiState.sidebarExpanded) onToggleSidebar() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isLargeScreen) {
                        IconButton(
                            onClick = onToggleSidebar,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (uiState.sidebarExpanded) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                                contentDescription = "切换",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (showExpanded) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "笔记",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = fontSize.sp
                            )
                        )
                        // 笔记数量徽章
                        if (uiState.notes.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = "${uiState.notes.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                if (showExpanded) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onFolderBrowserClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                "打开文件夹",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Settings,
                                "设置",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onCreateNote,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                "新建",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            if (showExpanded) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 搜索框
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    placeholder = {
                        Text(
                            "搜索...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Search,
                            null,
                            Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                )

                // 收藏过滤
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = uiState.showStarredOnly,
                        onClick = onToggleStarredFilter,
                        label = {
                            Text(
                                "收藏",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (uiState.showStarredOnly) Icons.Filled.Star else Icons.Outlined.Star,
                                null,
                                Modifier.size(14.dp)
                            )
                        },
                        modifier = Modifier.height(28.dp)
                    )
                }

                // 笔记列表
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // 笔记列表
                            if (uiState.notes.isEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Description,
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                        )
                                        Text(
                                            "暂无笔记",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            } else {
                                items(items = uiState.notes, key = { it.id }) { note ->
                                    NoteItem(
                                        note = note,
                                        onClick = { onNoteClick(note.id) },
                                        onDelete = { onDeleteNote(note) },
                                        onToggleStar = { onToggleStar(note.id) },
                                        onTogglePin = { onTogglePin(note.id) },
                                        onRename = { onRename(note) }
                                    )
                                }
                            }

                            // 最近打开的外部文件
                            if (isInitialized && recentFiles.isNotEmpty()) {
                                item {
                                    HorizontalDivider(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }
                                item {
                                    Text(
                                        text = "最近文件",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                items(recentFiles.size, key = { recentFiles[it].fileUri.toString() }) { index ->
                                    val file = recentFiles[index]
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp, vertical = 1.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.Transparent
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onExternalFileClick(file.fileUri, file.folderUri) }
                                                .padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Outlined.Description,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = file.fileName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f)
                                            )
                                            // 删除按钮
                                            IconButton(
                                                onClick = { onRemoveRecentFile(file.fileUri) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Delete,
                                                    contentDescription = "移除",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 底部留白
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                }

                // 回收站
                if (uiState.deletedNotesCount > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTrashClick() }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            null,
                            Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "回收站",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                "${uiState.deletedNotesCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneHome(
    uiState: NoteListUiState,
    recentFiles: List<RecentExternalFile>,
    isInitialized: Boolean,
    onSettingsClick: () -> Unit,
    onCreateNote: () -> Unit,
    onFolderBrowserClick: () -> Unit,
    onExternalFileClick: (Uri, Uri) -> Unit,
    onRemoveRecentFile: (Uri) -> Unit,
    onSearch: (String) -> Unit,
    onToggleStarredFilter: () -> Unit,
    onNoteClick: (Long) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onToggleStar: (Long) -> Unit,
    onTogglePin: (Long) -> Unit,
    onRename: (Note) -> Unit,
    onTrashClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0=笔记, 1=待办
    var isSearching by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        when {
                            // 笔记 Tab + 搜索中：标题区变搜索输入框
                            selectedTab == 0 && isSearching -> {
                                OutlinedTextField(
                                    value = uiState.searchQuery,
                                    onValueChange = onSearch,
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = {
                                        Text(
                                            "搜索...",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    },
                                    leadingIcon = {
                                        IconButton(onClick = {
                                            isSearching = false
                                            onSearch("")
                                        }) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "关闭搜索",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    },
                                    trailingIcon = {
                                        if (uiState.searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { onSearch("") }) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "清除",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    },
                                    singleLine = true
                                )
                            }
                            // 笔记 Tab + 非搜索：标题 + 数量徽章
                            selectedTab == 0 -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "笔记",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (uiState.notes.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        ) {
                                            Text(
                                                text = "${uiState.notes.size}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            // 待办 Tab：标题留空（复用 TodoScreen 自带标题）
                            else -> {}
                        }
                    },
                    actions = {
                        // 仅笔记 Tab 且非搜索态显示操作按钮
                        if (selectedTab == 0 && !isSearching) {
                            IconButton(onClick = { isSearching = true }) {
                                Icon(
                                    Icons.Outlined.Search,
                                    contentDescription = "搜索",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = onFolderBrowserClick) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    contentDescription = "打开文件夹",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = onSettingsClick) {
                                Icon(
                                    Icons.Outlined.Settings,
                                    contentDescription = "设置",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = onCreateNote) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "新建",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("笔记") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("待办") }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> PhoneNoteListContent(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                uiState = uiState,
                recentFiles = recentFiles,
                isInitialized = isInitialized,
                onToggleStarredFilter = onToggleStarredFilter,
                onNoteClick = onNoteClick,
                onDeleteNote = onDeleteNote,
                onToggleStar = onToggleStar,
                onTogglePin = onTogglePin,
                onRename = onRename,
                onExternalFileClick = onExternalFileClick,
                onRemoveRecentFile = onRemoveRecentFile,
                onTrashClick = onTrashClick
            )
            1 -> Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                TodoScreen()
            }
        }
    }
}
@Composable
private fun PhoneNoteListContent(
    modifier: Modifier = Modifier,
    uiState: NoteListUiState,
    recentFiles: List<RecentExternalFile>,
    isInitialized: Boolean,
    onToggleStarredFilter: () -> Unit,
    onNoteClick: (Long) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onToggleStar: (Long) -> Unit,
    onTogglePin: (Long) -> Unit,
    onRename: (Note) -> Unit,
    onExternalFileClick: (Uri, Uri) -> Unit,
    onRemoveRecentFile: (Uri) -> Unit,
    onTrashClick: () -> Unit
) {
    Column(modifier = modifier) {
        // 收藏过滤
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = uiState.showStarredOnly,
                onClick = onToggleStarredFilter,
                label = {
                    Text("收藏", style = MaterialTheme.typography.labelSmall)
                },
                leadingIcon = {
                    Icon(
                        if (uiState.showStarredOnly) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                },
                modifier = Modifier.height(28.dp)
            )
        }

        // 笔记列表
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // 空列表占位
                if (uiState.notes.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Description,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            Text(
                                "暂无笔记",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    items(items = uiState.notes, key = { it.id }) { note ->
                        NoteItem(
                            note = note,
                            onClick = { onNoteClick(note.id) },
                            onDelete = { onDeleteNote(note) },
                            onToggleStar = { onToggleStar(note.id) },
                            onTogglePin = { onTogglePin(note.id) },
                            onRename = { onRename(note) }
                        )
                    }
                }

                // 最近打开的外部文件
                if (isInitialized && recentFiles.isNotEmpty()) {
                    item {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                    item {
                        Text(
                            text = "最近文件",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    items(recentFiles.size, key = { recentFiles[it].fileUri.toString() }) { index ->
                        val file = recentFiles[index]
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onExternalFileClick(file.fileUri, file.folderUri) }
                                    .padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = file.fileName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onRemoveRecentFile(file.fileUri) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "移除",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }

                // 底部留白
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // 回收站入口
        if (uiState.deletedNotesCount > 0) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTrashClick() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "回收站",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        "${uiState.deletedNotesCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
