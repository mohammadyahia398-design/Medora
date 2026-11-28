package com.example.medora.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// One saved lecture. summary and slidesJson (v3) are filled by the optional AI step.
// v5: folder + tags (organizing), originalFile (copy of the imported file in app storage),
// insightsJson (AI terms, comparison tables and classifications).
@Entity(tableName = "lectures")
data class Lecture(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subject: String = "",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''") val summary: String = "",
    @ColumnInfo(defaultValue = "''") val slidesJson: String = "",
    @ColumnInfo(defaultValue = "''") val folder: String = "",
    @ColumnInfo(defaultValue = "''") val tags: String = "",
    @ColumnInfo(defaultValue = "''") val originalFile: String = "",
    @ColumnInfo(defaultValue = "''") val insightsJson: String = "",
    // v7: stickers placed on the note (JSON, see Stickers).
    @ColumnInfo(defaultValue = "''") val stickersJson: String = "",
) {
    // Tags are saved as one comma-separated text.
    fun tagList(): List<String> = splitTags(tags)

    companion object {
        fun splitTags(raw: String): List<String> =
            raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    }
}
