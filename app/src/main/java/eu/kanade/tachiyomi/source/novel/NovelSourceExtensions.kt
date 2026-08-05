package eu.kanade.tachiyomi.source.novel

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.novelsource.NovelSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

fun NovelSource.getNameForNovelInfo(): String {
    val preferences = Injekt.get<SourcePreferences>()
    val enabledLanguages = preferences.enabledLanguages().get()
        .filterNot { it in listOf("all", "other") }
    val hasOneActiveLanguages = enabledLanguages.size == 1
    val isInEnabledLanguages = lang in enabledLanguages
    return when {
        // For edge cases where user disables a source they got novel of in their library.
        hasOneActiveLanguages && !isInEnabledLanguages -> toString()
        // Hide the language tag when only one language is used.
        hasOneActiveLanguages && isInEnabledLanguages -> name
        else -> toString()
    }
}
