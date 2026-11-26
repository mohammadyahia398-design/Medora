package com.example.medora.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// One flashcard. Always linked to the lecture it came from (lectureId + source text).
// Deleting a lecture deletes its cards.
// The FSRS fields (v3) say when the card is due again. due = 0 means "new, due now".
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
    @ColumnInfo(defaultValue = "0") val due: Long = 0,
    @ColumnInfo(defaultValue = "0") val stability: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val difficulty: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val reps: Int = 0,
    @ColumnInfo(defaultValue = "0") val lapses: Int = 0,
    @ColumnInfo(defaultValue = "0") val lastReview: Long = 0,
)
