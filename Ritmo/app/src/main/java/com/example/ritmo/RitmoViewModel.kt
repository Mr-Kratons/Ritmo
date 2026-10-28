package com.example.ritmo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ritmo.data.Album
import com.example.ritmo.data.Artist
import com.example.ritmo.data.Playlist
import com.example.ritmo.data.Song
import com.example.ritmo.data.SongSort
import com.example.ritmo.data.ThemeMode
import com.example.ritmo.data.sortedBySort
import com.example.ritmo.util.normalizeForSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchResults(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
) {
    val isEmpty: Boolean
        get() = songs.isEmpty() && albums.isEmpty() && artists.isEmpty() && playlists.isEmpty()
}

class RitmoViewModel(app: Application) : AndroidViewModel(app) {

    private val ritmo = app as RitmoApp
    val repo = ritmo.repository
    val player = ritmo.player
    private val prefs = ritmo.prefs

    init {
        player.connect()
    }

    // ---------------- Estado para a interface ----------------

    val themeMode = prefs.themeMode
    val songSort = prefs.songSort
    val minDurationSec = prefs.minDurationSec
    val musicOnly = prefs.musicOnly

    val loaded = repo.loaded
    val albums = repo.albums
    val artists = repo.artists
    val playlists = repo.playlists
    val favoriteIds = repo.favoriteIds
    val favoriteSongs = repo.favoriteSongs
    val recentlyPlayed = repo.recentlyPlayed
    val mostPlayed = repo.mostPlayed

    val playerState = player.state
    val positionMs = player.positionMs

    val songs: StateFlow<List<Song>> = combine(repo.songs, prefs.songSort) { list, sort ->
        list.sortedBySort(sort)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recentlyAdded: StateFlow<List<Song>> = repo.songs
        .map { list -> list.sortedByDescending { it.dateAddedSec }.take(30) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ---------------- Mensagens (snackbar) ----------------

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: Flow<String> = merge(_messages, player.errors)

    // ---------------- Busca ----------------

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    fun setQuery(value: String) {
        _query.value = value
    }

    val searchResults: StateFlow<SearchResults> = combine(
        _query, repo.songs, repo.albums, repo.artists, repo.playlists
    ) { q, songs, albums, artists, playlists ->
        search(q, songs, albums, artists, playlists)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, SearchResults())

    private fun search(
        q: String,
        songs: List<Song>,
        albums: List<Album>,
        artists: List<Artist>,
        playlists: List<Playlist>,
    ): SearchResults {
        val tokens = q.normalizeForSearch().split(' ').filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return SearchResults()
        fun hit(text: String) = tokens.all { it in text }
        return SearchResults(
            songs = songs.filter { s -> tokens.all { it in s.searchKey } }
                .sortedBy { if (it.titleKey.startsWith(tokens.first())) 0 else 1 }
                .take(60),
            albums = albums.filter { hit(it.title.normalizeForSearch() + " " + it.artist.normalizeForSearch()) }
                .take(20),
            artists = artists.filter { hit(it.name.normalizeForSearch()) }.take(20),
            playlists = playlists.filter { hit(it.name.normalizeForSearch()) },
        )
    }

    // ---------------- Reprodução ----------------

    fun play(songs: List<Song>, index: Int = 0) = player.playSongs(songs, index)

    fun shufflePlay(songs: List<Song>) = player.playSongs(songs, 0, forceShuffle = true)

    fun playNext(songs: List<Song>) {
        player.playNext(songs)
        _messages.tryEmit(if (songs.size == 1) "Vai tocar a seguir" else "${songs.size} músicas vão tocar a seguir")
    }

    fun enqueue(songs: List<Song>) {
        player.enqueue(songs)
        _messages.tryEmit(if (songs.size == 1) "Adicionada à fila" else "${songs.size} músicas adicionadas à fila")
    }

    // ---------------- Curtidas e playlists ----------------

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            val liked = repo.toggleFavorite(song.id)
            _messages.tryEmit(if (liked) "Adicionada às músicas curtidas" else "Removida das músicas curtidas")
        }
    }

    fun createPlaylist(name: String, songs: List<Song> = emptyList(), onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repo.createPlaylist(name, songs.map { it.id })
            _messages.tryEmit("Playlist “${name.trim()}” criada")
            onCreated(id)
        }
    }

    fun addToPlaylist(playlistId: Long, playlistName: String, songs: List<Song>) {
        viewModelScope.launch {
            val added = repo.addToPlaylist(playlistId, songs.map { it.id })
            _messages.tryEmit(
                when {
                    added == 0 -> "Já está em “$playlistName”"
                    added == 1 -> "Adicionada a “$playlistName”"
                    else -> "$added músicas adicionadas a “$playlistName”"
                }
            )
        }
    }

    fun renamePlaylist(id: Long, name: String) {
        viewModelScope.launch { repo.renamePlaylist(id, name) }
    }

    fun deletePlaylist(id: Long, name: String) {
        viewModelScope.launch {
            repo.deletePlaylist(id)
            _messages.tryEmit("Playlist “$name” excluída")
        }
    }

    fun removeFromPlaylist(id: Long, entryIds: List<Long>) {
        viewModelScope.launch { repo.removeFromPlaylist(id, entryIds) }
    }

    fun reorderPlaylist(id: Long, orderedEntryIds: List<Long>) {
        viewModelScope.launch { repo.reorderPlaylist(id, orderedEntryIds) }
    }

    // ---------------- Biblioteca e ajustes ----------------

    fun onPermissionGranted() = repo.onPermissionGranted()

    fun refreshLibrary() {
        viewModelScope.launch {
            repo.refresh()
            _messages.tryEmit("Biblioteca atualizada")
        }
    }

    fun setThemeMode(mode: ThemeMode) = prefs.setThemeMode(mode)

    fun setSongSort(sort: SongSort) = prefs.setSongSort(sort)

    fun setMinDurationSec(sec: Int) {
        prefs.setMinDurationSec(sec)
        viewModelScope.launch { repo.refresh() }
    }

    fun setMusicOnly(value: Boolean) {
        prefs.setMusicOnly(value)
        viewModelScope.launch { repo.refresh() }
    }
}
