package com.mdnote.markdownnote.ui.components.glass

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider

/**
 * 判断当前设备是否支持真实的 Window blur-behind
 *
 * 条件：API ≥ 31 (Android 12) 且设备未禁用 cross-window blur
 */
@Composable
fun isBlurBehindSupported(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
    val context = LocalContext.current
    val wm = context.getSystemService(WindowManager::class.java) ?: return false
    return wm.isCrossWindowBlurEnabled
}

/**
 * 为当前 Dialog/Popup 的 Window 设置毛玻璃属性
 *
 * - 有 blur 支持：启用 FLAG_BLUR_BEHIND + setBlurBehindRadius，降低 scrim 让模糊背景透出
 * - 无 blur 支持：设置标准 scrim 保证对比度
 *
 * 必须在 Dialog/Popup 的 content 内部调用（依赖 DialogWindowProvider）
 *
 * @param blurSupported 是否支持真实 blur-behind（由 isBlurBehindSupported() 获取）
 */
@Composable
fun ApplyGlassWindowProperties(blurSupported: Boolean) {
    val view = LocalView.current
    val density = LocalDensity.current
    val window = (view.parent as? DialogWindowProvider)?.window ?: return

    // 立即清除 Window/decorView/contentView 的默认背景（不等待 SideEffect，确保首次绘制前生效）
    window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(0))
    window.decorView.background = null
    window.decorView.findViewById<android.view.View>(android.R.id.content)?.background = null

    val radiusPx = with(density) { GlassDefaults.blurBehindRadius.roundToPx() }
    val dimAmount = if (blurSupported) {
        GlassDefaults.ScrimAlphaBlurred
    } else {
        GlassDefaults.ScrimAlphaFallback
    }

    SideEffect {
        // 设置 scrim（dim）透明度
        val params = window.attributes
        params.dimAmount = dimAmount
        window.attributes = params

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (blurSupported) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                val blurParams = window.attributes
                blurParams.setBlurBehindRadius(radiusPx)
                window.attributes = blurParams
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            }
        }
    }
}

/**
 * 清除 Popup 容器的默认背景
 *
 * Compose Popup 的内容视图（android:id/content 及其上的若干 View 层）可能携带默认
 * 不透明背景，透过半透明毛玻璃底色会显形为矩形色块（直角矩形与圆角玻璃边界不一致）。
 * 在 Popup 的 content 顶部调用本函数，自下而上清除这些背景。
 *
 * 注意：仅在 Popup 内容中调用；误在 Activity 内容中调用会清除 Activity 视图背景。
 */
@Composable
fun ClearPopupBackground() {
    val view = LocalView.current
    SideEffect {
        var v: android.view.View? = view
        while (v != null) {
            v.background = null
            v = v.parent as? android.view.View
        }
    }
}

/**
 * 毛玻璃背景 Modifier
 *
 * 自动根据设备 blur-behind 支持情况选择渲染路径：
 * - 支持：半透明底色（alpha=0.70），让模糊后的背景透出
 * - 降级：提高不透明度（alpha=0.92）+ 叠加纵向渐变高光，模拟毛玻璃亮度过渡
 *
 * 统一添加亮色描边（玻璃边缘反光）和柔和阴影
 *
 * @param cornerRadius 圆角半径
 * @param asPopup 是否用于 Popup 类容器（DropdownMenu/浮动工具栏）。Popup 无法应用
 *                window blur，改用半透明冷灰底 + 强制高光渐变模拟 iOS 玻璃质感
 */
fun Modifier.glassBackground(
    cornerRadius: Dp = 20.dp,
    asPopup: Boolean = false
): Modifier = this.composed {
    val blurSupported = isBlurBehindSupported()
    val colors = if (asPopup) glassPopupColorScheme() else glassColorScheme(blurSupported)
    val showGradient = asPopup || !blurSupported
    val shape = RoundedCornerShape(cornerRadius)

    this
        // 裁剪到圆角形状
        // 注：不使用 Modifier.shadow()，因其 drawBehind 实现会透过半透明背景
        // 在暗色模式下形成可见淡影；毛玻璃深度感由描边 + scrim 提供
        .clip(shape)
        // 半透明底色（传入 shape 确保按圆角形状绘制，不依赖 clip 裁剪矩形）
        .background(colors.containerColor, shape = shape)
        // 降级或 Popup 时叠加纵向渐变高光（模拟 iOS 毛玻璃亮度过渡）
        .then(
            if (showGradient) {
                Modifier.background(
                    brush = Brush.verticalGradient(
                        colors = listOf(colors.gradientHighlight, Color.Transparent)
                    ),
                    shape = shape
                )
            } else {
                Modifier
            }
        )
        // 玻璃边缘描边
        .border(
            width = GlassDefaults.borderWidth,
            color = colors.borderColor,
            shape = shape
        )
}
