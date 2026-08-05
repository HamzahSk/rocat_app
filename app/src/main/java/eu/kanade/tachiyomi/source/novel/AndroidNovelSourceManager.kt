package eu.kanade.tachiyomi.source.novel

import android.content.Context
import eu.kanade.tachiyomi.extension.novel.NovelExtensionManager
import eu.kanade.tachiyomi.novelsource.NovelCatalogueSource
import eu.kanade.tachiyomi.novelsource.NovelSource
import eu.kanade.tachiyomi.novelsource.online.HttpNovelSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import tachiyomi.domain.source.novel.model.StubNovelSource
import tachiyomi.domain.source.novel.repository.NovelStubSourceRepository
import tachiyomi.domain.source.novel.service.NovelSourceManager
import tachiyomi.source.local.entries.novel.LocalNovelSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.ConcurrentHashMap

/**
 * Android implementation of [NovelSourceManager]. Seeds the built-in sample, Royal Road and
 * local EPUB sources and registers any novel sources shipped inside installed extension packages.
 */
class AndroidNovelSourceManager(
    private val context: Context,
    private val extensionManager: NovelExtensionManager,
    private val sourceRepository: NovelStubSourceRepository,
) : NovelSourceManager {

    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private val sourcesMapFlow = MutableStateFlow(ConcurrentHashMap<Long, NovelSource>())

    private val stubSourcesMap = ConcurrentHashMap<Long, StubNovelSource>()

    override val catalogueSources: Flow<List<NovelCatalogueSource>> = sourcesMapFlow.map {
        it.values.filterIsInstance<NovelCatalogueSource>()
    }

    init {
        scope.launch {
            extensionManager.installedExtensionsFlow
                .collectLatest { extensions ->
                    val mutableMap = ConcurrentHashMap<Long, NovelSource>(
                        mapOf(
                            SampleNovelSource.SAMPLE_SOURCE_ID to SampleNovelSource(),
                            RoyalRoadNovelSource().id to RoyalRoadNovelSource(),
                            LocalNovelSource.ID to LocalNovelSource(
                                context,
                                Injekt.get(),
                                Injekt.get(),
                            ),
                        ),
                    )
                    extensions.forEach { extension ->
                        extension.sources.forEach {
                            mutableMap[it.id] = it
                            registerStubSource(StubNovelSource.from(it))
                        }
                    }
                    sourcesMapFlow.value = mutableMap
                    _isInitialized.value = true
                }
        }

        scope.launch {
            sourceRepository.subscribeAllNovel()
                .collectLatest { sources ->
                    val mutableMap = stubSourcesMap.toMutableMap()
                    sources.forEach {
                        mutableMap[it.id] = it
                    }
                }
        }
    }

    override fun get(sourceKey: Long): NovelSource? {
        return sourcesMapFlow.value[sourceKey]
    }

    override fun getOrStub(sourceKey: Long): NovelSource {
        return sourcesMapFlow.value[sourceKey] ?: stubSourcesMap.getOrPut(sourceKey) {
            runBlocking { createStubSource(sourceKey) }
        }
    }

    override fun getOnlineSources() = sourcesMapFlow.value.values.filterIsInstance<HttpNovelSource>()

    override fun getCatalogueSources() = sourcesMapFlow.value.values.filterIsInstance<NovelCatalogueSource>()

    override fun getStubSources(): List<StubNovelSource> {
        val onlineSourceIds = getOnlineSources().map { it.id }
        return stubSourcesMap.values.filterNot { it.id in onlineSourceIds }
    }

    private fun registerStubSource(source: StubNovelSource) {
        scope.launch {
            val dbSource = sourceRepository.getStubNovelSource(source.id)
            if (dbSource == source) return@launch
            sourceRepository.upsertStubNovelSource(source.id, source.lang, source.name)
        }
    }

    private suspend fun createStubSource(id: Long): StubNovelSource {
        sourceRepository.getStubNovelSource(id)?.let {
            return it
        }
        extensionManager.getSourceData(id)?.let {
            registerStubSource(it)
            return it
        }
        return StubNovelSource(id = id, lang = "", name = "")
    }
}
