package com.example.medora.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.medora.data.NoteImages
import com.example.medora.data.Sticker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// A layer of stickers over a note. Fills its parent.
// Editable (onChange given): one finger moves, two fingers resize and rotate, long press asks to remove.
// Read only (onChange null): just shows them. Empty spots pass touches through to what is below.
@Composable
fun StickerBoard(
    stickers: List<Sticker>,
    modifier: Modifier = Modifier,
    onChange: ((Sticker) -> Unit)? = null,
    onLongPress: ((Sticker) -> Unit)? = null,
) {
    // Layout is always left-to-right here: x is measured from the left edge on every language.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(modifier = modifier) {
            val boardW = constraints.maxWidth.toFloat()
            val boardH = constraints.maxHeight.toFloat()
            // Base size follows the board width, so a sticker looks the same on every screen.
            val baseDp = 72.dp * (maxWidth / 360.dp)
            val basePx = with(LocalDensity.current) { baseDp.toPx() }
            stickers.forEach { s ->
                key(s.id) { StickerItem(s, boardW, boardH, basePx, onChange, onLongPress) }
            }
        }
    }
}

@Composable
private fun StickerItem(
    sticker: Sticker,
    boardW: Float,
    boardH: Float,
    basePx: Float,
    onChange: ((Sticker) -> Unit)?,
    onLongPress: ((Sticker) -> Unit)?,
) {
    val current by rememberUpdatedState(sticker)
    val density = LocalDensity.current
    val sizePx = basePx * sticker.scale
    val sizeDp = with(density) { sizePx.toDp() }
    var mod: Modifier = Modifier
        .offset {
            IntOffset(
                (sticker.x * boardW - sizePx / 2f).roundToInt(),
                (sticker.y * boardH - sizePx / 2f).roundToInt(),
            )
        }
        .size(sizeDp)
    if (onChange != null && boardW > 0f && boardH > 0f) {
        mod = mod
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    val s = current
                    onChange(
                        s.copy(
                            x = (s.x + pan.x / boardW).coerceIn(0f, 1f),
                            y = (s.y + pan.y / boardH).coerceIn(0f, 1f),
                            scale = (s.scale * zoom).coerceIn(0.3f, 6f),
                            rot = s.rot + rotation,
                        ),
                    )
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onLongPress?.invoke(current) })
            }
    }
    Box(modifier = mod) {
        Box(
            modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = sticker.rot },
            contentAlignment = Alignment.Center,
        ) {
            if (sticker.image.isNotEmpty()) {
                StickerPhoto(sticker.image)
            } else {
                Text(
                    text = sticker.emoji,
                    fontSize = with(density) { (sizePx * 0.7f).toSp() },
                )
            }
        }
    }
}

// Photo sticker: centered square crop, rounded corners and a white edge.
@Composable
private fun StickerPhoto(path: String) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(null, path) {
        value = withContext(Dispatchers.IO) { NoteImages.load(context, path, 512) }
    }
    val b = bitmap
    if (b != null) {
        val shape = RoundedCornerShape(18.dp)
        Image(
            bitmap = b.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(shape).border(3.dp, Color.White, shape),
        )
    }
}
