package com.example.medora.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// One flashcard. Always linked to the lecture it came from (lectureId + source text).
// Deleting a lecture deletes its cards.
@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = Lecture::class,
            parentColumns = ["id"],
            childColumns = ["lectureId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("lectureId")],
)
data class Card(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lectureId: Long,
    val front: String,
    val back: String,
    val source: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
