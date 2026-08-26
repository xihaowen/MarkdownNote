package com.mdnote.markdownnote.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mdnote.markdownnote.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Mermaid 图表渲染器
 *
 * 支持的图表类型:
 * - 流程图 (flowchart/graph)
 * - 时序图 (sequenceDiagram)
 * - 甘特图 (gantt)
 * - 类图 (classDiagram)
 * - 状态图 (stateDiagram)
 * - 饼图 (pie)
 * - ER图 (erDiagram)
 * - 用户旅程图 (journey)
 *
 * 特性:
 * - 自动适配深色/浅色主题
 * - 支持缩放和滚动
 * - 支持离线使用（本地 assets）
 */
@Composable
fun MermaidRenderer(
    mermaidCode: String,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    enableZoom: Boolean = true,
    minScale: Float = 0.5f,
    maxScale: Float = 3f,
    onLoadSuccess: (() -> Unit)? = null,
    onLoadError: ((String) -> Unit)? = null
) {
    // 缩放状态
    var scale by remember { mutableFloatStateOf(1f) }
    val transformableState = rememberTransformableState { zoomChange, _, _ ->
        if (enableZoom) {
            scale = (scale * zoomChange).coerceIn(minScale, maxScale)
        }
    }

    // 加载状态
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // 生成唯一 ID
    val diagramId = remember { "mermaid-${UUID.randomUUID()}" }

    val context = LocalContext.current
    val mermaidScript by produceState<String?>(initialValue = null, context) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open(MERMAID_ASSET_NAME).bufferedReader().use { it.readText() }
            }.onFailure { error ->
                onLoadError?.invoke("无法加载内置 Mermaid: ${error.message}")
            }.getOrNull()
        }
    }

    // 生成 HTML 模板
    val htmlContent = remember(mermaidCode, isDarkTheme, diagramId, mermaidScript) {
        mermaidScript?.let { script ->
            buildMermaidHtml(mermaidCode, isDarkTheme, diagramId, script)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDarkTheme) DarkMarkdownCodeBackground else LightMarkdownCodeBackground)
    ) {
        // 加载中提示
        if (isLoading && loadError == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = if (isDarkTheme) PrimaryLight else Primary
                )
            }
        }

        // 错误提示
        loadError?.let { error ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Mermaid \u56fe\u8868\u89e3\u6790\u5931\u8d25",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                // 显示原始代码
                Text(
                    text = "```mermaid",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = mermaidCode,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Text(
                    text = "```",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // WebView 渲染
        if (loadError == null && htmlContent != null) {
            MermaidWebView(
                htmlContent = htmlContent,
                onLoadSuccess = {
                    isLoading = false
                    onLoadSuccess?.invoke()
                },
                onLoadError = { error ->
                    isLoading = false
                    loadError = error
                    onLoadError?.invoke(error)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .transformable(state = transformableState)
            )
        }
    }
}

/**
 * WebView 组件
 */
@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
@Composable
private fun MermaidWebView(
    htmlContent: String,
    onLoadSuccess: () -> Unit,
    onLoadError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentWebView by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            currentWebView?.apply {
                removeJavascriptInterface("MermaidLoaded")
                stopLoading()
                destroy()
            }
            currentWebView = null
        }
    }

    AndroidView(
        factory = { ctx ->
            createMermaidWebView(ctx).apply {
                currentWebView = this
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(message: android.webkit.ConsoleMessage?): Boolean {
                        message?.message()?.let { msg ->
                            if (msg.contains("error", ignoreCase = true) ||
                                msg.contains("failed", ignoreCase = true)) {
                                onLoadError(msg)
                            }
                        }
                        return true
                    }
                }

                // JavaScript 接口
                addJavascriptInterface(
                    object {
                        @android.webkit.JavascriptInterface
                        fun onSuccess() {
                            post { onLoadSuccess() }
                        }

                        @android.webkit.JavascriptInterface
                        fun onError(message: String) {
                            post { onLoadError(message) }
                        }
                    },
                    "MermaidLoaded"
                )

                // 加载 HTML
                loadDataWithBaseURL(
                    null,
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        modifier = modifier,
        update = { webView ->
            // 主题变化时重新加载
            // 注意：这里简化处理，实际可能需要更精细的更新逻辑
        }
    )
}

/**
 * 创建配置好的 WebView
 */
@SuppressLint("SetJavaScriptEnabled")
private fun createMermaidWebView(context: Context): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = false
            blockNetworkLoads = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            // 禁用本地文件访问，防止恶意 Mermaid 代码访问本地文件
            allowFileAccess = false
            allowContentAccess = false
        }

        // 透明背景
        setBackgroundColor(Color.TRANSPARENT)

        // 禁用滚动（由外部处理）
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
    }
}

/**
 * 生成 Mermaid HTML 模板
 *
 * Mermaid 运行库随 APK 打包，渲染过程不访问网络。
 */
internal fun buildMermaidHtml(
    mermaidCode: String,
    isDarkTheme: Boolean,
    diagramId: String,
    mermaidScript: String
): String {
    // Mermaid 主题配置
    val mermaidTheme = if (isDarkTheme) "dark" else "default"

    // 背景颜色（透明）
    val bgColor = "transparent"

    // 文字颜色
    val textColor = if (isDarkTheme) "#D4D4D4" else "#1C1B1F"

    // 转义 mermaidCode 中的特殊字符
    val escapedCode = escapeMermaidCode(mermaidCode)
    val safeMermaidScript = mermaidScript.replace("</script", "<\\/script", ignoreCase = true)

    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        html, body {
            width: 100%;
            height: auto;
            min-height: 100px;
            background-color: $bgColor;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            overflow-x: auto;
            overflow-y: hidden;
        }
        #container {
            width: 100%;
            padding: 16px;
            display: flex;
            justify-content: center;
            align-items: flex-start;
        }
        #$diagramId {
            width: 100%;
            min-width: fit-content;
        }
        /* 适配深色主题 */
        .mermaid {
            color: $textColor;
        }
        /* 错误提示样式 */
        .error-message {
            color: ${if (isDarkTheme) "#F2B8B5" else "#B3261E"};
            padding: 16px;
            text-align: center;
        }
    </style>
</head>
<body>
    <div id="container">
        <pre id="$diagramId" class="mermaid">$escapedCode</pre>
    </div>

    <!-- Mermaid 10.9.6，随 APK 本地分发 -->
    <script>$safeMermaidScript</script>

    <script>
        (function() {
            var mermaidLoaded = false;
            var renderFinished = false;
            var renderTimeout;
            var diagramId = '$diagramId';

            function reportError(message) {
                if (renderFinished) return;
                renderFinished = true;
                clearTimeout(renderTimeout);
                if (window.MermaidLoaded) {
                    window.MermaidLoaded.onError(message);
                }
            }

            // Mermaid 初始化配置
            function initMermaid() {
                mermaid.initialize({
                    startOnLoad: false,
                    theme: '$mermaidTheme',
                    securityLevel: 'strict',
                    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
                    flowchart: {
                        useMaxWidth: true,
                        htmlLabels: true,
                        curve: 'basis'
                    },
                    sequence: {
                        useMaxWidth: true,
                        diagramMarginX: 50,
                        diagramMarginY: 10,
                        actorMargin: 50,
                        width: 150,
                        height: 65
                    },
                    gantt: {
                        useMaxWidth: true,
                        leftPadding: 75,
                        gridLineStartPadding: 35,
                        barHeight: 20,
                        barGap: 4,
                        topPadding: 50
                    },
                    class: {
                        useMaxWidth: true
                    },
                    state: {
                        useMaxWidth: true
                    },
                    pie: {
                        useMaxWidth: true
                    },
                    er: {
                        useMaxWidth: true
                    },
                    journey: {
                        useMaxWidth: true
                    }
                });
            }

            // 渲染图表
            function renderDiagram() {
                try {
                    var element = document.getElementById(diagramId);
                    if (element) {
                        renderTimeout = setTimeout(function() {
                            reportError('Mermaid render timeout');
                        }, 5000);
                        mermaid.run({
                            nodes: [element]
                        }).then(function() {
                            if (renderFinished) return;
                            mermaidLoaded = true;
                            renderFinished = true;
                            clearTimeout(renderTimeout);
                            if (window.MermaidLoaded) {
                                window.MermaidLoaded.onSuccess();
                            }
                        }).catch(function(error) {
                            console.error('Mermaid render error:', error);
                            element.innerHTML = '<div class="error-message">' +
                                '\u56fe\u8868\u6e32\u67d3\u5931\u8d25: ' + error.message +
                                '</div>';
                            reportError(error.message);
                        });
                    }
                } catch (e) {
                    console.error('Mermaid init error:', e);
                    reportError(e.message);
                }
            }

            // 等待 Mermaid 库加载完成
            function waitForMermaid() {
                if (typeof mermaid !== 'undefined') {
                    initMermaid();
                    renderDiagram();
                } else {
                    setTimeout(waitForMermaid, 100);
                }
            }

            // 开始加载
            waitForMermaid();
        })();
    </script>
</body>
</html>
    """.trimIndent()
}

private const val MERMAID_ASSET_NAME = "mermaid-10.9.6.min.js"

/**
 * 转义 Mermaid 代码中的特殊字符
 */
private fun escapeMermaidCode(code: String): String {
    return code
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
