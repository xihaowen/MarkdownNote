package com.mdnote.markdownnote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 字数统计在每次文本变化时复用固定正则
private val WHITESPACE_PATTERN = Regex("\\s")
private val CHINESE_CHARACTER_PATTERN = Regex("[\\u4e00-\\u9fa5]")
private val WORD_SEPARATOR_PATTERN = Regex("\\s+")

/**
 * 字数统计信息
 */
data class WordCountInfo(
    val characters: Int,      // 字符数
    val charactersNoSpaces: Int, // 不含空格的字符数
    val words: Int,          // 单词数（中文按字数计算）
    val lines: Int,          // 行数
    val paragraphs: Int,     // 段落数
    val readingTime: Int     // 预计阅读时间（分钟）
)

/**
 * 计算文本的字数统计
 */
fun calculateWordCount(text: String): WordCountInfo {
    if (text.isEmpty()) {
        return WordCountInfo(0, 0, 0, 0, 0, 0)
    }

    val characters = text.length
    val charactersNoSpaces = text.replace(WHITESPACE_PATTERN, "").length

    // 计算单词数（中文字符算一个词，英文按空格分割）
    val chineseChars = text.count { it.code in 0x4E00..0x9FFF }
    val englishWords = text
        .replace(CHINESE_CHARACTER_PATTERN, " ")
        .split(WORD_SEPARATOR_PATTERN)
        .count { it.isNotBlank() }
    val words = chineseChars + englishWords

    // 计算行数
    val lines = if (text.isEmpty()) 0 else text.lines().size

    // 计算段落数（非空行）
    val paragraphs = text.lines().count { it.isNotBlank() }

    // 预计阅读时间（假设每分钟阅读300字）
    val readingTime = if (words > 0) (words / 300.0).toInt().coerceAtLeast(1) else 0

    return WordCountInfo(
        characters = characters,
        charactersNoSpaces = charactersNoSpaces,
        words = words,
        lines = lines,
        paragraphs = paragraphs,
        readingTime = readingTime
    )
}

/**
 * 字数统计状态栏组件
 */
@Composable
fun WordCountBar(
    text: String,
    modifier: Modifier = Modifier
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
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧统计
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(label = "字符", value = wordCount.characters)
                StatItem(label = "字数", value = wordCount.words)
                StatItem(label = "行", value = wordCount.lines)
                StatItem(label = "段落", value = wordCount.paragraphs)
            }

            // 右侧阅读时间
            if (wordCount.readingTime > 0) {
                Text(
                    text = "约 ${wordCount.readingTime} 分钟阅读",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Normal
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}
