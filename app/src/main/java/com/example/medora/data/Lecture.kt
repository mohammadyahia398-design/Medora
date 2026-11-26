package com.example.medora.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// One saved lecture. summary and slidesJson (v3) are filled by the optional AI step.
@Entity(tableName = "lectures")
data class Lecture(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subject: String = "",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''") val summary: String = "",
    @ColumnInfo(defaultValue = "''") val slidesJson: String = "",
)
