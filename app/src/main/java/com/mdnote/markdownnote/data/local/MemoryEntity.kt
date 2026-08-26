package com.mdnote.markdownnote.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * AI 记忆类型枚举
 * - ROOT: 根记忆，长期核心知识
 * - DAILY: 每日总结
 * - FRAGMENT: 片段记忆，短期细节
 */
enum class MemoryType { ROOT, DAILY, FRAGMENT }

/**
 * AI 记忆数据库实体
 * 对应数据库中的 ai_memory 表
 */
@Entity(tableName = "ai_memory", indices = [Index("type"), Index("createdAt")])
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: MemoryType,
    val content: String,
    val importance: Float,
    val sourceNoteId: Long? = null,
    val createdAt: Long,
    val lastAccessedAt: Long,
    val accessCount: Int = 0
)
