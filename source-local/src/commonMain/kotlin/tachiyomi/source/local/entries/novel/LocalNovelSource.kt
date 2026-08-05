package tachiyomi.source.local.entries.novel

import eu.kanade.tachiyomi.novelsource.NovelCatalogueSource
import eu.kanade.tachiyomi.source.UnmeteredSource

expect class LocalNovelSource : NovelCatalogueSource, UnmeteredSource
