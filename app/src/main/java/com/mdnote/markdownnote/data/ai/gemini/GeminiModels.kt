package com.mdnote.markdownnote.data.ai.gemini

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Gemini generateContent 请求体
 * 字段命名依据 Google AI 官方文档：
 * - generationConfig 为 camelCase（官方 JSON 即如此）
 * - system_instruction 为 snake_case，需 @SerialName 显式指定
 */
@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    @SerialName("generationConfig") val generationConfig: GeminiGenerationConfig,
    @SerialName("system_instruction") val systemInstruction: GeminiSystemInstruction? = null
)

@Serializable
data class GeminiGenerationConfig(
    val temperature: Float = 0.7f,
    val maxOutputTokens: Int
)

/** contents 中的消息体，role 取值 user / model */
@Serializable
data class GeminiContent(
    val role: String,
    val parts: List<GeminiPart>
)

/** system_instruction 不含 role，仅 parts */
@Serializable
data class GeminiSystemInstruction(
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(val text: String)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList()
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null
)
