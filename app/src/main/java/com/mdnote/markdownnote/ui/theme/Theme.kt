package com.mdnote.markdownnote.ui.theme

import android.app.Activity
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.mdnote.markdownnote.data.repository.FolderRepository

/**
 * 全局字号设置
 */
val LocalFontSize = compositionLocalOf { 16 }

/**
 * 文档基础路径（用于解析相对图片路径）
 * 存储外部文档的 URI，用于解析相对路径的图片
 */
val LocalDocumentUri = compositionLocalOf<Uri?> { null }

/**
 * 文件夹 URI（用于解析相对图片路径）
 * 当从文件夹浏览器打开文件时，存储文件夹的 URI
 */
val LocalFolderUri = compositionLocalOf<Uri?> { null }

/**
 * 文件夹仓库（用于图片路径解析）
 * 提供 resolveImageUri 方法
 */
val LocalFolderRepository = compositionLocalOf<FolderRepository?> { null }

/**
 * 获取当前字号
 */
@Composable
fun currentFontSize(): Int = LocalFontSize.current

/**
 * 浅色主题配色方案
 */
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = PrimaryDark,
    secondary = Secondary,
    onSecondary = Color.Black,
    secondaryContainer = SecondaryDark,
    tertiary = Accent,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightDividerColor,
    outlineVariant = LightDividerColor,
    error = Error,
    onError = Color.White
)

/**
 * 深色主题配色方案
 */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = PrimaryDark,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = PrimaryLight,
    secondary = Secondary,
    onSecondary = Color.Black,
    secondaryContainer = SecondaryDark,
    tertiary = Accent,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkDividerColor,
    outlineVariant = DarkDividerColor,
    error = ErrorDark,
    onError = Error
)

/**
 * Markdown 颜色配置
 * 根据主题返回对应的 Markdown 语法高亮颜色
 */
data class MarkdownColors(
    val heading: Color,
    val link: Color,
    val code: Color,
    val quote: Color,
    val codeBackground: Color,
    val blockquoteBorder: Color
)

/**
 * 获取当前主题的 Markdown 颜色
 */
@Composable
fun getMarkdownColors(isDarkTheme: Boolean): MarkdownColors {
    return if (isDarkTheme) {
        MarkdownColors(
            heading = DarkMarkdownHeading,
            link = DarkMarkdownLink,
            code = DarkMarkdownCode,
            quote = DarkMarkdownQuote,
            codeBackground = DarkMarkdownCodeBackground,
            blockquoteBorder = DarkMarkdownBlockquoteBorder
        )
    } else {
        MarkdownColors(
            heading = LightMarkdownHeading,
            link = LightMarkdownLink,
            code = LightMarkdownCode,
            quote = LightMarkdownQuote,
            codeBackground = LightMarkdownCodeBackground,
            blockquoteBorder = LightMarkdownBlockquoteBorder
        )
    }
}

/**
 * 应用主题
 * 支持 Material 3 动态颜色和深色模式
 *
 * @param darkTheme 是否使用深色主题
 * @param dynamicColor 是否使用动态颜色（Android 12+）
 * @param content 主题包裹的内容
 */
@Composable
fun MarkdownNoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,  // 禁用动态颜色，使用自定义主题
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Android 12+ 支持动态颜色
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * 无干扰模式主题
 * 隐藏状态栏，全屏编辑
 */
@Composable
fun DistractionFreeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
