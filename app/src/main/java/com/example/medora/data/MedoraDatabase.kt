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

// Version 3: FSRS fields on cards, AI fields on lectures, review logs, quiz results,
// AI questions. Existing lectures and cards are kept (old cards become "new, due now").
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `cards` ADD COLUMN `due` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `cards` ADD COLUMN `stability` REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `cards` ADD COLUMN `difficulty` REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `cards` ADD COLUMN `reps` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `cards` ADD COLUMN `lapses` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `cards` ADD COLUMN `lastReview` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `lectures` ADD COLUMN `summary` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `lectures` ADD COLUMN `slidesJson` TEXT NOT NULL DEFAULT ''")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `review_logs` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`cardId` INTEGER NOT NULL, " +
                "`rating` INTEGER NOT NULL, " +
                "`reviewedAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`cardId`) REFERENCES `cards`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_review_logs_cardId` ON `review_logs` (`cardId`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `quiz_results` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`score` INTEGER NOT NULL, " +
                "`total` INTEGER NOT NULL, " +
                "`takenAt` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `ai_questions` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`lectureId` INTEGER NOT NULL, " +
                "`question` TEXT NOT NULL, " +
                "`options` TEXT NOT NULL, " +
                "`correctIndex` INTEGER NOT NULL, " +
                "`source` TEXT NOT NULL, " +
                "FOREIGN KEY(`lectureId`) REFERENCES `lectures`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_questions_lectureId` ON `ai_questions` (`lectureId`)")
    }
}

@Database(
    entities = [Lecture::class, Card::class, ReviewLog::class, QuizResult::class, AiQuestion::class],
    version = 3,
    exportSchema = false,
)
abstract class MedoraDatabase : RoomDatabase() {
    abstract fun lectureDao(): LectureDao
    abstract fun cardDao(): CardDao
    abstract fun statsDao(): StatsDao
    abstract fun aiQuestionDao(): AiQuestionDao

    companion object {
        @Volatile private var instance: MedoraDatabase? = null

        fun get(context: Context): MedoraDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MedoraDatabase::class.java,
                    "medora.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
    }
}
