package com.example.medora.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// A study pack groups several lectures (for one exam, say): their summaries,
// terms, tables, cards and questions are shown and exported together.
// lectureIds is a comma-separated list; deleted lectures are simply skipped.
@Entity(tableName = "study_packs")
data class StudyPack(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val lectureIds: String = "",
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun ids(): List<Long> = lectureIds.split(',').mapNotNull { it.trim().toLongOrNull() }.distinct()

    companion object {
        fun join(ids: Collection<Long>): String = ids.distinct().joinToString(",")
    }
}

@Dao
interface StudyPackDao {
    @Query("SELECT * FROM study_packs ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<StudyPack>>

    @Query("SELECT * FROM study_packs WHERE id = :id")
    fun observe(id: Long): Flow<StudyPack?>

    @Insert
    suspend fun insert(pack: StudyPack): Long

    @Query("UPDATE study_packs SET name = :name, lectureIds = :lectureIds WHERE id = :id")
    suspend fun update(id: Long, name: String, lectureIds: String)

    @Query("DELETE FROM study_packs WHERE id = :id")
    suspend fun delete(id: Long)
}
