package eu.kanade.tachiyomi.extension.novel

import android.content.Context
import android.graphics.drawable.Drawable
import eu.kanade.tachiyomi.extension.novel.model.NovelExtension
import eu.kanade.tachiyomi.extension.novel.model.NovelLoadResult
import eu.kanade.tachiyomi.extension.novel.util.NovelExtensionLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import tachiyomi.domain.source.novel.model.StubNovelSource

/**
 * Tracks the novel extensions installed on the device. This is the text based counterpart of
 * [eu.kanade.tachiyomi.extension.manga.MangaExtensionManager].
 *
 * The remote extension API (browse/install/update) is out of scope for now: extensions are loaded
 * from the packages already present on the device.
 */
class NovelExtensionManager(
    private val context: Context,
) {

    val scope = CoroutineScope(SupervisorJob())

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val iconMap = mutableMapOf<String, Drawable>()

    private val installedExtensionsMapFlow = MutableStateFlow(emptyMap<String, NovelExtension.Installed>())
    val installedExtensionsFlow = installedExtensionsMapFlow.mapExtensions(scope)

    private val untrustedExtensionsMapFlow = MutableStateFlow(emptyMap<String, NovelExtension.Untrusted>())
    val untrustedExtensionsFlow = untrustedExtensionsMapFlow.mapExtensions(scope)

    init {
        initExtensions()
    }

    fun getExtensionPackage(sourceId: Long): String? {
        return installedExtensionsFlow.value.find { extension ->
            extension.sources.any { it.id == sourceId }
        }
            ?.pkgName
    }

    fun getAppIconForSource(sourceId: Long): Drawable? {
        val pkgName = installedExtensionsMapFlow.value.values
            .find { ext ->
                ext.sources.any { it.id == sourceId }
            }
            ?.pkgName
            ?: return null

        return iconMap[pkgName] ?: iconMap.getOrPut(pkgName) {
            NovelExtensionLoader.getNovelExtensionPackageInfoFromPkgName(context, pkgName)!!.applicationInfo!!
                .loadIcon(context.packageManager)
        }
    }

    private var availableExtensionsSourcesData: Map<Long, StubNovelSource> = emptyMap()

    private fun setupAvailableExtensionsSourcesDataMap(extensions: List<NovelExtension.Available>) {
        if (extensions.isEmpty()) return
        availableExtensionsSourcesData = extensions
            .flatMap { ext -> ext.sources.map { it.toStubSource() } }
            .associateBy { it.id }
    }

    fun getSourceData(id: Long) = availableExtensionsSourcesData[id]

    /**
     * Loads and registers the installed extensions.
     */
    private fun initExtensions() {
        val extensions = NovelExtensionLoader.loadNovelExtensions(context)

        installedExtensionsMapFlow.value = extensions
            .filterIsInstance<NovelLoadResult.Success>()
            .associate { it.extension.pkgName to it.extension }

        untrustedExtensionsMapFlow.value = extensions
            .filterIsInstance<NovelLoadResult.Untrusted>()
            .associate { it.extension.pkgName to it.extension }

        _isInitialized.value = true
    }

    fun setAvailableExtensions(extensions: List<NovelExtension.Available>) {
        setupAvailableExtensionsSourcesDataMap(extensions)
    }

    /**
     * Unregisters the extension in this and the source managers given its package name.
     *
     * @param pkgName The package name of the uninstalled application.
     */
    fun unregisterExtension(pkgName: String) {
        installedExtensionsMapFlow.value -= pkgName
        untrustedExtensionsMapFlow.value -= pkgName
    }

    private operator fun <T : NovelExtension> Map<String, T>.plus(extension: T) = plus(extension.pkgName to extension)

    private fun <T : NovelExtension> StateFlow<Map<String, T>>.mapExtensions(
        scope: CoroutineScope,
    ): StateFlow<List<T>> {
        return map { it.values.toList() }.stateIn(scope, SharingStarted.Lazily, value.values.toList())
    }
}
