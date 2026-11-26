package com.example.medora.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LectureDao {
    @Query("SELECT * FROM lectures ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Lecture>>

    @Query("SELECT * FROM lectures WHERE id = :id")
    fun observe(id: Long): Flow<Lecture?>

    @Insert
    suspend fun insert(lecture: Lecture): Long

    @Query("UPDATE lectures SET summary = :summary, slidesJson = :slidesJson WHERE id = :id")
    suspend fun saveAi(id: Long, summary: String, slidesJson: String)

    @Delete
    suspend fun delete(lecture: Lecture)
}
