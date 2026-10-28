package com.example.ritmo.data

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import com.example.ritmo.util.normalizeForSearch

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val artistId: Long,
    val durationMs: Long,
    val trackNumber: Int = 0,
    val year: Int = 0,
    val dateAddedSec: Long = 0L,
    val sizeBytes: Long = 0L,
) {
    val uri: Uri get() = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
    val titleKey: String by lazy(LazyThreadSafetyMode.NONE) { title.normalizeForSearch() }
    val artistKey: String by lazy(LazyThreadSafetyMode.NONE) { artist.normalizeForSearch() }
    val albumKey: String by lazy(LazyThreadSafetyMode.NONE) { album.normalizeForSearch() }
    val searchKey: String by lazy(LazyThreadSafetyMode.NONE) { "$titleKey $artistKey $albumKey" }
}

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val year: Int,
    val songs: List<Song>,
) {
    val durationMs: Long get() = songs.sumOf { it.durationMs }
    val cover: Song? get() = songs.firstOrNull()
}

data class Artist(
    val id: Long,
    val name: String,
    val songs: List<Song>,
    val albums: List<Album>,
) {
    val cover: Song? get() = songs.firstOrNull()
}

data class PlaylistEntry(val entryId: Long, val song: Song)

data class Playlist(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val entries: List<PlaylistEntry>,
) {
    val songs: List<Song> by lazy(LazyThreadSafetyMode.NONE) { entries.map { it.song } }
    val durationMs: Long get() = entries.sumOf { it.song.durationMs }

    /** Até 4 músicas de álbuns diferentes, para montar a capa em mosaico. */
    val coverSongs: List<Song> by lazy(LazyThreadSafetyMode.NONE) {
        entries.asSequence().map { it.song }.distinctBy { it.albumId }.take(4).toList()
    }
}

enum class SongSort(val label: String) {
    TITLE("Título"),
    ARTIST("Artista"),
    ALBUM("Álbum"),
    RECENT("Adicionadas recentemente"),
}

enum class ThemeMode(val label: String) {
    SYSTEM("Automático"),
    DARK("Escuro"),
    LIGHT("Claro"),
}

private fun trackKey(s: Song): Int = if (s.trackNumber == 0) Int.MAX_VALUE else s.trackNumber

fun List<Song>.sortedBySort(sort: SongSort): List<Song> = when (sort) {
    SongSort.TITLE -> sortedBy { it.titleKey }
    SongSort.ARTIST -> sortedWith(
        compareBy<Song>({ it.artistKey }, { it.albumKey }, { trackKey(it) }, { it.titleKey })
    )
    SongSort.ALBUM -> sortedWith(
        compareBy<Song>({ it.albumKey }, { trackKey(it) }, { it.titleKey })
    )
    SongSort.RECENT -> sortedByDescending { it.dateAddedSec }
}

/** Capa do álbum pelo caminho clássico do MediaStore (usado nas notificações e como alternativa). */
val Song.albumArtUri: Uri
    get() = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
