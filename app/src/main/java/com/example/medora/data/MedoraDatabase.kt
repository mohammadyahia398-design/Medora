package com.example.medora.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Lecture::class], version = 1, exportSchema = false)
abstract class MedoraDatabase : RoomDatabase() {
    abstract fun lectureDao(): LectureDao

    companion object {
        @Volatile private var instance: MedoraDatabase? = null

        fun get(context: Context): MedoraDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MedoraDatabase::class.java,
                    "medora.db",
                ).build().also { instance = it }
            }
    }
}
