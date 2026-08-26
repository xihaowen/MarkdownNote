package com.mdnote.markdownnote.util

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import com.mdnote.markdownnote.data.ai.AiConfig
import com.mdnote.markdownnote.data.ai.AiProviderPresets
import com.mdnote.markdownnote.data.ai.ProviderType
import com.mdnote.markdownnote.data.ai.TemperatureStyle
import javax.inject.Inject
import javax.inject.Singleton

// DataStore 扩展
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * 编辑器字体选项
 */
enum class EditorFont(val displayName: String, val fontName: String) {
    SYSTEM("系统默认", "system"),
    JETBRAINS_MONO("JetBrains Mono", "jetbrains_mono"),
    FIRA_CODE("Fira Code", "fira_code"),
    ROBOTO_MONO("Roboto Mono", "roboto_mono")
}

/**
 * 编辑器字号
 */
enum class EditorFontSize(val displayName: String, val size: Int) {
    SMALL("小", 14),
    MEDIUM("中", 16),
    LARGE("大", 18),
    EXTRA_LARGE("超大", 20)
}

/**
 * 应用设置数据类
 */
data class AppSettings(
    // 编辑器设置
    val editorFont: EditorFont = EditorFont.SYSTEM,
    val editorFontSize: EditorFontSize = EditorFontSize.MEDIUM,
    val showLineNumbers: Boolean = false,

    // 显示模式
    val typewriterMode: Boolean = false,        // 打字机模式
    val focusMode: Boolean = false,              // 聚焦模式

    // 主题设置
    val darkMode: Boolean = false,
    val followSystemTheme: Boolean = true,

    // 侧边栏
    val sidebarExpanded: Boolean = true,

    // 渲染引擎
    val useMarkwonEngine: Boolean = true,   // 使用 Markwon 渲染引擎（默认开启）

    // AI 助手设置
    val aiEnabled: Boolean = false,
    val aiProviderType: ProviderType = ProviderType.OPENAI_COMPATIBLE,
    val aiProviderPresetId: String = "deepseek",
    val aiPlanId: String = "standard",
    val aiApiKey: String = "",
    val aiBaseUrl: String = "",
    val aiModel: String = "",
    val aiMaxTokens: Int = AiConfig.DEFAULT_MAX_TOKENS,
    val aiTemperature: Float = 0.7f,
    val aiSoulContent: String = "",
    val aiMemoryEnabled: Boolean = true,
    val aiDreamEnabled: Boolean = true
)

/**
 * 设置管理器
 * 使用 DataStore 持久化应用设置
 * API Key 等敏感信息使用 EncryptedSharedPreferences 加密存储
 */
@Singleton
class SettingsManager @Inject constructor(
    private val context: Context,
    private val aiProviderPresets: AiProviderPresets
) {
    private object PreferencesKeys {
        val EDITOR_FONT = stringPreferencesKey("editor_font")
        val EDITOR_FONT_SIZE = stringPreferencesKey("editor_font_size")
        val SHOW_LINE_NUMBERS = booleanPreferencesKey("show_line_numbers")
        val TYPEWRITER_MODE = booleanPreferencesKey("typewriter_mode")
        val FOCUS_MODE = booleanPreferencesKey("focus_mode")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val FOLLOW_SYSTEM_THEME = booleanPreferencesKey("follow_system_theme")
        val SIDEBAR_EXPANDED = booleanPreferencesKey("sidebar_expanded")
        val USE_MARKWON_ENGINE = booleanPreferencesKey("use_markwon_engine")

        // AI 助手
        val AI_ENABLED = booleanPreferencesKey("ai_enabled")
        val AI_PROVIDER_TYPE = stringPreferencesKey("ai_provider_type")
        val AI_PLAN_ID = stringPreferencesKey("ai_plan_id")
        val AI_PROVIDER_PRESET_ID = stringPreferencesKey("ai_provider_preset_id")
        val AI_BASE_URL = stringPreferencesKey("ai_base_url")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val AI_MAX_TOKENS = intPreferencesKey("ai_max_tokens")
        val AI_TEMPERATURE = floatPreferencesKey("ai_temperature")
        val AI_SOUL_CONTENT = stringPreferencesKey("ai_soul_content")
        val AI_MEMORY_ENABLED = booleanPreferencesKey("ai_memory_enabled")
        val AI_DREAM_ENABLED = booleanPreferencesKey("ai_dream_enabled")

    }

    // 加密 SharedPreferences，用于存储敏感数据
    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secret_settings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    // 用版本号触发 settings Flow 重发，使 UI 能读到最新密钥
    private val apiKeyVersion = MutableStateFlow(0)

    /**
     * 获取设置流
     */
    val settings: Flow<AppSettings> = combine(
        context.dataStore.data,
        apiKeyVersion
    ) { preferences, _ ->
        val defaultProvider = aiProviderPresets.byId(aiProviderPresets.defaults.providerId)
        val defaultPlan = defaultProvider.plans.firstOrNull { it.id == aiProviderPresets.defaults.planId }
            ?: defaultProvider.plans.firstOrNull()
            ?: aiProviderPresets.custom.plans.first()
        val defaultModel = defaultPlan.models.firstOrNull { it.id == aiProviderPresets.defaults.modelId }

        AppSettings(
            editorFont = preferences[PreferencesKeys.EDITOR_FONT]?.let {
                EditorFont.entries.find { font -> font.fontName == it }
            } ?: EditorFont.SYSTEM,
            editorFontSize = preferences[PreferencesKeys.EDITOR_FONT_SIZE]?.let {
                EditorFontSize.entries.find { size -> size.name == it }
            } ?: EditorFontSize.MEDIUM,
            showLineNumbers = preferences[PreferencesKeys.SHOW_LINE_NUMBERS] ?: false,
            typewriterMode = preferences[PreferencesKeys.TYPEWRITER_MODE] ?: false,
            focusMode = preferences[PreferencesKeys.FOCUS_MODE] ?: false,
            darkMode = preferences[PreferencesKeys.DARK_MODE] ?: false,
            followSystemTheme = preferences[PreferencesKeys.FOLLOW_SYSTEM_THEME] ?: true,
            sidebarExpanded = preferences[PreferencesKeys.SIDEBAR_EXPANDED] ?: true,
            useMarkwonEngine = preferences[PreferencesKeys.USE_MARKWON_ENGINE] ?: true,
            aiEnabled = preferences[PreferencesKeys.AI_ENABLED] ?: aiProviderPresets.defaults.aiEnabled,
            aiProviderType = preferences[PreferencesKeys.AI_PROVIDER_TYPE]?.let {
                ProviderType.entries.find { pt -> pt.name == it }
            } ?: defaultPlan.providerType,
            aiProviderPresetId = preferences[PreferencesKeys.AI_PROVIDER_PRESET_ID]
                ?: aiProviderPresets.defaults.providerId,
            aiPlanId = preferences[PreferencesKeys.AI_PLAN_ID]
                ?: aiProviderPresets.defaults.planId,
            aiApiKey = encryptedPrefs.getString(KEY_AI_API_KEY, "") ?: "",
            aiBaseUrl = preferences[PreferencesKeys.AI_BASE_URL] ?: defaultPlan.baseUrl,
            aiModel = preferences[PreferencesKeys.AI_MODEL] ?: (defaultModel?.id ?: ""),
            aiMaxTokens = preferences[PreferencesKeys.AI_MAX_TOKENS]
                ?: (defaultModel?.maxOutputTokens ?: AiConfig.DEFAULT_MAX_TOKENS),
            aiTemperature = preferences[PreferencesKeys.AI_TEMPERATURE]
                ?: TemperatureStyle.valueOf(aiProviderPresets.defaults.temperatureStyle).temperature,
            aiSoulContent = preferences[PreferencesKeys.AI_SOUL_CONTENT] ?: "",
            aiMemoryEnabled = preferences[PreferencesKeys.AI_MEMORY_ENABLED]
                ?: aiProviderPresets.defaults.memoryEnabled,
            aiDreamEnabled = preferences[PreferencesKeys.AI_DREAM_ENABLED]
                ?: aiProviderPresets.defaults.dreamEnabled
        )
    }

    /**
     * 更新编辑器字号
     */
    suspend fun setEditorFontSize(fontSize: EditorFontSize) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.EDITOR_FONT_SIZE] = fontSize.name
        }
    }

    /**
     * 切换打字机模式
     */
    suspend fun toggleTypewriterMode() {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.TYPEWRITER_MODE] ?: false
            preferences[PreferencesKeys.TYPEWRITER_MODE] = !current
        }
    }

    /**
     * 切换聚焦模式
     */
    suspend fun toggleFocusMode() {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.FOCUS_MODE] ?: false
            preferences[PreferencesKeys.FOCUS_MODE] = !current
        }
    }

    /**
     * 设置深色模式
     */
    suspend fun setDarkMode(dark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE] = dark
            preferences[PreferencesKeys.FOLLOW_SYSTEM_THEME] = false
        }
    }

    /**
     * 设置跟随系统主题
     */
    suspend fun setFollowSystemTheme(follow: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FOLLOW_SYSTEM_THEME] = follow
        }
    }

    /**
     * 设置侧边栏展开状态
     */
    suspend fun setSidebarExpanded(expanded: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SIDEBAR_EXPANDED] = expanded
        }
    }

    // AI 助手设置
    suspend fun setAiEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AI_ENABLED] = enabled
        }
    }

    suspend fun setAiProviderType(providerType: ProviderType) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AI_PROVIDER_TYPE] = providerType.name
        }
    }
    suspend fun setAiProviderPresetId(id: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AI_PROVIDER_PRESET_ID] = id
        }
    }
    suspend fun setAiPlanId(id: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AI_PLAN_ID] = id
        }
    }

    suspend fun setAiApiKey(apiKey: String) {
        encryptedPrefs.edit().putString(KEY_AI_API_KEY, apiKey).apply()
        apiKeyVersion.value++
    }

    suspend fun setAiBaseUrl(baseUrl: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AI_BASE_URL] = baseUrl
        }
    }

    suspend fun setAiModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AI_MODEL] = model
        }
    }

    suspend fun setAiMaxTokens(maxTokens: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AI_MAX_TOKENS] = maxTokens
        }
    }

    suspend fun setAiTemperature(temperature: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AI_TEMPERATURE] = temperature
        }
    }

    suspend fun setAiSoulContent(content: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AI_SOUL_CONTENT] = content
        }
    }

    suspend fun setAiMemoryEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AI_MEMORY_ENABLED] = enabled
        }
    }

    suspend fun setAiDreamEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AI_DREAM_ENABLED] = enabled
        }
    }

    companion object {
        private const val KEY_AI_API_KEY = "ai_api_key_encrypted"
    }
}

fun AppSettings.toAiConfig(): AiConfig = AiConfig(
    providerType = aiProviderType,
    apiKey = aiApiKey,
    baseUrl = aiBaseUrl,
    model = aiModel,
    maxTokens = aiMaxTokens,
    temperature = aiTemperature
)
