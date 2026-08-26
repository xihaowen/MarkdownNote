package com.mdnote.markdownnote.ui.components.glass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * 毛玻璃 AlertDialog
 *
 * iOS 风格毛玻璃质感的确认/输入对话框。API 与 material3 [androidx.compose.material3.AlertDialog]
 * 完全一致，替换时只改函数名即可，所有内容 lambda 原样保留。
 *
 * - API 31+ 且设备支持：启用真实 blur-behind，半透明底色让模糊背景透出
 * - 其他情况：降级为半透明渐变模拟毛玻璃质感
 *
 * @param onDismissRequest 关闭回调
 * @param confirmButton 确认按钮（通常是 TextButton）
 * @param modifier Modifier
 * @param dismissButton 取消按钮（可选）
 * @param icon 顶部图标（可选）
 * @param title 标题（可选）
 * @param text 正文内容（可选）
 * @param properties Dialog 属性
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    properties: DialogProperties = DialogProperties()
) {
    // 判断 blur-behind 支持，传给 Window 属性设置（确保一致）
    val blurSupported = isBlurBehindSupported()

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = properties
    ) {
        // 设置 Window 的 blur-behind 和 scrim 透明度
        ApplyGlassWindowProperties(blurSupported)
        // Surface 的 .surface() 会追加 .clip + .background(Transparent)，在半透明毛玻璃上
        // 可能产生微妙的渲染叠加；Box 完全由 glassBackground 控制渲染
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSurface
        ) {
            Box(
                modifier = Modifier.glassBackground(cornerRadius = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(GlassDialogPadding)
                ) {
                    // 图标
                    icon?.let {
                        Box(
                            modifier = Modifier
                                .padding(bottom = GlassIconPadding)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            icon()
                        }
                    }
                    // 标题
                    title?.let {
                        Box(
                            modifier = Modifier
                                .padding(bottom = GlassTitlePadding)
                                .align(
                                    if (icon == null) Alignment.Start
                                    else Alignment.CenterHorizontally
                                )
                        ) {
                            title()
                        }
                    }
                    // 正文
                    text?.let {
                        Box(
                            modifier = Modifier
                                .padding(bottom = GlassTextPadding)
                                .align(Alignment.Start)
                        ) {
                            text()
                        }
                    }
                    // 按钮行
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        horizontalArrangement = Arrangement.spacedBy(GlassButtonsSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        dismissButton?.invoke()
                        confirmButton()
                    }
                }
            }
        }
    }
}

// ===== 布局间距常量（复制自 material3 AlertDialog）=====
private val GlassDialogPadding = PaddingValues(all = 24.dp)
private val GlassIconPadding = 16.dp
private val GlassTitlePadding = 16.dp
private val GlassTextPadding = 24.dp
private val GlassButtonsSpacing = 8.dp
