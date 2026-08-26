package com.mdnote.markdownnote.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserMathTest {

    @Test
    fun blockFormulaKeepsSourceText() {
        val elements = MarkdownParser.parse("Before\n\n\$\$E = mc^2\$\$\n\nAfter")

        val math = elements.filterIsInstance<MarkdownElement.MathBlock>().single()
        assertEquals("E = mc^2", math.latex)
    }

    @Test
    fun inlineFormulaKeepsSourceText() {
        val spans = MarkdownParser.parseInlineSpans("Energy: \$E = mc^2\$")

        assertTrue(spans.any { it == Span.Math("E = mc^2") })
    }
}
