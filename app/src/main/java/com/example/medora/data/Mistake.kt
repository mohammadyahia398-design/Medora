package com.example.medora.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// A wrong answer saved from a quiz, exam or practice session (v4).
// kind = "quiz" | "exam" | "practice". Deleting a lecture deletes its mistakes.
@Entity(
    tableName = "mistakes",
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
data class Mistake(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lectureId: Long,
    val kind: String,
    val question: String,
    val chosen: String,
    val correct: String,
    val source: String,
    val at: Long = System.currentTimeMillis(),
)

@Dao
interface MistakeDao {
    @Insert
    suspend fun insertAll(items: List<Mistake>)

    @Query("SELECT * FROM mistakes ORDER BY at DESC")
    suspend fun getAll(): List<Mistake>

    @Query("SELECT COUNT(*) FROM mistakes")
    fun observeCount(): Flow<Int>
}
