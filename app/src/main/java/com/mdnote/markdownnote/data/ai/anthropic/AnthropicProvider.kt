package com.mdnote.markdownnote.data.ai.anthropic

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

class AnthropicProvider(
    private val client: OkHttpClient,
    private val sseClient: OkHttpClient
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

        val systemMessage = messages.find { it.role == AiMessage.Role.SYSTEM }
        val chatMessages = messages.filter { it.role != AiMessage.Role.SYSTEM }
            .map { AnthropicMessage(it.role.name.lowercase(), it.content) }

        val requestBody = AnthropicRequest(
            model = config.model,
            messages = chatMessages,
            max_tokens = config.maxTokens,
            temperature = config.temperature,
            stream = false,
            system = systemMessage?.content
        )
        val bodyJson = json.encodeToString(AnthropicRequest.serializer(), requestBody)

        val request = Request.Builder()
            .url("${config.baseUrl.trimEnd('/')}/messages")
            .addHeader("x-api-key", config.apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
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
                val parsed = json.decodeFromString(AnthropicResponse.serializer(), responseBody)
                val content = parsed.content.firstOrNull()?.text ?: ""
                emit(AiResult.Complete(content))
            }
        } catch (e: IOException) {
            emit(AiResult.Error("网络错误: ${e.message}", retryable = true))
        } catch (e: kotlinx.serialization.SerializationException) {
            emit(AiResult.Error("响应解析失败: ${e.message}", retryable = false))
        }
    }.flowOn(Dispatchers.IO)
}
