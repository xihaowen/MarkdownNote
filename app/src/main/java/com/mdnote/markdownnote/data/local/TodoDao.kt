package com.mdnote.markdownnote.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Query("SELECT * FROM todos WHERE dueDate >= :startOfDay AND dueDate < :endOfDay ORDER BY createdAt ASC")
    fun getTodosByDate(startOfDay: Long, endOfDay: Long): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE isCompleted = 0 AND dueDate < :todayStart ORDER BY dueDate ASC")
    fun getOverdueTodos(todayStart: Long): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos ORDER BY dueDate ASC, createdAt ASC")
    fun getAllTodos(): Flow<List<TodoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoEntity): Long

    @Update
    suspend fun updateTodo(todo: TodoEntity)

    @Delete
    suspend fun deleteTodo(todo: TodoEntity)

    @Query("DELETE FROM todos WHERE isCompleted = 1 AND completedAt < :threshold")
    suspend fun deleteCompletedBefore(threshold: Long)

    @Query("SELECT COUNT(*) FROM todos WHERE isCompleted = 0")
    fun getActiveTodoCount(): Flow<Int>

    @Query("DELETE FROM todos WHERE isCompleted = 1")
    suspend fun deleteAllCompleted()
}
