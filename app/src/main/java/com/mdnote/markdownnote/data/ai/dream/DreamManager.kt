package com.mdnote.markdownnote.data.ai.dream

import com.mdnote.markdownnote.data.ai.AiResult
import com.mdnote.markdownnote.data.ai.AiService
import com.mdnote.markdownnote.data.ai.memory.MemoryManager
import com.mdnote.markdownnote.domain.model.AiMessage
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 梦境管理器
 * 编排三阶段梦境周期，使用 AI 进行记忆整理和整合
 *
 * 三阶段：
 * 1. 浅睡 (LIGHT_SLEEP) - 整理近期片段记忆，去重合并
 * 2. 深睡 (DEEP_SLEEP) - 将重要片段提炼为每日摘要
 * 3. REM - 将多日记忆整合为根记忆
 */
class DreamManager @Inject constructor(
    private val aiService: AiService,
    private val memoryManager: MemoryManager
) {
    companion object {
        private const val RECENT_DAYS = 7
    }

    private var _currentPhase: DreamPhase? = null
    val currentPhase: DreamPhase? get() = _currentPhase

    /** 执行完整梦境周期（三阶段） */
    suspend fun runDreamCycle(): DreamResult {
        val results = mutableListOf<DreamPhaseResult>()

        // Phase 1: 浅睡 - 整理片段记忆
        _currentPhase = DreamPhase(DreamPhaseEnum.LIGHT_SLEEP, "整理近期片段记忆", true)
        val lightResult = runLightSleep()
        results.add(lightResult)
        if (lightResult.failed) {
            _currentPhase = null
            return DreamResult(results, failed = true, errorMessage = lightResult.errorMessage)
        }

        // Phase 2: 深睡 - 提炼每日摘要
        _currentPhase = DreamPhase(DreamPhaseEnum.DEEP_SLEEP, "提炼每日摘要", true)
        val deepResult = runDeepSleep()
        results.add(deepResult)
        if (deepResult.failed) {
            _currentPhase = null
            return DreamResult(results, failed = true, errorMessage = deepResult.errorMessage)
        }

        // Phase 3: REM - 整合根记忆
        _currentPhase = DreamPhase(DreamPhaseEnum.REM, "整合根记忆", true)
        val remResult = runRemSleep()
        results.add(remResult)

        _currentPhase = null
        return DreamResult(results, failed = remResult.failed, errorMessage = remResult.errorMessage)
    }

    /** 浅睡：整理近期片段记忆，去重合并 */
    private suspend fun runLightSleep(): DreamPhaseResult {
        return try {
            val fragments = memoryManager.getRecentFragments(RECENT_DAYS)
            if (fragments.isEmpty()) {
                DreamPhaseResult(DreamPhaseEnum.LIGHT_SLEEP, "无近期片段记忆需要整理")
            } else {
                // 使用 AI 去重合并
                val fragmentText = fragments.joinToString("\n---\n") { it.content }
                val messages = listOf(
                    AiMessage(AiMessage.Role.SYSTEM, "你是记忆整理助手。分析以下片段记忆，找出重复或相似的内容，将它们合并为更精炼的条目。输出格式：每条一行，不要编号。如果内容无重复，原样输出。"),
                    AiMessage(AiMessage.Role.USER, fragmentText)
                )
                val result = collectAiResult(messages)
                if (result is AiResult.Complete) {
                    val mergedLines = result.content.lines().filter { it.isNotBlank() }
                    // 先保存合并后的，再删除旧片段（防止崩溃时数据丢失）
                    mergedLines.forEach { line ->
                        memoryManager.saveFragment(line.trim(), importance = 0.6f)
                    }
                    val oldIds = fragments.map { it.id }
                    memoryManager.deleteFragments(oldIds)
                    DreamPhaseResult(DreamPhaseEnum.LIGHT_SLEEP, "合并了 ${fragments.size} 条片段为 ${mergedLines.size} 条")
                } else if (result is AiResult.Error) {
                    DreamPhaseResult(DreamPhaseEnum.LIGHT_SLEEP, "浅睡失败: ${result.message}", failed = true, errorMessage = result.message)
                } else {
                    DreamPhaseResult(DreamPhaseEnum.LIGHT_SLEEP, "浅睡未完成")
                }
            }
        } catch (e: Exception) {
            DreamPhaseResult(DreamPhaseEnum.LIGHT_SLEEP, "浅睡异常: ${e.message}", failed = true, errorMessage = e.message)
        }
    }

    /** 深睡：将重要片段提炼为每日摘要 */
    private suspend fun runDeepSleep(): DreamPhaseResult {
        return try {
            val fragments = memoryManager.getRecentFragments(RECENT_DAYS)
            if (fragments.isEmpty()) {
                DreamPhaseResult(DreamPhaseEnum.DEEP_SLEEP, "无片段记忆需要提炼")
            } else {
                val fragmentText = fragments.joinToString("\n---\n") { it.content }
                val messages = listOf(
                    AiMessage(AiMessage.Role.SYSTEM, "你是记忆整理助手。将以下片段记忆提炼为简洁的每日摘要，保留最重要的信息。输出摘要文本，不要编号。"),
                    AiMessage(AiMessage.Role.USER, fragmentText)
                )
                val result = collectAiResult(messages)
                if (result is AiResult.Complete && result.content.isNotBlank()) {
                    // 保存每日记忆
                    memoryManager.saveDaily(result.content.trim(), importance = 0.7f)
                    // 保存成功后再删除已合并的片段
                    val mergedIds = fragments.map { it.id }
                    memoryManager.deleteFragments(mergedIds)
                    DreamPhaseResult(DreamPhaseEnum.DEEP_SLEEP, "提炼了 ${fragments.size} 条片段为每日摘要")
                } else if (result is AiResult.Error) {
                    DreamPhaseResult(DreamPhaseEnum.DEEP_SLEEP, "深睡失败: ${result.message}", failed = true, errorMessage = result.message)
                } else {
                    DreamPhaseResult(DreamPhaseEnum.DEEP_SLEEP, "深睡未完成")
                }
            }
        } catch (e: Exception) {
            DreamPhaseResult(DreamPhaseEnum.DEEP_SLEEP, "深睡异常: ${e.message}", failed = true, errorMessage = e.message)
        }
    }

    /** REM：将多日记忆整合为根记忆 */
    private suspend fun runRemSleep(): DreamPhaseResult {
        return try {
            val dailyMemories = memoryManager.getAllDailyMemories()
            val rootMemory = memoryManager.getRootMemory()
            if (dailyMemories.isEmpty() && rootMemory.isBlank()) {
                DreamPhaseResult(DreamPhaseEnum.REM, "无记忆需要整合")
            } else {
                val dailyText = dailyMemories.joinToString("\n---\n") { it.content }
                val contextText = if (rootMemory.isNotBlank()) {
                    "## 当前根记忆\n$rootMemory\n\n## 每日记忆\n$dailyText"
                } else {
                    "## 每日记忆\n$dailyText"
                }
                val messages = listOf(
                    AiMessage(AiMessage.Role.SYSTEM, "你是记忆整理助手。将以下每日记忆整合到根记忆中。输出完整的更新后根记忆内容。保留原有重要信息，融入新的关键信息，删除过时内容。"),
                    AiMessage(AiMessage.Role.USER, contextText)
                )
                val result = collectAiResult(messages)
                if (result is AiResult.Complete && result.content.isNotBlank()) {
                    memoryManager.updateRootMemory(result.content.trim())
                    DreamPhaseResult(DreamPhaseEnum.REM, "整合了 ${dailyMemories.size} 条每日记忆到根记忆")
                } else if (result is AiResult.Error) {
                    DreamPhaseResult(DreamPhaseEnum.REM, "REM 失败: ${result.message}", failed = true, errorMessage = result.message)
                } else {
                    DreamPhaseResult(DreamPhaseEnum.REM, "REM 未完成")
                }
            }
        } catch (e: Exception) {
            DreamPhaseResult(DreamPhaseEnum.REM, "REM 异常: ${e.message}", failed = true, errorMessage = e.message)
        }
    }

    /**
     * 收集 AI 流式响应的最终结果
     * 过滤 Loading/Stream 中间状态，只返回 Complete 或 Error
     */
    private suspend fun collectAiResult(messages: List<AiMessage>): AiResult {
        return aiService.chat(messages)
            .first { it is AiResult.Complete || it is AiResult.Error }
    }
}

/**
 * 梦境阶段执行结果
 */
data class DreamPhaseResult(
    val phase: DreamPhaseEnum,
    val message: String,
    val failed: Boolean = false,
    val errorMessage: String? = null
)

/**
 * 梦境周期整体结果
 */
data class DreamResult(
    val phaseResults: List<DreamPhaseResult>,
    val failed: Boolean = false,
    val errorMessage: String? = null
)
