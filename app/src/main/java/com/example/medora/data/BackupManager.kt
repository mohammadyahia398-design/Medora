package com.example.medora.data

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// Backup = one .zip with the Room database (lectures, cards, reviews, quizzes)
// and the note images. API keys are NOT included (they stay on the device).
// Save it to Google Drive from the file picker to move to a new phone.
// Restore also accepts the old backup (a plain .db file, without images).
object BackupManager {
    private const val DB_NAME = "medora.db"
    private const val DB_ENTRY = "medora.db"
    private val IMAGE_ENTRY = Regex("""note_images/[A-Za-z0-9._-]+""")

    fun export(context: Context, target: Uri): Boolean {
        return try {
            val db = MedoraDatabase.get(context)
            val sql = db.openHelper.writableDatabase
            sql.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
            val used = mutableSetOf<String>()
            sql.query("SELECT content FROM lectures").use { c ->
                while (c.moveToNext()) {
                    c.getString(0).orEmpty().lineSequence().forEach { line ->
                        NoteImages.pathOf(line)?.let { used += it }
                    }
                }
            }
            NoteImages.deleteUnused(context, used)
            val file = context.getDatabasePath(DB_NAME)
            val out = context.contentResolver.openOutputStream(target, "w") ?: return false
            out.use { o ->
                ZipOutputStream(o).use { zip ->
                    zip.putNextEntry(ZipEntry(DB_ENTRY))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                    used.sorted().forEach { path ->
                        val img = NoteImages.file(context, path)
                        if (img.exists()) {
                            zip.putNextEntry(ZipEntry(path))
                            img.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun restore(context: Context, source: Uri): Boolean {
        val temp = File(context.cacheDir, "restore.tmp")
        val stage = File(context.cacheDir, "restore_images")
        return try {
            stage.deleteRecursively()
            val head = ByteArray(2)
            val n = context.contentResolver.openInputStream(source)?.use { it.read(head) } ?: return false
            val isZip = n == 2 && head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte()
            var images = 0
            if (isZip) {
                var hasDb = false
                val input = context.contentResolver.openInputStream(source) ?: return false
                ZipInputStream(input).use { zip ->
                    while (true) {
                        val e = zip.nextEntry ?: break
                        when {
                            e.isDirectory -> {}
                            e.name == DB_ENTRY -> {
                                temp.outputStream().use { zip.copyTo(it) }
                                hasDb = true
                            }
                            // Only our own image names: nothing can be written outside the folder.
                            IMAGE_ENTRY.matches(e.name) -> {
                                val f = File(stage, e.name.substringAfter('/'))
                                f.parentFile?.mkdirs()
                                f.outputStream().use { zip.copyTo(it) }
                                images++
                            }
                        }
                    }
                }
                if (!hasDb) return false
            } else {
                val input = context.contentResolver.openInputStream(source) ?: return false
                input.use { i -> temp.outputStream().use { i.copyTo(it) } }
            }
            if (!isValid(temp)) return false
            MedoraDatabase.closeAndReset()
            val dbFile = context.getDatabasePath(DB_NAME)
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
            temp.copyTo(dbFile, overwrite = true)
            if (isZip) NoteImages.replaceAll(context, if (images > 0) stage else null)
            true
        } catch (e: Exception) {
            false
        } finally {
            temp.delete()
            File(temp.path + "-wal").delete()
            File(temp.path + "-shm").delete()
            stage.deleteRecursively()
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
