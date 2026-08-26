package com.mdnote.markdownnote.data.ai.anthropic

import com.mdnote.markdownnote.data.ai.AiConfig
import kotlinx.serialization.Serializable

@Serializable
data class AnthropicRequest(
    val model: String,
    val messages: List<AnthropicMessage>,
    val max_tokens: Int = AiConfig.DEFAULT_MAX_TOKENS,
    val temperature: Float = 0.7f,
    val stream: Boolean = true,
    val system: String? = null
)

@Serializable
data class AnthropicMessage(
    val role: String,
    val content: String
)

@Serializable
data class AnthropicResponse(
    val content: List<AnthropicContentBlock> = emptyList()
)

@Serializable
data class AnthropicContentBlock(
    val type: String = "",
    val text: String = ""
)
