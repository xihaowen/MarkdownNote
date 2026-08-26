package com.mdnote.markdownnote.data.ai

sealed class AiResult {
    data class Loading(val message: String = "") : AiResult()
    data class Stream(val chunk: String, val accumulated: String) : AiResult()
    data class Complete(val content: String) : AiResult()
    data class Error(val message: String, val retryable: Boolean) : AiResult()
}
