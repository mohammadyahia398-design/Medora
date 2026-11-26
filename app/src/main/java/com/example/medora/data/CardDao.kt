package com.example.medora.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE lectureId = :lectureId ORDER BY createdAt DESC")
    fun observeByLecture(lectureId: Long): Flow<List<Card>>

    @Query("SELECT * FROM cards")
    suspend fun getAll(): List<Card>

    @Insert
    suspend fun insertAll(cards: List<Card>)

    @Delete
    suspend fun delete(card: Card)
}
