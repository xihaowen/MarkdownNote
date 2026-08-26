package com.mdnote.markdownnote.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mdnote.markdownnote.ui.components.glass.GlassDropdownMenu

/**
 * Markdown 编辑器状态栏。
 * 特点：
 * - 字数统计
 * - 行列信息
 * - 阅读时间
 * - 编辑模式切换
 * - 主题切换
 */
@Composable
fun EditorStatusBar(
    text: String,
    modifier: Modifier = Modifier,
    cursorLine: Int = 0,
    cursorColumn: Int = 0,
    selectedCount: Int = 0,
    editMode: EditorMode = EditorMode.EDIT,
    onModeChange: (EditorMode) -> Unit = {},
    isDarkTheme: Boolean = false,
    onThemeToggle: () -> Unit = {},
    typewriterMode: Boolean = false,
    onFocusModeChange: (Boolean) -> Unit = {},
    focusMode: Boolean = false,
    onTypewriterModeChange: (Boolean) -> Unit = {},
    showDetails: Boolean = true
) {
    val wordCount = remember(text) { calculateWordCount(text) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .height(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧统计信息
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 字数
                StatusItem(
                    label = "字数",
                    value = wordCount.words.toString(),
                    showDetails = showDetails
                )

                // 字符数
                StatusItem(
                    label = "字符",
                    value = wordCount.characters.toString(),
                    showDetails = showDetails
                )

                // 行数
                StatusItem(
                    label = "行",
                    value = wordCount.lines.toString(),
                    showDetails = showDetails
                )

                // 选中字符数
                AnimatedVisibility(
                    visible = selectedCount > 0,
                    enter = fadeIn() + slideInHorizontally(),
                    exit = fadeOut() + slideOutHorizontally()
                ) {
                    StatusItem(
                        label = "选中",
                        value = selectedCount.toString(),
                        highlight = true,
                        showDetails = showDetails
                    )
                }
            }

            // 中间阅读时间
            if (showDetails && wordCount.readingTime > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${wordCount.readingTime} 分钟阅读",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 右侧位置和模式
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 行列信息
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "行 ${cursorLine + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = ",",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "列 ${cursorColumn + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 分隔符
                VerticalDivider(
                    modifier = Modifier.height(12.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // 编辑模式切换
                ModeSelector(
                    currentMode = editMode,
                    onModeChange = onModeChange,
                    showDetails = showDetails
                )

                // 打字机模式
                if (showDetails) {
                    StatusToggle(
                        icon = Icons.Default.Edit,
                        isActive = typewriterMode,
                        activeTooltip = "打字机模式",
                        inactiveTooltip = "打字机模式",
                        onClick = { onTypewriterModeChange(!typewriterMode) }
                    )

                    // 焦点模式
                    StatusToggle(
                        icon = Icons.Default.Highlight,
                        isActive = focusMode,
                        activeTooltip = "焦点模式",
                        inactiveTooltip = "焦点模式",
                        onClick = { onFocusModeChange(!focusMode) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusItem(
    label: String,
    value: String,
    highlight: Boolean = false,
    showDetails: Boolean = true
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (showDetails) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.width(3.dp))
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = if (highlight) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ModeSelector(
    currentMode: EditorMode,
    onModeChange: (EditorMode) -> Unit,
    showDetails: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { expanded = true }
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = when (currentMode) {
                    EditorMode.EDIT -> Icons.Default.Edit
                    EditorMode.PREVIEW -> Icons.Default.Visibility
                    EditorMode.SPLIT -> Icons.Default.ViewColumn
                },
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (showDetails) {
                Text(
                    text = when (currentMode) {
                        EditorMode.EDIT -> "编辑"
                        EditorMode.PREVIEW -> "预览"
                        EditorMode.SPLIT -> "分屏"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        GlassDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(120.dp)
        ) {
            EditorMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = when (mode) {
                                    EditorMode.EDIT -> Icons.Default.Edit
                                    EditorMode.PREVIEW -> Icons.Default.Visibility
                                    EditorMode.SPLIT -> Icons.Default.ViewColumn
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = when (mode) {
                                    EditorMode.EDIT -> "编辑模式"
                                    EditorMode.PREVIEW -> "预览模式"
                                    EditorMode.SPLIT -> "分屏模式"
                                }
                            )
                        }
                    },
                    onClick = {
                        onModeChange(mode)
                        expanded = false
                    },
                    trailingIcon = {
                        if (currentMode == mode) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun StatusToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    activeTooltip: String,
    inactiveTooltip: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor = when {
        isActive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        isHovered -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color.Transparent
    }

    val iconColor = when {
        isActive -> MaterialTheme.colorScheme.primary
        isHovered -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = if (isActive) activeTooltip else inactiveTooltip,
            modifier = Modifier.size(14.dp),
            tint = iconColor
        )
    }
}

/**
 * 编辑模式枚举
 */
enum class EditorMode {
    EDIT,       // 纯编辑模式
    PREVIEW,    // 纯预览模式
    SPLIT       // 分屏模式
}
