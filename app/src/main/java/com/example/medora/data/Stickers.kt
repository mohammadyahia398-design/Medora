package com.example.medora.data

import org.json.JSONArray
import org.json.JSONObject

// One sticker placed on a note. x and y are fractions (0..1) of the note board,
// scale 1 = normal size, rot = degrees. A sticker is either a photo (image path) or an emoji.
data class Sticker(
    val id: Long,
    val image: String = "",
    val emoji: String = "",
    val x: Float = 0.5f,
    val y: Float = 0.4f,
    val scale: Float = 1f,
    val rot: Float = 0f,
)

// Stickers are saved with the note as one JSON text (lectures.stickersJson).
object Stickers {
    private val IMAGE_PATH = Regex("""note_images/[A-Za-z0-9._-]+""")

    val BUILT_IN = listOf(
        "\uD83E\uDDE0", "\u2764\uFE0F", "\uD83E\uDDB4", "\uD83D\uDC8A", "\uD83D\uDC89", "\uD83E\uDE7A",
        "\uD83E\uDDEC", "\uD83D\uDD2C", "\uD83E\uDDEA", "\uD83E\uDE78", "\uD83E\uDDB7", "\uD83D\uDC41\uFE0F",
        "\u2B50", "\u2705", "\u2757", "\u26A0\uFE0F", "\uD83D\uDCA1", "\uD83D\uDCCC",
        "\uD83D\uDD25", "\uD83D\uDCD6", "\uD83C\uDFAF", "\uD83D\uDC4D", "\uD83D\uDE00", "\uD83D\uDD14",
    )

    fun parse(json: String): List<Sticker> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val image = o.optString("image").takeIf { IMAGE_PATH.matches(it) } ?: ""
                val emoji = o.optString("emoji")
                if (image.isEmpty() && emoji.isEmpty()) return@mapNotNull null
                Sticker(
                    id = o.optLong("id", i.toLong()),
                    image = image,
                    emoji = emoji,
                    x = o.optDouble("x", 0.5).toFloat().coerceIn(0f, 1f),
                    y = o.optDouble("y", 0.4).toFloat().coerceIn(0f, 1f),
                    scale = o.optDouble("scale", 1.0).toFloat().coerceIn(0.3f, 6f),
                    rot = o.optDouble("rot", 0.0).toFloat(),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun toJson(list: List<Sticker>): String {
        if (list.isEmpty()) return ""
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("image", it.image)
                    .put("emoji", it.emoji)
                    .put("x", it.x.toDouble())
                    .put("y", it.y.toDouble())
                    .put("scale", it.scale.toDouble())
                    .put("rot", it.rot.toDouble()),
            )
        }
        return arr.toString()
    }

    // Photo stickers use the same image folder as note images (kept in backups).
    fun imagePaths(json: String): Set<String> =
        parse(json).mapNotNull { s -> s.image.takeIf { it.isNotEmpty() } }.toSet()
}
