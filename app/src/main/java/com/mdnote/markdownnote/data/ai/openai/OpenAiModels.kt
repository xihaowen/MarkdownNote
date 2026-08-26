package com.mdnote.markdownnote.data.ai.openai

import com.mdnote.markdownnote.data.ai.AiConfig
import kotlinx.serialization.Serializable

@Serializable
data class OpenAiRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val stream: Boolean = true,
    val max_tokens: Int = AiConfig.DEFAULT_MAX_TOKENS,
    val temperature: Float = 0.7f
)

@Serializable
data class OpenAiMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenAiResponse(
    val choices: List<OpenAiChoice> = emptyList()
)

@Serializable
data class OpenAiChoice(
    val message: OpenAiMessage? = null,
    val delta: OpenAiMessage? = null
)

@Serializable
data class OpenAiStreamChunk(
    val choices: List<OpenAiChoice> = emptyList()
)
