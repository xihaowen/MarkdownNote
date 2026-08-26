package com.mdnote.markdownnote.ui.reader

import androidx.compose.ui.unit.IntOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class FloatingToolbarStateTest {

    @Test
    fun dismiss_hides_toolbar_and_clears_selected_text() {
        val state = FloatingToolbarState(
            selectedText = "需要关闭的选区",
            offset = IntOffset(24, 48),
            isVisible = true
        )

        assertEquals(
            FloatingToolbarState(offset = IntOffset(24, 48)),
            state.dismiss()
        )
    }
}
