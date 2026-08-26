package com.mdnote.markdownnote.domain.model

import com.mdnote.markdownnote.data.local.MemoryEntity
import com.mdnote.markdownnote.data.local.MemoryType

/**
 * AI 记忆领域模型
 * 用于业务逻辑层的数据传递
 */
data class Memory(
    val id: Long = 0,
    val type: MemoryType,
    val content: String,
    val importance: Float,
    val sourceNoteId: Long? = null,
    val createdAt: Long,
    val lastAccessedAt: Long,
    val accessCount: Int = 0
)

/**
 * Memory 领域模型转数据库实体
 */
fun Memory.toEntity() = MemoryEntity(
    id = id,
    type = type,
    content = content,
    importance = importance,
    sourceNoteId = sourceNoteId,
    createdAt = createdAt,
    lastAccessedAt = lastAccessedAt,
    accessCount = accessCount
)

/**
 * MemoryEntity 数据库实体转领域模型
 */
fun MemoryEntity.toDomainModel() = Memory(
    id = id,
    type = type,
    content = content,
    importance = importance,
    sourceNoteId = sourceNoteId,
    createdAt = createdAt,
    lastAccessedAt = lastAccessedAt,
    accessCount = accessCount
)
