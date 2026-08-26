package com.mdnote.markdownnote.domain.model

/**
 * 待办事项领域模型
 */
data class Todo(
    val id: Long = 0,
    val content: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val dueDate: Long = System.currentTimeMillis(),
    val noteId: Long? = null
) {
    /**
     * 是否是今天的任务
     */
    fun isToday(): Boolean {
        val today = java.util.Calendar.getInstance()
        val taskDate = java.util.Calendar.getInstance()
        taskDate.timeInMillis = dueDate

        return today.get(java.util.Calendar.YEAR) == taskDate.get(java.util.Calendar.YEAR) &&
                today.get(java.util.Calendar.DAY_OF_YEAR) == taskDate.get(java.util.Calendar.DAY_OF_YEAR)
    }
}
