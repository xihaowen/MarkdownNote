package com.mdnote.markdownnote.ui.components.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * 近全屏毛玻璃弹窗容器
 *
 * 对标 iOS 大型内容 sheet：近全屏 + 大圆角 + 真实背景模糊（或降级）+ 进场缩放淡入动画。
 * 内部组装 [GlassSheetTopBar]（固定标题栏）+ 内容区域（占满剩余空间，内容自管滚动）。
 *
 * 完全复用 [isBlurBehindSupported] / [ApplyGlassWindowProperties] / [glassBackground]，
 * 与 [GlassDialog] 质感一致。
 *
 * ## 底部系统栏避让原理
 *
 * 本容器基于 [BasicAlertDialog]，其 Dialog 是独立 window，覆盖整个屏幕（含导航栏区域），
 * 因此 Dialog window 自身的 insets 中 navigationBars 为 0（它没有被导航栏遮挡，而是盖在上方）。
 * 不能在 Dialog 内容内用 navigationBarsPadding/imePadding 避让。
 *
 * 正确做法：在进入 Dialog 前（本函数在宿主 Activity 的 composition 中被调用）读取
 * [WindowInsets]，此时读到的是 Activity window 的真实系统栏高度。将此高度作为外层
 * 容器的底部 padding，使整个毛玻璃容器上移到导航栏上方，而非仅靠 Column 内 Spacer
 * 挤压内容（那样毛玻璃圆角/描边仍会画到导航栏区域）。
 *
 * @param onDismissRequest 关闭回调（scrim 点击 / 返回键触发）
 * @param title 标题栏标题
 * @param onClose 标题栏 ✕ 关闭按钮回调（通常与 onDismissRequest 相同）
 * @param modifier Modifier
 * @param trailingAction 标题栏右侧可选操作（如「清空」）
 * @param content 正文内容（自管布局与滚动）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassSheet(
    onDismissRequest: () -> Unit,
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val blurSupported = isBlurBehindSupported()

    // 进场动画进度（0→1），驱动 alpha + scale
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }
    // Dialog 是独立 window，其内 WindowInsets 全为 0，必须在进入 Dialog 前读取宿主 Activity 的真实 insets。
    // Android 15+ (targetSdk 35) 强制 edge-to-edge，此处的 navigationBars 一定返回真实导航栏高度。
    val bottomInset = WindowInsets.navigationBars.union(WindowInsets.ime)
        .asPaddingValues().calculateBottomPadding()

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = DialogProperties(
            // 近全屏关键参数：禁用平台默认宽度限制 + 让内容延伸到系统栏
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        ApplyGlassWindowProperties(blurSupported)

        CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSurface
        ) {
            // 近全屏容器：fillMaxSize + 四周小边距 + 大圆角毛玻璃
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // （原先仅在 Column 内加 Spacer，毛玻璃圆角/描边仍画到导航栏区域，导致弹窗底部超出屏幕）
                    .padding(top = 10.dp, start = 10.dp, end = 10.dp)
                    .padding(bottom = 10.dp + bottomInset + 30.dp)
                    // 进场缩放（0.92→1.0）+ 淡入
                    .graphicsLayer {
                        alpha = progress.value
                        scaleX = 0.92f + 0.08f * progress.value
                        scaleY = 0.92f + 0.08f * progress.value
                    }
                    .glassBackground(cornerRadius = 24.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 固定标题栏
                    GlassSheetTopBar(
                        title = title,
                        onClose = onClose,
                        trailing = trailingAction
                    )
                    // 正文区域（占满剩余空间，content 自管滚动/布局）
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}
