package com.example.ritmo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.ritmo.RitmoViewModel
import com.example.ritmo.data.Album
import com.example.ritmo.data.Song
import com.example.ritmo.data.SongSort
import com.example.ritmo.playback.PlayerState
import com.example.ritmo.ui.Routes
import com.example.ritmo.ui.components.ArtistRow
import com.example.ritmo.ui.components.CoverArt
import com.example.ritmo.ui.components.EmptyState
import com.example.ritmo.ui.components.LikedRow
import com.example.ritmo.ui.components.NameDialog
import com.example.ritmo.ui.components.PlaylistRow
import com.example.ritmo.ui.components.SongRow
import com.example.ritmo.ui.components.SortSheet

@Composable
fun LibraryScreen(vm: RitmoViewModel, nav: NavController) {
    val songs by vm.songs.collectAsStateWithLifecycle()
    val albums by vm.albums.collectAsStateWithLifecycle()
    val artists by vm.artists.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val favorites by vm.favoriteSongs.collectAsStateWithLifecycle()
    val sort by vm.songSort.collectAsStateWithLifecycle()
    val state by vm.playerState.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showNew by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val tabs = listOf("Playlists", "Músicas", "Álbuns", "Artistas")
    val libraryEmpty = loaded && songs.isEmpty()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Biblioteca", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = { showNew = true }) {
                Icon(Icons.Rounded.Add, contentDescription = "Nova playlist")
            }
        }
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tabs.forEachIndexed { index, label ->
                FilterChip(
                    selected = tab == index,
                    onClick = { tab = index },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.primary,
                        selectedLabelColor = colors.onPrimary,
                    ),
                )
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (libraryEmpty && tab != 0) {
                EmptyState(
                    icon = Icons.Rounded.MusicNote,
                    title = "Nenhuma música encontrada",
                    message = "Coloque arquivos de música no aparelho e atualize a biblioteca.",
                    actionLabel = "Atualizar biblioteca",
                    onAction = { vm.refreshLibrary() },
                )
            } else {
                when (tab) {
                    0 -> PlaylistsTab(
                        playlists = playlists,
                        likedCount = favorites.size,
                        onOpenLiked = { nav.navigate(Routes.FAVORITES) },
                        onOpen = { nav.navigate(Routes.playlist(it)) },
                        onNew = { showNew = true },
                    )
                    1 -> SongsTab(vm, songs, sort, state, onSort = { showSort = true })
                    2 -> AlbumsTab(albums) { nav.navigate(Routes.album(it)) }
                    else -> ArtistsTab(vm, nav)
                }
            }
        }
    }

    if (showNew) {
        NameDialog(
            title = "Nova playlist",
            initial = "",
            confirmLabel = "Criar",
            onDismiss = { showNew = false },
            onConfirm = { name ->
                showNew = false
                vm.createPlaylist(name) { id -> nav.navigate(Routes.playlist(id)) }
            },
        )
    }
    if (showSort) {
        SortSheet(current = sort, onPick = { vm.setSongSort(it) }, onDismiss = { showSort = false })
    }
}

@Composable
private fun PlaylistsTab(
    playlists: List<com.example.ritmo.data.Playlist>,
    likedCount: Int,
    onOpenLiked: () -> Unit,
    onOpen: (Long) -> Unit,
    onNew: () -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        item(key = "liked") { LikedRow(likedCount, onOpenLiked) }
        if (playlists.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    title = "Crie sua primeira playlist",
                    message = "Monte listas com as músicas do aparelho e arraste para deixar na ordem que quiser.",
                    actionLabel = "Nova playlist",
                    onAction = onNew,
                )
            }
        }
        items(playlists, key = { it.id }) { playlist ->
            PlaylistRow(playlist, onClick = { onOpen(playlist.id) })
        }
    }
}

@Composable
private fun SongsTab(
    vm: RitmoViewModel,
    songs: List<Song>,
    sort: SongSort,
    state: PlayerState,
    onSort: () -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        item(key = "controls") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onSort) {
                    Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(sort.label)
                }
                Spacer(Modifier.weight(1f))
                FilledTonalButton(onClick = { vm.shufflePlay(songs) }, enabled = songs.isNotEmpty()) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Aleatório")
                }
            }
        }
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            SongRow(
                song = song,
                current = song.id == state.current?.id,
                playing = state.isPlaying,
                onClick = { vm.play(songs, index) },
            )
        }
    }
}

@Composable
private fun AlbumsTab(albums: List<Album>, onOpen: (Long) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(albums, key = { it.id }) { album ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onOpen(album.id) }
            ) {
                CoverArt(
                    album.cover,
                    Modifier.fillMaxWidth().aspectRatio(1f),
                    quality = 360,
                    shape = RoundedCornerShape(10.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    album.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    album.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ArtistsTab(vm: RitmoViewModel, nav: NavController) {
    val artists by vm.artists.collectAsStateWithLifecycle()
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        items(artists, key = { it.id }) { artist ->
            ArtistRow(artist, onClick = { nav.navigate(Routes.artist(artist.id)) })
        }
    }
}
