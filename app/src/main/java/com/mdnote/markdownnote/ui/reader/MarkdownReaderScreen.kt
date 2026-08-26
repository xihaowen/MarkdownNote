package com.mdnote.markdownnote.ui.reader

import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.PopupWindow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.hilt.navigation.compose.hiltViewModel
// 阅读页不可见时暂停业务 Flow 收集
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mdnote.markdownnote.data.repository.FolderRepository
import com.mdnote.markdownnote.data.repository.RecentExternalFile
import com.mdnote.markdownnote.util.toAiConfig
import com.mdnote.markdownnote.ui.ai.AiAction
import com.mdnote.markdownnote.ui.ai.AiActionType
import com.mdnote.markdownnote.ui.ai.AiFloatingToolbar
import com.mdnote.markdownnote.ui.ai.AiPanelScreen
import com.mdnote.markdownnote.ui.ai.AiPanelViewModel
import com.mdnote.markdownnote.ui.ai.PendingAction
import com.mdnote.markdownnote.ui.ai.SystemAction
import com.mdnote.markdownnote.ui.components.DocumentOutline
import com.mdnote.markdownnote.ui.components.LiveMarkdownEditor
import com.mdnote.markdownnote.ui.components.glass.GlassDialog
import com.mdnote.markdownnote.ui.components.glass.GlassDropdownMenu
import com.mdnote.markdownnote.ui.components.glass.glassBackground
import com.mdnote.markdownnote.ui.noteedit.NoteEditViewModel
import com.mdnote.markdownnote.ui.theme.AiPurple
import com.mdnote.markdownnote.ui.theme.LocalDocumentUri
import com.mdnote.markdownnote.ui.theme.LocalFolderUri
import com.mdnote.markdownnote.ui.theme.LocalFolderRepository
import com.mdnote.markdownnote.util.MarkdownParser

/** AI 浮动工具栏的可测试状态，关闭时必须同时清空选中文本。 */
internal data class FloatingToolbarState(
    val selectedText: String = "",
    val offset: IntOffset = IntOffset.Zero,
    val isVisible: Boolean = false
) {
    // 统一关闭状态，避免工具栏不可见但旧选区文本残留。
    fun dismiss(): FloatingToolbarState = copy(selectedText = "", isVisible = false)
}

/**
 * Markdown 编辑器屏幕
 * 阅读与逐行实时编辑的统一入口。
 * 集成 AI 助手：工具栏按钮、浮动工具栏、侧边对话面板
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownReaderScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAiConfig: () -> Unit = {},
    viewModel: NoteEditViewModel = hiltViewModel(),
    aiViewModel: AiPanelViewModel = hiltViewModel(),
    externalFileUri: Uri? = null,
    folderUri: Uri? = null,
    folderRepository: FolderRepository
) {
    // 阅读与编辑状态按宿主生命周期收集
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // AI 面板状态
    var isAiPanelOpen by remember { mutableStateOf(false) }
    var showAiMenu by remember { mutableStateOf(false) }
    var showAiEnableDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // 浮动工具栏的可见性、选区和位置使用同一状态，保证可原子关闭。
    var floatingToolbarState by remember { mutableStateOf(FloatingToolbarState()) }
    var onCopyRef by remember { mutableStateOf<(() -> Unit)?>(null) }
    var onCutRef by remember { mutableStateOf<(() -> Unit)?>(null) }
    var onPasteRef by remember { mutableStateOf<(() -> Unit)?>(null) }
    var onSelectAllRef by remember { mutableStateOf<(() -> Unit)?>(null) }

    val density = LocalDensity.current

    // 自定义 TextToolbar：AI 启用时拦截系统菜单，显示 AI 浮动工具栏
    val defaultTextToolbar = LocalTextToolbar.current
    val aiTextToolbar = remember(defaultTextToolbar, settings.aiEnabled) {
        if (settings.aiEnabled) {
            object : TextToolbar {
                override val status: TextToolbarStatus
                    get() = defaultTextToolbar.status

                override fun showMenu(
                    rect: Rect,
                    onCopyRequested: (() -> Unit)?,
                    onPasteRequested: (() -> Unit)?,
                    onCutRequested: (() -> Unit)?,
                    onSelectAllRequested: (() -> Unit)?
                ) {
                    onCopyRef = onCopyRequested
                    onCutRef = onCutRequested
                    onPasteRef = onPasteRequested
                    onSelectAllRef = onSelectAllRequested
                    // 位置：选区上方居中
                    val toolbarOffset = with(density) {
                        IntOffset(
                            x = ((rect.left + (rect.right - rect.left) / 2).toInt() - 150.dp.roundToPx()).coerceAtLeast(0),
                            y = (rect.top.toInt() - 48.dp.roundToPx()).coerceAtLeast(0)
                        )
                    }
                    // 显示时保留已有选区文本，只更新位置和可见状态。
                    floatingToolbarState = floatingToolbarState.copy(
                        offset = toolbarOffset,
                        isVisible = true
                    )
                }

                override fun hide() {
                    // 所有关闭入口统一清空选区，防止 Popup 在下一次重组时恢复。
                    floatingToolbarState = floatingToolbarState.dismiss()
                }
            }
        } else {
            defaultTextToolbar
        }
    }
    val dismissFloatingToolbar = {
        // 点击或滚动时同时关闭 Compose 文本菜单与 AI Popup。
        aiTextToolbar.hide()
        floatingToolbarState = floatingToolbarState.dismiss()
    }
    val currentDismissFloatingToolbar = rememberUpdatedState(dismissFloatingToolbar)

    // 提供文档 URI 和文件夹 URI 给子组件使用（用于解析相对图片路径）
    val actualFolderUri = uiState.externalFolderUri ?: folderUri
    val actualFileUri = uiState.externalFileUri ?: externalFileUri

    CompositionLocalProvider(
        LocalDocumentUri provides actualFileUri,
        LocalFolderUri provides actualFolderUri,
        LocalFolderRepository provides folderRepository,
        LocalTextToolbar provides aiTextToolbar
    ) {
        // 如果从文件夹浏览器或文件管理器打开外部文件，加载文件内容
        LaunchedEffect(externalFileUri) {
            if (externalFileUri != null) {
                val needReload = uiState.content.isEmpty() ||
                    uiState.externalFileUri != externalFileUri

                if (needReload) {
                    val content = folderRepository.readFileContent(externalFileUri)
                    if (content != null) {
                        val fileName = folderRepository.getFileName(externalFileUri)
                        val effectiveFolderUri = folderUri
                            ?: folderRepository.getFolderForFile(externalFileUri)

                        viewModel.loadExternalFile(fileName, content, externalFileUri, effectiveFolderUri)

                        if (effectiveFolderUri != null) {
                            folderRepository.addRecentFile(RecentExternalFile(
                                fileUri = externalFileUri,
                                folderUri = effectiveFolderUri,
                                fileName = fileName
                            ))
                        }
                    }
                }
            }
        }

        // 提取大纲
        val outline = remember(uiState.content) {
            MarkdownParser.extractOutline(uiState.content)
        }

        // 是否显示大纲
        var showOutline by remember { mutableStateOf(true) }

        // 大纲跳转目标行
        var scrollToLine by remember { mutableIntStateOf(-1) }
        var scrollTrigger by remember { mutableIntStateOf(0) }

        Scaffold(
            modifier = Modifier.pointerInput(Unit) {
                awaitEachGesture {
                    // 仅观察首次按下，不消费事件；空白点击、按钮点击和用户滚动都会关闭旧工具栏。
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    currentDismissFloatingToolbar.value.invoke()
                }
            },
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        modifier = Modifier.glassBackground(cornerRadius = 14.dp),
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        actionContentColor = MaterialTheme.colorScheme.primary,
                        action = data.visuals.actionLabel?.let { label ->
                            {
                                TextButton(onClick = { data.performAction() }) {
                                    Text(label)
                                }
                            }
                        }
                    ) {
                        Text(data.visuals.message)
                    }
                }
            },
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = uiState.title.ifEmpty { "未命名文档" },
                                maxLines = 1
                            )
                            if (!uiState.isSaved) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "•",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 20.sp
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.saveNote()
                            onNavigateBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        if (canUndo) {
                            IconButton(onClick = { viewModel.undo() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "撤销"
                                )
                            }
                        }
                        // AI 助手按钮
                        Box {
                            TextButton(
                                onClick = {
                                    if (!settings.aiEnabled || !settings.toAiConfig().isValid) {
                                        showAiEnableDialog = true
                                    } else {
                                        showAiMenu = true
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI 助手",
                                    tint = if (settings.aiEnabled) AiPurple else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Text(
                                    text = "AI",
                                    color = if (settings.aiEnabled) AiPurple else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }

                            // AI 下拉菜单
                            GlassDropdownMenu(
                                expanded = showAiMenu,
                                onDismissRequest = { showAiMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("扫描分析") },
                                    onClick = {
                                        showAiMenu = false
                                        isAiPanelOpen = true
                                        aiViewModel.sendScan(uiState.content)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("AI 修改") },
                                    onClick = {
                                        showAiMenu = false
                                        isAiPanelOpen = true
                                        // AI 修改需要指令，先打开面板进入聊天模式
                                        aiViewModel.sendModify(uiState.content, "请修改此内容", null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("生成内容") },
                                    onClick = {
                                        showAiMenu = false
                                        isAiPanelOpen = true
                                        aiViewModel.sendGenerate("", uiState.content)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("AI 对话") },
                                    onClick = {
                                        showAiMenu = false
                                        isAiPanelOpen = true
                                    }
                                )
                            }
                        }

                        // 大纲切换
                        IconButton(onClick = { showOutline = !showOutline }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = if (showOutline) "隐藏大纲" else "显示大纲",
                                tint = if (showOutline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 收藏
                        IconButton(onClick = { viewModel.toggleStar() }) {
                            Icon(
                                imageVector = if (uiState.note?.isStarred == true) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = if (uiState.note?.isStarred == true) "取消收藏" else "收藏",
                                tint = if (uiState.note?.isStarred == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 分享
                        IconButton(onClick = {
                            val shareIntent = viewModel.createShareIntent(context)
                            if (shareIntent != null) {
                                context.startActivity(Intent.createChooser(shareIntent, "分享文档"))
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "分享")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 左侧大纲
                if (showOutline && outline.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .width(240.dp)
                            .fillMaxHeight(),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        tonalElevation = 1.dp
                    ) {
                        DocumentOutline(
                            outline = outline,
                            currentLine = 0,
                            onItemClick = { item ->
                                // 大纲点击跳转到对应行
                                scrollToLine = item.lineIndex
                                scrollTrigger++
                            },
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
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
                }

                // 中间编辑器区域
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    LiveMarkdownEditor(
                        content = uiState.content,
                        onContentChange = { newContent ->
                            viewModel.updateContent(newContent)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        fontSize = settings.editorFontSize.size.sp,
                        enabled = true,
                        placeholder = "开始输入 Markdown...",
                        folderRepository = folderRepository,
                        folderUri = actualFolderUri,
                        typewriterMode = settings.typewriterMode,
                        focusMode = settings.focusMode,
                        scrollToLine = scrollToLine,
                        scrollTrigger = scrollTrigger,
                        onSelectedTextChange = { text ->
                            if (text.isNotBlank()) {
                                // 选区改变时只更新文本，showMenu 负责更新可见性和位置。
                                floatingToolbarState = floatingToolbarState.copy(selectedText = text)
                            } else {
                                floatingToolbarState = floatingToolbarState.dismiss()
                            }
                        }
                    )

                    // 浮动 AI 工具栏（用 Popup 在选区位置弹出）
                    if (floatingToolbarState.isVisible &&
                        floatingToolbarState.selectedText.isNotBlank() &&
                        settings.aiEnabled
                    ) {
                        Popup(
                            alignment = Alignment.TopStart,
                            offset = floatingToolbarState.offset
                        ) {
                            AiFloatingToolbar(
                                selectedText = floatingToolbarState.selectedText,
                                onAction = { action, text ->
                                    if (!settings.toAiConfig().isValid) {
                                        showAiEnableDialog = true
                                    } else {
                                        floatingToolbarState = floatingToolbarState.dismiss()
                                        isAiPanelOpen = true
                                        handleAiFloatingAction(
                                            action = action,
                                            selectedText = text,
                                            fullContent = uiState.content,
                                            aiViewModel = aiViewModel
                                        )
                                    }
                                },
                                onSystemAction = { action ->
                                    when (action) {
                                        SystemAction.COPY -> onCopyRef?.invoke()
                                        SystemAction.CUT -> onCutRef?.invoke()
                                        SystemAction.PASTE -> onPasteRef?.invoke()
                                        SystemAction.SELECT_ALL -> onSelectAllRef?.invoke()
                                    }
                                    floatingToolbarState = floatingToolbarState.dismiss()
                                }
                            )
                        }
                    }
                }

                // 右侧 AI 面板（约 40% 宽度）
                AnimatedVisibility(
                    visible = isAiPanelOpen,
                    enter = slideInHorizontally(initialOffsetX = { it }),
                    exit = slideOutHorizontally(targetOffsetX = { it })
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(320.dp)
                    ) {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .align(Alignment.CenterStart)
                        )
                        AiPanelScreen(
                            viewModel = aiViewModel,
                            onClose = {
                                isAiPanelOpen = false
                                aiViewModel.clearPanel()
                            },
                            onApplyResult = { result, pendingAction ->
                                // 将 AI 结果应用到编辑器
                                handleApplyResult(
                                    result = result,
                                    pendingAction = pendingAction,
                                    currentContent = uiState.content,
                                    onContentChange = { viewModel.updateContent(it) }
                                )
                            }
                        )
                    }
                }
            }
        }
        if (showAiEnableDialog) {
            GlassDialog(
                onDismissRequest = { showAiEnableDialog = false },
                title = { Text("AI 助手尚未配置") },
                text = { Text("需要先选择服务商并填入密钥才能使用 AI 功能。是否现在配置？") },
                confirmButton = {
                    TextButton(onClick = {
                        showAiEnableDialog = false
                        onNavigateToAiConfig()
                    }) {
                        Text("去配置", color = AiPurple)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAiEnableDialog = false }) {
                        Text("稍后")
                    }
                }
            )
        }
    }
}

/**
 * 处理浮动工具栏的 AI 操作
 * 将 AiAction 映射到 AiPanelViewModel 的对应方法
 */
private fun handleAiFloatingAction(
    action: AiAction,
    selectedText: String,
    fullContent: String,
    aiViewModel: AiPanelViewModel
) {
    when (action) {
        AiAction.EXPLAIN -> aiViewModel.sendExplain(selectedText, fullContent)
        AiAction.MODIFY -> aiViewModel.sendModify(fullContent, "修改选中文本", selectedText)
        AiAction.TRANSLATE -> aiViewModel.sendModify(fullContent, "翻译选中文本", selectedText)
        AiAction.EXPAND -> aiViewModel.sendModify(fullContent, "扩写选中文本", selectedText)
        AiAction.CONDENSE -> aiViewModel.sendModify(fullContent, "缩写选中文本", selectedText)
    }
}

/**
 * 处理 AI 结果应用到编辑器
 * 根据 PendingAction 类型决定如何修改内容
 */
private fun handleApplyResult(
    result: String,
    pendingAction: PendingAction?,
    currentContent: String,
    onContentChange: (String) -> Unit
) {
    if (pendingAction == null) return

    when (pendingAction.type) {
        AiActionType.SCAN -> {
            // 扫描分析结果追加到文档末尾
            onContentChange(currentContent + "\n\n---\n\n**AI 分析**\n\n$result")
        }
        AiActionType.MODIFY -> {
            // 如果有原始选中文本，替换原文中的选中部分
            val original = pendingAction.originalContent
            if (original != null && original.isNotBlank()) {
                val newContent = currentContent.replace(original, result)
                onContentChange(newContent)
            } else {
                // 无选中文本，追加到末尾
                onContentChange(currentContent + "\n\n$result")
            }
        }
        AiActionType.GENERATE -> {
            // 生成内容追加到编辑器末尾
            onContentChange(currentContent + "\n\n$result")
        }
        AiActionType.EXPLAIN -> {
            // 如果有原始选中文本，替换选中部分为解释结果
            val original = pendingAction.originalContent
            if (original != null && original.isNotBlank()) {
                val newContent = currentContent.replace(original, result)
                onContentChange(newContent)
            } else {
                onContentChange(currentContent + "\n\n---\n\n**AI 解释**\n\n$result")
            }
        }
        AiActionType.CHAT -> {
            // 聊天结果追加到编辑器末尾
            onContentChange(currentContent + "\n\n$result")
        }
    }
}
