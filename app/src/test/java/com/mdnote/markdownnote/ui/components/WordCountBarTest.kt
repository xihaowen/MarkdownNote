package com.mdnote.markdownnote.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/** 固定字数统计现有行为，保证性能优化不改变统计结果。 */
class WordCountBarTest {

    @Test
    fun empty_text_keeps_all_counts_at_zero() {
        assertEquals(
            WordCountInfo(0, 0, 0, 0, 0, 0),
            calculateWordCount("")
        )
    }

    @Test
    fun mixed_text_preserves_existing_counting_rules() {
        assertEquals(
            WordCountInfo(
                characters = 17,
                charactersNoSpaces = 14,
                words = 5,
                lines = 2,
                paragraphs = 2,
                readingTime = 1
            ),
            calculateWordCount("你好 Kotlin\nAI note")
        )
    }
}
