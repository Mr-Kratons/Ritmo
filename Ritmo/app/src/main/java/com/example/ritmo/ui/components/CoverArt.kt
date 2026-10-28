package com.example.ritmo.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import com.example.ritmo.data.Song
import com.example.ritmo.data.albumArtUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/** Carrega a capa do álbum (do arquivo ou do MediaStore) e guarda em cache na memória. */
object CoverLoader {
    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 8).toInt()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }
    private val missing: MutableSet<String> = java.util.Collections.synchronizedSet(HashSet())

    fun key(song: Song, sizePx: Int): String =
        if (song.albumId > 0) "a${song.albumId}:$sizePx" else "s${song.id}:$sizePx"

    fun cached(key: String): Bitmap? = cache.get(key)

    suspend fun load(context: Context, song: Song, sizePx: Int): Bitmap? {
        val key = key(song, sizePx)
        cache.get(key)?.let { return it }
        if (key in missing) return null
        val bitmap = withContext(Dispatchers.IO) { loadFromDevice(context.applicationContext, song, sizePx) }
        if (bitmap != null) cache.put(key, bitmap) else missing.add(key)
        return bitmap
    }

    private fun loadFromDevice(context: Context, song: Song, sizePx: Int): Bitmap? {
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                return context.contentResolver.loadThumbnail(song.uri, Size(sizePx, sizePx), null)
            } catch (_: Exception) {
            }
        } else {
            try {
                val bytes = context.contentResolver.openInputStream(song.albumArtUri)?.use { it.readBytes() }
                if (bytes != null) decodeSampled(bytes, sizePx)?.let { return it }
            } catch (_: Exception) {
            }
        }
        // Última tentativa: capa embutida no próprio arquivo de áudio.
        return try {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, song.uri)
                retriever.embeddedPicture?.let { decodeSampled(it, sizePx) }
            } finally {
                retriever.release()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeSampled(bytes: ByteArray, sizePx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= sizePx && bounds.outHeight / (sample * 2) >= sizePx) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }
}

/** [quality] é o tamanho em pixels do bitmap carregado; o tamanho na tela vem do [modifier]. */
@Composable
fun CoverArt(
    song: Song?,
    modifier: Modifier = Modifier,
    quality: Int = 256,
    shape: Shape = RoundedCornerShape(6.dp),
) {
    val context = LocalContext.current
    val cacheKey = song?.let { CoverLoader.key(it, quality) }
    val bitmap by produceState<Bitmap?>(cacheKey?.let { CoverLoader.cached(it) }, cacheKey) {
        value = if (song == null) null else CoverLoader.load(context, song, quality)
    }
    Box(modifier.clip(shape)) {
        val bmp = bitmap
        if (bmp != null) {
            val image = remember(bmp) { bmp.asImageBitmap() }
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            CoverPlaceholder(song?.album.orEmpty(), Modifier.fillMaxSize())
        }
    }
}

/** Sem capa: gradiente estável por álbum, para a lista não ficar toda igual. */
@Composable
fun CoverPlaceholder(seed: String, modifier: Modifier = Modifier) {
    val hue = (abs(seed.hashCode()) % 360).toFloat()
    val brush = remember(hue) {
        Brush.linearGradient(
            listOf(Color.hsv(hue, 0.42f, 0.46f), Color.hsv((hue + 45f) % 360f, 0.55f, 0.26f))
        )
    }
    Box(modifier.background(brush), contentAlignment = Alignment.Center) {
        Icon(
            Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxSize(0.42f),
        )
    }
}

/** Capa de playlist: mosaico 2x2 quando há 4 álbuns diferentes, senão a primeira capa. */
@Composable
fun PlaylistCover(
    coverSongs: List<Song>,
    modifier: Modifier = Modifier,
    quality: Int = 256,
    shape: Shape = RoundedCornerShape(6.dp),
) {
    when {
        coverSongs.isEmpty() -> IconCover(
            icon = Icons.AutoMirrored.Rounded.QueueMusic,
            brush = Brush.linearGradient(listOf(Color(0xFF3A3E4B), Color(0xFF22252F))),
            modifier = modifier,
            shape = shape,
        )
        coverSongs.size < 4 -> CoverArt(coverSongs.first(), modifier, quality, shape)
        else -> Column(modifier.clip(shape)) {
            val half = quality / 2
            Row(Modifier.weight(1f)) {
                CoverArt(coverSongs[0], Modifier.weight(1f).fillMaxHeight(), half, RectangleShape)
                CoverArt(coverSongs[1], Modifier.weight(1f).fillMaxHeight(), half, RectangleShape)
            }
            Row(Modifier.weight(1f)) {
                CoverArt(coverSongs[2], Modifier.weight(1f).fillMaxHeight(), half, RectangleShape)
                CoverArt(coverSongs[3], Modifier.weight(1f).fillMaxHeight(), half, RectangleShape)
            }
        }
    }
}

@Composable
fun IconCover(
    icon: ImageVector,
    brush: Brush,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp),
) {
    Box(modifier.clip(shape).background(brush), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.fillMaxSize(0.46f))
    }
}

/** Cor predominante da capa, usada para tingir a tela "Tocando agora". */
@Composable
fun rememberDominantColor(song: Song?, fallback: Color): State<Color> {
    val context = LocalContext.current
    return produceState(fallback, song?.albumId, fallback) {
        val bitmap = song?.let { CoverLoader.load(context, it, 128) }
        value = if (bitmap == null) {
            fallback
        } else {
            withContext(Dispatchers.Default) {
                val palette = Palette.from(bitmap).maximumColorCount(16).generate()
                val swatch = palette.vibrantSwatch ?: palette.mutedSwatch
                    ?: palette.darkVibrantSwatch ?: palette.dominantSwatch
                swatch?.let { Color(it.rgb) } ?: fallback
            }
        }
    }
}

val LikedGradient = Brush.linearGradient(listOf(Color(0xFFFFB020), Color(0xFFFF5F6D)))
val ShuffleGradient = Brush.linearGradient(listOf(Color(0xFF2BB5A0), Color(0xFF2A6FDB)))

@Composable
fun surfaceMuted(): Color = MaterialTheme.colorScheme.onSurfaceVariant
