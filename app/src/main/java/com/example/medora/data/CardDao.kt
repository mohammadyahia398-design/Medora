package com.example.medora.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE lectureId = :lectureId ORDER BY createdAt DESC")
    fun observeByLecture(lectureId: Long): Flow<List<Card>>

    @Query("SELECT * FROM cards")
    suspend fun getAll(): List<Card>

    @Query("SELECT * FROM cards WHERE due <= :now ORDER BY due ASC LIMIT :limit")
    suspend fun getDue(now: Long, limit: Int): List<Card>

    @Query("SELECT COUNT(*) FROM cards WHERE due <= :now")
    fun observeDueCount(now: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE reps > 0")
    fun observeLearnedCount(): Flow<Int>

    @Insert
    suspend fun insertAll(cards: List<Card>)

    @Update
    suspend fun update(card: Card)

    @Delete
    suspend fun delete(card: Card)
}
