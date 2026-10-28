package com.example.ritmo.data

import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Lê as músicas que já estão no aparelho (MediaStore). Nada de nuvem. */
class MediaStoreSource(private val context: Context) {

    suspend fun querySongs(minDurationMs: Long, musicOnly: Boolean): List<Song> =
        withContext(Dispatchers.IO) {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DISPLAY_NAME,
            )
            val selection = buildString {
                append("${MediaStore.Audio.Media.DURATION} >= ?")
                if (musicOnly) append(" AND ${MediaStore.Audio.Media.IS_MUSIC} != 0")
            }
            val result = ArrayList<Song>()
            try {
                context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    arrayOf(minDurationMs.toString()),
                    null
                )?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val artistIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                    val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val trackCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                    val yearCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                    val addedCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                    val sizeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                    val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

                    while (c.moveToNext()) {
                        val displayName = c.getString(nameCol).orEmpty()
                        result.add(
                            Song(
                                id = c.getLong(idCol),
                                title = c.getString(titleCol).cleanOr(displayName.substringBeforeLast('.')),
                                artist = c.getString(artistCol).cleanOr("Artista desconhecido"),
                                album = c.getString(albumCol).cleanOr("Álbum desconhecido"),
                                albumId = c.getLong(albumIdCol),
                                artistId = c.getLong(artistIdCol),
                                durationMs = c.getLong(durCol),
                                trackNumber = c.getInt(trackCol),
                                year = c.getInt(yearCol),
                                dateAddedSec = c.getLong(addedCol),
                                sizeBytes = c.getLong(sizeCol),
                            )
                        )
                    }
                }
            } catch (_: SecurityException) {
                // Sem permissão ainda: devolve lista vazia.
            }
            result
        }

    private fun String?.cleanOr(fallback: String): String {
        val s = this?.trim().orEmpty()
        return if (s.isEmpty() || s == MediaStore.UNKNOWN_STRING) fallback else s
    }
}
