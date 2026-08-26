package com.mdnote.markdownnote.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.SkipQueryVerification
import kotlinx.coroutines.flow.Flow

/**
 * AI 记忆数据访问对象
 */
@Dao
interface MemoryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(memory: MemoryEntity): Long

    @Query("SELECT * FROM ai_memory WHERE type = :type ORDER BY importance DESC, createdAt DESC LIMIT :limit")
    suspend fun getByTypeTopK(type: MemoryType, limit: Int): List<MemoryEntity>

    @Query("SELECT * FROM ai_memory WHERE type = 'FRAGMENT' AND createdAt >= :sinceTimestamp ORDER BY createdAt DESC")
    suspend fun getRecentFragments(sinceTimestamp: Long): List<MemoryEntity>

    @SkipQueryVerification
    @Query("""
        SELECT m.* FROM ai_memory m
        JOIN ai_memory_fts f ON m.rowid = f.rowid
        WHERE f MATCH :query
        ORDER BY m.importance DESC, m.lastAccessedAt DESC
        LIMIT :limit
    """)
    suspend fun search(query: String, limit: Int = 10): List<MemoryEntity>

    @Query("UPDATE ai_memory SET lastAccessedAt = :timestamp, accessCount = accessCount + 1 WHERE id IN (:ids)")
    suspend fun updateAccessBatch(ids: List<Long>, timestamp: Long)

    @Query("UPDATE ai_memory SET content = :content, importance = :importance WHERE id = :id")
    suspend fun updateContent(id: Long, content: String, importance: Float)

    @Query("DELETE FROM ai_memory WHERE type = 'FRAGMENT' AND id IN (:ids)")
    suspend fun deleteFragmentsByIds(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM ai_memory WHERE type = :type")
    suspend fun countByType(type: MemoryType): Int
}
