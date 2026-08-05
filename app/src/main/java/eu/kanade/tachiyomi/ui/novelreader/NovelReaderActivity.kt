package eu.kanade.tachiyomi.ui.novelreader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.util.view.setComposeContent
import kotlinx.coroutines.flow.distinctUntilChanged
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Text based novel reader. Renders the chapter content returned by a [NovelReaderViewModel] inside
 * a [LazyColumn] so arbitrarily long chapters stay scrollable without blocking the main thread.
 *
 * Supports inline EPUB illustrations (as [NovelChapterContent.Image]), font size, line spacing and
 * light/dark/sepia themes via [NovelReaderPreferences]. Reading progress is written back to the
 * local database by the [NovelReaderViewModel].
 */
class NovelReaderActivity : BaseActivity() {

    private val readerPreferences: NovelReaderPreferences by lazy {
        Injekt.get<NovelReaderPreferences>()
    }

    private val viewModel by viewModels<NovelReaderViewModel>(
        factoryProducer = {
            val sourceId = intent.getLongExtra(EXTRA_SOURCE_ID, -1L)
            val novelId = intent.getLongExtra(EXTRA_NOVEL_ID, -1L)
            val chapterUrl = intent.getStringExtra(EXTRA_CHAPTER_URL).orEmpty()
            val chapterName = intent.getStringExtra(EXTRA_CHAPTER_NAME).orEmpty()
            NovelReaderViewModelFactory(sourceId, novelId, chapterUrl, chapterName)
        },
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (readerPreferences.keepScreenOn().get()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        setComposeContent {
            NovelReaderScreen(
                viewModel = viewModel,
                readerPreferences = readerPreferences,
            )
        }
    }

    companion object {
        private const val EXTRA_SOURCE_ID = "source_id"
        private const val EXTRA_NOVEL_ID = "novel_id"
        private const val EXTRA_CHAPTER_URL = "chapter_url"
        private const val EXTRA_CHAPTER_NAME = "chapter_name"

        fun newIntent(
            context: Context,
            sourceId: Long,
            novelId: Long,
            chapterUrl: String,
            chapterName: String,
        ): Intent {
            return Intent(context, NovelReaderActivity::class.java).apply {
                putExtra(EXTRA_SOURCE_ID, sourceId)
                putExtra(EXTRA_NOVEL_ID, novelId)
                putExtra(EXTRA_CHAPTER_URL, chapterUrl)
                putExtra(EXTRA_CHAPTER_NAME, chapterName)
            }
        }
    }
}

@Composable
private fun NovelReaderScreen(
    viewModel: NovelReaderViewModel,
    readerPreferences: NovelReaderPreferences,
) {
    val theme by readerPreferences.theme().collectAsState()
    val fontSize by readerPreferences.fontSize().collectAsState()
    val lineSpacing by readerPreferences.lineSpacing().collectAsState()

    val uiState = viewModel.uiState
    var showSettings by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.loadChapter()
    }

    LaunchedEffect(Unit) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { viewModel.onScrolled(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .clickable { showSettings = !showSettings },
    ) {
        when {
            uiState.error != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = uiState.error, color = theme.textColor)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadChapter() }) {
                        Text(text = "Retry")
                    }
                }
            }
            uiState.isLoading || uiState.blocks == null -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = theme.textColor,
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    state = listState,
                    contentPadding = PaddingValues(
                        horizontal = 20.dp,
                        vertical = 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    itemsIndexed(uiState.blocks, key = { index, _ -> index }) { _, block ->
                        when (block) {
                            is NovelChapterContent.Text -> {
                                Text(
                                    text = block.text,
                                    color = theme.textColor,
                                    style = TextStyle(
                                        fontSize = fontSize.sp,
                                        lineHeight = (fontSize * lineSpacing).sp,
                                    ),
                                )
                            }
                            is NovelChapterContent.Image -> {
                                AsyncImage(
                                    model = block.bytes,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showSettings) {
            ReaderSettingsBar(
                theme = theme,
                fontSize = fontSize,
                lineSpacing = lineSpacing,
                onFontSizeChange = { readerPreferences.fontSize().set(it) },
                onLineSpacingChange = { readerPreferences.lineSpacing().set(it) },
                onThemeChange = { readerPreferences.theme().set(it) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(theme.backgroundColor),
            )
        }
    }
}

@Composable
private fun ReaderSettingsBar(
    theme: NovelReaderTheme,
    fontSize: Int,
    lineSpacing: Float,
    onFontSizeChange: (Int) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onThemeChange: (NovelReaderTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.navigationBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HorizontalDivider(color = theme.textColor.copy(alpha = 0.2f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = {
                onFontSizeChange((fontSize - 1).coerceAtLeast(NovelReaderPreferences.FONT_SIZE_MIN))
            }) {
                Text("A-", color = theme.textColor)
            }
            Slider(
                value = fontSize.toFloat(),
                onValueChange = { onFontSizeChange(it.toInt()) },
                valueRange =
                NovelReaderPreferences.FONT_SIZE_MIN.toFloat()..NovelReaderPreferences.FONT_SIZE_MAX.toFloat(),
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = {
                onFontSizeChange((fontSize + 1).coerceAtMost(NovelReaderPreferences.FONT_SIZE_MAX))
            }) {
                Text("A+", color = theme.textColor)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Line height", color = theme.textColor)
            Slider(
                value = lineSpacing,
                onValueChange = onLineSpacingChange,
                valueRange = NovelReaderPreferences.LINE_SPACING_MIN..NovelReaderPreferences.LINE_SPACING_MAX,
                modifier = Modifier.weight(1f),
            )
            Text(String.format("%.1f", lineSpacing), color = theme.textColor)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            NovelReaderTheme.entries.forEach { candidate ->
                TextButton(onClick = { onThemeChange(candidate) }) {
                    Text(
                        text = candidate.name,
                        color = if (candidate == theme) theme.textColor else theme.textColor.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
