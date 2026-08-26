package com.mdnote.markdownnote.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 笔记数据访问对象 (DAO)
 */
@Dao
interface NoteDao {

    // ==================== 正常笔记查询 ====================

    /**
     * 获取所有未删除的笔记摘要（不含 content 大文本），用于列表页
     */
    @Query("SELECT id, title, '' as content, isStarred, isPinned, lastModifiedTime, isDeleted, deletedAt, externalFolderUri FROM notes WHERE isDeleted = 0 ORDER BY isPinned DESC, lastModifiedTime DESC")
    fun getAllNoteSummaries(): Flow<List<NoteEntity>>

    /**
     * 搜索未删除的笔记（标题或内容）
     */
    @Query("""
        SELECT * FROM notes
        WHERE isDeleted = 0
        AND (title LIKE '%' || :keyword || '%' OR content LIKE '%' || :keyword || '%')
        ORDER BY isPinned DESC, lastModifiedTime DESC
    """)
    fun searchNotes(keyword: String): Flow<List<NoteEntity>>

    /**
     * 获取收藏的笔记（未删除）
     */
    @Query("SELECT * FROM notes WHERE isStarred = 1 AND isDeleted = 0 ORDER BY isPinned DESC, lastModifiedTime DESC")
    fun getStarredNotes(): Flow<List<NoteEntity>>

    // ==================== 回收站查询 ====================

    /**
     * 获取回收站中的笔记
     */
    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedNotes(): Flow<List<NoteEntity>>

    /**
     * 获取回收站笔记数量
     */
    @Query("SELECT COUNT(*) FROM notes WHERE isDeleted = 1")
    fun getDeletedNotesCount(): Flow<Int>

    // ==================== 单条查询 ====================

    /**
     * 根据ID获取笔记
     */
    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: Long): NoteEntity?

    // ==================== 插入/更新/删除 ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Long)

    /**
     * 永久删除所有回收站笔记
     */
    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun clearTrashBin()

    /**
     * 永久删除过期笔记
     */
    @Query("DELETE FROM notes WHERE isDeleted = 1 AND deletedAt < :expireTime")
    suspend fun deleteExpiredNotes(expireTime: Long)

    // ==================== 状态切换 ====================

    /**
     * 切换收藏状态
     */
    @Query("UPDATE notes SET isStarred = NOT isStarred WHERE id = :noteId")
    suspend fun toggleStar(noteId: Long)

    /**
     * 切换置顶状态
     */
    @Query("UPDATE notes SET isPinned = NOT isPinned WHERE id = :noteId")
    suspend fun togglePin(noteId: Long)

    /**
     * 移动到回收站（软删除）
     */
    @Query("UPDATE notes SET isDeleted = 1, deletedAt = :deletedTime WHERE id = :noteId")
    suspend fun moveToTrash(noteId: Long, deletedTime: Long)

    /**
     * 从回收站恢复
     */
    @Query("UPDATE notes SET isDeleted = 0, deletedAt = NULL WHERE id = :noteId")
    suspend fun restoreFromTrash(noteId: Long)

    /**
     * 更新标题
     */
    @Query("UPDATE notes SET title = :title, lastModifiedTime = :lastModifiedTime WHERE id = :noteId")
    suspend fun updateTitle(noteId: Long, title: String, lastModifiedTime: Long)

    /**
     * 更新内容
     */
    @Query("UPDATE notes SET content = :content, lastModifiedTime = :lastModifiedTime WHERE id = :noteId")
    suspend fun updateContent(noteId: Long, content: String, lastModifiedTime: Long)
}
