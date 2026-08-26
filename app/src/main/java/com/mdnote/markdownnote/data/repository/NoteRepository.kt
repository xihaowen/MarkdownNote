package com.mdnote.markdownnote.data.repository

import com.mdnote.markdownnote.data.local.NoteDao
import com.mdnote.markdownnote.data.local.NoteEntity
import com.mdnote.markdownnote.domain.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 笔记数据仓库
 */
@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao
) {
    // ==================== 正常笔记操作 ====================

    /**
     * 获取所有笔记摘要（不含 content），用于列表页
     */
    fun getAllNoteSummaries(): Flow<List<Note>> = noteDao.getAllNoteSummaries()
        .map { entities -> entities.map { it.toDomain() } }
        .catch { e ->
            Log.e("NoteRepository", "获取笔记摘要失败", e)
            emit(emptyList())
        }

    /**
     * 搜索笔记（标题+内容）
     */
    fun searchNotes(keyword: String): Flow<List<Note>> = noteDao.searchNotes(keyword)
        .map { entities -> entities.map { it.toDomain() } }
        .catch { e ->
            Log.e("NoteRepository", "搜索笔记失败", e)
            emit(emptyList())
        }

    /**
     * 获取收藏笔记
     */
    fun getStarredNotes(): Flow<List<Note>> = noteDao.getStarredNotes()
        .map { entities -> entities.map { it.toDomain() } }
        .catch { e ->
            Log.e("NoteRepository", "获取收藏笔记失败", e)
            emit(emptyList())
        }

    /**
     * 根据ID获取笔记
     */
    suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)?.toDomain()

    /**
     * 保存笔记（新增或更新）
     */
    suspend fun saveNote(note: Note): Long {
        val entity = NoteEntity.fromDomain(note)
        val currentTime = System.currentTimeMillis()

        return if (note.id == 0L) {
            noteDao.insertNote(entity.copy(lastModifiedTime = currentTime))
        } else {
            noteDao.updateNote(entity.copy(lastModifiedTime = currentTime))
            note.id
        }
    }

    /**
     * 创建新笔记
     */
    suspend fun createNote(title: String = "", content: String = ""): Long {
        val note = Note(
            title = title.ifEmpty { "无标题笔记" },
            content = content,
            lastModifiedTime = System.currentTimeMillis()
        )
        return noteDao.insertNote(NoteEntity.fromDomain(note))
    }

    /**
     * 重命名笔记
     */
    suspend fun renameNote(noteId: Long, newTitle: String) {
        noteDao.updateTitle(noteId, newTitle, System.currentTimeMillis())
    }

    /**
     * 更新笔记内容（自动保存使用）
     */
    suspend fun updateContent(noteId: Long, content: String) {
        noteDao.updateContent(noteId, content, System.currentTimeMillis())
    }

    /**
     * 切换收藏
     */
    suspend fun toggleStar(noteId: Long) {
        noteDao.toggleStar(noteId)
    }

    /**
     * 切换置顶
     */
    suspend fun togglePin(noteId: Long) {
        noteDao.togglePin(noteId)
    }

    // ==================== 回收站操作 ====================

    /**
     * 获取回收站笔记
     */
    fun getDeletedNotes(): Flow<List<Note>> = noteDao.getDeletedNotes()
        .map { entities -> entities.map { it.toDomain() } }
        .catch { e ->
            Log.e("NoteRepository", "获取回收站笔记失败", e)
            emit(emptyList())
        }

    /**
     * 获取回收站笔记数量
     */
    fun getDeletedNotesCount(): Flow<Int> = noteDao.getDeletedNotesCount()
        .catch { e ->
            Log.e("NoteRepository", "获取回收站数量失败", e)
            emit(0)
        }

    /**
     * 移动到回收站（软删除）
     */
    suspend fun moveToTrash(noteId: Long) {
        noteDao.moveToTrash(noteId, System.currentTimeMillis())
    }

    /**
     * 从回收站恢复
     */
    suspend fun restoreFromTrash(noteId: Long) {
        noteDao.restoreFromTrash(noteId)
    }

    /**
     * 永久删除笔记
     */
    suspend fun permanentlyDelete(noteId: Long) {
        noteDao.deleteNoteById(noteId)
    }

    /**
     * 清空回收站
     */
    suspend fun clearTrashBin() {
        noteDao.clearTrashBin()
    }

    /**
     * 清理过期笔记（30天前删除的）
     */
    suspend fun cleanupExpiredNotes() {
        val expireTime = System.currentTimeMillis() - TRASH_EXPIRY_DAYS * 24 * 60 * 60 * 1000L
        noteDao.deleteExpiredNotes(expireTime)
    }

    // ==================== 辅助方法 ====================

    /**
     * 从内容提取标题
     */
    fun extractTitle(content: String): String {
        val firstLine = content.lines().firstOrNull { it.isNotBlank() } ?: return "无标题笔记"
        return firstLine
            .replace(Regex("^#+\\s*"), "")
            .trim()
            .takeIf { it.isNotBlank() } ?: "无标题笔记"
    }

    companion object {
        const val TRASH_EXPIRY_DAYS = 30
    }
}
