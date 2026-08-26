package com.mdnote.markdownnote.data.ai.soul

import com.mdnote.markdownnote.util.SettingsManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SoulManager @Inject constructor(
    private val settingsManager: SettingsManager
) {
    /** 获取当前 SOUL 人格内容 */
    suspend fun getSoulContent(): String {
        return settingsManager.settings.first().aiSoulContent
    }

    /** 保存 SOUL 人格内容 */
    suspend fun saveSoulContent(content: String) {
        settingsManager.setAiSoulContent(content)
    }

    /** 构建 SOUL 注入文本（用于 system prompt） */
    suspend fun buildSoulPrompt(): String {
        val soulContent = getSoulContent()
        return if (soulContent.isNotBlank()) {
            "## 你的人格定义\n$soulContent\n"
        } else {
            ""
        }
    }
}
