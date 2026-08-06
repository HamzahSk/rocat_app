package eu.kanade.tachiyomi.ui.novelreader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class NovelReaderPreferencesTest {

    private fun storeWith(
        vararg initial: InMemoryPreferenceStore.InMemoryPreference<*>,
    ) = InMemoryPreferenceStore(initial.asSequence())

    @Test
    fun `defaults are applied`() {
        val preferences = NovelReaderPreferences(storeWith())

        assertEquals(NovelReaderPreferences.DEFAULT_FONT_SIZE, preferences.fontSize().get())
        assertEquals(NovelReaderPreferences.DEFAULT_LINE_SPACING, preferences.lineSpacing().get())
        assertEquals(NovelReaderPreferences.DEFAULT_PADDING, preferences.padding().get())
        assertEquals(NovelReaderFontFamily.SYSTEM, preferences.fontFamily().get())
        assertEquals(NovelReaderTheme.LIGHT, preferences.theme().get())
        assertTrue(preferences.keepScreenOn().get())
    }

    @Test
    fun `font family and padding are read from the store`() {
        val preferences = NovelReaderPreferences(
            storeWith(
                InMemoryPreferenceStore.InMemoryPreference(
                    key = "novel_reader_font_family",
                    data = NovelReaderFontFamily.SERIF,
                    defaultValue = NovelReaderFontFamily.SYSTEM,
                ),
                InMemoryPreferenceStore.InMemoryPreference(
                    key = "novel_reader_padding",
                    data = 32,
                    defaultValue = NovelReaderPreferences.DEFAULT_PADDING,
                ),
            ),
        )

        assertEquals(NovelReaderFontFamily.SERIF, preferences.fontFamily().get())
        assertEquals(32, preferences.padding().get())
    }

    @Test
    fun `theme and line spacing are read from the store`() {
        val preferences = NovelReaderPreferences(
            storeWith(
                InMemoryPreferenceStore.InMemoryPreference(
                    key = "novel_reader_theme",
                    data = NovelReaderTheme.SEPIA,
                    defaultValue = NovelReaderTheme.LIGHT,
                ),
                InMemoryPreferenceStore.InMemoryPreference(
                    key = "novel_reader_line_spacing",
                    data = 2.0f,
                    defaultValue = NovelReaderPreferences.DEFAULT_LINE_SPACING,
                ),
            ),
        )

        assertEquals(NovelReaderTheme.SEPIA, preferences.theme().get())
        assertEquals(2.0f, preferences.lineSpacing().get())
    }

    @Test
    fun `font family maps to a compose font family`() {
        assertEquals(
            androidx.compose.ui.text.font.FontFamily.Serif,
            NovelReaderFontFamily.SERIF.composeFontFamily,
        )
        assertEquals(
            androidx.compose.ui.text.font.FontFamily.Monospace,
            NovelReaderFontFamily.MONOSPACE.composeFontFamily,
        )
    }
}
