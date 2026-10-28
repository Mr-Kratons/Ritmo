package com.example.ritmo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.ritmo.RitmoViewModel
import com.example.ritmo.ui.Routes
import com.example.ritmo.ui.components.BackHeader
import com.example.ritmo.ui.components.CoverArt
import com.example.ritmo.ui.components.CoverCard
import com.example.ritmo.ui.components.EmptyState
import com.example.ritmo.ui.components.IconCover
import com.example.ritmo.ui.components.LikedGradient
import com.example.ritmo.ui.components.LocalAddToPlaylist
import com.example.ritmo.ui.components.NotFound
import com.example.ritmo.ui.components.PlayActions
import com.example.ritmo.ui.components.SectionTitle
import com.example.ritmo.ui.components.SongRow
import com.example.ritmo.util.albumsCount
import com.example.ritmo.util.formatLongDuration
import com.example.ritmo.util.songsCount

@Composable
fun AlbumScreen(vm: RitmoViewModel, nav: NavController, albumId: Long) {
    val albums by vm.albums.collectAsStateWithLifecycle()
    val state by vm.playerState.collectAsStateWithLifecycle()
    val addToPlaylist = LocalAddToPlaylist.current
    val colors = MaterialTheme.colorScheme
    val album = albums.firstOrNull { it.id == albumId }
    if (album == null) {
        NotFound("Este álbum não está mais no aparelho.") { nav.popBackStack() }
        return
    }
    val details = remember(album) {
        listOfNotNull(
            album.year.takeIf { it > 0 }?.toString(),
            songsCount(album.songs.size),
            formatLongDuration(album.durationMs),
        ).joinToString(", ")
    }
    val artistTarget = album.songs.firstOrNull()?.artistId.takeIf { album.artist != "Vários artistas" }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "back") { BackHeader(onBack = { nav.popBackStack() }) }
        item(key = "cover") {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CoverArt(album.cover, Modifier.size(220.dp), quality = 480, shape = RoundedCornerShape(12.dp))
            }
        }
        item(key = "info") {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    album.title,
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    album.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (artistTarget != null) colors.primary else colors.onSurfaceVariant,
                    modifier = if (artistTarget != null) {
                        Modifier.clickable { nav.navigate(Routes.artist(artistTarget)) }
                    } else Modifier,
                )
                Text(details, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
        item(key = "actions") {
            PlayActions(
                onPlay = { vm.play(album.songs, 0) },
                onShuffle = { vm.shufflePlay(album.songs) },
                leading = {
                    IconButton(onClick = { addToPlaylist(album.songs) }) {
                        Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = "Adicionar álbum à playlist")
                    }
                    IconButton(onClick = { vm.enqueue(album.songs) }) {
                        Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = "Adicionar álbum à fila")
                    }
                },
            )
        }
        itemsIndexed(album.songs, key = { _, song -> song.id }) { index, song ->
            SongRow(
                song = song,
                current = song.id == state.current?.id,
                playing = state.isPlaying,
                onClick = { vm.play(album.songs, index) },
            )
        }
    }
}

@Composable
fun ArtistScreen(vm: RitmoViewModel, nav: NavController, artistId: Long) {
    val artists by vm.artists.collectAsStateWithLifecycle()
    val state by vm.playerState.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val artist = artists.firstOrNull { it.id == artistId }
    if (artist == null) {
        NotFound("Este artista não está mais no aparelho.") { nav.popBackStack() }
        return
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "back") { BackHeader(onBack = { nav.popBackStack() }) }
        item(key = "cover") {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CoverArt(artist.cover, Modifier.size(180.dp), quality = 480, shape = CircleShape)
            }
        }
        item(key = "info") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    artist.name,
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${albumsCount(artist.albums.size)}, ${songsCount(artist.songs.size)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        item(key = "actions") {
            PlayActions(
                onPlay = { vm.play(artist.songs, 0) },
                onShuffle = { vm.shufflePlay(artist.songs) },
            )
        }
        if (artist.albums.isNotEmpty()) {
            item(key = "albums-title") { SectionTitle("Álbuns") }
            item(key = "albums") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(artist.albums, key = { it.id }) { album ->
                        CoverCard(
                            title = album.title,
                            subtitle = album.year.takeIf { it > 0 }?.toString(),
                            onClick = { nav.navigate(Routes.album(album.id)) },
                        ) { m -> CoverArt(album.cover, m, quality = 360, shape = RoundedCornerShape(10.dp)) }
                    }
                }
            }
        }
        item(key = "songs-title") { SectionTitle("Músicas") }
        itemsIndexed(artist.songs, key = { _, song -> song.id }) { index, song ->
            SongRow(
                song = song,
                subtitle = song.album,
                current = song.id == state.current?.id,
                playing = state.isPlaying,
                onClick = { vm.play(artist.songs, index) },
            )
        }
    }
}

@Composable
fun FavoritesScreen(vm: RitmoViewModel, nav: NavController) {
    val songs by vm.favoriteSongs.collectAsStateWithLifecycle()
    val state by vm.playerState.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "back") { BackHeader(onBack = { nav.popBackStack() }) }
        item(key = "info") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconCover(Icons.Rounded.Favorite, LikedGradient, Modifier.size(96.dp), RoundedCornerShape(12.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Músicas curtidas", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        songsCount(songs.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
        if (songs.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Rounded.FavoriteBorder,
                    title = "Nada curtido ainda",
                    message = "Toque no coração durante a reprodução para guardar suas músicas favoritas aqui.",
                )
            }
        } else {
            item(key = "actions") {
                PlayActions(
                    onPlay = { vm.play(songs, 0) },
                    onShuffle = { vm.shufflePlay(songs) },
                )
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
}
