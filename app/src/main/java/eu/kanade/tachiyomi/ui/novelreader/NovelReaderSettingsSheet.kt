package eu.kanade.tachiyomi.ui.novelreader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import java.util.Locale

/**
 * Bottom sheet used to tweak the text rendering of the novel reader. Every control writes straight
 * into [NovelReaderPreferences] so the changes are applied to the open chapter in real time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelReaderSettingsSheet(
    theme: NovelReaderTheme,
    fontSize: Int,
    fontFamily: NovelReaderFontFamily,
    lineSpacing: Float,
    padding: Int,
    onFontSizeChange: (Int) -> Unit,
    onFontFamilyChange: (NovelReaderFontFamily) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onPaddingChange: (Int) -> Unit,
    onThemeChange: (NovelReaderTheme) -> Unit,
    onDismissRequest: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.padding.medium)
                .padding(bottom = MaterialTheme.padding.large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        ) {
            Text(
                text = stringResource(MR.strings.novel_reader_settings),
                style = MaterialTheme.typography.titleLarge,
            )

            ReaderTextSizeRow(
                fontSize = fontSize,
                onFontSizeChange = onFontSizeChange,
            )

            ReaderSliderRow(
                title = stringResource(MR.strings.novel_reader_line_spacing),
                value = lineSpacing,
                valueText = String.format(Locale.ROOT, "%.1f", lineSpacing),
                valueRange = NovelReaderPreferences.LINE_SPACING_MIN..NovelReaderPreferences.LINE_SPACING_MAX,
                onValueChange = onLineSpacingChange,
            )

            ReaderSliderRow(
                title = stringResource(MR.strings.novel_reader_padding),
                value = padding.toFloat(),
                valueText = padding.toString(),
                valueRange = NovelReaderPreferences.PADDING_MIN.toFloat()..NovelReaderPreferences.PADDING_MAX.toFloat(),
                onValueChange = { onPaddingChange(it.toInt()) },
            )

            Text(
                text = stringResource(MR.strings.novel_reader_theme),
                style = MaterialTheme.typography.titleSmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
            ) {
                NovelReaderTheme.entries.forEach { candidate ->
                    FilterChip(
                        selected = candidate == theme,
                        onClick = { onThemeChange(candidate) },
                        label = {
                            Text(text = themeLabel(candidate))
                        },
                    )
                }
            }

            Text(
                text = stringResource(MR.strings.novel_reader_font_family),
                style = MaterialTheme.typography.titleSmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
            ) {
                NovelReaderFontFamily.entries.forEach { candidate ->
                    FilterChip(
                        selected = candidate == fontFamily,
                        onClick = { onFontFamilyChange(candidate) },
                        label = {
                            Text(
                                text = candidate.displayName,
                                fontFamily = candidate.composeFontFamily,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun themeLabel(theme: NovelReaderTheme): String {
    return stringResource(
        when (theme) {
            NovelReaderTheme.LIGHT -> MR.strings.novel_reader_theme_light
            NovelReaderTheme.DARK -> MR.strings.novel_reader_theme_dark
            NovelReaderTheme.SEPIA -> MR.strings.novel_reader_theme_sepia
        },
    )
}

@Composable
private fun ReaderTextSizeRow(
    fontSize: Int,
    onFontSizeChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        TextButton(
            onClick = {
                onFontSizeChange((fontSize - 1).coerceAtLeast(NovelReaderPreferences.FONT_SIZE_MIN))
            },
        ) {
            Text(text = "A-")
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(MR.strings.novel_reader_text_size),
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = fontSize.toFloat(),
                onValueChange = { onFontSizeChange(it.toInt()) },
                valueRange =
                NovelReaderPreferences.FONT_SIZE_MIN.toFloat()..NovelReaderPreferences.FONT_SIZE_MAX.toFloat(),
            )
        }
        TextButton(
            onClick = {
                onFontSizeChange((fontSize + 1).coerceAtMost(NovelReaderPreferences.FONT_SIZE_MAX))
            },
        ) {
            Text(text = "A+")
        }
    }
}

@Composable
private fun ReaderSliderRow(
    title: String,
    value: Float,
    valueText: String,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.width(MaterialTheme.padding.small))
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
        )
    }
}
