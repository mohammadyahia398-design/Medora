package com.example.medora.export

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// Writes a simple .pptx by hand (a PPTX is a ZIP of XML files). No library.
// Slides come from the lecture's slidesJson: [{"title":"...","bullets":["..."]}].
// Markdown tables (| a | b |) in the bullets or in the note become real PowerPoint tables.
object PptxExporter {
    private class SlideData(
        val title: String,
        val lines: List<String>,
        val bullets: Boolean,
        val table: List<List<String>>? = null,
        val header: Boolean = false,
    )

    private const val TABLE_ROWS_PER_SLIDE = 8

    private const val NS =
        "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" " +
            "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" " +
            "xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\""
    private const val XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
    private const val REL_NS = "http://schemas.openxmlformats.org/package/2006/relationships"
    private const val REL_BASE = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val CT_BASE = "application/vnd.openxmlformats-officedocument.presentationml"

    private const val EMPTY_TREE =
        "<p:spTree><p:nvGrpSpPr><p:cNvPr id=\"1\" name=\"\"/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>" +
            "<p:grpSpPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"0\" cy=\"0\"/>" +
            "<a:chOff x=\"0\" y=\"0\"/><a:chExt cx=\"0\" cy=\"0\"/></a:xfrm></p:grpSpPr>"

    // True when the note has at least one markdown table (so a PowerPoint can be made from it).
    fun hasTable(content: String): Boolean =
        content.lineSequence().any { val t = it.trim(); t.startsWith("|") && !isSeparator(t) }

    fun write(context: Context, uri: Uri, title: String, subject: String, slidesJson: String, content: String = "") {
        val slides = mutableListOf<SlideData>()
        slides += SlideData(title, listOf(subject).filter { it.isNotBlank() }, false)
        if (slidesJson.isNotBlank()) {
            val arr = JSONArray(slidesJson)
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val b = o.optJSONArray("bullets")
                val lines = mutableListOf<String>()
                if (b != null) for (j in 0 until b.length()) lines += b.optString(j)
                addSlides(slides, o.optString("title"), lines.filter { it.isNotBlank() })
            }
        }
        // Tables written in the note itself, each under its nearest "# " heading.
        var heading = title
        val noteLines = content.split("\n")
        var i = 0
        while (i < noteLines.size) {
            val t = noteLines[i].trim()
            if (t.startsWith("# ")) heading = t.removePrefix("# ").trim()
            if (t.startsWith("|")) {
                val start = i
                while (i < noteLines.size && noteLines[i].trim().startsWith("|")) i++
                addTableSlides(slides, heading, noteLines.subList(start, i))
            } else {
                i++
            }
        }
        if (slides.size < 2) throw IllegalStateException("No slides")

        val out = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("Cannot open output")
        out.use { raw ->
            ZipOutputStream(raw).use { zip ->
                zip.add("[Content_Types].xml", contentTypes(slides.size))
                zip.add("_rels/.rels", rootRels())
                zip.add("ppt/presentation.xml", presentation(slides.size))
                zip.add("ppt/_rels/presentation.xml.rels", presentationRels(slides.size))
                zip.add("ppt/tableStyles.xml", tableStyles())
                zip.add("ppt/slideMasters/slideMaster1.xml", master())
                zip.add("ppt/slideMasters/_rels/slideMaster1.xml.rels", masterRels())
                zip.add("ppt/slideLayouts/slideLayout1.xml", layout())
                zip.add("ppt/slideLayouts/_rels/slideLayout1.xml.rels", layoutRels())
                zip.add("ppt/theme/theme1.xml", theme())
                slides.forEachIndexed { n, s ->
                    zip.add("ppt/slides/slide${n + 1}.xml", slide(s, cover = n == 0))
                    zip.add("ppt/slides/_rels/slide${n + 1}.xml.rels", slideRels())
                }
            }
        }
    }

    // One AI slide: its text lines stay together; each table gets its own slide(s).
    private fun addSlides(slides: MutableList<SlideData>, title: String, lines: List<String>) {
        val text = mutableListOf<String>()
        val tables = mutableListOf<List<String>>()
        var i = 0
        while (i < lines.size) {
            if (lines[i].trim().startsWith("|")) {
                val start = i
                while (i < lines.size && lines[i].trim().startsWith("|")) i++
                tables += lines.subList(start, i)
            } else {
                text += lines[i]
                i++
            }
        }
        if (text.isNotEmpty() || tables.isEmpty()) slides += SlideData(title, text, true)
        tables.forEach { addTableSlides(slides, title, it) }
    }

    // Long tables are split; the header row repeats on every part.
    private fun addTableSlides(slides: MutableList<SlideData>, title: String, raw: List<String>) {
        val trimmed = raw.map { it.trim() }
        val rows = trimmed.filter { !isSeparator(it) }.map { cells(it) }
        if (rows.isEmpty()) return
        val header = trimmed.size > 1 && isSeparator(trimmed[1])
        val cols = rows.maxOf { it.size }.coerceAtLeast(1)
        val padded = rows.map { r -> List(cols) { c -> r.getOrElse(c) { "" } } }
        val head = if (header) padded.first() else null
        val body = if (header) padded.drop(1) else padded
        if (body.isEmpty()) {
            slides += SlideData(title, emptyList(), false, padded, header)
            return
        }
        body.chunked(TABLE_ROWS_PER_SLIDE).forEach { part ->
            slides += SlideData(title, emptyList(), false, if (head != null) listOf(head) + part else part, header)
        }
    }

    private fun cells(line: String): List<String> =
        line.trim().trim('|').split('|').map { it.trim() }

    private fun isSeparator(line: String): Boolean =
        cells(line).all { c -> c.isNotEmpty() && c.all { it == '-' || it == ':' } }

    private fun ZipOutputStream.add(name: String, text: String) {
        putNextEntry(ZipEntry(name))
        write(text.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun contentTypes(n: Int): String {
        val sb = StringBuilder()
        sb.append(XML)
        sb.append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
        sb.append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
        sb.append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
        sb.append("<Override PartName=\"/ppt/presentation.xml\" ContentType=\"$CT_BASE.presentation.main+xml\"/>")
        sb.append("<Override PartName=\"/ppt/tableStyles.xml\" ContentType=\"$CT_BASE.tableStyles+xml\"/>")
        sb.append("<Override PartName=\"/ppt/slideMasters/slideMaster1.xml\" ContentType=\"$CT_BASE.slideMaster+xml\"/>")
        sb.append("<Override PartName=\"/ppt/slideLayouts/slideLayout1.xml\" ContentType=\"$CT_BASE.slideLayout+xml\"/>")
        sb.append("<Override PartName=\"/ppt/theme/theme1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.theme+xml\"/>")
        for (i in 1..n) {
            sb.append("<Override PartName=\"/ppt/slides/slide$i.xml\" ContentType=\"$CT_BASE.slide+xml\"/>")
        }
        sb.append("</Types>")
        return sb.toString()
    }

    private fun rootRels() = XML +
        "<Relationships xmlns=\"$REL_NS\">" +
        "<Relationship Id=\"rId1\" Type=\"$REL_BASE/officeDocument\" Target=\"ppt/presentation.xml\"/>" +
        "</Relationships>"

    private fun presentation(n: Int): String {
        val ids = StringBuilder()
        for (i in 1..n) ids.append("<p:sldId id=\"${255 + i}\" r:id=\"rId${i + 1}\"/>")
        return XML + "<p:presentation $NS>" +
            "<p:sldMasterIdLst><p:sldMasterId id=\"2147483648\" r:id=\"rId1\"/></p:sldMasterIdLst>" +
            "<p:sldIdLst>$ids</p:sldIdLst>" +
            "<p:sldSz cx=\"12192000\" cy=\"6858000\"/><p:notesSz cx=\"6858000\" cy=\"9144000\"/>" +
            "</p:presentation>"
    }

    private fun presentationRels(n: Int): String {
        val sb = StringBuilder(XML)
        sb.append("<Relationships xmlns=\"$REL_NS\">")
        sb.append("<Relationship Id=\"rId1\" Type=\"$REL_BASE/slideMaster\" Target=\"slideMasters/slideMaster1.xml\"/>")
        for (i in 1..n) {
            sb.append("<Relationship Id=\"rId${i + 1}\" Type=\"$REL_BASE/slide\" Target=\"slides/slide$i.xml\"/>")
        }
        sb.append("<Relationship Id=\"rId${n + 2}\" Type=\"$REL_BASE/theme\" Target=\"theme/theme1.xml\"/>")
        sb.append("<Relationship Id=\"rId${n + 3}\" Type=\"$REL_BASE/tableStyles\" Target=\"tableStyles.xml\"/>")
        sb.append("</Relationships>")
        return sb.toString()
    }

    private fun master() = XML + "<p:sldMaster $NS>" +
        "<p:cSld><p:bg><p:bgRef idx=\"1001\"><a:schemeClr val=\"bg1\"/></p:bgRef></p:bg>$EMPTY_TREE</p:spTree></p:cSld>" +
        "<p:clrMap bg1=\"lt1\" tx1=\"dk1\" bg2=\"lt2\" tx2=\"dk2\" accent1=\"accent1\" accent2=\"accent2\" " +
        "accent3=\"accent3\" accent4=\"accent4\" accent5=\"accent5\" accent6=\"accent6\" hlink=\"hlink\" folHlink=\"folHlink\"/>" +
        "<p:sldLayoutIdLst><p:sldLayoutId id=\"2147483649\" r:id=\"rId1\"/></p:sldLayoutIdLst>" +
        "</p:sldMaster>"

    private fun masterRels() = XML + "<Relationships xmlns=\"$REL_NS\">" +
        "<Relationship Id=\"rId1\" Type=\"$REL_BASE/slideLayout\" Target=\"../slideLayouts/slideLayout1.xml\"/>" +
        "<Relationship Id=\"rId2\" Type=\"$REL_BASE/theme\" Target=\"../theme/theme1.xml\"/>" +
        "</Relationships>"

    private fun layout() = XML + "<p:sldLayout $NS type=\"blank\" preserve=\"1\">" +
        "<p:cSld name=\"Blank\">$EMPTY_TREE</p:spTree></p:cSld>" +
        "<p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sldLayout>"

    private fun layoutRels() = XML + "<Relationships xmlns=\"$REL_NS\">" +
        "<Relationship Id=\"rId1\" Type=\"$REL_BASE/slideMaster\" Target=\"../slideMasters/slideMaster1.xml\"/>" +
        "</Relationships>"

    private fun slideRels() = XML + "<Relationships xmlns=\"$REL_NS\">" +
        "<Relationship Id=\"rId1\" Type=\"$REL_BASE/slideLayout\" Target=\"../slideLayouts/slideLayout1.xml\"/>" +
        "</Relationships>"

    private fun slide(s: SlideData, cover: Boolean): String {
        val titleY = if (cover) 2200000 else 300000
        val titleH = if (cover) 1400000 else 1000000
        val titleSize = if (cover) 4400 else 3200
        val bodyY = if (cover) 3700000 else 1500000
        val bodyH = if (cover) 1500000 else 4800000
        val bodySize = if (cover) 2400 else 2200
        val sb = StringBuilder(XML)
        sb.append("<p:sld $NS><p:cSld>$EMPTY_TREE")
        sb.append(shape(2, "Title", titleY, titleH, listOf(s.title), titleSize, bold = true, bullets = false, color = "0F172A"))
        if (s.table != null) {
            sb.append(table(3, s.table, s.header))
        } else if (s.lines.isNotEmpty()) {
            sb.append(shape(3, "Body", bodyY, bodyH, s.lines, bodySize, bold = false, bullets = s.bullets, color = "334155"))
        }
        sb.append("</p:spTree></p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sld>")
        return sb.toString()
    }

    private fun shape(
        id: Int, name: String, y: Int, h: Int, lines: List<String>,
        size: Int, bold: Boolean, bullets: Boolean, color: String,
    ): String {
        val sb = StringBuilder()
        sb.append("<p:sp><p:nvSpPr><p:cNvPr id=\"$id\" name=\"$name\"/><p:cNvSpPr txBox=\"1\"/><p:nvPr/></p:nvSpPr>")
        sb.append("<p:spPr><a:xfrm><a:off x=\"457200\" y=\"$y\"/><a:ext cx=\"11277600\" cy=\"$h\"/></a:xfrm>")
        sb.append("<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></p:spPr>")
        sb.append("<p:txBody><a:bodyPr wrap=\"square\" rtlCol=\"0\"><a:normAutofit/></a:bodyPr><a:lstStyle/>")
        for (line in lines) sb.append(paragraph(line, size, bold, bullets, color))
        sb.append("</p:txBody></p:sp>")
        return sb.toString()
    }

    private fun tableStyles() = XML +
        "<a:tblStyleLst xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" " +
        "def=\"{5C22544A-7EE6-4342-B048-85BDC9FD1C3A}\"/>"

    // A real PowerPoint table: colored header row, light banded rows, thin borders.
    private fun table(id: Int, rows: List<List<String>>, header: Boolean): String {
        val cols = rows.first().size
        val totalW = 11277600
        val top = 1500000
        val rowH = minOf(560000, (6858000 - top - 300000) / rows.size)
        val size = when {
            cols <= 3 -> 1800
            cols <= 5 -> 1500
            else -> 1200
        }
        val all = rows.flatten().filter { it.isNotBlank() }
        val rtl = all.count { isRtl(it) } * 2 > all.size
        val colW = totalW / cols
        val sb = StringBuilder()
        sb.append("<p:graphicFrame><p:nvGraphicFramePr><p:cNvPr id=\"$id\" name=\"Table\"/>")
        sb.append("<p:cNvGraphicFramePr><a:graphicFrameLocks noGrp=\"1\"/></p:cNvGraphicFramePr><p:nvPr/></p:nvGraphicFramePr>")
        sb.append("<p:xfrm><a:off x=\"457200\" y=\"$top\"/><a:ext cx=\"$totalW\" cy=\"${rowH * rows.size}\"/></p:xfrm>")
        sb.append("<a:graphic><a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/table\"><a:tbl>")
        sb.append("<a:tblPr firstRow=\"${if (header) 1 else 0}\" bandRow=\"1\"/>")
        sb.append("<a:tblGrid>")
        for (c in 0 until cols) {
            val w = if (c == cols - 1) totalW - colW * (cols - 1) else colW
            sb.append("<a:gridCol w=\"$w\"/>")
        }
        sb.append("</a:tblGrid>")
        rows.forEachIndexed { r, row ->
            val isHead = header && r == 0
            val fill = when {
                isHead -> "0E7490"
                (if (header) r - 1 else r) % 2 == 1 -> "F1F5F9"
                else -> "FFFFFF"
            }
            val color = if (isHead) "FFFFFF" else "1E293B"
            sb.append("<a:tr h=\"$rowH\">")
            // Arabic tables: first column on the right (reversed here, so every viewer shows it right).
            (if (rtl) row.asReversed() else row).forEach { cell -> sb.append(tableCell(cell, size, isHead, color, fill)) }
            sb.append("</a:tr>")
        }
        sb.append("</a:tbl></a:graphicData></a:graphic></p:graphicFrame>")
        return sb.toString()
    }

    private fun tableCell(text: String, size: Int, bold: Boolean, color: String, fill: String): String {
        val rtl = isRtl(text)
        val lang = if (rtl) "ar-SA" else "en-US"
        val para = if (text.isBlank()) {
            "<a:p><a:endParaRPr lang=\"$lang\" sz=\"$size\" dirty=\"0\"/></a:p>"
        } else {
            "<a:p><a:pPr algn=\"${if (rtl) "r" else "l"}\" rtl=\"${if (rtl) 1 else 0}\"/>" +
                "<a:r><a:rPr lang=\"$lang\" sz=\"$size\" b=\"${if (bold) 1 else 0}\" dirty=\"0\">" +
                "<a:solidFill><a:srgbClr val=\"$color\"/></a:solidFill>" +
                "<a:latin typeface=\"Arial\"/><a:cs typeface=\"Arial\"/></a:rPr>" +
                "<a:t>${esc(text)}</a:t></a:r></a:p>"
        }
        val line = { tag: String -> "<a:$tag w=\"9525\"><a:solidFill><a:srgbClr val=\"CBD5E1\"/></a:solidFill></a:$tag>" }
        return "<a:tc><a:txBody><a:bodyPr/><a:lstStyle/>$para</a:txBody>" +
            "<a:tcPr marL=\"91440\" marR=\"91440\" marT=\"45720\" marB=\"45720\" anchor=\"ctr\">" +
            line("lnL") + line("lnR") + line("lnT") + line("lnB") +
            "<a:solidFill><a:srgbClr val=\"$fill\"/></a:solidFill></a:tcPr></a:tc>"
    }

    private fun paragraph(text: String, size: Int, bold: Boolean, bullet: Boolean, color: String): String {
        val rtl = isRtl(text)
        val align = if (rtl) "r" else "l"
        val lang = if (rtl) "ar-SA" else "en-US"
        val pAttrs = (if (bullet) "marL=\"342900\" indent=\"-342900\" " else "") +
            "algn=\"$align\" rtl=\"${if (rtl) 1 else 0}\""
        val pKids = "<a:spcAft><a:spcPts val=\"600\"/></a:spcAft>" +
            (if (bullet) "<a:buFont typeface=\"Arial\"/><a:buChar char=\"&#8226;\"/>" else "<a:buNone/>")
        return "<a:p><a:pPr $pAttrs>$pKids</a:pPr>" +
            "<a:r><a:rPr lang=\"$lang\" sz=\"$size\" b=\"${if (bold) 1 else 0}\" dirty=\"0\">" +
            "<a:solidFill><a:srgbClr val=\"$color\"/></a:solidFill>" +
            "<a:latin typeface=\"Arial\"/><a:cs typeface=\"Arial\"/></a:rPr>" +
            "<a:t>${esc(text)}</a:t></a:r></a:p>"
    }

    // Right-to-left if the first letter found is Arabic.
    private fun isRtl(s: String): Boolean {
        for (ch in s) {
            if (ch in '\u0600'..'\u06FF' || ch in '\u0750'..'\u077F') return true
            if (ch.isLetter()) return false
        }
        return false
    }

    private fun esc(s: String): String {
        val sb = StringBuilder()
        for (ch in s) {
            when {
                ch == '&' -> sb.append("&amp;")
                ch == '<' -> sb.append("&lt;")
                ch == '>' -> sb.append("&gt;")
                ch == '"' -> sb.append("&quot;")
                ch.code < 32 && ch != '\n' && ch != '\t' -> {}
                else -> sb.append(ch)
            }
        }
        return sb.toString().replace('\n', ' ')
    }

    private fun theme(): String {
        val fills = "<a:solidFill><a:schemeClr val=\"phClr\"/></a:solidFill>".repeat(3)
        val lines = listOf(6350, 12700, 19050).joinToString("") {
            "<a:ln w=\"$it\"><a:solidFill><a:schemeClr val=\"phClr\"/></a:solidFill></a:ln>"
        }
        val effects = "<a:effectStyle><a:effectLst/></a:effectStyle>".repeat(3)
        val font = "<a:latin typeface=\"Arial\"/><a:ea typeface=\"\"/><a:cs typeface=\"Arial\"/>"
        fun clr(n: String, v: String) = "<a:$n><a:srgbClr val=\"$v\"/></a:$n>"
        return XML + "<a:theme xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" name=\"Medora\">" +
            "<a:themeElements><a:clrScheme name=\"Medora\">" +
            clr("dk1", "000000") + clr("lt1", "FFFFFF") + clr("dk2", "1F2937") + clr("lt2", "F3F4F6") +
            clr("accent1", "0E7490") + clr("accent2", "2563EB") + clr("accent3", "16A34A") +
            clr("accent4", "D97706") + clr("accent5", "DC2626") + clr("accent6", "7C3AED") +
            clr("hlink", "2563EB") + clr("folHlink", "7C3AED") +
            "</a:clrScheme><a:fontScheme name=\"Medora\"><a:majorFont>$font</a:majorFont><a:minorFont>$font</a:minorFont></a:fontScheme>" +
            "<a:fmtScheme name=\"Medora\"><a:fillStyleLst>$fills</a:fillStyleLst><a:lnStyleLst>$lines</a:lnStyleLst>" +
            "<a:effectStyleLst>$effects</a:effectStyleLst><a:bgFillStyleLst>$fills</a:bgFillStyleLst></a:fmtScheme>" +
            "</a:themeElements></a:theme>"
    }
}
