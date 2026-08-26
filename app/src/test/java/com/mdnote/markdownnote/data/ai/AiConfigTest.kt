package com.mdnote.markdownnote.data.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiConfigTest {

    private fun config(baseUrl: String) = AiConfig(
        apiKey = "test-key",
        baseUrl = baseUrl,
        model = "test-model"
    )

    @Test
    fun valid_requiresHttpsUrlWithHost() {
        assertTrue(config("https://api.example.com/v1").isValid)
        assertFalse(config("http://api.example.com/v1").isValid)
        assertFalse(config("https:///missing-host").isValid)
        assertFalse(config("not-a-url").isValid)
    }

    @Test
    fun valid_requiresCredentialsAndModel() {
        assertFalse(config("https://api.example.com").copy(apiKey = " ").isValid)
        assertFalse(config("https://api.example.com").copy(model = " ").isValid)
    }
}
