package eu.kanade.tachiyomi.ui.novelreader

import androidx.compose.ui.graphics.Color
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

/**
 * Reader preferences for text based novels. Mirrors the LNReader chapter reader settings
 * (text size, line height, theme) but persisted through the standard [PreferenceStore].
 */
class NovelReaderPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun fontSize() = preferenceStore.getInt("novel_reader_font_size", DEFAULT_FONT_SIZE)

    fun lineSpacing() = preferenceStore.getFloat("novel_reader_line_spacing", DEFAULT_LINE_SPACING)

    fun theme() = preferenceStore.getEnum("novel_reader_theme", NovelReaderTheme.LIGHT)

    fun keepScreenOn() = preferenceStore.getBoolean("novel_reader_keep_screen_on", true)

    companion object {
        const val FONT_SIZE_MIN = 12
        const val FONT_SIZE_MAX = 40
        const val DEFAULT_FONT_SIZE = 18

        const val LINE_SPACING_MIN = 1.0f
        const val LINE_SPACING_MAX = 2.5f
        const val DEFAULT_LINE_SPACING = 1.5f
    }
}

/**
 * Color themes of the text reader, equivalent to the `light`/`dark`/`sepia` themes of LNReader's
 * `ChapterReaderSettings`.
 */
enum class NovelReaderTheme(val backgroundColor: Color, val textColor: Color) {
    LIGHT(Color(0xFFFAFAF7), Color(0xFF1F2328)),
    DARK(Color(0xFF111214), Color(0xFFD9D9D9)),
    SEPIA(Color(0xFFF5ECD9), Color(0xFF3E3A33)),
}
