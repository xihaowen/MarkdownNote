package com.mdnote.markdownnote.ui.components.glass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties

/**
 * 毛玻璃下拉菜单
 *
 * 基于 material3 [DropdownMenu]，替换容器颜色为半透明、添加玻璃边缘描边，
 * 并尝试启用 blur-behind（API 31+）。签名与常用 DropdownMenu 调用一致，
 * 替换时只改函数名，content lambda 原样保留。
 *
 * @param expanded 是否展开
 * @param onDismissRequest 关闭回调
 * @param modifier Modifier
 * @param offset 相对锚点的偏移
 * @param scrollState 滚动状态
 * @param properties Popup 属性
 * @param content 菜单项内容
 */
@Composable
fun GlassDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = glassPopupColorScheme()

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        scrollState = scrollState,
        properties = properties,
        // shadowElevation=0：避免 Modifier.shadow 的 drawBehind 在半透明底色下形成矩形淡影（见 b7c0b9a）
        shape = GlassDefaults.menuShape,
        containerColor = colors.containerColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(GlassDefaults.borderWidth, colors.borderColor),
        content = {
            ClearPopupBackground()
            content()
        }
    )
}
