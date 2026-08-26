package com.mdnote.markdownnote.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdnote.markdownnote.data.ai.AiConfig
import com.mdnote.markdownnote.data.ai.AiModelPreset
import com.mdnote.markdownnote.data.ai.AiPlanPreset
import com.mdnote.markdownnote.data.ai.AiProviderPreset
import com.mdnote.markdownnote.data.ai.AiProviderPresets
import com.mdnote.markdownnote.data.ai.AiResult
import com.mdnote.markdownnote.data.ai.AiService
import com.mdnote.markdownnote.data.ai.ProviderType
import com.mdnote.markdownnote.data.ai.TemperatureStyle
import com.mdnote.markdownnote.data.repository.TodoRepository
import com.mdnote.markdownnote.domain.model.AiMessage
import com.mdnote.markdownnote.util.AppSettings
import com.mdnote.markdownnote.util.EditorFontSize
import com.mdnote.markdownnote.util.SettingsManager
import com.mdnote.markdownnote.util.toAiConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 测试连接的状态 */
sealed class ConnectionState {
    data object Idle : ConnectionState()
    data object Testing : ConnectionState()
    data class Success(val message: String) : ConnectionState()
    data class Failed(val message: String) : ConnectionState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val todoRepository: TodoRepository,
    private val aiService: AiService,
    val aiProviderPresets: AiProviderPresets
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsManager.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setEditorFontSize(fontSize: EditorFontSize) {
        viewModelScope.launch {
            settingsManager.setEditorFontSize(fontSize)
        }
    }

    fun setDarkMode(dark: Boolean) {
        viewModelScope.launch {
            settingsManager.setDarkMode(dark)
        }
    }

    fun setFollowSystemTheme(follow: Boolean) {
        viewModelScope.launch {
            settingsManager.setFollowSystemTheme(follow)
        }
    }

    fun toggleTypewriterMode() {
        viewModelScope.launch {
            settingsManager.toggleTypewriterMode()
        }
    }

    fun toggleFocusMode() {
        viewModelScope.launch {
            settingsManager.toggleFocusMode()
        }
    }

    fun clearCompletedTodos() {
        viewModelScope.launch {
            todoRepository.clearAllCompleted()
        }
    }

    // AI 助手设置方法

    fun setAiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setAiEnabled(enabled)
        }
    }

    fun setAiProviderType(type: ProviderType) {
        viewModelScope.launch {
            settingsManager.setAiProviderType(type)
            // 切换供应商时自动填充默认 Base URL
            settingsManager.setAiBaseUrl(AiConfig.defaultBaseUrl(type))
        }
    }

    fun setAiApiKey(key: String) {
        viewModelScope.launch {
            settingsManager.setAiApiKey(key)
        }
    }

    fun setAiBaseUrl(url: String) {
        viewModelScope.launch {
            settingsManager.setAiBaseUrl(url)
        }
    }

    fun setAiModel(model: String) {
        viewModelScope.launch {
            settingsManager.setAiModel(model)
        }
    }

    fun setAiMaxTokens(tokens: Int) {
        viewModelScope.launch {
            settingsManager.setAiMaxTokens(tokens)
        }
    }

    fun setAiTemperature(temp: Float) {
        viewModelScope.launch {
            settingsManager.setAiTemperature(temp)
        }
    }

    fun setAiMemoryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setAiMemoryEnabled(enabled)
        }
    }

    fun setAiDreamEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setAiDreamEnabled(enabled)
        }
    }
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    /** 选择预置服务商：写入 presetId、协议、baseUrl；单模型服务商自动选中模型 */
    fun setProvider(preset: AiProviderPreset) {
        viewModelScope.launch {
            settingsManager.setAiProviderPresetId(preset.id)
            // 单套餐服务商连 plan 一起选
            if (preset.plans.size == 1) {
                val plan = preset.plans.first()
                settingsManager.setAiPlanId(plan.id)
                settingsManager.setAiProviderType(plan.providerType)
                settingsManager.setAiBaseUrl(plan.baseUrl)
                if (plan.models.size == 1) {
                    val m = plan.models.first()
                    settingsManager.setAiModel(m.id)
                    settingsManager.setAiMaxTokens(m.maxOutputTokens)
                }
            }
            _connectionState.value = ConnectionState.Idle
        }
    }

    /** 选择套餐：写入 planId、协议、baseUrl；单模型套餐自动选中 */
    fun setPlan(plan: AiPlanPreset) {
        viewModelScope.launch {
            settingsManager.setAiPlanId(plan.id)
            settingsManager.setAiProviderType(plan.providerType)
            settingsManager.setAiBaseUrl(plan.baseUrl)
            if (plan.models.size == 1) {
                val m = plan.models.first()
                settingsManager.setAiModel(m.id)
                settingsManager.setAiMaxTokens(m.maxOutputTokens)
            }
            _connectionState.value = ConnectionState.Idle
        }
    }

    /** 选择模型：写入模型名与输出上限 */
    fun setModel(model: AiModelPreset) {
        viewModelScope.launch {
            settingsManager.setAiModel(model.id)
            settingsManager.setAiMaxTokens(model.maxOutputTokens)
            _connectionState.value = ConnectionState.Idle
        }
    }

    /** 选择回答风格档位 */
    fun setTemperatureStyle(style: TemperatureStyle) {
        viewModelScope.launch {
            settingsManager.setAiTemperature(style.temperature)
        }
    }

    /** 测试连接：发一条最小消息验证配置是否可用 */
    fun testConnection() {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Testing
            val settings = settingsManager.settings.first()
            if (!settings.toAiConfig().isValid) {
                _connectionState.value = ConnectionState.Failed("配置不完整，请先选服务商、模型并填入密钥")
                return@launch
            }
            val testMessages = listOf(AiMessage(AiMessage.Role.USER, "你好"))
            try {
                val result = aiService.chat(testMessages)
                    .firstOrNull { it is AiResult.Complete || it is AiResult.Error }
                _connectionState.value = when (result) {
                    is AiResult.Complete -> ConnectionState.Success("连接正常，模型可用")
                    is AiResult.Error -> parseConnectionError(result.message)
                    null -> ConnectionState.Failed("未收到响应")
                    else -> ConnectionState.Idle
                }
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Failed("请求异常：${e.message}")
            }
        }
    }

    /** 把原始错误信息转成人话提示 */
    private fun parseConnectionError(raw: String): ConnectionState.Failed {
        val msg = raw.lowercase()
        return when {
            "401" in msg -> ConnectionState.Failed("密钥无效或已过期，请检查")
            "404" in msg -> ConnectionState.Failed("地址错误或网络不通")
            "400" in msg && "model" in msg -> ConnectionState.Failed("模型名不被支持，请重新选择")
            else -> ConnectionState.Failed(raw)
        }
    }
}
