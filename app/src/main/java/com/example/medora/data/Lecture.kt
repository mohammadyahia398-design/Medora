package com.example.medora.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// One saved lecture. "content" stays empty until the import batch.
@Entity(tableName = "lectures")
data class Lecture(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subject: String = "",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
