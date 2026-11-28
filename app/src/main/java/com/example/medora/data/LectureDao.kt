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

    @Query("SELECT * FROM lectures")
    suspend fun getAll(): List<Lecture>

    @Insert
    suspend fun insert(lecture: Lecture): Long

    @Query("UPDATE lectures SET summary = :summary, slidesJson = :slidesJson, insightsJson = :insightsJson WHERE id = :id")
    suspend fun saveAi(id: Long, summary: String, slidesJson: String, insightsJson: String)

    @Query("UPDATE lectures SET title = :title, content = :content, stickersJson = :stickersJson WHERE id = :id")
    suspend fun updateNote(id: Long, title: String, content: String, stickersJson: String)

    @Query("UPDATE lectures SET subject = :subject, folder = :folder, tags = :tags WHERE id = :id")
    suspend fun organize(id: Long, subject: String, folder: String, tags: String)

    @Delete
    suspend fun delete(lecture: Lecture)
}
