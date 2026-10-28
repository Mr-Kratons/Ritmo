package com.example.ritmo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.ritmo.RitmoViewModel
import com.example.ritmo.data.Playlist
import com.example.ritmo.data.Song
import com.example.ritmo.ui.Routes
import com.example.ritmo.ui.components.CoverArt
import com.example.ritmo.ui.components.CoverCard
import com.example.ritmo.ui.components.EmptyState
import com.example.ritmo.ui.components.IconCover
import com.example.ritmo.ui.components.LikedGradient
import com.example.ritmo.ui.components.PlaylistCover
import com.example.ritmo.ui.components.SectionTitle
import com.example.ritmo.ui.components.ShuffleGradient
import java.util.Calendar

@Composable
fun HomeScreen(vm: RitmoViewModel, nav: NavController) {
    val songs by vm.songs.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val recent by vm.recentlyPlayed.collectAsStateWithLifecycle()
    val most by vm.mostPlayed.collectAsStateWithLifecycle()
    val added by vm.recentlyAdded.collectAsStateWithLifecycle()
    val albums by vm.albums.collectAsStateWithLifecycle()
    val artists by vm.artists.collectAsStateWithLifecycle()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "header") { HomeHeader(onSettings = { nav.navigate(Routes.SETTINGS) }) }

        if (loaded && songs.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Rounded.MusicNote,
                    title = "Nenhuma música encontrada",
                    message = "Coloque arquivos de música no aparelho e eles aparecem aqui. Se você já tem músicas, toque em atualizar.",
                    actionLabel = "Atualizar biblioteca",
                    onAction = { vm.refreshLibrary() },
                )
            }
        } else if (songs.isNotEmpty()) {
            item(key = "quick") { QuickAccess(vm, nav, songs, playlists) }

            if (recent.isNotEmpty()) {
                item(key = "recent-title") { SectionTitle("Tocadas recentemente") }
                item(key = "recent") { SongCarousel(vm, recent.take(20)) }
            }
            if (most.isNotEmpty()) {
                item(key = "most-title") { SectionTitle("Mais tocadas") }
                item(key = "most") { SongCarousel(vm, most.take(20)) }
            }
            item(key = "added-title") { SectionTitle("Adicionadas recentemente") }
            item(key = "added") { SongCarousel(vm, added.take(20)) }

            if (albums.isNotEmpty()) {
                item(key = "albums-title") { SectionTitle("Álbuns") }
                item(key = "albums") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(albums.take(20), key = { _, a -> a.id }) { _, album ->
                            CoverCard(
                                title = album.title,
                                subtitle = album.artist,
                                onClick = { nav.navigate(Routes.album(album.id)) },
                            ) { m -> CoverArt(album.cover, m, quality = 360, shape = RoundedCornerShape(10.dp)) }
                        }
                    }
                }
            }
            if (artists.isNotEmpty()) {
                item(key = "artists-title") { SectionTitle("Artistas") }
                item(key = "artists") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(artists.take(20), key = { _, a -> a.id }) { _, artist ->
                            CoverCard(
                                title = artist.name,
                                subtitle = null,
                                onClick = { nav.navigate(Routes.artist(artist.id)) },
                                width = 120.dp,
                                centered = true,
                            ) { m -> CoverArt(artist.cover, m, quality = 360, shape = CircleShape) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(onSettings: () -> Unit) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when (hour) {
        in 5..11 -> "Bom dia"
        in 12..17 -> "Boa tarde"
        else -> "Boa noite"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(greeting, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = onSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = "Configurações")
        }
    }
}

private class Tile(val title: String, val onClick: () -> Unit, val cover: @Composable () -> Unit)

@Composable
private fun QuickAccess(vm: RitmoViewModel, nav: NavController, songs: List<Song>, playlists: List<Playlist>) {
    val tiles = buildList {
        add(Tile("Músicas curtidas", { nav.navigate(Routes.FAVORITES) }) {
            IconCover(Icons.Rounded.Favorite, LikedGradient, Modifier.fillMaxSize(), RectangleShape)
        })
        add(Tile("Tudo em ordem aleatória", { vm.shufflePlay(songs) }) {
            IconCover(Icons.Rounded.Shuffle, ShuffleGradient, Modifier.fillMaxSize(), RectangleShape)
        })
        playlists.take(4).forEach { playlist ->
            add(Tile(playlist.name, { nav.navigate(Routes.playlist(playlist.id)) }) {
                PlaylistCover(playlist.coverSongs, Modifier.fillMaxSize(), quality = 120, shape = RectangleShape)
            })
        }
    }
    Column(
        Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tiles.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { tile -> QuickTile(tile.title, Modifier.weight(1f), tile.onClick, tile.cover) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuickTile(title: String, modifier: Modifier, onClick: () -> Unit, cover: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.size(56.dp)) { cover() }
            Text(
                title,
                modifier = Modifier.padding(horizontal = 12.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@Composable
private fun SongCarousel(vm: RitmoViewModel, songs: List<Song>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
            CoverCard(
                title = song.title,
                subtitle = song.artist,
                onClick = { vm.play(songs, index) },
            ) { m -> CoverArt(song, m, quality = 360, shape = RoundedCornerShape(10.dp)) }
        }
    }
}
