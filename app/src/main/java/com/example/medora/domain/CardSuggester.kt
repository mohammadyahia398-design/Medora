package com.example.medora.domain

// Local, rule-based helper (no AI). Finds lines shaped like "Term: definition"
// (also "Term - definition") and suggests them as flashcards.
object CardSuggester {
    data class Suggestion(val front: String, val back: String, val source: String)

    private val separators = listOf(":", "：", " - ", " – ", " — ")
    private val numbering = Regex("^\\d+[.)]\\s*")
    private val spaces = Regex("\\s+")

    fun suggest(content: String, existingFronts: Set<String> = emptySet()): List<Suggestion> {
        val seen = existingFronts.map { it.trim().lowercase() }.toMutableSet()
        val out = mutableListOf<Suggestion>()
        for (raw in content.lineSequence()) {
            val line = raw.trim()
                .trimStart('-', '•', '*', '–', '—', '·')
                .trim()
                .replace(numbering, "")
            if (line.length < 10 || line.contains("://")) continue
            var best = -1
            var bestSep = ""
            for (sep in separators) {
                val at = line.indexOf(sep)
                if (at > 0 && (best == -1 || at < best)) {
                    best = at
                    bestSep = sep
                }
            }
            if (best == -1) continue
            val term = line.substring(0, best).trim()
            val definition = line.substring(best + bestSep.length).trim()
            if (term.length < 2 || term.length > 60) continue
            if (term.split(spaces).size > 8 || term.all { it.isDigit() }) continue
            if (definition.length < 8) continue
            if (!seen.add(term.lowercase())) continue
            out.add(Suggestion(term, definition, line))
            if (out.size >= 200) break
        }
        return out
    }
}
