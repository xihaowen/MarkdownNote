package com.mdnote.markdownnote.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsSubPageTest {

    @Test
    fun sheetTitleReflectsCurrentSubPage() {
        assertEquals("设置", settingsSheetTitle(SettingsSubPage.MAIN))
        assertEquals("AI 助手", settingsSheetTitle(SettingsSubPage.AI_CONFIG))
        assertEquals("开源许可", settingsSheetTitle(SettingsSubPage.OPEN_SOURCE_LICENSES))
    }
}
