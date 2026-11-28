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

// Version 4: saved mistakes (wrong answers from quiz, exam and practice).
private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `mistakes` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`lectureId` INTEGER NOT NULL, " +
                "`kind` TEXT NOT NULL, " +
                "`question` TEXT NOT NULL, " +
                "`chosen` TEXT NOT NULL, " +
                "`correct` TEXT NOT NULL, " +
                "`source` TEXT NOT NULL, " +
                "`at` INTEGER NOT NULL, " +
                "FOREIGN KEY(`lectureId`) REFERENCES `lectures`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_mistakes_lectureId` ON `mistakes` (`lectureId`)")
    }
}

// Version 5: lecture folder, tags, original file and AI insights; study packs.
private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `lectures` ADD COLUMN `folder` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `lectures` ADD COLUMN `tags` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `lectures` ADD COLUMN `originalFile` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `lectures` ADD COLUMN `insightsJson` TEXT NOT NULL DEFAULT ''")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `study_packs` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`lectureIds` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL)"
        )
    }
}

// Version 6: study planner tasks. Nothing else changes, so all data is kept.
private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `tasks` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`title` TEXT NOT NULL, " +
                "`subject` TEXT NOT NULL, " +
                "`day` INTEGER NOT NULL, " +
                "`minutes` INTEGER NOT NULL, " +
                "`done` INTEGER NOT NULL, " +
                "`doneAt` INTEGER NOT NULL, " +
                "`focusMinutes` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL)"
        )
    }
}

@Database(
    entities = [Lecture::class, Card::class, ReviewLog::class, QuizResult::class, AiQuestion::class, Mistake::class, StudyPack::class, StudyTask::class],
    version = 6,
    exportSchema = false,
)
abstract class MedoraDatabase : RoomDatabase() {
    abstract fun lectureDao(): LectureDao
    abstract fun cardDao(): CardDao
    abstract fun statsDao(): StatsDao
    abstract fun aiQuestionDao(): AiQuestionDao
    abstract fun mistakeDao(): MistakeDao
    abstract fun studyPackDao(): StudyPackDao
    abstract fun taskDao(): StudyTaskDao

    companion object {
        @Volatile private var instance: MedoraDatabase? = null

        // Used by restore: closes the database so the file can be replaced.
        fun closeAndReset() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }

        fun get(context: Context): MedoraDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MedoraDatabase::class.java,
                    "medora.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build().also { instance = it }
            }
    }
}
