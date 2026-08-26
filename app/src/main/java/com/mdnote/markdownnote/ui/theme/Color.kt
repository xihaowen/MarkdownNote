package com.mdnote.markdownnote.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================
// MarkdownNote 浅色主题
// ============================================

// 主色调 - 优雅的蓝绿色
val Primary = Color(0xFF006B5E)
val PrimaryDark = Color(0xFF004D44)
val PrimaryLight = Color(0xFF4DB8A6)

// 次要色调
val Secondary = Color(0xFF03DAC6)
val SecondaryDark = Color(0xFF018786)

// 浅色背景
val LightBackground = Color(0xFFFBFDFB)
val LightSurface = Color(0xFFFBFDFB)
val LightSurfaceVariant = Color(0xFFF4F7F5)

// 浅色文字
val LightOnBackground = Color(0xFF1C1B1F)
val LightOnSurface = Color(0xFF1C1B1F)
val LightOnSurfaceVariant = Color(0xFF666666)

// ============================================
// MarkdownNote 深色主题
// ============================================

// 深色背景
val DarkBackground = Color(0xFF121816)
val DarkSurface = Color(0xFF18211F)
val DarkSurfaceVariant = Color(0xFF23302D)

// 深色文字
val DarkOnBackground = Color(0xFFE1EAE7)
val DarkOnSurface = Color(0xFFE1EAE7)
val DarkOnSurfaceVariant = Color(0xFFAAB8B4)

// ============================================
// Markdown 语法高亮颜色 - 浅色主题
// ============================================

val LightMarkdownHeading = Color(0xFF1A1A1A)      // 标题 - 深黑色
val LightMarkdownLink = Color(0xFF1565C0)         // 链接 - 蓝色
val LightMarkdownCode = Color(0xFFD32F2F)         // 代码 - 红色
val LightMarkdownQuote = Color(0xFF616161)        // 引用 - 灰色
val LightMarkdownCodeBackground = Color(0xFFF5F5F5)
val LightMarkdownBlockquoteBorder = Color(0xFF9E9E9E)

// ============================================
// Markdown 语法高亮颜色 - 深色主题
// ============================================

val DarkMarkdownHeading = Color(0xFFE0E0E0)       // 标题 - 浅白色
val DarkMarkdownLink = Color(0xFF64B5F6)          // 链接 - 亮蓝色
val DarkMarkdownCode = Color(0xFFEF9A9A)          // 代码 - 亮红色
val DarkMarkdownQuote = Color(0xFFBDBDBD)         // 引用 - 亮灰色
val DarkMarkdownCodeBackground = Color(0xFF2D2D30)
val DarkMarkdownBlockquoteBorder = Color(0xFF757575)

// ============================================
// 侧边栏颜色
// ============================================

val LightSidebarBackground = Color(0xFFF5F5F5)
val DarkSidebarBackground = Color(0xFF252526)

val LightSidebarItemHover = Color(0xFFE8E8E8)
val DarkSidebarItemHover = Color(0xFF3C3C3C)

val LightSidebarItemSelected = Color(0xFFE0E0E0)
val DarkSidebarItemSelected = Color(0xFF37373D)

// ============================================
// 编辑器特殊颜色
// ============================================

// 当前行高亮（打字机模式）
val LightCurrentLineBackground = Color(0xFFFFFBF0)
val DarkCurrentLineBackground = Color(0xFF2A2D2E)

// 聚焦模式淡化
val LightInactiveParagraphAlpha = 0.3f
val DarkInactiveParagraphAlpha = 0.3f

// 分割线
val LightDividerColor = Color(0xFFE0E0E0)
val DarkDividerColor = Color(0xFF3C3C3C)

// ============================================
// 强调色
// ============================================

val Accent = Color(0xFFFF7043)
val Error = Color(0xFFB3261E)
val ErrorDark = Color(0xFFF2B8B5)

// 收藏星标
val StarColor = Color(0xFFFFC107)

// 置顶图标
val PinColor = Color(0xFF006B5E)

// ============================================
// 代码语法高亮颜色 - 浅色主题
// ============================================

val LightCodeKeyword = Color(0xFF0000FF)       // 关键词 - 蓝色
val LightCodeString = Color(0xFF008000)        // 字符串 - 绿色
val LightCodeComment = Color(0xFF808080)       // 注释 - 灰色
val LightCodeNumber = Color(0xFF098658)        // 数字 - 深绿
val LightCodeFunction = Color(0xFF795E26)      // 函数名 - 棕色
val LightCodeOperator = Color(0xFFD16969)      // 操作符 - 红色
val LightCodeVariable = Color(0xFF001080)      // 变量 - 深蓝
val LightCodeClass = Color(0xFF267F99)         // 类名 - 青色

// ============================================
// 代码语法高亮颜色 - 深色主题
// ============================================

val DarkCodeKeyword = Color(0xFF569CD6)        // 关键词 - 亮蓝
val DarkCodeString = Color(0xFFCE9178)         // 字符串 - 橙色
val DarkCodeComment = Color(0xFF6A9955)        // 注释 - 绿色
val DarkCodeNumber = Color(0xFFB5CEA8)         // 数字 - 浅绿
val DarkCodeFunction = Color(0xFFDCDCAA)       // 函数名 - 黄色
val DarkCodeOperator = Color(0xFFD4D4D4)       // 操作符 - 白色
val DarkCodeVariable = Color(0xFF9CDCFE)       // 变量 - 浅蓝
val DarkCodeClass = Color(0xFF4EC9B0)          // 类名 - 青色

// ============================================
// 别名（兼容旧代码）
// ============================================

val MarkdownHeading = LightMarkdownHeading
val MarkdownLink = LightMarkdownLink
val MarkdownCode = LightMarkdownCode
val MarkdownQuote = LightMarkdownQuote
val MarkdownCodeBackground = LightMarkdownCodeBackground
val MarkdownCodeBackgroundDark = DarkMarkdownCodeBackground

// ============================================
// AI 助手强调色
// ============================================

val AiPurple = Color(0xFF5B5FC7)
