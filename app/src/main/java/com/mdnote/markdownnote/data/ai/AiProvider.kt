package com.mdnote.markdownnote.data.ai

import com.mdnote.markdownnote.domain.model.AiMessage
import kotlinx.coroutines.flow.Flow

/**
 * AI Provider 统一接口
 * 所有 AI 服务提供商（OpenAI、Anthropic 等）均实现此接口
 */
interface AiProvider {
    /**
     * 非流式对话
     * 一次性返回 AiResult.Complete
     */
    fun chat(messages: List<AiMessage>, config: AiConfig): Flow<AiResult>
}
