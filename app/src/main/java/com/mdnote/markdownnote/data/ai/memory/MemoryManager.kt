package com.mdnote.markdownnote.data.ai.memory

import android.content.Context
import com.mdnote.markdownnote.data.local.MemoryDao
import com.mdnote.markdownnote.data.local.MemoryEntity
import com.mdnote.markdownnote.data.local.MemoryType
import com.mdnote.markdownnote.domain.model.Memory
import com.mdnote.markdownnote.domain.model.toDomainModel
import com.mdnote.markdownnote.util.SettingsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject

/**
 * AI 记忆管理器
 * 管理记忆的读写、搜索和合并操作，封装 MemoryDao 提供高层记忆操作
 */
class MemoryManager @Inject constructor(
    private val memoryDao: MemoryDao,
    private val settingsManager: SettingsManager,
    @param:ApplicationContext private val appContext: Context
) {
    companion object {
        private const val MEMORY_DIR = "ai/memory"
        private const val MEMORY_FILE = "MEMORY.md"
        private const val MAX_SEARCH_RESULTS = 10
    }

    /** 保存片段记忆 */
    suspend fun saveFragment(content: String, importance: Float = 0.5f, sourceNoteId: Long? = null) {
        val now = System.currentTimeMillis()
        memoryDao.insert(MemoryEntity(
            type = MemoryType.FRAGMENT,
            content = content,
            importance = importance,
            sourceNoteId = sourceNoteId,
            createdAt = now,
            lastAccessedAt = now,
            accessCount = 1
        ))
    }

    /** 保存每日记忆 */
    suspend fun saveDaily(content: String, importance: Float = 0.7f) {
        val now = System.currentTimeMillis()
        memoryDao.insert(MemoryEntity(
            type = MemoryType.DAILY,
            content = content,
            importance = importance,
            createdAt = now,
            lastAccessedAt = now,
            accessCount = 1
        ))
    }

    /** 保存提取的记忆片段（由外部调用 AI 提取后传入，避免循环依赖 AiService） */
    suspend fun saveExtractedFragments(fragments: List<String>, importance: Float = 0.5f) {
        fragments.filter { it.isNotBlank() }.forEach { fragment ->
            saveFragment(fragment.trim(), importance)
        }
    }

    /** 搜索相关记忆 */
    suspend fun search(query: String, limit: Int = MAX_SEARCH_RESULTS): List<Memory> {
        val results = if (query.isNotBlank()) {
            try {
                memoryDao.search(query, limit)
            } catch (_: Exception) {
                // FTS 搜索失败时降级为普通查询
                memoryDao.getByTypeTopK(MemoryType.FRAGMENT, limit)
            }
        } else {
            memoryDao.getByTypeTopK(MemoryType.FRAGMENT, limit)
        }
        // 批量更新访问记录
        if (results.isNotEmpty()) {
            val now = System.currentTimeMillis()
            memoryDao.updateAccessBatch(results.map { it.id }, now)
        }
        return results.map { it.toDomainModel() }
    }

    /** 构建记忆上下文（用于 system prompt 注入） */
    suspend fun buildMemoryContext(query: String): String {
        if (!settingsManager.settings.first().aiMemoryEnabled) return ""
        val memories = search(query)
        if (memories.isEmpty()) return ""
        val memoryText = memories.joinToString("\n") { "- ${it.content}" }
        return "## 相关记忆\n$memoryText\n"
    }

    /** 获取根记忆内容 */
    suspend fun getRootMemory(): String {
        val rootEntities = memoryDao.getByTypeTopK(MemoryType.ROOT, 1)
        return rootEntities.firstOrNull()?.content ?: ""
    }

    /** 更新根记忆 */
    suspend fun updateRootMemory(content: String) {
        val existing = memoryDao.getByTypeTopK(MemoryType.ROOT, 1)
        val now = System.currentTimeMillis()
        if (existing.isNotEmpty()) {
            memoryDao.updateContent(existing.first().id, content, 1.0f)
        } else {
            memoryDao.insert(MemoryEntity(
                type = MemoryType.ROOT,
                content = content,
                importance = 1.0f,
                createdAt = now,
                lastAccessedAt = now,
                accessCount = 1
            ))
        }
        // 同步写入 MEMORY.md 文件
        saveMemoryFile(content)
    }

    /** 获取最近片段记忆（梦境用） */
    suspend fun getRecentFragments(days: Int = 7): List<Memory> {
        val sinceTimestamp = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        return memoryDao.getRecentFragments(sinceTimestamp).map { it.toDomainModel() }
    }

    /** 获取所有每日记忆（梦境用） */
    suspend fun getAllDailyMemories(): List<Memory> {
        return memoryDao.getByTypeTopK(MemoryType.DAILY, 100).map { it.toDomainModel() }
    }

    /** 删除片段记忆（梦境深睡用） */
    suspend fun deleteFragments(ids: List<Long>) {
        memoryDao.deleteFragmentsByIds(ids)
    }

    /** 写入 MEMORY.md 文件到应用内部存储 */
    private fun saveMemoryFile(content: String) {
        try {
            val dir = File(appContext.filesDir, MEMORY_DIR)
            dir.mkdirs()
            val file = File(dir, MEMORY_FILE)
            file.writeText(content)
        } catch (_: Exception) {
            // 文件写入失败不影响主流程
        }
    }
}

/**
 * 记忆统计数据
 */
data class MemoryStats(
    val fragmentCount: Int,
    val dailyCount: Int,
    val rootCount: Int
)
