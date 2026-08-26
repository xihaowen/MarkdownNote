package com.mdnote.markdownnote.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mdnote.markdownnote.ui.theme.LocalDocumentUri
import com.mdnote.markdownnote.ui.theme.LocalFolderUri
import com.mdnote.markdownnote.ui.theme.LocalFolderRepository
import com.mdnote.markdownnote.ui.theme.*
import com.mdnote.markdownnote.data.repository.FolderRepository
import com.mdnote.markdownnote.util.CodeSyntaxHighlighter
import com.mdnote.markdownnote.util.ImageManager
import com.mdnote.markdownnote.util.MarkdownElement
import com.mdnote.markdownnote.util.MarkdownParser
import com.mdnote.markdownnote.util.Span
import com.mdnote.markdownnote.util.TableAlignment
import com.mdnote.markdownnote.markwon.MarkwonComposeRenderer

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState

@Composable
fun MarkdownRenderer(
    markdown: String,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    useMarkwon: Boolean = true
) {
    if (useMarkwon) {
        // 使用 Markwon 引擎渲染
        val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.5f
        MarkwonComposeRenderer(
            markdown = markdown,
            modifier = modifier,
            isDarkTheme = isDarkTheme,
            fontSize = fontSize.value
        )
        return
    }

    val elements = remember(markdown) { MarkdownParser.parse(markdown) }
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(
            items = elements,
            key = { index, _ -> index }
        ) { _, element ->
            RenderElement(
                element = element,
                fontSize = fontSize,
                onLinkClick = { url ->
                    if (ImageManager.isInternalImagePath(url)) {
                        // 内部图片
                    } else {
                        try {
                            uriHandler.openUri(url)
                        } catch (e: Exception) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e: Exception) { }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun RenderElement(
    element: MarkdownElement,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    onLinkClick: (String) -> Unit
) {
    when (element) {
        is MarkdownElement.YamlFrontMatter -> RenderYamlFrontMatter(element)
        is MarkdownElement.Heading -> RenderHeading(element)
        is MarkdownElement.Paragraph -> RenderParagraph(element, fontSize, onLinkClick)
        is MarkdownElement.UnorderedList -> RenderUnorderedList(element, fontSize, onLinkClick)
        is MarkdownElement.OrderedList -> RenderOrderedList(element, fontSize, onLinkClick)
        is MarkdownElement.TaskList -> RenderTaskList(element, fontSize, onLinkClick)
        is MarkdownElement.CodeBlock -> RenderCodeBlock(element)
        is MarkdownElement.MermaidBlock -> RenderMermaidBlock(element)
        is MarkdownElement.BlockQuote -> RenderBlockQuote(element, fontSize, onLinkClick)
        is MarkdownElement.HorizontalRule -> RenderHorizontalRule()
        is MarkdownElement.Image -> RenderImage(element)
        is MarkdownElement.Link -> RenderLink(element, onLinkClick)
        is MarkdownElement.Table -> RenderTable(element)
        is MarkdownElement.MathBlock -> RenderMathBlock(element)
    }
}

/**
 * 渲染 YAML Front Matter
 */
@Composable
private fun RenderYamlFrontMatter(yaml: MarkdownElement.YamlFrontMatter) {
    val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.5f
    val bgColor = if (isDarkTheme) MarkdownCodeBackgroundDark else MarkdownCodeBackground

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "YAML Front Matter",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = yaml.content,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RenderHeading(heading: MarkdownElement.Heading) {
    val fontSize = when (heading.level) {
        1 -> 32.sp; 2 -> 28.sp; 3 -> 24.sp; 4 -> 20.sp; 5 -> 18.sp; else -> 16.sp
    }
    val fontWeight = when (heading.level) {
        1 -> FontWeight.Black; 2 -> FontWeight.Bold; 3 -> FontWeight.SemiBold; else -> FontWeight.Medium
    }
    val linkColor = MarkdownLink
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    // 如果有解析的 spans，使用 AnnotatedString 渲染
    if (heading.spans.isNotEmpty()) {
        val annotatedString = buildAnnotatedString {
            fun renderSpans(spanList: List<Span>) {
                spanList.forEach { span ->
                    when (span) {
                        is Span.PlainText -> append(span.text)
                        is Span.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            renderSpans(span.spans)
                        }
                        is Span.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            renderSpans(span.spans)
                        }
                        is Span.BoldItalic -> withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                            renderSpans(span.spans)
                        }
                        is Span.Code -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = androidx.compose.ui.graphics.Color(0x33888888))) {
                            append(span.text)
                        }
                        is Span.Link -> {
                            pushStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
                            append(span.text)
                            pop()
                        }
                        is Span.Math -> withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = tertiaryColor)) {
                            append("\$")
                            append(span.latex)
                            append("\$")
                        }
                        is Span.Highlight -> withStyle(SpanStyle(background = androidx.compose.ui.graphics.Color(0x44FFFF00))) {
                            renderSpans(span.spans)
                        }
                        is Span.Strikethrough -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = onSurfaceVariantColor)) {
                            renderSpans(span.spans)
                        }
                        is Span.Superscript -> withStyle(SpanStyle(fontSize = fontSize * 0.7f, baselineShift = androidx.compose.ui.text.style.BaselineShift.Superscript)) {
                            append(span.text)
                        }
                        is Span.Subscript -> withStyle(SpanStyle(fontSize = fontSize * 0.7f, baselineShift = androidx.compose.ui.text.style.BaselineShift.Subscript)) {
                            append(span.text)
                        }
                        // 行内图片不显示（Compose文本渲染限制）
                        is Span.Image -> { /* 不显示任何内容 */ }
                        is Span.FootnoteRef -> withStyle(SpanStyle(color = tertiaryColor, fontSize = fontSize * 0.8f, baselineShift = androidx.compose.ui.text.style.BaselineShift.Superscript)) {
                            append("[${span.label}]")
                        }
                        is Span.Colored -> withStyle(SpanStyle(color = parseHtmlColor(span.color))) {
                            renderSpans(span.spans)
                        }
                        is Span.Underline -> withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                            renderSpans(span.spans)
                        }
                        is Span.Emoji -> append(span.emoji)
                    }
                }
            }
            renderSpans(heading.spans)
        }

        Text(
            text = annotatedString,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = MarkdownHeading,
            modifier = Modifier.fillMaxWidth().padding(top = if (heading.level <= 2) 16.dp else 8.dp, bottom = 8.dp)
        )
    } else {
        // 兼容旧数据，直接显示文本
        Text(
            text = heading.text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = MarkdownHeading,
            modifier = Modifier.fillMaxWidth().padding(top = if (heading.level <= 2) 16.dp else 8.dp, bottom = 8.dp)
        )
    }
}

@Composable
private fun RenderParagraph(paragraph: MarkdownElement.Paragraph, fontSize: androidx.compose.ui.unit.TextUnit = 16.sp, onLinkClick: (String) -> Unit) {
    val textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize)
    if (paragraph.spans.size == 1 && paragraph.spans[0] is Span.PlainText) {
        Text(
            text = (paragraph.spans[0] as Span.PlainText).text,
            style = textStyle,
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        StyledText(spans = paragraph.spans, style = textStyle, onLinkClick = onLinkClick)
    }
}

@Composable
private fun RenderUnorderedList(list: MarkdownElement.UnorderedList, fontSize: androidx.compose.ui.unit.TextUnit = 16.sp, onLinkClick: (String) -> Unit) {
    val textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize)
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        list.items.forEach { itemSpans ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                Text(text = "•  ", style = textStyle, color = MaterialTheme.colorScheme.primary)
                StyledText(spans = itemSpans, style = textStyle, onLinkClick = onLinkClick, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RenderOrderedList(list: MarkdownElement.OrderedList, fontSize: androidx.compose.ui.unit.TextUnit = 16.sp, onLinkClick: (String) -> Unit) {
    val textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize)
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        list.items.forEachIndexed { index, itemSpans ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                Text(text = "${list.startIndex + index}.  ", style = textStyle, color = MaterialTheme.colorScheme.primary)
                StyledText(spans = itemSpans, style = textStyle, onLinkClick = onLinkClick, modifier = Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenderCodeBlock(codeBlock: MarkdownElement.CodeBlock) {
    val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.5f
    val backgroundColor = if (isDarkTheme) MarkdownCodeBackgroundDark else MarkdownCodeBackground
    val context = LocalContext.current
    val lines = remember(codeBlock.code) { codeBlock.code.lines() }
    val lineCount = lines.size

    // 行号文本
    val lineNumbers = remember(lineCount) {
        buildString {
            for (i in 1..lineCount) {
                append(i.toString().padStart(lineCount.toString().length))
                if (i < lineCount) append("\n")
            }
        }
    }

    // 高亮后的代码
    val highlightedCode = remember(codeBlock.code, codeBlock.language, isDarkTheme) {
        CodeSyntaxHighlighter.highlight(codeBlock.code, codeBlock.language, isDarkTheme)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
    ) {
        // 标题栏：语言名称 + 复制按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDarkTheme) DarkSurfaceVariant else LightSurfaceVariant)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 语言标签
            if (codeBlock.language.isNotEmpty()) {
                Text(
                    text = codeBlock.language.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDarkTheme) DarkOnSurfaceVariant else LightOnSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = "CODE",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDarkTheme) DarkOnSurfaceVariant else LightOnSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            // 复制按钮
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("code", codeBlock.code)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "复制代码",
                    tint = if (isDarkTheme) DarkOnSurfaceVariant else LightOnSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // 代码区域：行号 + 代码
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            // 行号列
            Text(
                text = lineNumbers,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = if (isDarkTheme) DarkCodeComment else LightCodeComment
                ),
                modifier = Modifier.padding(end = 16.dp)
            )
            // 代码列
            Text(
                text = highlightedCode,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RenderBlockQuote(blockQuote: MarkdownElement.BlockQuote, fontSize: androidx.compose.ui.unit.TextUnit = 16.sp, onLinkClick: (String) -> Unit) {
    val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.5f
    val backgroundColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(backgroundColor).padding(start = 12.dp)
    ) {
        Box(modifier = Modifier.width(4.dp).height(IntrinsicSize.Max).background(MaterialTheme.colorScheme.primary))
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            blockQuote.elements.forEach { RenderElement(it, fontSize, onLinkClick) }
        }
    }
}

@Composable
private fun RenderHorizontalRule() {
    HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

/**
 * 图片加载状态
 */
private sealed class ImageLoadState {
    object Loading : ImageLoadState()
    object Success : ImageLoadState()
    data class Error(val reason: String) : ImageLoadState()
    data class InvalidPath(val reason: String) : ImageLoadState()
}

@Composable
private fun RenderImage(image: MarkdownElement.Image) {
    val context = LocalContext.current
    val documentUri = LocalDocumentUri.current
    val folderUri = LocalFolderUri.current
    val folderRepository = LocalFolderRepository.current

    // 图片 URI 和错误原因
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var pathError by remember { mutableStateOf<String?>(null) }
    var isResolving by remember { mutableStateOf(true) }

    // 解析图片 URI
    LaunchedEffect(image.url, documentUri, folderUri) {
        val url = image.url.trim()

        when {
            // 1. 网络 URL (http:// 或 https://)
            url.startsWith("http://") || url.startsWith("https://") -> {
                imageUri = Uri.parse(url)
                pathError = null
                isResolving = false
            }

            // 2. content:// URI
            url.startsWith("content://") -> {
                imageUri = Uri.parse(url)
                pathError = null
                isResolving = false
            }

            // 3. file:// URI
            url.startsWith("file://") -> {
                imageUri = Uri.parse(url)
                pathError = null
                isResolving = false
            }

            // 4. 本地绝对路径 (/storage/... 或 /sdcard/... 或 Windows 盘符 C:/...)
            url.startsWith("/") || (url.length >= 2 && url[1] == ':') -> {
                val file = java.io.File(url)
                if (file.exists()) {
                    imageUri = Uri.fromFile(file)
                    pathError = null
                } else {
                    imageUri = null
                    pathError = "本地文件不存在: $url"
                }
                isResolving = false
            }

            // 5. 相对路径 - 优先使用文件夹 URI 解析
            url.startsWith("./") || url.startsWith("../") ||
            url.startsWith(".\\") || url.startsWith("..\\") ||
            (!url.contains(":") && !url.startsWith("/")) -> {

                val normalizedPath = url
                    .replace('\\', '/')
                    .removePrefix("./")
                    .let { path -> generateSequence(path) { if (it.startsWith("../")) it.removePrefix("../") else null }.lastOrNull() ?: path }

                // 如果有文件夹 URI 和 FolderRepository，使用它们解析
                if (folderUri != null && folderRepository != null) {
                    val resolvedUri = folderRepository.resolveImageUri(folderUri, normalizedPath)
                    if (resolvedUri != null) {
                        imageUri = resolvedUri
                        pathError = null
                    } else {
                        imageUri = null
                        pathError = "图片文件不存在: $normalizedPath"
                    }
                    isResolving = false
                }
                // 否则尝试使用文档 URI
                else if (documentUri != null) {
                    val (uri, error) = resolveRelativePathFromDocument(context, documentUri, normalizedPath)
                    imageUri = uri
                    pathError = error
                    isResolving = false
                }
                // 没有任何 URI，提示用户选择文件夹
                else {
                    imageUri = null
                    pathError = "请先选择文件夹（点击左侧文件夹图标）"
                    isResolving = false
                }
            }

            // 6. 应用内部图片路径
            ImageManager.isInternalImagePath(url) -> {
                val file = ImageManager.loadImageFile(context, url)
                if (file != null) {
                    imageUri = Uri.fromFile(file)
                    pathError = null
                } else {
                    imageUri = null
                    pathError = "内部图片文件不存在"
                }
                isResolving = false
            }

            // 7. 空路径
            url.isBlank() -> {
                imageUri = null
                pathError = "图片路径为空"
                isResolving = false
            }

            // 8. 其他情况
            else -> {
                imageUri = null
                pathError = "不支持的图片路径格式: $url"
                isResolving = false
            }
        }
    }

    // 加载状态
    var loadState by remember { mutableStateOf<ImageLoadState>(ImageLoadState.Loading) }

    // 正在解析时显示加载中
    if (isResolving) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        }
        return
    }

    // 路径无效时直接显示错误
    if (imageUri == null) {
        ImageErrorDisplay(
            altText = image.altText,
            url = image.url,
            reason = pathError ?: "未知错误"
        )
        return
    }

    // 尝试加载图片
    Box(modifier = Modifier.fillMaxWidth()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUri)
                .crossfade(true)
                .listener(
                    onSuccess = { _, _ ->
                        loadState = ImageLoadState.Success
                    },
                    onError = { _, error ->
                        loadState = ImageLoadState.Error(
                            reason = error.throwable.message ?: "加载失败"
                        )
                    }
                )
                .build(),
            contentDescription = image.altText,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth
        )

        // 加载失败时显示错误
        when (val state = loadState) {
            is ImageLoadState.Error -> {
                ImageErrorDisplay(
                    altText = image.altText,
                    url = image.url,
                    reason = state.reason
                )
            }
            else -> {}
        }
    }
}

/**
 * 基于文档 URI 解析相对路径的图片
 * 注意：Android SAF 权限限制，打开单个文件时无法访问同级目录的其他文件
 */
private fun resolveRelativePathFromDocument(
    context: Context,
    documentUri: Uri,
    relativePath: String
): Pair<Uri?, String?> {
    return try {
        Log.d("ImageLoader", "尝试解析相对路径: $relativePath")
        Log.d("ImageLoader", "文档 URI: $documentUri")

        // 使用 DocumentsContract 解析相对路径
        val documentId = DocumentsContract.getDocumentId(documentUri)
        Log.d("ImageLoader", "文档 ID: $documentId")

        val parentDocId = documentId.substringBeforeLast("/")
        Log.d("ImageLoader", "父目录 ID: $parentDocId")

        // 尝试构建父目录的子文档 URI
        val childUri = DocumentsContract.buildChildDocumentsUriUsingTree(documentUri, parentDocId)
        Log.d("ImageLoader", "子文档 URI: $childUri")

        // 尝试查询子文档（这里可能会因为权限问题失败）
        try {
            context.contentResolver.query(
                childUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                ),
                null, null, null
            )?.use { cursor ->
                Log.d("ImageLoader", "查询成功，找到 ${cursor.count} 个子项")

                // 解析相对路径
                val pathParts = relativePath.split("/").filter { it.isNotEmpty() }
                Log.d("ImageLoader", "路径部分: $pathParts")

                // 查找目标文件
                while (cursor.moveToNext()) {
                    val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)

                    if (nameIndex >= 0 && idIndex >= 0) {
                        val name = cursor.getString(nameIndex)
                        val id = cursor.getString(idIndex)
                        Log.d("ImageLoader", "找到文件: $name, ID: $id")

                        // 匹配文件名（最后一部分）
                        if (name == pathParts.last()) {
                            val targetUri = DocumentsContract.buildDocumentUriUsingTree(documentUri, id)
                            Log.d("ImageLoader", "找到目标图片 URI: $targetUri")
                            return Pair(targetUri, null)
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("ImageLoader", "权限不足，无法访问目录: ${e.message}")
            return Pair(null, "Android 权限限制：打开单个文件时无法访问同级目录的图片。请从应用内打开整个文件夹，或将图片导入到应用中。")
        }

        Pair(null, "未找到图片: $relativePath")
    } catch (e: Exception) {
        Log.e("ImageLoader", "解析相对路径失败", e)
        Pair(null, "解析失败: ${e.message}")
    }
}

/**
 * 显示图片加载错误的组件
 */
@Composable
private fun ImageErrorDisplay(
    altText: String,
    url: String,
    reason: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // 显示原始 Markdown 语法
        Text(
            text = "![${altText}](${url})",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        // 显示错误原因（浅红色）
        Text(
            text = "[$reason]",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFE57373) // 浅红色
        )
    }
}

@Composable
private fun RenderLink(link: MarkdownElement.Link, onLinkClick: (String) -> Unit) {
    Text(
        text = link.text,
        style = MaterialTheme.typography.bodyLarge.copy(color = MarkdownLink, textDecoration = TextDecoration.Underline),
        modifier = Modifier.clickable { onLinkClick(link.url) }
    )
}

@Composable
    private fun StyledText(
        spans: List<Span>,
        style: androidx.compose.ui.text.TextStyle,
        onLinkClick: (String) -> Unit,
        modifier: Modifier = Modifier
    ) {
        val context = LocalContext.current
        val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.5f
        val codeBgColor = if (isDarkTheme) MarkdownCodeBackgroundDark else MarkdownCodeBackground
        val linkColor = MarkdownLink
        val tertiaryColor = MaterialTheme.colorScheme.tertiary
        val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

        val annotatedString = buildAnnotatedString {
            fun renderSpans(spanList: List<Span>) {
                spanList.forEach { span ->
                    when (span) {
                        is Span.PlainText -> append(span.text)
                        is Span.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            renderSpans(span.spans)
                        }
                        is Span.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            renderSpans(span.spans)
                        }
                        is Span.BoldItalic -> withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                            renderSpans(span.spans)
                        }
                        is Span.Code -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = androidx.compose.ui.graphics.Color(0x33888888))) {
                            append(span.text)
                        }
                        is Span.Link -> {
                            withLink(
                                LinkAnnotation.Clickable(
                                    tag = span.url,
                                    styles = TextLinkStyles(
                                        style = SpanStyle(
                                            color = linkColor,
                                            textDecoration = TextDecoration.Underline
                                        )
                                    ),
                                    linkInteractionListener = { onLinkClick(span.url) }
                                )
                            ) {
                                append(span.text)
                            }
                        }
                        is Span.Math -> withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = tertiaryColor)) {
                            append("\$")
                            append(span.latex)
                            append("\$")
                        }
                        is Span.Highlight -> withStyle(SpanStyle(background = androidx.compose.ui.graphics.Color(0x44FFFF00))) {
                            renderSpans(span.spans)
                        }
                        is Span.Strikethrough -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = onSurfaceVariantColor)) {
                            renderSpans(span.spans)
                        }
                        is Span.Superscript -> withStyle(SpanStyle(fontSize = style.fontSize * 0.7f, baselineShift = androidx.compose.ui.text.style.BaselineShift.Superscript)) {
                            append(span.text)
                        }
                        is Span.Subscript -> withStyle(SpanStyle(fontSize = style.fontSize * 0.7f, baselineShift = androidx.compose.ui.text.style.BaselineShift.Subscript)) {
                            append(span.text)
                        }
                        // 行内图片不显示（Compose文本渲染限制）
                        is Span.Image -> { /* 不显示任何内容 */ }
                        is Span.FootnoteRef -> withStyle(SpanStyle(color = tertiaryColor, fontSize = style.fontSize * 0.8f, baselineShift = androidx.compose.ui.text.style.BaselineShift.Superscript)) {
                            append("[${span.label}]")
                        }
                        is Span.Colored -> withStyle(SpanStyle(color = parseHtmlColor(span.color))) {
                            renderSpans(span.spans)
                        }
                        is Span.Underline -> withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                            renderSpans(span.spans)
                        }
                        is Span.Emoji -> append(span.emoji)
                    }
                }
            }
            renderSpans(spans)
        }

        Text(text = annotatedString, style = style, modifier = modifier)
    }

    /**
     * 解析 HTML 颜色值
     */
    private fun parseHtmlColor(colorStr: String): androidx.compose.ui.graphics.Color {
        return try {
            val color = colorStr.trim().lowercase()
            when {
                color.startsWith("#") -> {
                    val hex = color.removePrefix("#")
                    when (hex.length) {
                        3 -> androidx.compose.ui.graphics.Color(
                            (hex[0].digitToInt(16) * 17),
                            (hex[1].digitToInt(16) * 17),
                            (hex[2].digitToInt(16) * 17)
                        )
                        6 -> androidx.compose.ui.graphics.Color(
                            hex.substring(0, 2).toInt(16),
                            hex.substring(2, 4).toInt(16),
                            hex.substring(4, 6).toInt(16)
                        )
                        8 -> androidx.compose.ui.graphics.Color(
                            hex.substring(0, 2).toInt(16),
                            hex.substring(2, 4).toInt(16),
                            hex.substring(4, 6).toInt(16),
                            hex.substring(6, 8).toInt(16)
                        )
                        else -> MarkdownLink
                    }
                }
                color == "red" -> androidx.compose.ui.graphics.Color.Red
                color == "green" -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
                color == "blue" -> androidx.compose.ui.graphics.Color.Blue
                color == "yellow" -> androidx.compose.ui.graphics.Color.Yellow
                color == "orange" -> androidx.compose.ui.graphics.Color(0xFFFF9800)
                color == "purple" -> androidx.compose.ui.graphics.Color(0xFF9C27B0)
                color == "pink" -> androidx.compose.ui.graphics.Color(0xFFE91E63)
                color == "cyan" -> androidx.compose.ui.graphics.Color.Cyan
                color == "magenta" -> androidx.compose.ui.graphics.Color.Magenta
                color == "white" -> androidx.compose.ui.graphics.Color.White
                color == "black" -> androidx.compose.ui.graphics.Color.Black
                color == "gray" || color == "grey" -> androidx.compose.ui.graphics.Color.Gray
                else -> MarkdownLink
            }
        } catch (e: Exception) {
            MarkdownLink
        }
    }

/**
 * 渲染表格
 */
@Composable
private fun RenderTable(table: MarkdownElement.Table) {
    val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.5f
    val headerBgColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val borderColor = if (isDarkTheme) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
    ) {
        // 表头
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBgColor)
        ) {
            table.headers.forEachIndexed { index, header ->
                val alignment = table.alignments.getOrElse(index) { TableAlignment.LEFT }
                Text(
                    text = header,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = when (alignment) {
                            TableAlignment.LEFT -> TextAlign.Start
                            TableAlignment.CENTER -> TextAlign.Center
                            TableAlignment.RIGHT -> TextAlign.End
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 数据行
        table.rows.forEachIndexed { rowIndex, row ->
            val rowBgColor = if (rowIndex % 2 == 0) {
                MaterialTheme.colorScheme.surface
            } else {
                if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(rowBgColor)
            ) {
                row.forEachIndexed { cellIndex, cell ->
                    val alignment = table.alignments.getOrElse(cellIndex) { TableAlignment.LEFT }
                    Text(
                        text = cell,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textAlign = when (alignment) {
                                TableAlignment.LEFT -> TextAlign.Start
                                TableAlignment.CENTER -> TextAlign.Center
                                TableAlignment.RIGHT -> TextAlign.End
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (rowIndex < table.rows.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 0.5.dp,
                    color = borderColor
                )
            }
        }
    }
}

/**
 * 渲染任务列表
 */
@Composable
private fun RenderTaskList(taskList: MarkdownElement.TaskList, fontSize: androidx.compose.ui.unit.TextUnit = 16.sp, onLinkClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        taskList.items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.checked,
                    onCheckedChange = null, // 只读模式
                    modifier = Modifier.size(20.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                StyledText(
                    spans = item.spans,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = fontSize,
                        color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    ),
                    onLinkClick = onLinkClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 渲染数学公式块
 */
@Composable
private fun RenderMathBlock(mathBlock: MarkdownElement.MathBlock) {
    val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.5f
    val backgroundColor = if (isDarkTheme) MarkdownCodeBackgroundDark else MarkdownCodeBackground

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        // 数学公式标题
        Text(
            text = "Math",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        // LaTeX代码
        Text(
            text = mathBlock.latex,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.tertiary
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 渲染 Mermaid 图表
 */
@Composable
private fun RenderMermaidBlock(mermaidBlock: MarkdownElement.MermaidBlock) {
    val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.5f

    MermaidRenderer(
        mermaidCode = mermaidBlock.code,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        isDarkTheme = isDarkTheme,
        enableZoom = true,
        minScale = 0.5f,
        maxScale = 3f
    )
}
