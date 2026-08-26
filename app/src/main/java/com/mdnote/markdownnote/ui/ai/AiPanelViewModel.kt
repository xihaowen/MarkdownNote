package com.mdnote.markdownnote.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdnote.markdownnote.data.ai.AiResult
import com.mdnote.markdownnote.data.ai.AiService
import com.mdnote.markdownnote.data.ai.memory.MemoryManager
import com.mdnote.markdownnote.domain.model.AiMessage
import com.mdnote.markdownnote.util.SettingsManager
import com.mdnote.markdownnote.util.toAiConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AI 面板模式
 */
enum class AiPanelMode { OPERATION, CHAT }

/**
 * 聊天消息角色
 */
enum class ChatRole { USER, AI }

/**
 * 聊天消息
 */
data class ChatMessage(
    val role: ChatRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * AI 操作类型
 */
enum class AiActionType { SCAN, MODIFY, GENERATE, EXPLAIN, CHAT }

/**
 * 待执行操作（用于 apply/reject）
 */
data class PendingAction(
    val type: AiActionType,
    val content: String,
    val originalContent: String? = null
)

/**
 * AI 对话面板 ViewModel
 * 管理对话消息、流式输出、操作确认等状态
 */
@HiltViewModel
class AiPanelViewModel @Inject constructor(
    private val aiService: AiService,
    private val settingsManager: SettingsManager,
    private val memoryManager: MemoryManager
) : ViewModel() {

    private var currentRequestJob: Job? = null
    private var currentActionType: AiActionType? = null
    private var currentOriginalContent: String? = null

    /** 对话消息列表 */
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    /** 当前流式输出累积的文本 */
    private val _currentResult = MutableStateFlow("")
    val currentResult: StateFlow<String> = _currentResult.asStateFlow()

    /** 是否正在加载（等待 AI 响应） */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** 当前面板模式 */
    private val _mode = MutableStateFlow(AiPanelMode.OPERATION)
    val mode: StateFlow<AiPanelMode> = _mode.asStateFlow()

    /** 待确认的操作（用于 apply/reject） */
    private val _pendingAction = MutableStateFlow<PendingAction?>(null)
    val pendingAction: StateFlow<PendingAction?> = _pendingAction.asStateFlow()

    /** 是否正在流式接收 */
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    /**
     * 发送扫描请求
     * 分析笔记内容，总结要点
     */
    fun sendScan(content: String) {
        _mode.value = AiPanelMode.OPERATION
        addUserMessage("扫描分析笔记")
        executeAiFlow(
            actionType = AiActionType.SCAN,
            flow = aiService.scan(content),
            pendingContent = content
        )
    }

    /**
     * 发送修改请求
     * 根据指令修改笔记内容
     */
    fun sendModify(content: String, instruction: String, selection: String?) {
        _mode.value = AiPanelMode.OPERATION
        val displayText = if (selection != null) {
            "修改选中文本：$instruction"
        } else {
            "编辑要求：$instruction"
        }
        addUserMessage(displayText)
        executeAiFlow(
            actionType = AiActionType.MODIFY,
            flow = aiService.modify(content, instruction, selection),
            pendingContent = selection ?: content,
            originalContent = selection
        )
    }

    /**
     * 发送生成请求
     * 根据提示生成 Markdown 内容
     */
    fun sendGenerate(prompt: String, context: String?) {
        _mode.value = AiPanelMode.OPERATION
        addUserMessage("生成：$prompt")
        executeAiFlow(
            actionType = AiActionType.GENERATE,
            flow = aiService.generate(prompt, context),
            pendingContent = prompt
        )
    }

    /**
     * 发送解释请求
     * 解释选中的文本
     */
    fun sendExplain(selectedText: String, context: String) {
        _mode.value = AiPanelMode.OPERATION
        val preview = if (selectedText.length > 30) {
            selectedText.take(30) + "..."
        } else {
            selectedText
        }
        addUserMessage("解释：\"$preview\"")
        executeAiFlow(
            actionType = AiActionType.EXPLAIN,
            flow = aiService.explain(selectedText, context),
            pendingContent = selectedText,
            originalContent = selectedText
        )
    }

    /**
     * 发送聊天消息
     * 自由对话模式
     */
    fun sendChat(message: String) {
        _mode.value = AiPanelMode.CHAT
        addUserMessage(message)
        // 构建完整对话历史给 AiService
        val chatMessages = _messages.value.map { msg ->
            AiMessage(
                role = if (msg.role == ChatRole.USER) AiMessage.Role.USER else AiMessage.Role.ASSISTANT,
                content = msg.content
            )
        }
        executeAiFlow(
            actionType = AiActionType.CHAT,
            flow = aiService.chat(chatMessages),
            pendingContent = message
        )
    }

    /**
     * 确认应用 AI 结果
     * 将 currentResult 作为最终结果返回给调用方
     */
    fun applyResult() {
        // 清除待确认状态
        _pendingAction.value = null
        _currentResult.value = ""
    }

    /**
     * 拒绝 AI 结果
     * 丢弃当前结果
     */
    fun rejectResult() {
        _pendingAction.value = null
        _currentResult.value = ""
    }

    /**
     * 清空面板
     * 重置所有状态
     */
    fun clearPanel() {
        currentRequestJob?.cancel()
        currentRequestJob = null
        currentActionType = null
        currentOriginalContent = null
        _messages.value = emptyList()
        _currentResult.value = ""
        _isLoading.value = false
        _isStreaming.value = false
        _pendingAction.value = null
        _mode.value = AiPanelMode.OPERATION
    }

    /**
     * 打断当前 AI 请求
     * 保留已生成的部分内容，方便用户继续使用
     */
    fun interruptGeneration() {
        val requestJob = currentRequestJob ?: return
        if (!requestJob.isActive) return

        val partialResult = _currentResult.value
        val actionType = currentActionType
        val originalContent = currentOriginalContent

        requestJob.cancel()
        currentRequestJob = null
        currentActionType = null
        currentOriginalContent = null
        _isLoading.value = false
        _isStreaming.value = false

        if (partialResult.isNotBlank() && actionType != null) {
            addAiMessage(partialResult)
            _pendingAction.value = PendingAction(
                type = actionType,
                content = partialResult,
                originalContent = originalContent
            )
        }
        _currentResult.value = ""
    }

    /**
     * 添加用户消息到列表
     */
    private fun addUserMessage(text: String) {
        _messages.value = _messages.value + ChatMessage(role = ChatRole.USER, content = text)
    }

    /**
     * 添加 AI 消息到列表
     */
    private fun addAiMessage(text: String) {
        _messages.value = _messages.value + ChatMessage(role = ChatRole.AI, content = text)
    }

    /**
     * 执行 AI 流式请求的通用逻辑
     * 处理 Loading/Stream/Complete/Error 四种状态
     */
    private fun executeAiFlow(
        actionType: AiActionType,
        flow: kotlinx.coroutines.flow.Flow<AiResult>,
        pendingContent: String,
        originalContent: String? = null
    ) {
        currentRequestJob?.cancel()
        currentActionType = actionType
        currentOriginalContent = originalContent

        var launchedJob: Job? = null
        launchedJob = viewModelScope.launch {
            try {
                _isLoading.value = true
                _isStreaming.value = false
                _currentResult.value = ""
                _pendingAction.value = null

                flow.collect { result ->
                    when (result) {
                        is AiResult.Loading -> {
                            _isLoading.value = true
                        }
                        is AiResult.Stream -> {
                            _isLoading.value = false
                            _isStreaming.value = true
                            _currentResult.value = result.accumulated
                        }
                        is AiResult.Complete -> {
                            _isLoading.value = false
                            _isStreaming.value = false
                            _currentResult.value = result.content
                            // 流式结束后，将累积文本添加为 AI 消息
                            addAiMessage(result.content)
                            // 操作模式和聊天模式下都设置待确认操作，允许用户将结果应用到文档
                            _pendingAction.value = PendingAction(
                                type = actionType,
                                content = result.content,
                                originalContent = originalContent
                            )
                            // 提取记忆（异步，不阻塞 UI）
                            val conversationContent = _messages.value.joinToString("\n") { msg ->
                                "${msg.role.name}: ${msg.content}"
                            }
                            viewModelScope.launch { extractMemory(conversationContent) }
                        }
                        is AiResult.Error -> {
                            _isLoading.value = false
                            _isStreaming.value = false
                            val errorMsg = result.message
                            _currentResult.value = ""
                            addAiMessage("出错了：$errorMsg")
                        }
                    }
                }
            } finally {
                if (currentRequestJob === launchedJob) {
                    currentRequestJob = null
                    currentActionType = null
                    currentOriginalContent = null
                }
                _isLoading.value = false
                _isStreaming.value = false
            }
        }
        currentRequestJob = launchedJob
    }

    /**
     * 从对话内容中提取记忆片段
     * 对话内容超过 100 字符时，调用 AI 提取关键事实、偏好或知识点
     */
    private suspend fun extractMemory(conversationContent: String) {
        if (conversationContent.length < 100) return
        val config = settingsManager.settings.first().toAiConfig()
        if (!config.isValid) return

        val messages = listOf(
            AiMessage(AiMessage.Role.SYSTEM, "从以下对话中提取关键事实、偏好或知识点。每条一行，不要编号，不要解释。如果没有值得记住的信息，输出空。"),
            AiMessage(AiMessage.Role.USER, conversationContent)
        )
        try {
            val result = aiService.chat(messages).first { it is AiResult.Complete || it is AiResult.Error }
            if (result is AiResult.Complete && result.content.isNotBlank()) {
                val fragments = result.content.lines().filter { it.isNotBlank() }
                memoryManager.saveExtractedFragments(fragments)
            }
        } catch (_: Exception) { }
    }
}
