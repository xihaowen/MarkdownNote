package com.mdnote.markdownnote.data.local

import androidx.room.Entity
import androidx.room.Fts4

/**
 * AI 记忆全文搜索虚拟表
 * 基于 FTS4 对 MemoryEntity 的 content 字段建立全文索引
 * 使用外部内容模式（contentEntity），FTS 表不存储自身内容副本
 */
@Fts4(contentEntity = MemoryEntity::class)
@Entity(tableName = "ai_memory_fts")
data class MemoryFts(
    val content: String
)
