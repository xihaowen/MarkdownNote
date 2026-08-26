package com.mdnote.markdownnote.ui.components.glass

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 毛玻璃视觉参数集中定义
 * 对标 iOS systemThinMaterial 材质风格，高透明、柔和模糊、中性灰白色调
 */
object GlassDefaults {

    // ===== 圆角（对齐 iOS 现代弹窗圆角）=====
    /** 对话框圆角形状 */
    val dialogShape = RoundedCornerShape(24.dp)
    /** 菜单圆角形状 */
    val menuShape = RoundedCornerShape(16.dp)
    /** Snackbar 圆角形状 */
    val snackbarShape = RoundedCornerShape(14.dp)
    /** 工具栏圆角形状 */
    val toolbarShape = RoundedCornerShape(16.dp)

    // ===== blur-behind =====
    /** 真实背景模糊半径（API 31+），大半径获得更柔和的 iOS 质感 */
    val blurBehindRadius: Dp = 40.dp

    // ===== 阴影 =====
    /** 容器阴影高度（增大获得更柔和的大范围阴影） */
    val shadowElevation: Dp = 12.dp

    // ===== 描边 =====
    /** 玻璃边缘描边宽度 */
    val borderWidth: Dp = 0.5.dp

    // ===== Scrim（背景遮罩）透明度 =====
    /** 有 blur-behind 时的 scrim 透明度（轻度压暗背景，保留通透感） */
    const val ScrimAlphaBlurred = 0.20f
    /** 降级模式下的 scrim 透明度 */
    const val ScrimAlphaFallback = 0.35f
}

/**
 * 毛玻璃颜色方案（单组配色）
 *
 * @param containerColor 容器半透明底色
 * @param borderColor 描边颜色
 * @param gradientHighlight 降级渐变高光色（顶部）
 */
data class GlassColorScheme(
    val containerColor: Color,
    val borderColor: Color,
    val gradientHighlight: Color
)

/**
 * 获取当前主题的毛玻璃颜色方案
 * 根据「是否暗色模式」和「是否支持真实模糊」返回对应配色
 *
 * 有 blur 时使用半透明底色（alpha≈0.62），保留毛玻璃通透感的同时保证文字可读性；
 * 降级时进一步提高不透明度（alpha≈0.80）弥补无真实模糊的对比度损失。
 *
 * @param blurSupported 是否支持真实 blur-behind
 */
@Composable
@ReadOnlyComposable
fun glassColorScheme(blurSupported: Boolean): GlassColorScheme {
    val isDark = isSystemInDarkTheme()
    return if (isDark) {
        if (blurSupported) {
            // 暗色 + 有 blur（保留毛玻璃通透感的同时确保内容清晰）
            GlassColorScheme(
                containerColor = Color(0xFF1C1C1E).copy(alpha = 0.64f),
                borderColor = Color.White.copy(alpha = 0.10f),
                gradientHighlight = Color.White.copy(alpha = 0.05f)
            )
        } else {
            // 暗色 + 降级
            GlassColorScheme(
                containerColor = Color(0xFF1C1C1E).copy(alpha = 0.80f),
                borderColor = Color.White.copy(alpha = 0.12f),
                gradientHighlight = Color.White.copy(alpha = 0.05f)
            )
        }
    } else {
        if (blurSupported) {
            // 亮色 + 有 blur（保留毛玻璃通透感的同时确保内容清晰）
            GlassColorScheme(
                containerColor = Color.White.copy(alpha = 0.60f),
                borderColor = Color.White.copy(alpha = 0.20f),
                gradientHighlight = Color.White.copy(alpha = 0.08f)
            )
        } else {
            // 亮色 + 降级
            GlassColorScheme(
                containerColor = Color.White.copy(alpha = 0.80f),
                borderColor = Color.White.copy(alpha = 0.25f),
                gradientHighlight = Color.White.copy(alpha = 0.08f)
            )
        }
    }
}

/**
 * Popup 浮层专用配色（半透明冷灰，模拟 iOS 玻璃质感）
 *
 * 用于 DropdownMenu、AiFloatingToolbar 等 Popup 类容器。Popup 无法应用 window 级
 * blur-behind（[ApplyGlassWindowProperties] 依赖 [DialogWindowProvider]，Popup 的
 * window 不是 Dialog）。但在纯白编辑器背景上真实 blur 与无 blur 视觉等价，故改用
 * 半透明冷灰底（透出背景内容制造通透感）+ 顶部高光渐变 + 亮色描边 + 阴影，组合出
 * iOS 玻璃质感。冷灰色调保证浮层在纯白背景上可辨。
 */
@Composable
@ReadOnlyComposable
fun glassPopupColorScheme(): GlassColorScheme {
    val isDark = isSystemInDarkTheme()
    // Popup 无法应用 window blur-behind，无模糊兜底时低 alpha 会透出底层文字使
    // 浮层显得过透；经真机验证 0.75 仍偏透，亮色→0.95、暗色→0.92 基本不透明
    return if (isDark) {
        GlassColorScheme(
            containerColor = Color(0xFF202028).copy(alpha = 0.92f),
            borderColor = Color.White.copy(alpha = 0.15f),
            gradientHighlight = Color.White.copy(alpha = 0.08f)
        )
    } else {
        GlassColorScheme(
            containerColor = Color(0xFFD8DCE4).copy(alpha = 0.95f),
            borderColor = Color.White.copy(alpha = 0.55f),
            gradientHighlight = Color.White.copy(alpha = 0.28f)
        )
    }
}
