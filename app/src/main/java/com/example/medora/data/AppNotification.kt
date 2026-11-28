package com.example.medora.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// One line in the notifications center. Only kind + number are saved; the screen builds the
// sentence, so it always shows in the current app language.
@Entity(tableName = "notifications")
data class AppNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val value: Int = 0,
    val at: Long = System.currentTimeMillis(),
    val read: Boolean = false,
) {
    companion object {
        const val KIND_REVIEW = "review"
        const val KIND_TASK = "task"
    }
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY at DESC LIMIT 200")
    fun observeAll(): Flow<List<AppNotification>>

    @Query("SELECT COUNT(*) FROM notifications WHERE `read` = 0")
    fun observeUnread(): Flow<Int>

    @Insert
    suspend fun insert(item: AppNotification): Long

    @Query("UPDATE notifications SET `read` = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE notifications SET `read` = 1 WHERE `read` = 0")
    suspend fun markAllRead()

    @Query("DELETE FROM notifications")
    suspend fun clear()
}
