package com.example.ritmo.data

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.room.withTransaction
import com.example.ritmo.data.db.AppDatabase
import com.example.ritmo.data.db.FavoriteEntity
import com.example.ritmo.data.db.PlayStatEntity
import com.example.ritmo.data.db.PlaylistEntity
import com.example.ritmo.data.db.PlaylistSongEntity
import com.example.ritmo.util.normalizeForSearch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MusicRepository(context: Context, val prefs: Prefs) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.create(appContext)
    private val playlistDao = db.playlists()
    private val favoriteDao = db.favorites()
    private val statDao = db.stats()
    private val source = MediaStoreSource(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ---------------- Biblioteca do aparelho ----------------

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _songById = MutableStateFlow<Map<Long, Song>>(emptyMap())
    val songById: StateFlow<Map<Long, Song>> = _songById.asStateFlow()

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val refreshMutex = Mutex()

    suspend fun refresh() = refreshMutex.withLock {
        val list = source.querySongs(
            minDurationMs = prefs.minDurationSec.value * 1000L,
            musicOnly = prefs.musicOnly.value,
        )
        publishLibrary(list)
    }

    private fun publishLibrary(list: List<Song>) {
        fun trackKey(s: Song) = if (s.trackNumber == 0) Int.MAX_VALUE else s.trackNumber

        val albumList = list.groupBy { it.albumId }.map { (id, songs) ->
            val sorted = songs.sortedWith(compareBy<Song>({ trackKey(it) }, { it.titleKey }))
            val artistNames = sorted.map { it.artist }.distinct()
            Album(
                id = id,
                title = sorted.first().album,
                artist = if (artistNames.size > 1) "Vários artistas" else artistNames.first(),
                year = sorted.maxOf { it.year },
                songs = sorted,
            )
        }.sortedBy { it.title.normalizeForSearch() }

        val albumMap = albumList.associateBy { it.id }
        val artistList = list.groupBy { it.artistId }.map { (id, songs) ->
            val sorted = songs.sortedWith(compareBy<Song>({ it.albumKey }, { trackKey(it) }, { it.titleKey }))
            Artist(
                id = id,
                name = sorted.first().artist,
                songs = sorted,
                albums = sorted.map { it.albumId }.distinct().mapNotNull { albumMap[it] },
            )
        }.sortedBy { it.name.normalizeForSearch() }

        // Tudo é publicado antes de `loaded`, para quem espera o carregamento já ver dados completos.
        _songById.value = list.associateBy { it.id }
        _albums.value = albumList
        _artists.value = artistList
        _songs.value = list
        _loaded.value = true
    }

    // Atualiza sozinho quando o usuário baixa/apaga músicas.
    private var observing = false
    private var observerJob: Job? = null
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = scheduleRefresh()
    }

    private fun scheduleRefresh() {
        observerJob?.cancel()
        observerJob = scope.launch {
            delay(1500)
            refresh()
        }
    }

    fun onPermissionGranted() {
        if (!observing) {
            observing = true
            appContext.contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer
            )
        }
        scope.launch { refresh() }
    }

    // ---------------- Playlists ----------------

    val playlists: StateFlow<List<Playlist>> = combine(
        playlistDao.observePlaylists(),
        playlistDao.observeEntries(),
        _songById,
    ) { lists, entries, songMap ->
        val byPlaylist = entries.groupBy { it.playlistId }
        lists.map { p ->
            Playlist(
                id = p.id,
                name = p.name,
                createdAt = p.createdAt,
                updatedAt = p.updatedAt,
                entries = byPlaylist[p.id].orEmpty().mapNotNull { e ->
                    songMap[e.songId]?.let { PlaylistEntry(e.entryId, it) }
                },
            )
        }
    }.flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.Eagerly, emptyList())

    suspend fun createPlaylist(name: String, songIds: List<Long> = emptyList()): Long = db.withTransaction {
        val now = System.currentTimeMillis()
        val id = playlistDao.insertPlaylist(PlaylistEntity(name = name.trim(), createdAt = now, updatedAt = now))
        if (songIds.isNotEmpty()) {
            playlistDao.insertEntries(
                songIds.mapIndexed { i, songId -> PlaylistSongEntity(playlistId = id, songId = songId, position = i) }
            )
        }
        id
    }

    suspend fun renamePlaylist(id: Long, name: String) =
        playlistDao.rename(id, name.trim(), System.currentTimeMillis())

    suspend fun deletePlaylist(id: Long) = playlistDao.delete(id)

    /** Adiciona só o que ainda não está na playlist. Devolve quantas músicas entraram. */
    suspend fun addToPlaylist(id: Long, songIds: List<Long>): Int = db.withTransaction {
        val existing = playlistDao.songIds(id).toHashSet()
        val fresh = songIds.distinct().filter { it !in existing }
        if (fresh.isNotEmpty()) {
            var pos = playlistDao.maxPosition(id)
            playlistDao.insertEntries(
                fresh.map { PlaylistSongEntity(playlistId = id, songId = it, position = ++pos) }
            )
            playlistDao.touch(id, System.currentTimeMillis())
        }
        fresh.size
    }

    suspend fun removeFromPlaylist(id: Long, entryIds: List<Long>) = db.withTransaction {
        playlistDao.deleteEntries(entryIds)
        playlistDao.touch(id, System.currentTimeMillis())
    }

    suspend fun reorderPlaylist(id: Long, orderedEntryIds: List<Long>) = db.withTransaction {
        orderedEntryIds.forEachIndexed { index, entryId -> playlistDao.setPosition(entryId, index) }
        playlistDao.touch(id, System.currentTimeMillis())
    }

    // ---------------- Curtidas ----------------

    val favoriteIds: StateFlow<Set<Long>> = favoriteDao.observeAll()
        .map { rows -> rows.mapTo(HashSet()) { it.songId } as Set<Long> }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    val favoriteSongs: StateFlow<List<Song>> = combine(favoriteDao.observeAll(), _songById) { rows, songMap ->
        rows.mapNotNull { songMap[it.songId] }
    }.flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** Devolve `true` se a música ficou curtida. */
    suspend fun toggleFavorite(songId: Long): Boolean =
        if (favoriteIds.value.contains(songId)) {
            favoriteDao.remove(songId)
            false
        } else {
            favoriteDao.add(FavoriteEntity(songId, System.currentTimeMillis()))
            true
        }

    // ---------------- Histórico ----------------

    val recentlyPlayed: StateFlow<List<Song>> = combine(statDao.observeAll(), _songById) { rows, songMap ->
        rows.filter { it.lastPlayedAt > 0 }
            .sortedByDescending { it.lastPlayedAt }
            .mapNotNull { songMap[it.songId] }
            .take(30)
    }.flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.Eagerly, emptyList())

    val mostPlayed: StateFlow<List<Song>> = combine(statDao.observeAll(), _songById) { rows, songMap ->
        rows.filter { it.playCount > 0 }
            .sortedWith(compareByDescending<PlayStatEntity> { it.playCount }.thenByDescending { it.lastPlayedAt })
            .mapNotNull { songMap[it.songId] }
            .take(30)
    }.flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun registerPlay(songId: Long) {
        scope.launch {
            db.withTransaction {
                val current = statDao.get(songId)
                statDao.put(PlayStatEntity(songId, (current?.playCount ?: 0) + 1, System.currentTimeMillis()))
            }
        }
    }
}
