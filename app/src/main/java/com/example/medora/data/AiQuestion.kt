package com.example.medora.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// A multiple-choice question made by the AI from one lecture.
// options are stored one per line.
@Entity(
    tableName = "ai_questions",
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
data class AiQuestion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lectureId: Long,
    val question: String,
    val options: String,
    val correctIndex: Int,
    val source: String,
)

@Dao
interface AiQuestionDao {
    @Query("SELECT * FROM ai_questions")
    suspend fun getAll(): List<AiQuestion>

    @Query("SELECT COUNT(*) FROM ai_questions")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM ai_questions WHERE lectureId = :lectureId")
    suspend fun deleteByLecture(lectureId: Long)

    @Insert
    suspend fun insertAll(items: List<AiQuestion>)
}
