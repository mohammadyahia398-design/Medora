package com.example.medora.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

// A copy of each imported file (PDF, photo, text) kept in the app's own folder,
// so the original can be opened again later. Stays on the phone; included in the .zip backup.
object OriginalFiles {
    const val DIR = "originals"
    private const val MAX_BYTES = 25_000_000L
    private val NAME = Regex("""[A-Za-z0-9._-]+""")

    fun file(context: Context, name: String): File = File(File(context.filesDir, DIR), name)

    // Copies the picked file. Returns the saved name, or "" if it could not be kept.
    fun save(context: Context, source: Uri, extension: String): String {
        val ext = extension.lowercase().filter { it.isLetterOrDigit() }.take(5).ifEmpty { "bin" }
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        val name = UUID.randomUUID().toString().replace("-", "").take(16) + "." + ext
        val target = File(dir, name)
        return try {
            val input = context.contentResolver.openInputStream(source) ?: return ""
            var copied = 0L
            input.use { i ->
                target.outputStream().use { o ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = i.read(buf)
                        if (n < 0) break
                        copied += n
                        if (copied > MAX_BYTES) throw IllegalStateException("too big")
                        o.write(buf, 0, n)
                    }
                }
            }
            name
        } catch (e: Exception) {
            target.delete()
            ""
        }
    }

    fun delete(context: Context, name: String) {
        if (name.isNotBlank() && NAME.matches(name)) file(context, name).delete()
    }

    // Opens the original in another app (PDF reader, gallery...). False if nothing can open it.
    fun open(context: Context, name: String): Boolean {
        if (name.isBlank() || !NAME.matches(name)) return false
        val f = file(context, name)
        if (!f.exists()) return false
        return try {
            val uri = FileProvider.getUriForFile(context, context.packageName + ".files", f)
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, mimeOf(name))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun mimeOf(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "pdf" -> "application/pdf"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "heic" -> "image/heic"
        "txt", "md" -> "text/plain"
        else -> "*/*"
    }

    fun deleteAll(context: Context) {
        File(context.filesDir, DIR).deleteRecursively()
    }

    // Removes copies that no lecture uses any more.
    fun deleteUnused(context: Context, used: Set<String>) {
        File(context.filesDir, DIR).listFiles()?.forEach { f ->
            if (f.name !in used) f.delete()
        }
    }

    // After a restore: the files from the backup replace the old ones (none = empty folder).
    fun replaceAll(context: Context, from: File?) {
        val dir = File(context.filesDir, DIR)
        dir.deleteRecursively()
        if (from != null && from.exists()) from.copyRecursively(dir, overwrite = true)
    }
}
