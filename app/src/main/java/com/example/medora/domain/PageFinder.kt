package com.example.medora.domain

// Finds the PDF page a piece of text came from. PDF imports put "[Page N]" markers in the
// lecture text. We locate the text and return the last marker before it.
// Returns null when there is no marker (TXT lectures) or the text is not found.
object PageFinder {
    private val marker = Regex("\\[Page (\\d+)]")

    fun pageOf(content: String?, vararg candidates: String): Int? {
        if (content.isNullOrEmpty()) return null
        for (candidate in candidates) {
            val key = candidate.trim()
            if (key.length < 8) continue
            var index = content.indexOf(key, ignoreCase = true)
            if (index < 0) {
                val head = key.take(40).trim()
                if (head.length >= 8) index = content.indexOf(head, ignoreCase = true)
            }
            if (index < 0) continue
            val last = marker.findAll(content.substring(0, index)).lastOrNull() ?: return null
            return last.groupValues[1].toIntOrNull()
        }
        return null
    }
}
