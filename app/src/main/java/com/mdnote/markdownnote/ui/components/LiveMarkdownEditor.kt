package com.mdnote.markdownnote.ui.components

import android.content.Context
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import com.mdnote.markdownnote.BuildConfig
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.Coil
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mdnote.markdownnote.data.repository.FolderRepository
import com.mdnote.markdownnote.ui.theme.*
import com.mdnote.markdownnote.util.ImageManager

private const val TAG = "LiveMarkdownEditor"

// 复用逐行渲染使用的固定正则，避免输入和重组时重复编译
private val HEADING_LINE_PATTERN = Regex("^(#{1,6})\\s+(.+)$")
private val IMAGE_LINE_PATTERN = Regex("""^!\[([^\]]*)\]\(([^)]+)\)""")
private val UNORDERED_LIST_LINE_PATTERN = Regex("^[-*+]\\s+(.*)$")
private val ORDERED_LIST_LINE_PATTERN = Regex("^(\\d+)\\.\\s+(.*)$")
private val ORDERED_LIST_DETECT_PATTERN = Regex("^\\d+\\.\\s")

/**
 * 代码块信息
 */
private data class CodeBlockInfo(
    val startLine: Int,
    val endLine: Int,
    val language: String
)

/**
 * 图片预加载信息
 */
private data class ImageInfo(
    val uri: Uri,
    val width: Int,
    val height: Int
)

/**
 * 渲染项类型
 */
private sealed class RenderItem {
    data class NormalLine(val lineIndex: Int, val line: String) : RenderItem()
    data class CodeBlock(val info: CodeBlockInfo, val code: String) : RenderItem()
}

/**
 * 编辑状态
 */
private sealed class EditState {
    data class NormalLine(
        val lineIndex: Int,
        val inputFocusRequest: InputFocusRequest = InputFocusRequest()
    ) : EditState()
    data class CodeBlock(val renderIndex: Int) : EditState()
    object None : EditState()
}

/** 编辑行的一次性输入焦点请求，消费后滚动重建不会再次弹出输入法。 */
internal data class InputFocusRequest(
    val initialCursorPos: Int = -1,
    val shouldRequestInputFocus: Boolean = false
) {
    // 消费请求时保留光标位置，只关闭一次性的输入法触发标记。
    fun consume(): InputFocusRequest = copy(shouldRequestInputFocus = false)
}

/**
 * 逐行实时渲染的 Markdown 编辑器。
 */
@Composable
fun LiveMarkdownEditor(
    content: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    enabled: Boolean = true,
    placeholder: String = "开始输入 Markdown...",
    typewriterMode: Boolean = false,
    focusMode: Boolean = false,
    scrollToLine: Int = -1,
    scrollTrigger: Int = 0,
    folderRepository: FolderRepository? = null,
    folderUri: Uri? = null,
    onSelectedTextChange: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.5f

    // 文本输入值
    var textFieldValue by remember(content) {
        mutableStateOf(TextFieldValue(content.replace("\t", "    ")))
    }

    // 同步外部内容变化
    val normalizedContent = content.replace("\t", "    ")
    if (normalizedContent != textFieldValue.text) {
        textFieldValue = TextFieldValue(normalizedContent, textFieldValue.selection)
    }

    // 解析 Markdown 为行
    val lines = remember(textFieldValue.text) {
        textFieldValue.text.lines()
    }

    // 解析代码块范围
    val codeBlocks = remember(lines) {
        parseCodeBlocks(lines)
    }

    // 构建渲染项列表（将代码块合并为单个项）
    val renderItems = remember(lines, codeBlocks) {
        buildRenderItems(lines, codeBlocks)
    }

    // ===== 预加载所有图片 =====
    // 存储图片信息: imageUrl -> ImageInfo
    val imageInfos = remember { mutableStateMapOf<String, ImageInfo>() }
    // 预加载是否完成
    var imagesPreloaded by remember { mutableStateOf(false) }

    // 提取所有图片 URL
    val imageUrls = remember(renderItems) {
        renderItems.mapNotNull { item ->
            when (item) {
                is RenderItem.NormalLine -> {
                    val match = IMAGE_LINE_PATTERN.find(item.line.trim())
                    match?.groupValues?.get(2)?.trim()
                }
                else -> null
            }
        }.distinct()
    }

    // 预加载所有图片
    LaunchedEffect(imageUrls, folderRepository, folderUri) {
        if (imageUrls.isEmpty()) {
            imagesPreloaded = true
            return@LaunchedEffect
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Preloading ${imageUrls.size} images...")
        }
        val imageLoader = Coil.imageLoader(context)

        imageUrls.forEach { imageUrl ->
            // 解析图片 URI
            val uri: Uri? = when {
                imageUrl.startsWith("http://") || imageUrl.startsWith("https://") -> Uri.parse(imageUrl)
                imageUrl.startsWith("content://") -> Uri.parse(imageUrl)
                imageUrl.startsWith("file://") -> Uri.parse(imageUrl)
                imageUrl.startsWith("/") || (imageUrl.length >= 2 && imageUrl[1] == ':') -> {
                    val file = java.io.File(imageUrl)
                    if (file.exists()) Uri.fromFile(file) else null
                }
                ImageManager.isInternalImagePath(imageUrl) -> {
                    val file = ImageManager.loadImageFile(context, imageUrl)
                    file?.let { Uri.fromFile(it) }
                }
                folderRepository != null && folderUri != null -> {
                    folderRepository.resolveImageUri(folderUri, imageUrl)
                }
                else -> null
            }

            if (uri != null) {
                val request = ImageRequest.Builder(context)
                    .data(uri)
                    .build()
                val result = imageLoader.execute(request)
                if (result is coil.request.SuccessResult) {
                    val drawable = result.drawable
                    imageInfos[imageUrl] = ImageInfo(uri, drawable.intrinsicWidth, drawable.intrinsicHeight)
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Preloaded image: $imageUrl, size=${drawable.intrinsicWidth}x${drawable.intrinsicHeight}")
                    }
                }
            }
        }

        imagesPreloaded = true
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Image preloading complete, loaded ${imageInfos.size} images")
        }
    }

    // 当前编辑状态
    var editState by remember { mutableStateOf<EditState>(EditState.None) }

    // 滚动状态
    val lazyListState = rememberLazyListState()
    val density = LocalDensity.current

    // 编辑器高度（用于打字机模式居中计算）
    var editorHeightPx by remember { mutableIntStateOf(0) }

    // 打字机模式滚动触发器：将滚动逻辑与 editState 解耦
    // 通过自增的 trigger 值来触发滚动，确保在布局更新后执行
    var typewriterScrollTrigger by remember { mutableIntStateOf(0) }

    // 当 editState 变化时，延迟触发滚动（等待布局重组完成）
    LaunchedEffect(editState, typewriterMode) {
        if (!typewriterMode) return@LaunchedEffect
        // 先等待足够多的帧，让 LazyColumn 完成重组
        // 不立即触发滚动，而是等待下一个 effect 执行
        repeat(5) {
            withFrameNanos { }
        }
        // 触发实际的滚动
        typewriterScrollTrigger++
    }

    // 实际执行打字机模式滚动的逻辑
    LaunchedEffect(typewriterScrollTrigger, typewriterMode) {
        if (!typewriterMode || typewriterScrollTrigger == 0) return@LaunchedEffect
        val currentEditState = editState
        if (currentEditState is EditState.None) return@LaunchedEffect

        // 确定目标 renderIndex
        val targetRenderIndex = when (currentEditState) {
            is EditState.NormalLine -> findRenderIndexForLine(renderItems, currentEditState.lineIndex)
            is EditState.CodeBlock -> currentEditState.renderIndex
            else -> return@LaunchedEffect
        }

        // typewriterMode 时第一个 item 是 Spacer，偏移 +1
        val actualIndex = if (typewriterMode) targetRenderIndex + 1 else targetRenderIndex

        if (BuildConfig.DEBUG) Log.d(TAG, "Typewriter: editState=$currentEditState, targetRenderIndex=$targetRenderIndex, actualIndex=$actualIndex, renderItemsCount=${renderItems.size}")
        if (targetRenderIndex < renderItems.size) {
            val ri = renderItems[targetRenderIndex]
            val desc = when (ri) {
                is RenderItem.NormalLine -> "NormalLine(lineIndex=${ri.lineIndex}, text=${ri.line.take(30)})"
                is RenderItem.CodeBlock -> "CodeBlock(startLine=${ri.info.startLine}, endLine=${ri.info.endLine})"
            }
            if (BuildConfig.DEBUG) Log.d(TAG, "Typewriter: targetItem=$desc")
        }

        // ===== 等待整个视口布局稳定 =====
        // 算法：记录每帧所有可见 item 的 (index, offset, size) 快照
        // 当连续 3 帧快照完全相同时，认为布局稳定
        data class ItemSnapshot(val index: Int, val offset: Int, val size: Int)

        var prevSnapshot: List<ItemSnapshot>? = null
        var stableCount = 0
        var maxIterations = 30 // 最多等待 30 帧

        while (maxIterations-- > 0) {
            withFrameNanos { }

            // 获取当前所有可见 item 的快照
            val currentSnapshot = lazyListState.layoutInfo.visibleItemsInfo.map {
                ItemSnapshot(it.index, it.offset, it.size)
            }

            if (BuildConfig.DEBUG) Log.d(TAG, "Typewriter stability: items=${currentSnapshot.map { "${it.index}:${it.offset}:${it.size}" }}, stableCount=$stableCount")

            // 检查快照是否相同
            if (currentSnapshot == prevSnapshot) {
                stableCount++
                if (stableCount >= 3) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Typewriter: layout stabilized after ${30 - maxIterations} frames")
                    break
                }
            } else {
                stableCount = 0
            }
            prevSnapshot = currentSnapshot
        }

        if (maxIterations <= 0) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Typewriter: layout did not stabilize, proceeding anyway")
        }

        // 记录 scrollToItem 前的状态
        val preScrollInfo = lazyListState.layoutInfo.visibleItemsInfo.find { it.index == actualIndex }
        if (BuildConfig.DEBUG) Log.d(TAG, "Typewriter pre-scroll: offset=${preScrollInfo?.offset}, size=${preScrollInfo?.size}")

        // 先滚动到目标项，使其在视口顶部
        lazyListState.scrollToItem(actualIndex)
        withFrameNanos { }

        // 记录 scrollToItem 后的状态
        val layoutInfo = lazyListState.layoutInfo
        val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == actualIndex }
        if (BuildConfig.DEBUG) Log.d(TAG, "Typewriter post-scroll: offset=${itemInfo?.offset}, size=${itemInfo?.size}, visibleItems=${layoutInfo.visibleItemsInfo.map { "${it.index}:${it.size}" }}")

        if (itemInfo != null) {
            val viewportHeight = layoutInfo.viewportSize.height
            val itemSize = itemInfo.size
            val centerOffset = -(viewportHeight / 2 - itemSize / 2)
            if (BuildConfig.DEBUG) Log.d(TAG, "Typewriter scroll: viewportHeight=$viewportHeight, itemSize=$itemSize, centerOffset=$centerOffset")
            lazyListState.scrollToItem(actualIndex, scrollOffset = centerOffset)
        }
    }

    // 外部滚动请求（大纲跳转）
    LaunchedEffect(scrollTrigger, scrollToLine) {
        if (scrollToLine >= 0 && scrollToLine < lines.size) {
            val renderIndex = findRenderIndexForLine(renderItems, scrollToLine)
            // 当 typewriterMode=true 时，第一个 item 是 Spacer，需要偏移 +1
            val actualIndex = if (typewriterMode) renderIndex + 1 else renderIndex
            lazyListState.animateScrollToItem(actualIndex, scrollOffset = -50)
        }
    }

    // 编辑器容器
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDarkTheme) DarkSurface else LightBackground)
            .onSizeChanged { size ->
                editorHeightPx = size.height
            }
    ) {
        if (lines.isEmpty() || (lines.size == 1 && lines[0].isEmpty())) {
            // 空状态 - 直接显示可编辑的 TextField
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                var emptyText by remember { mutableStateOf("") }
                val focusRequester = remember { FocusRequester() }

                BasicTextField(
                    value = emptyText,
                    onValueChange = { newValue ->
                        emptyText = newValue
                        onContentChange(newValue)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester),
                    enabled = enabled,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = fontSize,
                        color = if (isDarkTheme) DarkOnSurface else LightOnSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        if (emptyText.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }
                )

                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(100)
                    focusRequester.requestFocus()
                }
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // 打字机模式顶部填充 - 只在有编辑状态时显示
                if (typewriterMode && editState !is EditState.None) {
                    item {
                        Spacer(modifier = Modifier.height(150.dp))
                    }
                }

                // 直接按下标消费原列表，避免每次重组创建 IndexedValue 临时列表
                itemsIndexed(
                    items = renderItems,
                    key = { index, _ -> "item_$index" }
                ) { renderIndex, renderItem ->
                    when (renderItem) {
                        is RenderItem.NormalLine -> {
                            val lineIndex = renderItem.lineIndex
                            val line = renderItem.line
                            val currentEditState = editState
                            val editingLineState = currentEditState as? EditState.NormalLine
                            val isEditing = editingLineState?.lineIndex == lineIndex

                            // 聚焦模式透明度：当编辑任何内容时，其他行都淡化
                            val alpha = if (focusMode && currentEditState !is EditState.None && !isEditing) {
                                0.35f
                            } else {
                                1f
                            }

                            // 焦点请求属于编辑状态，滚动重建时沿用已消费的状态而不重复弹出输入法。
                            val inputFocusRequest = editingLineState?.inputFocusRequest ?: InputFocusRequest()

                            NormalLineRenderer(
                                line = line,
                                lineIndex = lineIndex,
                                isEditing = isEditing,
                                initialCursorPos = inputFocusRequest.initialCursorPos,
                                shouldRequestInputFocus = inputFocusRequest.shouldRequestInputFocus,
                                isDarkTheme = isDarkTheme,
                                fontSize = fontSize,
                                alpha = alpha,
                                enabled = enabled,
                                folderRepository = folderRepository,
                                folderUri = folderUri,
                                context = context,
                                imageInfos = imageInfos,
                                onLineClick = {
                                    if (BuildConfig.DEBUG) Log.d(TAG, "onLineClick: lineIndex=$lineIndex")
                                    // 用户点击预览行时才显式请求一次输入焦点。
                                    editState = EditState.NormalLine(
                                        lineIndex = lineIndex,
                                        inputFocusRequest = InputFocusRequest(shouldRequestInputFocus = true)
                                    )
                                },
                                onLineChange = { newLine ->
                                    val newLines = lines.toMutableList()
                                    newLines[lineIndex] = newLine
                                    val newContent = newLines.joinToString("\n")
                                    textFieldValue = TextFieldValue(newContent)
                                    onContentChange(newContent)
                                },
                                onEnterKey = { cursorPosition ->
                                    // 在光标位置分割内容
                                    val currentLine = lines[lineIndex]
                                    val textBeforeCursor = currentLine.substring(0, cursorPosition)
                                    val textAfterCursor = currentLine.substring(cursorPosition)

                                    val newLines = lines.toMutableList()
                                    newLines[lineIndex] = textBeforeCursor  // 当前行保留光标前内容
                                    newLines.add(lineIndex + 1, textAfterCursor)  // 新行放光标后内容
                                    val newContent = newLines.joinToString("\n")
                                    textFieldValue = TextFieldValue(newContent)
                                    onContentChange(newContent)
                                    // 移动到新行
                                    // 回车创建的新行需要一次输入焦点，后续滚动重建不重复请求。
                                    editState = EditState.NormalLine(
                                        lineIndex = lineIndex + 1,
                                        inputFocusRequest = InputFocusRequest(shouldRequestInputFocus = true)
                                    )
                                },
                                onBackspaceAtStart = { currentLineText ->
                                    // 在行首按删除键，合并到上一行
                                    if (lineIndex > 0) {
                                        val newLines = lines.toMutableList()
                                        val prevLine = newLines[lineIndex - 1]
                                        // 保存上一行的长度，作为合并后的光标位置
                                        val prevLineLength = prevLine.length
                                        if (BuildConfig.DEBUG) Log.d(TAG, "Merge lines: lineIndex=$lineIndex, prevLine='$prevLine', prevLineLength=$prevLineLength, currentLineText='$currentLineText'")
                                        // 合并上一行和当前行
                                        newLines[lineIndex - 1] = prevLine + currentLineText
                                        newLines.removeAt(lineIndex)
                                        val newContent = newLines.joinToString("\n")
                                        textFieldValue = TextFieldValue(newContent)
                                        onContentChange(newContent)
                                        // 移动到上一行，光标在原上一行末尾
                                        val newEditState = EditState.NormalLine(
                                            lineIndex = lineIndex - 1,
                                            inputFocusRequest = InputFocusRequest(
                                                initialCursorPos = prevLineLength,
                                                shouldRequestInputFocus = true
                                            )
                                        )
                                        if (BuildConfig.DEBUG) Log.d(TAG, "Setting editState to: $newEditState")
                                        editState = newEditState
                                    }
                                },
                                onInputFocusRequestConsumed = {
                                    val latestEditState = editState
                                    if (latestEditState is EditState.NormalLine &&
                                        latestEditState.lineIndex == lineIndex &&
                                        latestEditState.inputFocusRequest.shouldRequestInputFocus
                                    ) {
                                        // 父级消费焦点请求，避免 LazyColumn 重建行时再次触发输入法。
                                        editState = latestEditState.copy(
                                            inputFocusRequest = latestEditState.inputFocusRequest.consume()
                                        )
                                    }
                                },
                                onSelectionChange = onSelectedTextChange
                            )
                        }

                        is RenderItem.CodeBlock -> {
                            // 代码块：支持点击编辑
                            val currentEditState = editState
                            val isEditingCodeBlock = currentEditState is EditState.CodeBlock &&
                                    currentEditState.renderIndex == renderIndex

                            // 聚焦模式透明度：当编辑任何内容时，其他代码块都淡化
                            val codeBlockAlpha = if (focusMode && currentEditState !is EditState.None && !isEditingCodeBlock) {
                                0.35f
                            } else {
                                1f
                            }

                            // 统一的代码块容器，添加 animateContentSize 平滑高度变化
                            CodeBlockContainer(
                                codeBlockInfo = renderItem.info,
                                code = renderItem.code,
                                isEditing = isEditingCodeBlock,
                                isDarkTheme = isDarkTheme,
                                fontSize = fontSize,
                                alpha = codeBlockAlpha,
                                enabled = enabled,
                                onCodeChange = { newCode ->
                                    // 更新代码块内容
                                    val newLines = lines.toMutableList()
                                    val codeLines = newCode.lines()
                                    for ((idx, codeLine) in codeLines.withIndex()) {
                                        val targetIdx = renderItem.info.startLine + 1 + idx
                                        if (targetIdx < renderItem.info.endLine) {
                                            newLines[targetIdx] = codeLine
                                        }
                                    }
                                    val newContent = newLines.joinToString("\n")
                                    textFieldValue = TextFieldValue(newContent)
                                    onContentChange(newContent)
                                },
                                onClick = {
                                    if (BuildConfig.DEBUG) Log.d(TAG, "CodeBlockContainer onClick: renderIndex=$renderIndex")
                                    editState = EditState.CodeBlock(renderIndex)
                                },
                                onFocusChange = { isFocused ->
                                    if (BuildConfig.DEBUG) Log.d(TAG, "CodeBlockContainer onFocusChange: renderIndex=$renderIndex, isFocused=$isFocused")
                                    if (isFocused) {
                                        editState = EditState.CodeBlock(renderIndex)
                                    }
                                }
                            )
                        }
                    }
                }

                // 打字机模式底部填充
                if (typewriterMode) {
                    item {
                        Spacer(modifier = Modifier.height(150.dp))
                    }
                }

                // 底部空白区域
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clickable(enabled = enabled) {
                                // 点击底部空白区域清除编辑状态
                                editState = EditState.None
                                val newContent = textFieldValue.text + "\n"
                                textFieldValue = TextFieldValue(newContent)
                                onContentChange(newContent)
                            }
                    )
                }
            }
        }
    }
}

/**
 * 解析代码块范围
 */
private fun parseCodeBlocks(lines: List<String>): List<CodeBlockInfo> {
    val codeBlocks = mutableListOf<CodeBlockInfo>()
    var i = 0
    while (i < lines.size) {
        val line = lines[i].trim()
        if (line.startsWith("```") || line.startsWith("~~~")) {
            val delim = if (line.startsWith("```")) "```" else "~~~"
            val language = line.removePrefix(delim).trim()
            var endLine = i + 1
            while (endLine < lines.size && !lines[endLine].trim().startsWith(delim)) {
                endLine++
            }
            codeBlocks.add(CodeBlockInfo(startLine = i, endLine = endLine, language = language))
            i = endLine + 1
        } else {
            i++
        }
    }
    return codeBlocks
}

/**
 * 构建渲染项列表
 */
private fun buildRenderItems(lines: List<String>, codeBlocks: List<CodeBlockInfo>): List<RenderItem> {
    val items = mutableListOf<RenderItem>()
    val codeBlockLines = mutableSetOf<Int>()

    // 标记所有代码块内的行
    for (block in codeBlocks) {
        for (i in block.startLine..block.endLine) {
            codeBlockLines.add(i)
        }
    }

    var i = 0
    while (i < lines.size) {
        // 检查是否是代码块开始
        val codeBlock = codeBlocks.find { it.startLine == i }
        if (codeBlock != null) {
            // 提取代码内容（不包括 ``` 行）
            val codeLines = lines.subList(codeBlock.startLine + 1, codeBlock.endLine)
            val code = codeLines.joinToString("\n")
            items.add(RenderItem.CodeBlock(codeBlock, code))
            i = codeBlock.endLine + 1
        } else if (i !in codeBlockLines) {
            // 普通行
            items.add(RenderItem.NormalLine(i, lines[i]))
            i++
        } else {
            i++
        }
    }

    return items
}

/**
 * 根据行号找到渲染项索引
 */
private fun findRenderIndexForLine(renderItems: List<RenderItem>, lineIndex: Int): Int {
    var index = 0
    for ((i, item) in renderItems.withIndex()) {
        when (item) {
            is RenderItem.NormalLine -> {
                if (item.lineIndex == lineIndex) {
                    return i
                }
            }
            is RenderItem.CodeBlock -> {
                // 代码块内的行，跳到代码块
                if (lineIndex >= item.info.startLine && lineIndex <= item.info.endLine) {
                    return i
                }
            }
        }
    }
    return 0
}

/**
 * 普通行渲染器
 */
@Composable
private fun NormalLineRenderer(
    line: String,
    lineIndex: Int,
    isEditing: Boolean,
    initialCursorPos: Int = -1,
    shouldRequestInputFocus: Boolean,
    isDarkTheme: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit,
    alpha: Float,
    enabled: Boolean,
    folderRepository: FolderRepository?,
    folderUri: Uri?,
    context: Context,
    imageInfos: Map<String, ImageInfo>,
    onLineClick: () -> Unit,
    onLineChange: (String) -> Unit,
    onEnterKey: (cursorPosition: Int) -> Unit,
    onBackspaceAtStart: (currentLineText: String) -> Unit,
    onInputFocusRequestConsumed: () -> Unit,
    onSelectionChange: ((String) -> Unit)? = null
) {
    // 使用 Box + animateContentSize 平滑高度变化
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        // 编辑模式
        if (isEditing) {
            EditableLine(
                line = line,
                initialCursorPos = initialCursorPos,
                shouldRequestInputFocus = shouldRequestInputFocus,
                isDarkTheme = isDarkTheme,
                fontSize = fontSize,
                enabled = enabled,
                onLineChange = onLineChange,
                onEnterKey = onEnterKey,
                onBackspaceAtStart = onBackspaceAtStart,
                onInputFocusRequestConsumed = onInputFocusRequestConsumed,
                onSelectionChange = onSelectionChange
            )
        } else {
            // 预览模式
            PreviewLine(
                line = line,
                isDarkTheme = isDarkTheme,
                fontSize = fontSize,
                alpha = alpha,
                enabled = enabled,
                folderRepository = folderRepository,
                folderUri = folderUri,
                context = context,
                imageInfos = imageInfos,
                onClick = onLineClick
            )
        }
    }

}

/**
 * 可编辑行
 */
@Composable
private fun EditableLine(
    line: String,
    initialCursorPos: Int = -1,
    shouldRequestInputFocus: Boolean,
    isDarkTheme: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit,
    enabled: Boolean,
    onLineChange: (String) -> Unit,
    onEnterKey: (cursorPosition: Int) -> Unit,
    onBackspaceAtStart: (currentLineText: String) -> Unit,
    onInputFocusRequestConsumed: () -> Unit,
    onSelectionChange: ((String) -> Unit)? = null
) {
    val density = LocalDensity.current
    val textColor = if (isDarkTheme) DarkOnSurface else LightOnSurface
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val currentOnLineChange = rememberUpdatedState(onLineChange)
    val currentOnEnterKey = rememberUpdatedState(onEnterKey)
    val currentOnSelectionChange = rememberUpdatedState(onSelectionChange)
    val currentOnBackspaceAtStart = rememberUpdatedState(onBackspaceAtStart)
    val currentOnInputFocusRequestConsumed = rememberUpdatedState(onInputFocusRequestConsumed)
    val textToolbar = LocalTextToolbar.current
    val currentTextToolbar = rememberUpdatedState(textToolbar)
    val editorState = remember { NativeEditableLineState(line, initialCursorPos) }
    var nativeEditText by remember { mutableStateOf<PreciseSelectionEditText?>(null) }

    AndroidView(
        factory = { context ->
            PreciseSelectionEditText(context).apply {
                // 编辑行改由原生 EditText 独占输入、光标和选区状态。
                setTextSize(TypedValue.COMPLEX_UNIT_PX, with(density) { fontSize.toPx() })
                setTextColor(textColor.toArgb())
                setHintTextColor(hintColor.toArgb())
                hint = "输入内容..."
                setBackgroundColor(AndroidColor.TRANSPARENT)
                setPadding(0, 0, 0, 0)
                gravity = Gravity.CENTER_VERTICAL
                setSingleLine(false)
                setHorizontallyScrolling(false)
                isEnabled = enabled
                setText(line)
                val initialSelection = editorState.initialCursorPos.coerceIn(0, line.length)
                setSelection(initialSelection)
                editorState.lastText = line

                onExactSelectionCompleted = { request ->
                    if (request == null) {
                        // 点击开始新操作或得到空选区时隐藏上一次的 AI 工具栏。
                        currentTextToolbar.value.hide()
                        currentOnSelectionChange.value?.invoke("")
                    } else {
                        // 复用 Compose 既有工具栏，保留 AI、复制、粘贴等原有菜单能力。
                        currentOnSelectionChange.value?.invoke(
                            completedDragSelectionText(text.toString(), request.selection)
                        )
                        currentTextToolbar.value.showMenu(
                            rect = Rect(
                                left = request.bounds.left,
                                top = request.bounds.top,
                                right = request.bounds.right,
                                bottom = request.bounds.bottom
                            ),
                            onCopyRequested = { onTextContextMenuItem(android.R.id.copy) },
                            onPasteRequested = { onTextContextMenuItem(android.R.id.paste) },
                            onCutRequested = { onTextContextMenuItem(android.R.id.cut) },
                            onSelectAllRequested = { onTextContextMenuItem(android.R.id.selectAll) }
                        )
                    }
                }
                this.onBackspaceAtStart = { currentOnBackspaceAtStart.value.invoke(it) }
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        sourceText: CharSequence?,
                        startOffset: Int,
                        replacedCount: Int,
                        insertedCount: Int
                    ) {
                        // 只在 afterTextChanged 统一处理完整文本，避免中间状态同步。
                    }

                    override fun onTextChanged(
                        sourceText: CharSequence?,
                        startOffset: Int,
                        replacedCount: Int,
                        insertedCount: Int
                    ) {
                        // 只在 afterTextChanged 统一处理完整文本，避免中间状态同步。
                    }

                    override fun afterTextChanged(editable: Editable?) {
                        if (editorState.isProgrammaticUpdate) return
                        val newText = editable?.toString().orEmpty()
                        val oldText = editorState.lastText
                        if (newText == oldText) return

                        val newlineIndex = newText.indexOf('\n')
                        if (newlineIndex >= 0) {
                            // 换行只在原生控件内短暂出现，立即交给既有分行逻辑处理。
                            val textWithoutNewline = newText.replace("\n", "")
                            editorState.isProgrammaticUpdate = true
                            setText(textWithoutNewline)
                            setSelection(newlineIndex.coerceIn(0, textWithoutNewline.length))
                            editorState.isProgrammaticUpdate = false
                            editorState.lastText = textWithoutNewline
                            currentOnLineChange.value.invoke(textWithoutNewline)
                            currentOnEnterKey.value.invoke(newlineIndex.coerceIn(0, textWithoutNewline.length))
                        } else {
                            editorState.lastText = newText
                            currentOnLineChange.value.invoke(newText)
                        }
                    }
                })

                // 保存视图引用，由一次性焦点请求控制键盘，不在每次 factory 重建时自动抢焦点。
                nativeEditText = this
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        update = { editText ->
            editText.isEnabled = enabled
            editText.setTextColor(textColor.toArgb())
            editText.setHintTextColor(hintColor.toArgb())
            if (editText.text.toString() != line) {
                // 只在父级文本实际变化时回写，避免拖动选区期间重置原生光标。
                editorState.isProgrammaticUpdate = true
                val previousSelection = editText.selectionStart.coerceAtLeast(0)
                editText.setText(line)
                editText.setSelection(previousSelection.coerceIn(0, line.length))
                editorState.lastText = line
                editorState.isProgrammaticUpdate = false
            }
        }
    )

    LaunchedEffect(shouldRequestInputFocus, nativeEditText) {
        val editText = nativeEditText ?: return@LaunchedEffect
        if (!shouldRequestInputFocus) return@LaunchedEffect
        // 只对用户点击、回车或合并行产生的一次性请求显示光标与输入法。
        editText.requestFocus()
        editText.post {
            val inputMethodManager = editText.context.getSystemService(InputMethodManager::class.java)
            inputMethodManager?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }
        currentOnInputFocusRequestConsumed.value.invoke()
    }
}

/** 编辑行的非 Compose 状态，防止输入回调因重组而重置。 */
private class NativeEditableLineState(
    initialText: String,
    val initialCursorPos: Int
) {
    // 区分模型回写和用户输入，避免 TextWatcher 形成内容同步循环。
    var isProgrammaticUpdate = false
    var lastText = initialText
}

/**
 * 预览行
 */
@Composable
private fun PreviewLine(
    line: String,
    isDarkTheme: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit,
    alpha: Float,
    enabled: Boolean,
    folderRepository: FolderRepository?,
    folderUri: Uri?,
    context: Context,
    imageInfos: Map<String, ImageInfo>,
    onClick: () -> Unit
) {
    val modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 2.dp)
        .clickable(enabled = enabled) { onClick() }

    val lineType = remember(line) { detectLineType(line) }

    when (lineType) {
        LineType.HEADING -> {
            val match = HEADING_LINE_PATTERN.find(line)
            if (match != null) {
                val level = match.groupValues[1].length
                val text = match.groupValues[2]
                val headingFontSize = when (level) {
                    1 -> 32.sp
                    2 -> 28.sp
                    3 -> 24.sp
                    4 -> 20.sp
                    5 -> 18.sp
                    else -> 16.sp
                }
                val fontWeight = when (level) {
                    1 -> FontWeight.Black
                    2 -> FontWeight.Bold
                    3 -> FontWeight.SemiBold
                    else -> FontWeight.Medium
                }
                val annotatedText = parseInlineMarkdown(text, isDarkTheme, fontSize)
                Text(
                    text = annotatedText,
                    modifier = modifier,
                    fontSize = headingFontSize,
                    fontWeight = fontWeight,
                    color = (if (isDarkTheme) DarkMarkdownHeading else LightMarkdownHeading).copy(alpha = alpha)
                )
            } else {
                RenderNormalLine(line, modifier, isDarkTheme, fontSize, alpha)
            }
        }

        LineType.EMPTY -> {
            // 空行也要可点击，显示占位符
            Box(
                modifier = modifier
                    .height(24.dp)  // 给空行一个最小高度
            ) {
                Text(
                    text = " ",  // 空格，确保可点击
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize),
                    color = (if (isDarkTheme) DarkOnSurface else LightOnSurface).copy(alpha = alpha)
                )
            }
        }

        LineType.IMAGE -> {
            val match = IMAGE_LINE_PATTERN.find(line.trim())
            if (match != null) {
                val altText = match.groupValues[1]
                val imageUrl = match.groupValues[2]
                val imageInfo = imageInfos[imageUrl.trim()]
                RenderImageLine(
                    altText = altText,
                    imageInfo = imageInfo,
                    modifier = modifier,
                    isDarkTheme = isDarkTheme,
                    onClick = onClick
                )
            } else {
                RenderNormalLine(line, modifier, isDarkTheme, fontSize, alpha)
            }
        }

        LineType.QUOTE -> {
            val text = line.removePrefix(">").removePrefix(" ").trim()
            val annotatedText = parseInlineMarkdown(text, isDarkTheme, fontSize)
            Row(
                modifier = modifier
                    .background(
                        if (isDarkTheme) DarkSurfaceVariant.copy(alpha = 0.3f) else LightSurfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(start = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(IntrinsicSize.Max)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = annotatedText,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize),
                    color = (if (isDarkTheme) DarkOnSurface else LightOnSurface).copy(alpha = alpha)
                )
            }
        }

        LineType.LIST -> {
            val match = UNORDERED_LIST_LINE_PATTERN.find(line.trimStart())
            if (match != null) {
                val text = match.groupValues[1]
                val annotatedText = parseInlineMarkdown(text, isDarkTheme, fontSize)
                Row(modifier = modifier.padding(start = 16.dp)) {
                    Text(
                        text = "•  ",
                        color = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                    )
                    Text(
                        text = annotatedText,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize),
                        color = (if (isDarkTheme) DarkOnSurface else LightOnSurface).copy(alpha = alpha)
                    )
                }
            } else {
                RenderNormalLine(line, modifier, isDarkTheme, fontSize, alpha)
            }
        }

        LineType.ORDERED_LIST -> {
            val match = ORDERED_LIST_LINE_PATTERN.find(line.trimStart())
            if (match != null) {
                val number = match.groupValues[1]
                val text = match.groupValues[2]
                val annotatedText = parseInlineMarkdown(text, isDarkTheme, fontSize)
                Row(modifier = modifier.padding(start = 16.dp)) {
                    Text(
                        text = "$number. ",
                        color = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                    )
                    Text(
                        text = annotatedText,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize),
                        color = (if (isDarkTheme) DarkOnSurface else LightOnSurface).copy(alpha = alpha)
                    )
                }
            } else {
                RenderNormalLine(line, modifier, isDarkTheme, fontSize, alpha)
            }
        }

        LineType.HORIZONTAL_RULE -> {
            HorizontalDivider(
                modifier = modifier.padding(vertical = 8.dp),
                thickness = 1.dp,
                color = (if (isDarkTheme) DarkDividerColor else LightDividerColor).copy(alpha = alpha)
            )
        }

        else -> {
            RenderNormalLine(line, modifier, isDarkTheme, fontSize, alpha)
        }
    }
}

@Composable
private fun RenderNormalLine(
    line: String,
    modifier: Modifier,
    isDarkTheme: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit,
    alpha: Float
) {
    val annotatedText = parseInlineMarkdown(line, isDarkTheme, fontSize)
    Text(
        text = annotatedText,
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize),
        color = (if (isDarkTheme) DarkOnSurface else LightOnSurface).copy(alpha = alpha)
    )
}

@Composable
private fun RenderImageLine(
    altText: String,
    imageInfo: ImageInfo?,
    modifier: Modifier,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    // 计算图片显示高度（基于预加载的尺寸）
    val displayWidth = 328.dp
    val density = LocalDensity.current

    val hasValidInfo = imageInfo != null && imageInfo.width > 0 && imageInfo.height > 0

    val imageDisplayHeight = remember(hasValidInfo, imageInfo) {
        if (hasValidInfo) {
            val widthPx = with(density) { displayWidth.toPx() }
            val ratio = imageInfo.height.toFloat() / imageInfo.width.toFloat()
            with(density) { (widthPx * ratio).toDp() }
        } else {
            200.dp // 默认高度（未知尺寸时）
        }
    }

    // 使用固定高度的 Box
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(imageDisplayHeight)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDarkTheme) DarkSurfaceVariant else LightSurfaceVariant)
            .clickable(enabled = true) { onClick() }
    ) {
        if (imageInfo != null) {
            // 显示图片（使用 Coil 的内存缓存）
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageInfo.uri)
                    .build(),
                contentDescription = altText,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // 加载失败状态
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = "图片",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = altText.ifEmpty { "图片加载失败" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 解析行内 Markdown 语法（包括高亮 ==text==，支持嵌套）
 */
private fun parseInlineMarkdown(
    text: String,
    isDarkTheme: Boolean,
    @Suppress("UNUSED_PARAMETER") fontSize: androidx.compose.ui.unit.TextUnit
): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                // 高亮 ==text== （支持内部嵌套其他语法）
                i + 1 < text.length && text[i] == '=' && text[i + 1] == '=' -> {
                    val end = text.indexOf("==", i + 2)
                    if (end != -1) {
                        val innerText = text.substring(i + 2, end)
                        withStyle(SpanStyle(background = Color(0x44FFFF00))) {
                            // 递归解析高亮内部的内容
                            append(parseInlineMarkdown(innerText, isDarkTheme, fontSize))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // 粗体 **text** （支持内部嵌套）
                i + 1 < text.length && text[i] == '*' && text[i + 1] == '*' -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        val innerText = text.substring(i + 2, end)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            // 递归解析粗体内部的内容
                            append(parseInlineMarkdown(innerText, isDarkTheme, fontSize))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // 斜体 *text*
                text[i] == '*' && (i == 0 || text[i - 1] != '*') -> {
                    val end = text.indexOf('*', i + 1)
                    if (end != -1 && end > i + 1) {
                        val innerText = text.substring(i + 1, end)
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(parseInlineMarkdown(innerText, isDarkTheme, fontSize))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // 行内代码 `code`（不解析内部语法）
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end != -1) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = if (isDarkTheme) DarkMarkdownCodeBackground else LightMarkdownCodeBackground
                            )
                        ) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // 链接 [text](url)
                text[i] == '[' -> {
                    val textEnd = text.indexOf(']', i + 1)
                    if (textEnd != -1 && textEnd + 1 < text.length && text[textEnd + 1] == '(') {
                        val urlEnd = text.indexOf(')', textEnd + 2)
                        if (urlEnd != -1) {
                            val linkText = text.substring(i + 1, textEnd)
                            withStyle(SpanStyle(color = if (isDarkTheme) DarkMarkdownLink else LightMarkdownLink, textDecoration = TextDecoration.Underline)) {
                                append(linkText)
                            }
                            i = urlEnd + 1
                        } else {
                            append(text[i])
                            i++
                        }
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}

/**
 * 行类型检测
 */
private enum class LineType {
    HEADING,
    QUOTE,
    LIST,
    ORDERED_LIST,
    IMAGE,
    HORIZONTAL_RULE,
    EMPTY,
    NORMAL
}

private fun detectLineType(line: String): LineType {
    val trimmed = line.trim()
    return when {
        trimmed.isEmpty() -> LineType.EMPTY
        trimmed.startsWith("#") -> LineType.HEADING
        trimmed.startsWith(">") -> LineType.QUOTE
        trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ") -> LineType.LIST
        ORDERED_LIST_DETECT_PATTERN.containsMatchIn(trimmed) -> LineType.ORDERED_LIST
        trimmed.startsWith("![") -> LineType.IMAGE
        trimmed == "---" || trimmed == "***" || trimmed == "___" -> LineType.HORIZONTAL_RULE
        else -> LineType.NORMAL
    }
}

/**
 * 统一的代码块容器
 * 编辑模式和预览模式共享相同的外层结构，确保高度变化平滑
 */
@Composable
private fun CodeBlockContainer(
    codeBlockInfo: CodeBlockInfo,
    code: String,
    isEditing: Boolean,
    isDarkTheme: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit,
    alpha: Float,
    enabled: Boolean,
    onCodeChange: (String) -> Unit,
    onClick: () -> Unit,
    onFocusChange: (Boolean) -> Unit
) {
    val backgroundColor = if (isDarkTheme) DarkMarkdownCodeBackground else LightMarkdownCodeBackground
    val focusRequester = remember { FocusRequester() }

    // 编辑代码内容
    var editingCode by remember(code) { mutableStateOf(code) }

    // 是否已经获得过焦点
    var hasHadFocus by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .alpha(alpha)
            .animateContentSize()  // 平滑高度变化
            .then(
                if (!isEditing) {
                    Modifier.clickable(enabled = enabled) { onClick() }
                } else {
                    Modifier
                }
            )
            .padding(12.dp)
    ) {
        // 语言标签
        if (codeBlockInfo.language.isNotEmpty()) {
            Text(
                text = codeBlockInfo.language.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = if (isDarkTheme) DarkOnSurfaceVariant else LightOnSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (isEditing) {
            // 编辑模式：显示原始代码块内容
            BasicTextField(
                value = editingCode,
                onValueChange = { newValue ->
                    editingCode = newValue
                    onCodeChange(newValue)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            hasHadFocus = true
                            onFocusChange(true)
                        } else if (hasHadFocus) {
                            onFocusChange(false)
                        }
                    },
                enabled = enabled,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize,
                    color = if (isDarkTheme) DarkOnSurface else LightOnSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
            )
        } else {
            // 预览模式：显示渲染后的代码
            CodeBlockContent(
                code = code,
                isDarkTheme = isDarkTheme,
                fontSize = fontSize
            )
        }
    }

    // 自动聚焦
    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
        }
    }
}

/**
 * 代码块内容渲染（用于预览模式）
 */
@Composable
private fun CodeBlockContent(
    code: String,
    isDarkTheme: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit
) {
    val textColor = if (isDarkTheme) DarkOnSurface else LightOnSurface
    val codeStyle = MaterialTheme.typography.bodyMedium.copy(
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize,
        color = textColor
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        code.lines().forEachIndexed { index, line ->
            Row(modifier = Modifier.fillMaxWidth()) {
                // 行号
                Text(
                    text = "${index + 1}".padStart(3),
                    style = codeStyle,
                    color = (if (isDarkTheme) DarkOnSurfaceVariant else LightOnSurfaceVariant).copy(alpha = 0.6f),
                    modifier = Modifier.padding(end = 8.dp)
                )
                // 代码内容
                Text(
                    text = line,
                    style = codeStyle
                )
            }
        }
    }
}
