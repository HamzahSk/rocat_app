package eu.kanade.tachiyomi.novelsource

import eu.kanade.tachiyomi.novelsource.model.SNovel

/**
 * Publication status of a novel. Values are mapped to [SNovel] status constants so they can be
 * stored in the same `status` integer column used by [eu.kanade.tachiyomi.source.model.SManga].
 */
enum class NovelStatus(val flagValue: Int, val label: String) {
    UNKNOWN(SNovel.UNKNOWN, "Unknown"),
    ONGOING(SNovel.ONGOING, "Ongoing"),
    COMPLETED(SNovel.COMPLETED, "Completed"),
    LICENSED(SNovel.LICENSED, "Licensed"),
    PUBLISHING_FINISHED(SNovel.PUBLISHING_FINISHED, "Publishing Finished"),
    CANCELLED(SNovel.CANCELLED, "Cancelled"),
    ON_HIATUS(SNovel.ON_HIATUS, "On Hiatus"),
    ;

    companion object {
        /**
         * Maps an LNReader style status string (e.g. "Ongoing") to the closest [SNovel] status.
         */
        fun fromString(value: String?): Int {
            return when (value?.trim()?.lowercase()) {
                "ongoing", "on-going", "actively being translated" -> ONGOING.flagValue
                "completed", "complete" -> COMPLETED.flagValue
                "licensed" -> LICENSED.flagValue
                "publishing finished", "publishing-finished", "finished" -> PUBLISHING_FINISHED.flagValue
                "cancelled", "canceled", "dropped" -> CANCELLED.flagValue
                "on hiatus", "on-hiatus", "hiatus", "paused" -> ON_HIATUS.flagValue
                else -> UNKNOWN.flagValue
            }
        }
    }
}
