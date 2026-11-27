package com.example.medora.data

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import java.io.File

// Backup = a copy of the Room database file (lectures, cards, reviews, quizzes).
// API keys are NOT included (they stay on the device).
object BackupManager {
    private const val DB_NAME = "medora.db"

    fun export(context: Context, target: Uri): Boolean {
        return try {
            val db = MedoraDatabase.get(context)
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
            val file = context.getDatabasePath(DB_NAME)
            val out = context.contentResolver.openOutputStream(target, "w") ?: return false
            out.use { o -> file.inputStream().use { it.copyTo(o) } }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun restore(context: Context, source: Uri): Boolean {
        val temp = File(context.cacheDir, "restore.tmp")
        return try {
            val input = context.contentResolver.openInputStream(source) ?: return false
            input.use { i -> temp.outputStream().use { i.copyTo(it) } }
            if (!isValid(temp)) return false
            MedoraDatabase.closeAndReset()
            val dbFile = context.getDatabasePath(DB_NAME)
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
            temp.copyTo(dbFile, overwrite = true)
            true
        } catch (e: Exception) {
            false
        } finally {
            temp.delete()
            File(temp.path + "-wal").delete()
            File(temp.path + "-shm").delete()
        }
    }

    // Must be a SQLite file, from this app (has lectures table), with a known version.
    private fun isValid(file: File): Boolean {
        val header = ByteArray(16)
        val read = file.inputStream().use { it.read(header) }
        if (read != 16 || String(header, 0, 15, Charsets.US_ASCII) != "SQLite format 3") return false
        return try {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                val hasLectures = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='lectures'",
                    null,
                ).use { it.moveToFirst() }
                db.version in 1..4 && hasLectures
            }
        } catch (e: Exception) {
            false
        }
    }

    fun restartApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (intent != null) context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}
