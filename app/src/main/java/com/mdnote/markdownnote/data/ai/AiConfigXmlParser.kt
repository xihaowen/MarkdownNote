package com.mdnote.markdownnote.data.ai

import org.w3c.dom.Element
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

/** XML 解析出的默认配置 */
data class AiDefaults(
    val providerId: String,
    val planId: String,            // 默认套餐
    val modelId: String,
    val temperatureStyle: String,   // PRECISE / BALANCED / CREATIVE
    val aiEnabled: Boolean,
    val memoryEnabled: Boolean,
    val dreamEnabled: Boolean
)

/** 解析结果容器 */
data class AiConfigData(
    val defaults: AiDefaults,
    val providers: List<AiProviderPreset>
)

/**
 * 用 JDK DOM 解析 res/raw/ai_config.xml
 *
 * 结构：aiConfig → defaults + providers → provider → plan → model
 * - 不依赖 Android Context，可在 JVM 单测中喂 InputStream 测试。
 * - 若 XML 未含 custom 服务商，自动追加空 custom（含一个空 standard plan）。
 * - 禁用 DOCTYPE 防 XXE。
 */
class AiConfigXmlParser {

    private val factory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
        try {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        } catch (_: Exception) {
            // 不同 JVM 实现特性名可能不支持，忽略
        }
    }

    fun parse(inputStream: InputStream): AiConfigData {
        val doc = factory.newDocumentBuilder().parse(inputStream)
        doc.documentElement.normalize()

        val defaultsEl = doc.getElementsByTagName("defaults").item(0) as Element
        val defaults = AiDefaults(
            providerId = defaultsEl.getAttribute("providerId"),
            planId = defaultsEl.getAttribute("planId").ifBlank { "standard" },
            modelId = defaultsEl.getAttribute("modelId"),
            temperatureStyle = defaultsEl.getAttribute("temperatureStyle"),
            aiEnabled = defaultsEl.getAttribute("aiEnabled").toBoolean(),
            memoryEnabled = defaultsEl.getAttribute("memoryEnabled").toBoolean(),
            dreamEnabled = defaultsEl.getAttribute("dreamEnabled").toBoolean()
        )
        val providersNodeList = doc.getElementsByTagName("provider")
        val providers = mutableListOf<AiProviderPreset>()
        for (i in 0 until providersNodeList.length) {
            val el = providersNodeList.item(i) as Element
            val plans = parsePlans(el)
            providers.add(AiProviderPreset(
                id = el.getAttribute("id"),
                displayName = el.getAttribute("name"),
                plans = plans
            ))
        }
        if (providers.none { it.id == "custom" }) {
            val emptyPlan = AiPlanPreset(
                id = "standard",
                displayName = "普通 API",
                providerType = ProviderType.OPENAI_COMPATIBLE,
                baseUrl = "",
                apiKeyApplyUrl = "",
                models = emptyList()
            )
            providers.add(AiProviderPreset(
                id = "custom",
                displayName = "自定义",
                plans = listOf(emptyPlan)
            ))
        }

        return AiConfigData(defaults, providers)
    }

    /** 解析某个 provider 下的所有 plan 节点 */
    private fun parsePlans(providerEl: Element): List<AiPlanPreset> {
        val planNodes = providerEl.getElementsByTagName("plan")
        val plans = mutableListOf<AiPlanPreset>()
        for (j in 0 until planNodes.length) {
            val p = planNodes.item(j) as Element
            val models = mutableListOf<AiModelPreset>()
            val modelNodes = p.getElementsByTagName("model")
            for (k in 0 until modelNodes.length) {
                val m = modelNodes.item(k) as Element
                models.add(AiModelPreset(
                    id = m.getAttribute("id"),
                    displayName = m.getAttribute("name"),
                    maxOutputTokens = m.getAttribute("maxOutputTokens").toIntOrNull() ?: 4096
                ))
            }
            plans.add(AiPlanPreset(
                id = p.getAttribute("id").ifBlank { "standard" },
                displayName = p.getAttribute("name").ifBlank { "普通 API" },
                providerType = runCatching {
                    ProviderType.valueOf(p.getAttribute("type"))
                }.getOrDefault(ProviderType.OPENAI_COMPATIBLE),
                baseUrl = p.getAttribute("baseUrl"),
                apiKeyApplyUrl = p.getAttribute("apiKeyApplyUrl"),
                models = models
            ))
        }
        if (plans.isEmpty()) {
            plans.add(AiPlanPreset(
                id = "standard",
                displayName = "普通 API",
                providerType = ProviderType.OPENAI_COMPATIBLE,
                baseUrl = "",
                apiKeyApplyUrl = "",
                models = emptyList()
            ))
        }
        return plans
    }
}
