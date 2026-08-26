package com.mdnote.markdownnote.data.repository

import android.util.Log
import com.mdnote.markdownnote.data.local.TodoDao
import com.mdnote.markdownnote.data.local.TodoEntity
import com.mdnote.markdownnote.domain.model.Todo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TodoRepository @Inject constructor(
    private val todoDao: TodoDao
) {
    companion object {
        private const val TAG = "TodoRepository"
    }

    /**
     * 获取今天的待办事项
     */
    fun getTodayTodos(): Flow<List<Todo>> {
        return try {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis

            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val endOfDay = calendar.timeInMillis

            todoDao.getTodosByDate(startOfDay, endOfDay)
                .map { entities -> entities.map { it.toDomain() } }
                .catch { e ->
                    Log.e(TAG, "获取今日待办失败", e)
                    emit(emptyList())
                }
        } catch (e: Exception) {
            Log.e(TAG, "获取今日待办失败", e)
            flowOf(emptyList())
        }
    }

    /**
     * 获取过期的待办事项
     */
    fun getOverdueTodos(): Flow<List<Todo>> {
        return try {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val todayStart = calendar.timeInMillis

            todoDao.getOverdueTodos(todayStart)
                .map { entities -> entities.map { it.toDomain() } }
                .catch { e ->
                    Log.e(TAG, "获取过期待办失败", e)
                    emit(emptyList())
                }
        } catch (e: Exception) {
            Log.e(TAG, "获取过期待办失败", e)
            flowOf(emptyList())
        }
    }

    /**
     * 获取所有待办事项
     */
    fun getAllTodos(): Flow<List<Todo>> = todoDao.getAllTodos()
        .map { entities -> entities.map { it.toDomain() } }
        .catch { e ->
            Log.e(TAG, "获取所有待办失败", e)
            emit(emptyList())
        }

    /**
     * 获取活跃待办数量
     */
    fun getActiveTodoCount(): Flow<Int> = todoDao.getActiveTodoCount()
        .catch { e ->
            Log.e(TAG, "获取活跃待办数量失败", e)
            emit(0)
        }

    /**
     * 添加待办事项
     */
    suspend fun addTodo(content: String, noteId: Long? = null): Long {
        return try {
            val entity = TodoEntity(
                content = content,
                noteId = noteId
            )
            todoDao.insertTodo(entity)
        } catch (e: Exception) {
            Log.e(TAG, "添加待办失败", e)
            -1
        }
    }

    /**
     * 更新待办事项
     */
    suspend fun updateTodo(todo: Todo) {
        try {
            todoDao.updateTodo(TodoEntity.fromDomain(todo))
        } catch (e: Exception) {
            Log.e(TAG, "更新待办失败", e)
        }
    }

    /**
     * 切换完成状态
     */
    suspend fun toggleCompleted(todo: Todo) {
        try {
            val updatedEntity = TodoEntity.fromDomain(todo).copy(
                isCompleted = !todo.isCompleted,
                completedAt = if (!todo.isCompleted) System.currentTimeMillis() else null
            )
            todoDao.updateTodo(updatedEntity)
        } catch (e: Exception) {
            Log.e(TAG, "切换待办完成状态失败", e)
        }
    }

    /**
     * 删除待办事项
     */
    suspend fun deleteTodo(todo: Todo) {
        try {
            todoDao.deleteTodo(TodoEntity.fromDomain(todo))
        } catch (e: Exception) {
            Log.e(TAG, "删除待办失败", e)
        }
    }

    /**
     * 清理已完成的旧待办
     */
    suspend fun cleanupOldCompleted() {
        try {
            val threshold = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            todoDao.deleteCompletedBefore(threshold)
        } catch (e: Exception) {
            Log.e(TAG, "清理旧已完成待办失败", e)
        }
    }

    /**
     * 清除所有已完成的待办
     */
    suspend fun clearAllCompleted() {
        try {
            todoDao.deleteAllCompleted()
        } catch (e: Exception) {
            Log.e(TAG, "清除所有已完成待办失败", e)
        }
    }
}
