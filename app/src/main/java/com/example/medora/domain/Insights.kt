package com.example.medora.domain

import org.json.JSONArray
import org.json.JSONObject

// AI "insights" for one lecture: key terms with definitions, comparison tables and
// classifications. Saved as JSON in lectures.insightsJson. Everything here is AI-made
// and is always shown with the AI badge, apart from the lecture's own text.
class Term(val term: String, val definition: String, val source: String)
class CompareTable(val title: String, val headers: List<String>, val rows: List<List<String>>)
class ClassGroup(val name: String, val items: List<String>)
class Classification(val title: String, val groups: List<ClassGroup>)

class Insights(
    val terms: List<Term>,
    val tables: List<CompareTable>,
    val classifications: List<Classification>,
) {
    val isEmpty: Boolean get() = terms.isEmpty() && tables.isEmpty() && classifications.isEmpty()

    // Same JSON shape the AI returns, so parse(toJson()) gives the same data back.
    fun toJson(): String {
        if (isEmpty) return ""
        val o = JSONObject()
        o.put("terms", JSONArray().apply {
            terms.forEach { put(JSONObject().put("term", it.term).put("definition", it.definition).put("source", it.source)) }
        })
        o.put("tables", JSONArray().apply {
            tables.forEach { t ->
                put(
                    JSONObject().put("title", t.title)
                        .put("headers", JSONArray(t.headers))
                        .put("rows", JSONArray().apply { t.rows.forEach { put(JSONArray(it)) } })
                )
            }
        })
        o.put("classifications", JSONArray().apply {
            classifications.forEach { c ->
                put(
                    JSONObject().put("title", c.title).put("groups", JSONArray().apply {
                        c.groups.forEach { g -> put(JSONObject().put("name", g.name).put("items", JSONArray(g.items))) }
                    })
                )
            }
        })
        return o.toString()
    }

    // Note-style text ("# " headings, "- " bullets, "|" tables) for the PDF export.
    fun toMarkdown(termsLabel: String, tablesLabel: String, classesLabel: String): String {
        val sb = StringBuilder()
        if (terms.isNotEmpty()) {
            sb.append("# ").append(termsLabel).append('\n')
            terms.forEach { sb.append("- ").append(it.term).append(": ").append(it.definition).append('\n') }
            sb.append('\n')
        }
        if (tables.isNotEmpty()) {
            sb.append("# ").append(tablesLabel).append('\n')
            tables.forEach { t ->
                if (t.title.isNotBlank()) sb.append(t.title).append('\n')
                sb.append(row(t.headers)).append('\n')
                sb.append(row(t.headers.map { "---" })).append('\n')
                t.rows.forEach { sb.append(row(it)).append('\n') }
                sb.append('\n')
            }
        }
        if (classifications.isNotEmpty()) {
            sb.append("# ").append(classesLabel).append('\n')
            classifications.forEach { c ->
                if (c.title.isNotBlank()) sb.append(c.title).append('\n')
                c.groups.forEach { g ->
                    sb.append("- ").append(g.name)
                    if (g.items.isNotEmpty()) sb.append(": ").append(g.items.joinToString(", "))
                    sb.append('\n')
                }
                sb.append('\n')
            }
        }
        return sb.toString().trim()
    }

    private fun row(cells: List<String>): String =
        "| " + cells.joinToString(" | ") { it.replace('|', '/').replace('\n', ' ') } + " |"

    companion object {
        val EMPTY = Insights(emptyList(), emptyList(), emptyList())

        // Reads the AI object (or a saved one). Bad or missing parts are skipped.
        fun from(o: JSONObject?): Insights {
            if (o == null) return EMPTY
            val terms = mutableListOf<Term>()
            o.optJSONArray("terms")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val t = arr.optJSONObject(i) ?: continue
                    val term = t.optString("term").trim()
                    val def = t.optString("definition").trim()
                    if (term.isNotEmpty() && def.isNotEmpty()) terms += Term(term, def, t.optString("source").trim())
                }
            }
            val tables = mutableListOf<CompareTable>()
            o.optJSONArray("tables")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val t = arr.optJSONObject(i) ?: continue
                    val headers = strings(t.optJSONArray("headers"))
                    if (headers.size < 2) continue
                    val rows = mutableListOf<List<String>>()
                    t.optJSONArray("rows")?.let { r ->
                        for (j in 0 until r.length()) {
                            val ra = r.optJSONArray(j) ?: continue
                            val cells = (0 until ra.length()).map { ra.optString(it).replace('\n', ' ').trim() }
                            if (cells.all { it.isEmpty() }) continue
                            // Same number of cells as headers, so the table lines up.
                            rows += (cells + List(headers.size) { "" }).take(headers.size)
                        }
                    }
                    if (rows.isNotEmpty()) tables += CompareTable(t.optString("title").trim(), headers, rows)
                }
            }
            val classes = mutableListOf<Classification>()
            o.optJSONArray("classifications")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val c = arr.optJSONObject(i) ?: continue
                    val groups = mutableListOf<ClassGroup>()
                    c.optJSONArray("groups")?.let { g ->
                        for (j in 0 until g.length()) {
                            val go = g.optJSONObject(j) ?: continue
                            val name = go.optString("name").trim()
                            if (name.isNotEmpty()) groups += ClassGroup(name, strings(go.optJSONArray("items")))
                        }
                    }
                    if (groups.isNotEmpty()) classes += Classification(c.optString("title").trim(), groups)
                }
            }
            return Insights(terms, tables, classes)
        }

        fun parse(json: String): Insights =
            if (json.isBlank()) EMPTY else try { from(JSONObject(json)) } catch (e: Exception) { EMPTY }

        private fun strings(arr: JSONArray?): List<String> {
            if (arr == null) return emptyList()
            return (0 until arr.length()).map { arr.optString(it).replace('\n', ' ').trim() }.filter { it.isNotEmpty() }
        }
    }
}
