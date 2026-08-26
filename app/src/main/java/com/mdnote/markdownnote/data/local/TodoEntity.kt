package com.mdnote.markdownnote.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mdnote.markdownnote.domain.model.Todo

/**
 * 待办事项数据库实体
 */
@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val dueDate: Long = System.currentTimeMillis(),
    val noteId: Long? = null
) {
    /**
     * 转换为领域模型
     */
    fun toDomain(): Todo {
        return Todo(
            id = id,
            content = content,
            isCompleted = isCompleted,
            createdAt = createdAt,
            completedAt = completedAt,
            dueDate = dueDate,
            noteId = noteId
        )
    }

    companion object {
        /**
         * 从领域模型创建实体
         */
        fun fromDomain(todo: Todo): TodoEntity {
            return TodoEntity(
                id = todo.id,
                content = todo.content,
                isCompleted = todo.isCompleted,
                createdAt = todo.createdAt,
                completedAt = todo.completedAt,
                dueDate = todo.dueDate,
                noteId = todo.noteId
            )
        }
    }
}
