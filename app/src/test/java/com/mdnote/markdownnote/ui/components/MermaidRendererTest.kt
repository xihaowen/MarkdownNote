package com.mdnote.markdownnote.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MermaidRendererTest {

    @Test
    fun htmlEmbedsLocalScriptAndEscapesDiagramSource() {
        val html = buildMermaidHtml(
            mermaidCode = "flowchart LR\nA[<unsafe>] --> B",
            isDarkTheme = false,
            diagramId = "diagram-1",
            mermaidScript = "window.__bundledMermaid = true;"
        )

        assertTrue(html.contains("window.__bundledMermaid = true;"))
        assertTrue(html.contains("A[&lt;unsafe&gt;] --&gt; B"))
        assertFalse(html.contains("https://"))
        assertFalse(html.contains("http://"))
    }

    @Test
    fun htmlReportsRenderTimeoutThroughJavascriptBridge() {
        val html = buildMermaidHtml(
            mermaidCode = "flowchart LR\nA --> B",
            isDarkTheme = true,
            diagramId = "diagram-2",
            mermaidScript = "window.mermaid = {};"
        )

        assertTrue(html.contains("Mermaid render timeout"))
        assertTrue(html.contains("window.MermaidLoaded.onError"))
    }
}
