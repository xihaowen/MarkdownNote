package com.mdnote.markdownnote.data.ai

import com.mdnote.markdownnote.data.ai.anthropic.AnthropicProvider
import com.mdnote.markdownnote.data.ai.gemini.GeminiProvider
import com.mdnote.markdownnote.data.ai.openai.OpenAiProvider
import com.mdnote.markdownnote.data.ai.memory.MemoryManager
import com.mdnote.markdownnote.data.ai.soul.SoulManager
import com.mdnote.markdownnote.di.SseClient
import com.mdnote.markdownnote.domain.model.AiMessage
import com.mdnote.markdownnote.util.SettingsManager
import com.mdnote.markdownnote.util.toAiConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * AI 服务实现
 * 根据用户配置选择 Provider，优先流式响应，失败时降级为非流式
 */
class AiServiceImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
    @param:SseClient private val sseClient: OkHttpClient,
    private val soulManager: SoulManager,
    private val memoryManager: MemoryManager,
    private val settingsManager: SettingsManager
) : AiService {

    private val openAiProvider = OpenAiProvider(okHttpClient, sseClient)
    private val anthropicProvider = AnthropicProvider(okHttpClient, sseClient)
    private val geminiProvider = GeminiProvider(okHttpClient, sseClient)

    override fun scan(content: String): Flow<AiResult> = flow {
        val config = settingsManager.settings.first().toAiConfig()
        val systemPrompt = buildSystemPrompt(
            methodPrompt = "你是 Markdown 笔记分析助手。分析笔记内容，总结要点，指出语法、结构或逻辑问题。用简洁的列表格式输出。",
            noteContext = content
        )
        val messages = listOf(
            AiMessage(AiMessage.Role.SYSTEM, systemPrompt),
            AiMessage(AiMessage.Role.USER, "请分析以下笔记内容")
        )
        emitAll(executeWithConfig(messages, config))
    }

    override fun modify(content: String, instruction: String, selection: String?): Flow<AiResult> = flow {
        val config = settingsManager.settings.first().toAiConfig()
        val systemPrompt = buildSystemPrompt(
            methodPrompt = "你是 Markdown 编辑助手。根据用户指令修改文本，只输出修改后的文本，不要解释。",
            noteContext = content
        )
        val userMessage = if (selection != null) {
            "修改选中文本：\"$selection\"\n指令：$instruction"
        } else {
            "修改全文\n指令：$instruction"
        }
        val messages = listOf(
            AiMessage(AiMessage.Role.SYSTEM, systemPrompt),
            AiMessage(AiMessage.Role.USER, userMessage)
        )
        emitAll(executeWithConfig(messages, config))
    }

    override fun generate(prompt: String, context: String?): Flow<AiResult> = flow {
        val config = settingsManager.settings.first().toAiConfig()
        val systemPrompt = buildSystemPrompt(
            methodPrompt = "你是 Markdown 内容生成助手。根据用户描述生成 Markdown 格式内容。",
            noteContext = context
        )
        val messages = listOf(
            AiMessage(AiMessage.Role.SYSTEM, systemPrompt),
            AiMessage(AiMessage.Role.USER, prompt)
        )
        emitAll(executeWithConfig(messages, config))
    }

    override fun explain(selectedText: String, context: String): Flow<AiResult> = flow {
        val config = settingsManager.settings.first().toAiConfig()
        val systemPrompt = buildSystemPrompt(
            methodPrompt = "你是 Markdown 解释助手。清晰解释选中文本的含义，结合上下文给出易懂的解释。",
            noteContext = context
        )
        val messages = listOf(
            AiMessage(AiMessage.Role.SYSTEM, systemPrompt),
            AiMessage(AiMessage.Role.USER, "请解释以下内容：\"$selectedText\"")
        )
        emitAll(executeWithConfig(messages, config))
    }

    override fun chat(messages: List<AiMessage>): Flow<AiResult> = flow {
        val config = settingsManager.settings.first().toAiConfig()
        val methodPrompt = "你是 Markdown 笔记助手，回答用户关于笔记内容的问题。"
        val allMessages = listOf(AiMessage(AiMessage.Role.SYSTEM, methodPrompt)) + messages
        emitAll(executeWithConfig(allMessages, config))
    }

    /**
     * 构建系统提示词
     * 组合 SOUL 人格 + 方法专属提示 + 记忆上下文 + 笔记上下文
     */
    private suspend fun buildSystemPrompt(methodPrompt: String, noteContext: String?): String {
        val parts = mutableListOf(methodPrompt)
        // 注入记忆上下文
        val memoryContext = memoryManager.buildMemoryContext(noteContext ?: methodPrompt)
        if (memoryContext.isNotBlank()) {
            parts.add(memoryContext)
        }
        // 注入 SOUL 人格（最高优先级，放在最前面）
        val soulPrompt = soulManager.buildSoulPrompt()
        if (soulPrompt.isNotBlank()) {
            parts.add(0, soulPrompt)
        }
        if (!noteContext.isNullOrBlank()) {
            parts.add("\n## 当前笔记内容\n$noteContext")
        }
        return parts.joinToString("\n")
    }

    /**
     * 根据 Provider 类型选择对应实现，执行请求
     * 直接使用非流式请求（更可靠），如果成功则尝试流式请求作为优化
     */
    private fun executeWithConfig(messages: List<AiMessage>, config: AiConfig): Flow<AiResult> = flow {
        val provider: AiProvider = when (config.providerType) {
            ProviderType.OPENAI_COMPATIBLE -> openAiProvider
            ProviderType.ANTHROPIC -> anthropicProvider
            ProviderType.GEMINI -> geminiProvider
        }

        try {
            emitAll(provider.chat(messages, config))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(AiResult.Error("请求失败：${e.message}", retryable = true))
        }
    }.flowOn(Dispatchers.IO)
}
