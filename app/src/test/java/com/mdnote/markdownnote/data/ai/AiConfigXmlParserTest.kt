package com.mdnote.markdownnote.data.ai

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class AiConfigXmlParserTest {

    private val parser = AiConfigXmlParser()

    /** 含双 plan 的智谱 + 单 plan 的 deepseek，无 custom → 应追加 custom */
    private val sampleXml = """
        <?xml version="1.0" encoding="utf-8"?>
        <aiConfig>
            <defaults providerId="zhipu" planId="coding" modelId="glm-4.6"
                     temperatureStyle="BALANCED" aiEnabled="false"
                     memoryEnabled="true" dreamEnabled="true" />
            <providers>
                <provider id="zhipu" name="智谱 AI（GLM）">
                    <plan id="standard" name="普通 API" type="OPENAI_COMPATIBLE"
                          baseUrl="https://open.bigmodel.cn/api/paas/v4" apiKeyApplyUrl="https://a">
                        <model id="glm-4.6" name="GLM-4.6 旗舰" maxOutputTokens="131072" />
                        <model id="glm-4-flash" name="GLM-4-Flash" maxOutputTokens="4096" />
                    </plan>
                    <plan id="coding" name="GLM Coding Plan（编程套餐）" type="OPENAI_COMPATIBLE"
                          baseUrl="https://open.bigmodel.cn/api/coding/paas/v4" apiKeyApplyUrl="https://b">
                        <model id="glm-4.6" name="GLM-4.6 旗舰" maxOutputTokens="131072" />
                    </plan>
                </provider>
                <provider id="deepseek" name="DeepSeek">
                    <plan id="standard" name="普通 API" type="OPENAI_COMPATIBLE"
                          baseUrl="https://api.deepseek.com" apiKeyApplyUrl="https://c">
                        <model id="deepseek-chat" name="V3 对话" maxOutputTokens="8192" />
                    </plan>
                </provider>
            </providers>
        </aiConfig>
    """.trimIndent()

    @Test
    fun parses_defaults_with_planId() {
        val data = parser.parse(ByteArrayInputStream(sampleXml.toByteArray()))
        assertEquals("zhipu", data.defaults.providerId)
        assertEquals("coding", data.defaults.planId)
        assertEquals("glm-4.6", data.defaults.modelId)
    }

    @Test
    fun parses_multiple_plans_for_zhipu() {
        val data = parser.parse(ByteArrayInputStream(sampleXml.toByteArray()))
        val zhipu = data.providers.first { it.id == "zhipu" }
        assertEquals(2, zhipu.plans.size)
        val coding = zhipu.plans.first { it.id == "coding" }
        assertEquals("GLM Coding Plan（编程套餐）", coding.displayName)
        assertEquals("https://open.bigmodel.cn/api/coding/paas/v4", coding.baseUrl)
    }

    @Test
    fun single_plan_provider_has_one_plan() {
        val data = parser.parse(ByteArrayInputStream(sampleXml.toByteArray()))
        val deepseek = data.providers.first { it.id == "deepseek" }
        assertEquals(1, deepseek.plans.size)
        assertEquals("standard", deepseek.plans.first().id)
    }

    @Test
    fun appends_custom_when_missing() {
        val data = parser.parse(ByteArrayInputStream(sampleXml.toByteArray()))
        // zhipu + deepseek + 追加的 custom = 3
        assertEquals(3, data.providers.size)
        val custom = data.providers.first { it.id == "custom" }
        assertTrue(custom.plans.isNotEmpty())
        assertTrue(custom.plans.first().models.isEmpty())
    }

    @Test
    fun coding_plan_models_parsed() {
        val data = parser.parse(ByteArrayInputStream(sampleXml.toByteArray()))
        val coding = data.providers.first { it.id == "zhipu" }
            .plans.first { it.id == "coding" }
        assertEquals(1, coding.models.size)
        assertEquals(131072, coding.models.first().maxOutputTokens)
    }
}
