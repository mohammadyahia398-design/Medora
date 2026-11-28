package com.example.medora.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.util.UUID
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// "My stickers": images the user picked once from the phone. Each one is saved with a white
// edge (classic sticker look): a cut-out with transparency gets an outline that follows its shape,
// a normal photo becomes a rounded sticker with a white frame.
// Files live in the note image folder with the prefix lib_ (kept in backups, never cleaned as unused).
// Putting a sticker on a note uses a copy, so deleting it from the library never breaks a note.
object StickerLibrary {
    private const val DIR = "note_images"
    private const val PREFIX = "lib_"
    private const val MAX_SIDE = 512
    private const val EDGE = 14
    private val LIB_PATH = Regex("""note_images/lib_[A-Za-z0-9._-]+\.png""")

    private fun dir(context: Context) = File(context.filesDir, DIR).apply { mkdirs() }

    // Library stickers, newest first, as note image paths.
    fun list(context: Context): List<String> =
        dir(context).listFiles { f -> f.name.startsWith(PREFIX) && f.name.endsWith(".png") }
            ?.sortedByDescending { it.lastModified() }
            ?.map { "$DIR/${it.name}" }
            ?: emptyList()

    fun delete(context: Context, path: String) {
        if (LIB_PATH.matches(path)) File(context.filesDir, path).delete()
    }

    // Copy for a note. Returns the new note image path, or null.
    fun useCopy(context: Context, path: String): String? {
        if (!LIB_PATH.matches(path)) return null
        val src = File(context.filesDir, path)
        if (!src.exists()) return null
        val name = UUID.randomUUID().toString().replace("-", "").take(16) + ".png"
        return try {
            src.copyTo(File(dir(context), name))
            "$DIR/$name"
        } catch (e: Exception) {
            null
        }
    }

    // Reads the picked image, adds the white edge and saves it in the library.
    fun add(context: Context, source: Uri): Boolean = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_SIDE || bounds.outHeight / (sample * 2) >= MAX_SIDE) sample *= 2
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
        val longest = maxOf(decoded.width, decoded.height)
        val scale = if (longest > MAX_SIDE) MAX_SIDE.toFloat() / longest else 1f
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation)
        }
        val src = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        val out = if (hasTransparency(src)) outlineCutout(src) else outlinePhoto(src)
        val name = PREFIX + UUID.randomUUID().toString().replace("-", "").take(16) + ".png"
        File(dir(context), name).outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
        out.recycle()
        true
    } catch (e: Exception) {
        false
    }

    // A real cut-out: more than a few percent of the picture is see-through.
    private fun hasTransparency(src: Bitmap): Boolean {
        val small = Bitmap.createScaledBitmap(src, 48, 48, true)
        val px = IntArray(48 * 48)
        small.getPixels(px, 0, 48, 0, 0, 48, 48)
        val clear = px.count { (it ushr 24) < 200 }
        return clear > px.size * 0.02
    }

    // White outline that follows the shape: the shape's mask is drawn white around a small circle.
    private fun outlineCutout(src: Bitmap): Bitmap {
        val pad = EDGE + 2
        val out = Bitmap.createBitmap(src.width + pad * 2, src.height + pad * 2, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val mask = src.extractAlpha()
        val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        for (r in intArrayOf(EDGE, EDGE * 2 / 3, EDGE / 3)) {
            for (a in 0 until 360 step 12) {
                val rad = Math.toRadians(a.toDouble())
                c.drawBitmap(mask, pad + (r * cos(rad)).toFloat(), pad + (r * sin(rad)).toFloat(), white)
            }
        }
        c.drawBitmap(mask, pad.toFloat(), pad.toFloat(), white)
        c.drawBitmap(src, pad.toFloat(), pad.toFloat(), null)
        mask.recycle()
        return out
    }

    // A normal photo: rounded corners and a white frame.
    private fun outlinePhoto(src: Bitmap): Bitmap {
        val pad = EDGE
        val w = src.width
        val h = src.height
        val out = Bitmap.createBitmap(w + pad * 2, h + pad * 2, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val radius = min(w, h) * 0.14f
        val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        c.drawRoundRect(RectF(0f, 0f, out.width.toFloat(), out.height.toFloat()), radius + pad, radius + pad, white)
        val photo = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pc = Canvas(photo)
        pc.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), radius, radius, Paint(Paint.ANTI_ALIAS_FLAG))
        val keep = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN) }
        pc.drawBitmap(src, 0f, 0f, keep)
        c.drawBitmap(photo, pad.toFloat(), pad.toFloat(), null)
        photo.recycle()
        return out
    }
}
