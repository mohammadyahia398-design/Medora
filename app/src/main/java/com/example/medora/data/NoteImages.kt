package com.example.medora.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.util.UUID

// Images inside notes. Each image is copied (made smaller) into the app's own folder,
// and the note keeps one line like:  ![image](note_images/abc.jpg)
// Stays on the phone (offline). Included in the .zip backup.
object NoteImages {
    private const val DIR = "note_images"
    private const val MAX_SIDE = 1600
    private val LINE = Regex("""^\s*!\[[^\]]*]\((note_images/[A-Za-z0-9._-]+)\)\s*$""")

    // The image path in a note line, or null if the line is not an image.
    fun pathOf(line: String): String? = LINE.matchEntire(line)?.groupValues?.get(1)

    fun file(context: Context, path: String): File = File(context.filesDir, path)

    // Copies the picked image into the app. Returns the line to put in the note, or null.
    fun save(context: Context, source: Uri): String? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val sample = sampleFor(bounds.outWidth, bounds.outHeight, MAX_SIDE)
        val decoded = context.contentResolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw IllegalStateException("Not an image")
        val rotation = context.contentResolver.openInputStream(source)?.use { input ->
            when (ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        val bitmap = fit(decoded, rotation)
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        val name = UUID.randomUUID().toString().replace("-", "").take(16) + ".jpg"
        File(dir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        bitmap.recycle()
        "![image]($DIR/$name)"
    } catch (e: Exception) {
        null
    }

    // Loads a smaller copy for showing on screen or in a PDF. Null if missing.
    fun load(context: Context, path: String, maxSide: Int): Bitmap? = try {
        val f = file(context, path)
        if (!f.exists()) {
            null
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.path, bounds)
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleFor(bounds.outWidth, bounds.outHeight, maxSide) }
            BitmapFactory.decodeFile(f.path, opts)
        }
    } catch (e: Exception) {
        null
    }

    fun deleteAll(context: Context) {
        File(context.filesDir, DIR).deleteRecursively()
    }

    // Removes images that no note uses any more (frees space).
    fun deleteUnused(context: Context, used: Set<String>) {
        File(context.filesDir, DIR).listFiles()?.forEach { f ->
            if ("$DIR/${f.name}" !in used) f.delete()
        }
    }

    // After a restore: the images from the backup replace the old ones (none = empty folder).
    fun replaceAll(context: Context, from: File?) {
        val dir = File(context.filesDir, DIR)
        dir.deleteRecursively()
        if (from != null && from.exists()) from.copyRecursively(dir, overwrite = true)
    }

    private fun sampleFor(w: Int, h: Int, maxSide: Int): Int {
        var s = 1
        while (w / (s * 2) >= maxSide || h / (s * 2) >= maxSide) s *= 2
        return s
    }

    private fun fit(src: Bitmap, rotation: Float): Bitmap {
        val longest = maxOf(src.width, src.height)
        val scale = if (longest > MAX_SIDE) MAX_SIDE.toFloat() / longest else 1f
        if (scale == 1f && rotation == 0f) return src
        val m = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation)
        }
        val out = Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
        if (out !== src) src.recycle()
        return out
    }
}
