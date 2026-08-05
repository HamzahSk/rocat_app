package eu.kanade.tachiyomi.novelsource.util

/**
 * Ported from LNReader's `src/utils/parseChapterNumber.ts`.
 *
 * Extracts a numeric chapter number from a chapter title, stripping the novel title and any
 * volume/season information first. Falls back to [fallback] when nothing can be parsed.
 */
object NovelChapterNumberParser {

    private val BASIC = Regex("(?<=ch[^\\d]*[\\s]*)([0-9]+)(\\.[0-9]+)?(\\.?[a-z]+)?")
    private val NUMBER = Regex("([0-9]+)(\\.[0-9]+)?(\\.?[a-z]+)?")
    private val UNWANTED_WHITESPACE = Regex("\\s(?=extra|special|omake)")
    private val UNWANTED = Regex("\\b(?:v|ver|vol|version|volume|season|s)[^a-z]?[0-9]+")

    fun parse(
        novelName: String,
        chapterName: String,
        chapterNumber: Double? = null,
    ): Double {
        if (chapterNumber != null && chapterNumber > -1) {
            return chapterNumber
        }

        var name = chapterName.lowercase()
        name = name.replace(novelName.lowercase(), "").trim()
        name = name.replaceFirst(",", ".")
        name = name.replaceFirst("-", ".")
        name = name.replace(UNWANTED_WHITESPACE, "")
        name = name.replace(UNWANTED, "")

        val basicMatch = BASIC.find(name)
        if (basicMatch != null) {
            val chapNo = getChapterNumberFromMatch(basicMatch)
            if (chapNo != null) {
                return chapNo
            }
        }

        val numberMatch = NUMBER.find(name)
        if (numberMatch != null) {
            val chapNo = getChapterNumberFromMatch(numberMatch)
            if (chapNo != null) {
                return chapNo
            }
        }

        return chapterNumber ?: -1.0
    }

    private fun getChapterNumberFromMatch(match: MatchResult): Double? {
        val initial = match.groupValues[1].toDoubleOrNull() ?: return null
        val subChapterDecimal = match.groupValues.getOrNull(2).orEmpty().takeIf { it.isNotEmpty() }
        val subChapterAlpha = match.groupValues.getOrNull(3).orEmpty().takeIf { it.isNotEmpty() }
        val addition = checkForDecimal(subChapterDecimal, subChapterAlpha)
        return initial + addition
    }

    private fun checkForDecimal(decimal: String?, alpha: String?): Double {
        if (decimal != null) {
            return decimal.toDoubleOrNull() ?: 0.0
        }

        if (alpha != null) {
            if (alpha.contains("extra")) return 0.99
            if (alpha.contains("omake")) return 0.98
            if (alpha.contains("special")) return 0.97

            val trimmedAlpha = alpha.drop(1)
            if (trimmedAlpha.length == 1) {
                return parseAlphaPostFix(trimmedAlpha[0])
            }
        }

        return 0.0
    }

    private fun parseAlphaPostFix(alpha: Char): Double {
        val number = alpha.code - ('a'.code - 1)
        if (number >= 10) return 0.0
        return number / 10.0
    }
}
