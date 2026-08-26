package com.mdnote.markdownnote.data.ai

import android.content.Context
import com.mdnote.markdownnote.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 回答风格档位（保持 enum，温度值不进 XML）
 */
enum class TemperatureStyle(val temperature: Float, val displayName: String) {
    PRECISE(0.3f, "严谨"),
    BALANCED(0.7f, "均衡"),
    CREATIVE(0.9f, "创意");

    companion object {
        /** 由当前 temperature 反推最接近的档位；value 先钳到 maxLimit 再匹配 */
        fun fromTemperature(value: Float, maxLimit: Float = 2f): TemperatureStyle {
            val clamped = value.coerceAtMost(maxLimit)
            return entries.minByOrNull { kotlin.math.abs(it.temperature - clamped) } ?: BALANCED
        }
    }
}

data class AiModelPreset(
    val id: String,              // 技术模型 ID
    val displayName: String,     // 中文友好名
    val maxOutputTokens: Int     // 该模型输出上限
)
/**
 * 服务商套餐（如普通 API / GLM Coding Plan 编程套餐）
 * 每个套餐是一组完整接入配置（专用 baseUrl / 申请链接 / 模型清单）
 */
data class AiPlanPreset(
    val id: String,                  // "standard" / "coding"
    val displayName: String,         // "普通 API" / "GLM Coding Plan（编程套餐）"
    val providerType: ProviderType,
    val baseUrl: String,
    val apiKeyApplyUrl: String,
    val models: List<AiModelPreset>
)

/**
 * 服务商预置
 *
 * 接入配置（协议/baseUrl/申请链接/模型）在 [plans] 中，按套餐区分。
 */
data class AiProviderPreset(
    val id: String,                  // "deepseek" / "openai" / "custom" ...
    val displayName: String,         // "DeepSeek（深度求索）"
    val plans: List<AiPlanPreset>    // 该服务商的套餐列表（≥1）
)

/**
 * 预置服务商清单（数据来源：res/raw/ai_config.xml）
 *
 * 修改服务商/模型/默认值请改 [R.raw.ai_config]，无需改本类。
 */
@Singleton
class AiProviderPresets @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val config: AiConfigData by lazy {
        context.resources.openRawResource(R.raw.ai_config).use { stream ->
            AiConfigXmlParser().parse(stream)
        }
    }

    /** 全部服务商（含 custom） */
    val all: List<AiProviderPreset> get() = config.providers

    /** 默认配置（来自 XML defaults 节点） */
    val defaults: AiDefaults get() = config.defaults

    /** 自定义服务商 */
    val custom: AiProviderPreset get() = all.first { it.id == "custom" }

    /** 按 id 查找，未命中回退到自定义 */
    fun byId(id: String): AiProviderPreset =
        all.firstOrNull { it.id == id } ?: custom
}
