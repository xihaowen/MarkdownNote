package com.mdnote.markdownnote.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mdnote.markdownnote.ui.components.glass.GlassDefaults
import com.mdnote.markdownnote.ui.components.glass.ClearPopupBackground
import com.mdnote.markdownnote.ui.components.glass.glassBackground

/**
 * AI 操作类型枚举
 */
enum class AiAction {
    EXPLAIN,    // 解释
    MODIFY,     // 修改
    TRANSLATE,  // 翻译
    EXPAND,     // 扩写
    CONDENSE    // 缩写
}

/**
 * 系统操作类型枚举
 */
enum class SystemAction {
    COPY,       // 复制
    CUT,        // 剪切
    PASTE,      // 粘贴
    SELECT_ALL  // 全选
}

/**
 * AI 浮动工具栏 - 文本选中时显示，提供系统操作 + AI 操作按钮
 *
 * 药丸形状的浮动工具栏，白色背景、无粗边框。
 * 包含系统按钮：复制、剪切、粘贴、全选。
 * 包含 AI 按钮：解释、修改、翻译、扩写、缩写。
 *
 * @param selectedText 当前选中的文本
 * @param onAction AI 操作回调
 * @param onSystemAction 系统操作回调
 * @param modifier 修饰符
 */
@Composable
fun AiFloatingToolbar(
    selectedText: String,
    onAction: (AiAction, String) -> Unit,
    onSystemAction: (SystemAction) -> Unit,
    modifier: Modifier = Modifier
) {
    // 不用 Modifier.shadow：其 drawBehind 在半透明底色下形成矩形淡影（见 b7c0b9a）
    ClearPopupBackground()
    Box(
        modifier = modifier
            .glassBackground(cornerRadius = 16.dp, asPopup = true)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 系统操作按钮
            SystemAction.entries.forEach { action ->
                SystemActionButton(
                    action = action,
                    onClick = { onSystemAction(action) }
                )
            }

            // 分隔线
            Surface(
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp),
                color = Color(0xFFB8B8C2),
                shape = RoundedCornerShape(1.dp)
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.padding(
                        horizontal = 0.5.dp,
                        vertical = 4.dp
                    )
                )
            }

            // AI 操作按钮
            AiAction.entries.forEach { action ->
                AiActionButton(
                    action = action,
                    onClick = { onAction(action, selectedText) }
                )
            }
        }
    }
}

/**
 * 系统操作按钮
 */
@Composable
private fun SystemActionButton(
    action: SystemAction,
    onClick: () -> Unit
) {
    val actionLabel = when (action) {
        SystemAction.COPY -> "复制"
        SystemAction.CUT -> "剪切"
        SystemAction.PASTE -> "粘贴"
        SystemAction.SELECT_ALL -> "全选"
    }

    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12)
    ) {
        Text(
            text = actionLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = AiToolbarColors.systemButtonText
        )
    }
}

/**
 * AI 操作按钮 - 纯文字、药丸形状
 */
@Composable
private fun AiActionButton(
    action: AiAction,
    onClick: () -> Unit
) {
    val actionLabel = when (action) {
        AiAction.EXPLAIN -> "解释"
        AiAction.MODIFY -> "修改"
        AiAction.TRANSLATE -> "翻译"
        AiAction.EXPAND -> "扩写"
        AiAction.CONDENSE -> "缩写"
    }

    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12)
    ) {
        Text(
            text = actionLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = AiToolbarColors.buttonText
        )
    }
}

/**
 * AI 工具栏颜色定义
 */
private object AiToolbarColors {
    /** AI 按钮文字颜色（紫色） */
    val buttonText = Color(0xFF5b5fc7)

    /** 系统按钮文字颜色（深灰） */
    val systemButtonText = Color(0xFF555555)
}
