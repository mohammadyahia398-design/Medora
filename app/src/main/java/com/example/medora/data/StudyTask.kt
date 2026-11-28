package com.example.medora.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// One task in the study planner. day = start of the planned day (local time, millis).
// minutes = planned focus time; focusMinutes = focus time really done on it (from the Focus timer).
@Entity(tableName = "tasks")
data class StudyTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subject: String = "",
    val day: Long,
    val minutes: Int = 25,
    val done: Boolean = false,
    val doneAt: Long = 0,
    val focusMinutes: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface StudyTaskDao {
    @Query("SELECT * FROM tasks ORDER BY day ASC, done ASC, createdAt ASC")
    fun observeAll(): Flow<List<StudyTask>>

    @Insert
    suspend fun insert(task: StudyTask): Long

    @Query("UPDATE tasks SET done = :done, doneAt = :at WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean, at: Long)

    @Query("UPDATE tasks SET focusMinutes = focusMinutes + :minutes WHERE id = :id")
    suspend fun addFocus(id: Long, minutes: Int)

    @Query("SELECT COUNT(*) FROM tasks WHERE day = :day AND done = 0")
    suspend fun countOpenOn(day: Long): Int

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)
}
