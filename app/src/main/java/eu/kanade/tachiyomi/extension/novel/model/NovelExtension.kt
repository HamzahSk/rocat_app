package eu.kanade.tachiyomi.extension.novel.model

import android.graphics.drawable.Drawable
import eu.kanade.tachiyomi.novelsource.NovelSource
import tachiyomi.domain.source.novel.model.StubNovelSource

sealed class NovelExtension {

    abstract val name: String
    abstract val pkgName: String
    abstract val versionName: String
    abstract val versionCode: Long
    abstract val libVersion: Double
    abstract val lang: String?
    abstract val isNsfw: Boolean

    data class Installed(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val libVersion: Double,
        override val lang: String,
        override val isNsfw: Boolean,
        val pkgFactory: String?,
        val sources: List<NovelSource>,
        val icon: Drawable?,
        val isShared: Boolean,
    ) : NovelExtension()

    data class Available(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val libVersion: Double,
        override val lang: String,
        override val isNsfw: Boolean,
        val sources: List<NovelSource>,
        val apkName: String,
        val iconUrl: String,
        val repoUrl: String,
    ) : NovelExtension() {

        data class NovelSource(
            val id: Long,
            val lang: String,
            val name: String,
            val baseUrl: String,
        ) {
            fun toStubSource(): StubNovelSource {
                return StubNovelSource(
                    id = this.id,
                    lang = this.lang,
                    name = this.name,
                )
            }
        }
    }

    data class Untrusted(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val libVersion: Double,
        val signatureHash: String,
        override val lang: String? = null,
        override val isNsfw: Boolean = false,
    ) : NovelExtension()
}
