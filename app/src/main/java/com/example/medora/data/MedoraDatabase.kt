package com.example.medora.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Version 2 adds the cards table. The migration keeps existing lectures safe.
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `cards` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`lectureId` INTEGER NOT NULL, " +
                "`front` TEXT NOT NULL, " +
                "`back` TEXT NOT NULL, " +
                "`source` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`lectureId`) REFERENCES `lectures`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cards_lectureId` ON `cards` (`lectureId`)")
    }
}

@Database(entities = [Lecture::class, Card::class], version = 2, exportSchema = false)
abstract class MedoraDatabase : RoomDatabase() {
    abstract fun lectureDao(): LectureDao
    abstract fun cardDao(): CardDao

    companion object {
        @Volatile private var instance: MedoraDatabase? = null

        fun get(context: Context): MedoraDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MedoraDatabase::class.java,
                    "medora.db",
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
