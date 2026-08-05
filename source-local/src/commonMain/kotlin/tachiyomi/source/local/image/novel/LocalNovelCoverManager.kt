package tachiyomi.source.local.image.novel

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.novelsource.model.SNovel

expect class LocalNovelCoverManager {

    fun find(novelUrl: String): UniFile?

    fun update(novel: SNovel, epubFile: UniFile): UniFile?
}
