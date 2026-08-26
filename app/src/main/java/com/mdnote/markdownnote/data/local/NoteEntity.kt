package com.mdnote.markdownnote.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mdnote.markdownnote.domain.model.Note

/**
 * Room 数据库实体类
 * 对应数据库中的 notes 表
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val lastModifiedTime: Long,
    val isStarred: Boolean = false,
    val isPinned: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val externalFolderUri: String? = null
) {
    fun toDomain(): Note = Note(
        id = id,
        title = title,
        content = content,
        lastModifiedTime = lastModifiedTime,
        isStarred = isStarred,
        isPinned = isPinned,
        isDeleted = isDeleted,
        deletedAt = deletedAt,
        externalFolderUri = externalFolderUri
    )

    companion object {
        fun fromDomain(note: Note): NoteEntity = NoteEntity(
            id = note.id,
            title = note.title,
            content = note.content,
            lastModifiedTime = note.lastModifiedTime,
            isStarred = note.isStarred,
            isPinned = note.isPinned,
            isDeleted = note.isDeleted,
            deletedAt = note.deletedAt,
            externalFolderUri = note.externalFolderUri
        )
    }
}
