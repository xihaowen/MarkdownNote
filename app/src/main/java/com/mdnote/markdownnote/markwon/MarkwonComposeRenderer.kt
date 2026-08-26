package com.mdnote.markdownnote.markwon

import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Markwon Compose 渲染器
 * 使用 AndroidView 包装 TextView 来渲染 Markdown
 */
@Composable
fun MarkwonComposeRenderer(
    markdown: String,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    fontSize: Float = 16f,
    scrollable: Boolean = true
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val markwon = remember(isDarkTheme) {
        MarkwonManager.getMarkwon(context, isDarkTheme)
    }

    val scrollState = rememberScrollState()
    val effectiveModifier = if (scrollable) {
        modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
    } else {
        modifier.fillMaxWidth()
    }

    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                // 设置基本属性
                textSize = fontSize
                with(density) {
                    setPadding(
                        16.dp.roundToPx(),
                        8.dp.roundToPx(),
                        16.dp.roundToPx(),
                        8.dp.roundToPx()
                    )
                }
                // 支持表格内的交互
                movementMethod = MarkwonManager.getTableAwareMovementMethod()
                // 设置文字颜色
                setTextColor(if (isDarkTheme) 0xFFE0E0E0.toInt() else 0xFF212121.toInt())
            }
        },
        modifier = effectiveModifier,
        update = { textView ->
            // 将 Tab 字符替换为 4 个空格
            val normalizedMarkdown = markdown.replace("\t", "    ")
            // 渲染 Markdown
            markwon.setMarkdown(textView, normalizedMarkdown)
        }
    )
}
