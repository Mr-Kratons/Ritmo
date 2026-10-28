package com.example.ritmo.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.ritmo.RitmoViewModel
import com.example.ritmo.ui.Routes
import com.example.ritmo.ui.components.AlbumRow
import com.example.ritmo.ui.components.ArtistRow
import com.example.ritmo.ui.components.EmptyState
import com.example.ritmo.ui.components.PlaylistRow
import com.example.ritmo.ui.components.SectionTitle
import com.example.ritmo.ui.components.SongRow

@Composable
fun SearchScreen(vm: RitmoViewModel, nav: NavController) {
    val query by vm.query.collectAsStateWithLifecycle()
    val results by vm.searchResults.collectAsStateWithLifecycle()
    val state by vm.playerState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val colors = MaterialTheme.colorScheme

    Column(Modifier.fillMaxSize()) {
        Text(
            "Buscar",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )
        TextField(
            value = query,
            onValueChange = { vm.setQuery(it) },
            singleLine = true,
            placeholder = { Text("Músicas, artistas, álbuns e playlists") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { vm.setQuery("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Limpar busca")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            shape = RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = colors.surfaceContainerHigh,
                unfocusedContainerColor = colors.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        when {
            query.isBlank() -> EmptyState(
                icon = Icons.Rounded.Search,
                title = "Encontre suas músicas",
                message = "Digite o nome de uma música, artista, álbum ou playlist. A busca ignora acentos.",
            )
            results.isEmpty -> EmptyState(
                icon = Icons.Rounded.SearchOff,
                title = "Nada encontrado",
                message = "Nenhum resultado para “${query.trim()}”.",
            )
            else -> LazyColumn(contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)) {
                if (results.songs.isNotEmpty()) {
                    item(key = "songs-title") { SectionTitle("Músicas") }
                    itemsIndexed(results.songs, key = { _, s -> "s${s.id}" }) { index, song ->
                        SongRow(
                            song = song,
                            current = song.id == state.current?.id,
                            playing = state.isPlaying,
                            onClick = {
                                focusManager.clearFocus()
                                vm.play(results.songs, index)
                            },
                        )
                    }
                }
                if (results.artists.isNotEmpty()) {
                    item(key = "artists-title") { SectionTitle("Artistas") }
                    items(results.artists, key = { "ar${it.id}" }) { artist ->
                        ArtistRow(artist, onClick = { nav.navigate(Routes.artist(artist.id)) })
                    }
                }
                if (results.albums.isNotEmpty()) {
                    item(key = "albums-title") { SectionTitle("Álbuns") }
                    items(results.albums, key = { "al${it.id}" }) { album ->
                        AlbumRow(album, onClick = { nav.navigate(Routes.album(album.id)) })
                    }
                }
                if (results.playlists.isNotEmpty()) {
                    item(key = "playlists-title") { SectionTitle("Playlists") }
                    items(results.playlists, key = { "pl${it.id}" }) { playlist ->
                        PlaylistRow(playlist, onClick = { nav.navigate(Routes.playlist(playlist.id)) })
                    }
                }
            }
        }
    }
}
