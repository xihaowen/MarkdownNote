package com.mdnote.markdownnote.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EditorValueChangePolicyTest {

    @Test
    fun exact_drag_keeps_pressed_character_as_forward_selection_anchor() {
        val selection = exactDragSelection(anchor = 20, current = 38, textLength = 80)

        assertEquals(20, selection.anchor)
        assertEquals(38, selection.edge)
    }

    @Test
    fun exact_drag_keeps_pressed_character_as_reverse_selection_anchor() {
        val selection = exactDragSelection(anchor = 40, current = 34, textLength = 80)

        assertEquals(40, selection.anchor)
        assertEquals(34, selection.edge)
    }

    @Test
    fun exact_drag_clamps_stale_position_to_current_text_length() {
        val selection = exactDragSelection(anchor = 90, current = -4, textLength = 36)

        assertEquals(36, selection.anchor)
        assertEquals(0, selection.edge)
    }

    @Test
    fun consuming_input_focus_request_preserves_cursor_without_requesting_focus_again() {
        val request = InputFocusRequest(
            initialCursorPos = 12,
            shouldRequestInputFocus = true
        )

        assertEquals(
            InputFocusRequest(initialCursorPos = 12),
            request.consume()
        )
    }

    @Test
    fun completed_drag_requests_compose_toolbar_only_for_nonempty_exact_selection() {
        val bounds = ExactSelectionToolbarBounds(
            left = 10f,
            top = 20f,
            right = 30f,
            bottom = 40f
        )
        val request = completedDragToolbarRequest(
            selection = ExactDragSelection(anchor = 8, edge = 3),
            bounds = bounds
        )

        assertEquals(ExactDragSelection(anchor = 8, edge = 3), request?.selection)
        assertEquals(bounds, request?.bounds)
        assertNull(
            completedDragToolbarRequest(
                selection = ExactDragSelection(anchor = 4, edge = 4),
                bounds = bounds
            )
        )
    }

    @Test
    fun completed_drag_extracts_text_only_from_final_selection_range() {
        val selectedText = completedDragSelectionText(
            text = "0123456789",
            selection = ExactDragSelection(anchor = 8, edge = 3)
        )

        assertEquals("34567", selectedText)
    }
}
