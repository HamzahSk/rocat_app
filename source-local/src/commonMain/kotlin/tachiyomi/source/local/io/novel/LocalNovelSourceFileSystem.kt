package tachiyomi.source.local.io.novel

import com.hippo.unifile.UniFile

expect class LocalNovelSourceFileSystem {

    fun getBaseDirectory(): UniFile?

    fun getFilesInBaseDirectory(): List<UniFile>

    fun getNovelFile(name: String): UniFile?
}
