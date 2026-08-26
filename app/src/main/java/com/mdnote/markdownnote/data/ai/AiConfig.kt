package com.mdnote.markdownnote.data.ai

import java.net.URI

enum class ProviderType(val displayName: String) {
    OPENAI_COMPATIBLE("OpenAI 兼容"),
    ANTHROPIC("Anthropic"),
    GEMINI("Google Gemini")
}

data class AiConfig(
    val providerType: ProviderType = ProviderType.OPENAI_COMPATIBLE,
    val apiKey: String = "",
    val baseUrl: String = "",
    val model: String = "",
    val maxTokens: Int = DEFAULT_MAX_TOKENS,
    val temperature: Float = 0.7f
) {
    val isValid: Boolean
        get() {
            val endpoint = runCatching { URI(baseUrl.trim()) }.getOrNull()
            return apiKey.isNotBlank() &&
                model.isNotBlank() &&
                endpoint?.scheme.equals("https", ignoreCase = true) &&
                !endpoint?.host.isNullOrBlank()
        }

    /** 安全的 maxTokens，限制在 1..2097152 范围内 */
    val safeMaxTokens: Int get() = maxTokens.coerceIn(1, 2097152)

    companion object {
        const val DEFAULT_MAX_TOKENS = 100352

        fun defaultBaseUrl(providerType: ProviderType) = when (providerType) {
            ProviderType.OPENAI_COMPATIBLE -> "https://api.openai.com/v1"
            ProviderType.ANTHROPIC -> "https://api.anthropic.com/v1"
            ProviderType.GEMINI -> "https://generativelanguage.googleapis.com/v1beta"
        }
    }
}
