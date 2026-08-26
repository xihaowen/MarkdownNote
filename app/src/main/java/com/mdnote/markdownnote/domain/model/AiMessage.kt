package com.mdnote.markdownnote.domain.model

data class AiMessage(
    val role: Role,
    val content: String
) {
    enum class Role {
        SYSTEM, USER, ASSISTANT
    }
}
