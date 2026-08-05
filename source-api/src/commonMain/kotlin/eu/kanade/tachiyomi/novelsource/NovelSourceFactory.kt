package eu.kanade.tachiyomi.novelsource

/**
 * A factory for creating novel sources at runtime.
 */
interface NovelSourceFactory {
    /**
     * Create a new copy of the sources.
     *
     * @return The created sources.
     */
    fun createSources(): List<NovelSource>
}
