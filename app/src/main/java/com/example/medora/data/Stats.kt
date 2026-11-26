package com.example.medora.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// One answer in a review session. rating: 1 Again, 2 Hard, 3 Good, 4 Easy.
@Entity(
    tableName = "review_logs",
    foreignKeys = [
        ForeignKey(
            entity = Card::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("cardId")],
)
data class ReviewLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val rating: Int,
    val reviewedAt: Long = System.currentTimeMillis(),
)

// One finished quiz.
@Entity(tableName = "quiz_results")
data class QuizResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val score: Int,
    val total: Int,
    val takenAt: Long = System.currentTimeMillis(),
)

@Dao
interface StatsDao {
    @Insert
    suspend fun insertLog(log: ReviewLog)

    @Insert
    suspend fun insertQuiz(result: QuizResult)

    @Query("SELECT COUNT(*) FROM review_logs WHERE reviewedAt >= :since")
    fun observeReviewsSince(since: Long): Flow<Int>

    @Query("SELECT * FROM quiz_results ORDER BY takenAt DESC LIMIT 10")
    fun observeRecentQuizzes(): Flow<List<QuizResult>>
}
