package com.mdnote.markdownnote.data.ai.gemini

import com.mdnote.markdownnote.data.ai.AiConfig
import com.mdnote.markdownnote.data.ai.AiProvider
import com.mdnote.markdownnote.data.ai.AiResult
import com.mdnote.markdownnote.domain.model.AiMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Google Gemini 原生协议 Provider
 *
 * 依据 Google AI 官方文档：
 * - 端点：{baseUrl}/models/{model}:generateContent
 * - 认证：x-goog-api-key header
 * - system 消息放入 system_instruction（仅 parts），其余放入 contents（role: user/model）
 */
class GeminiProvider(
    private val client: OkHttpClient,
    private val sseClient: OkHttpClient   // 与现有 Provider 构造对齐，当前 chat 非流式不使用
) : AiProvider {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override fun chat(
        messages: List<AiMessage>,
        config: AiConfig
    ): Flow<AiResult> = flow {
        if (!config.isValid) {
            emit(AiResult.Error("AI 配置不完整，请检查设置", retryable = false))
            return@flow
        }
        val systemMsg = messages.find { it.role == AiMessage.Role.SYSTEM }
        val contents = messages.filter { it.role != AiMessage.Role.SYSTEM }
            .map { GeminiContent(role = mapRole(it.role), parts = listOf(GeminiPart(it.content))) }
        val requestBody = GeminiRequest(
            contents = contents,
            generationConfig = GeminiGenerationConfig(
                temperature = config.temperature,
                maxOutputTokens = config.safeMaxTokens
            ),
            systemInstruction = systemMsg?.let {
                GeminiSystemInstruction(parts = listOf(GeminiPart(it.content)))
            }
        )
        val bodyJson = json.encodeToString(GeminiRequest.serializer(), requestBody)
        // generateContent 端点，认证用 x-goog-api-key header
        val url = "${config.baseUrl.trimEnd('/')}/models/${config.model}:generateContent"
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("x-goog-api-key", config.apiKey)
            .post(bodyJson.toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                if (!response.isSuccessful || responseBody == null) {
                    val retryable = response.code in 500..599
                    emit(AiResult.Error("API 错误 (${response.code}): $responseBody", retryable = retryable))
                    return@flow
                }
                val parsed = json.decodeFromString(GeminiResponse.serializer(), responseBody)
                val content = parsed.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                emit(AiResult.Complete(content))
            }
        } catch (e: IOException) {
            emit(AiResult.Error("网络错误: ${e.message}", retryable = true))
        } catch (e: kotlinx.serialization.SerializationException) {
            emit(AiResult.Error("响应解析失败: ${e.message}", retryable = false))
        }
    }.flowOn(Dispatchers.IO)

    /** AiMessage.Role 映射到 Gemini role（ASSISTANT -> model） */
    private fun mapRole(role: AiMessage.Role): String = when (role) {
        AiMessage.Role.USER -> "user"
        AiMessage.Role.ASSISTANT -> "model"
        AiMessage.Role.SYSTEM -> "user"
    }
}
