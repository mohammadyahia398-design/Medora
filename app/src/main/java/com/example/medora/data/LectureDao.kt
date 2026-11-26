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

    @Insert
    suspend fun insert(lecture: Lecture): Long

    @Delete
    suspend fun delete(lecture: Lecture)
}
