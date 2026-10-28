package com.example.ritmo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ritmo.data.Playlist
import com.example.ritmo.data.Song
import com.example.ritmo.data.SongSort
import com.example.ritmo.util.normalizeForSearch
import com.example.ritmo.util.songsCount
import kotlinx.coroutines.launch

/**
 * Janela deslizante padrão. O conteúdo recebe `close`, que fecha com animação
 * e só então avisa [onDismiss].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RitmoSheet(
    onDismiss: () -> Unit,
    fullHeight: Boolean = false,
    content: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = fullHeight)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = { scope.launch { state.hide() }.invokeOnCompletion { onDismiss() } }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(if (fullHeight) Modifier.fillMaxHeight(0.94f).imePadding() else Modifier) {
            content(close)
        }
    }
}

@Composable
fun OptionRow(icon: ImageVector, label: String, tint: Color = Color.Unspecified, onClick: () -> Unit) {
    val color = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface else tint
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Spacer(Modifier.width(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

@Composable
fun SongOptionsSheet(
    song: Song,
    favorite: Boolean,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onEnqueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleFavorite: () -> Unit,
    onGoAlbum: () -> Unit,
    onGoArtist: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    RitmoSheet(onDismiss) { close ->
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverArt(song, Modifier.size(56.dp), quality = 160)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = colors.outlineVariant)
        OptionRow(Icons.Rounded.SkipNext, "Tocar a seguir") { onPlayNext(); close() }
        OptionRow(Icons.AutoMirrored.Rounded.QueueMusic, "Adicionar à fila") { onEnqueue(); close() }
        OptionRow(Icons.AutoMirrored.Rounded.PlaylistAdd, "Adicionar à playlist") { onAddToPlaylist(); close() }
        OptionRow(
            icon = if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            label = if (favorite) "Remover das curtidas" else "Curtir",
            tint = if (favorite) colors.primary else Color.Unspecified,
        ) { onToggleFavorite(); close() }
        OptionRow(Icons.Rounded.Album, "Ir para o álbum") { onGoAlbum(); close() }
        OptionRow(Icons.Rounded.Person, "Ir para o artista") { onGoArtist(); close() }
        Spacer(Modifier.navigationBarsPadding().height(16.dp))
    }
}

@Composable
fun AddToPlaylistSheet(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPick: (Playlist) -> Unit,
    onCreate: () -> Unit,
) {
    RitmoSheet(onDismiss) { close ->
        Text(
            "Adicionar à playlist",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        OptionRow(Icons.Rounded.Add, "Nova playlist", tint = MaterialTheme.colorScheme.primary) {
            onCreate(); close()
        }
        LazyColumn(Modifier.heightIn(max = 380.dp)) {
            items(playlists, key = { it.id }) { playlist ->
                PlaylistRow(playlist, onClick = { onPick(playlist); close() })
            }
        }
        Spacer(Modifier.navigationBarsPadding().height(16.dp))
    }
}

@Composable
fun SortSheet(current: SongSort, onPick: (SongSort) -> Unit, onDismiss: () -> Unit) {
    RitmoSheet(onDismiss) { close ->
        Text(
            "Ordenar por",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        SongSort.entries.forEach { sort ->
            val selected = sort == current
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onPick(sort); close() }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    sort.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (selected) Icon(Icons.Rounded.Check, contentDescription = "Selecionado", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.navigationBarsPadding().height(16.dp))
    }
}

/** Escolher várias músicas de uma vez para colocar numa playlist. */
@Composable
fun SongPickerSheet(
    songs: List<Song>,
    alreadyIn: Set<Long>,
    onDismiss: () -> Unit,
    onConfirm: (List<Song>) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<Long>() }
    val filtered = remember(query, songs) {
        val key = query.normalizeForSearch()
        if (key.isEmpty()) songs else songs.filter { key in it.searchKey }
    }

    RitmoSheet(onDismiss, fullHeight = true) { close ->
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Adicionar músicas", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(
                enabled = selected.isNotEmpty(),
                onClick = {
                    val picked = songs.filter { it.id in selected }
                    onConfirm(picked)
                    close()
                },
            ) {
                Text(if (selected.isEmpty()) "Adicionar" else "Adicionar (${selected.size})")
            }
        }
        TextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Buscar no aparelho") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, contentDescription = "Limpar busca") }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = colors.surfaceContainerHighest,
                unfocusedContainerColor = colors.surfaceContainerHighest,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(filtered, key = { it.id }) { song ->
                val inPlaylist = song.id in alreadyIn
                val checked = song.id in selected
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !inPlaylist) {
                            if (checked) selected.remove(song.id) else selected.add(song.id)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoverArt(song, Modifier.size(44.dp), quality = 160)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            song.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (inPlaylist) colors.onSurfaceVariant else colors.onSurface,
                        )
                        Text(
                            if (inPlaylist) "Já está na playlist" else song.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (inPlaylist) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.onSurfaceVariant)
                    } else {
                        Checkbox(checked = checked, onCheckedChange = null)
                    }
                }
            }
        }
    }
}
