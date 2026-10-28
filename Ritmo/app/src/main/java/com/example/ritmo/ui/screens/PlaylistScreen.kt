package com.example.ritmo.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.ritmo.RitmoViewModel
import com.example.ritmo.data.Playlist
import com.example.ritmo.data.PlaylistEntry
import com.example.ritmo.ui.components.BackHeader
import com.example.ritmo.ui.components.ConfirmDialog
import com.example.ritmo.ui.components.EmptyState
import com.example.ritmo.ui.components.NameDialog
import com.example.ritmo.ui.components.NotFound
import com.example.ritmo.ui.components.PlayActions
import com.example.ritmo.ui.components.PlaylistCover
import com.example.ritmo.ui.components.SongPickerSheet
import com.example.ritmo.ui.components.SongRow
import com.example.ritmo.util.formatLongDuration
import com.example.ritmo.util.songsCount
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun PlaylistScreen(vm: RitmoViewModel, nav: NavController, playlistId: Long) {
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val allSongs by vm.songs.collectAsStateWithLifecycle()
    val state by vm.playerState.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val playlist = playlists.firstOrNull { it.id == playlistId }

    var editing by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }

    // Cópia local usada durante o arrastar; só vai para o banco quando o dedo solta.
    val local = remember(playlistId) {
        mutableStateListOf<PlaylistEntry>().also { it.addAll(playlist?.entries.orEmpty()) }
    }
    LaunchedEffect(playlist?.entries) {
        if (!dragging) {
            local.clear()
            local.addAll(playlist?.entries.orEmpty())
        }
    }
    val lazyState = rememberLazyListState()
    val reorder = rememberReorderableLazyListState(lazyState) { from, to ->
        val f = local.indexOfFirst { it.entryId == from.key }
        val t = local.indexOfFirst { it.entryId == to.key }
        if (f >= 0 && t >= 0) local.add(t, local.removeAt(f))
    }

    if (playlist == null) {
        NotFound("Esta playlist não existe mais.") { nav.popBackStack() }
        return
    }
    val inPlaylist = remember(playlist.entries) { playlist.entries.mapTo(HashSet()) { it.song.id } }

    Column(Modifier.fillMaxSize()) {
        BackHeader(onBack = { nav.popBackStack() }) {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Mais opções")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Tocar a seguir") },
                        leadingIcon = { Icon(Icons.Rounded.SkipNext, contentDescription = null) },
                        onClick = { menuOpen = false; vm.playNext(playlist.songs) },
                    )
                    DropdownMenuItem(
                        text = { Text("Adicionar à fila") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null) },
                        onClick = { menuOpen = false; vm.enqueue(playlist.songs) },
                    )
                    DropdownMenuItem(
                        text = { Text("Renomear") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        onClick = { menuOpen = false; showRename = true },
                    )
                    DropdownMenuItem(
                        text = { Text("Excluir playlist", color = colors.error) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = colors.error) },
                        onClick = { menuOpen = false; showDelete = true },
                    )
                }
            }
        }

        LazyColumn(
            state = lazyState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item(key = "header") { PlaylistHeader(playlist) }
            item(key = "actions") {
                PlayActions(
                    onPlay = { vm.play(playlist.songs, 0) },
                    onShuffle = { vm.shufflePlay(playlist.songs) },
                    leading = {
                        IconButton(onClick = { showPicker = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = "Adicionar músicas")
                        }
                        IconButton(onClick = { editing = !editing }) {
                            Icon(
                                if (editing) Icons.Rounded.Check else Icons.Rounded.Edit,
                                contentDescription = if (editing) "Concluir edição" else "Editar ordem e remover músicas",
                                tint = if (editing) colors.primary else colors.onSurface,
                            )
                        }
                    },
                )
            }
            if (editing && local.isNotEmpty()) {
                item(key = "hint") {
                    Text(
                        "Arraste pelo ícone ≡ para mudar a ordem.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                }
            }
            if (local.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.AutoMirrored.Rounded.QueueMusic,
                        title = "Playlist vazia",
                        message = "Toque em + para escolher músicas do aparelho.",
                        actionLabel = "Adicionar músicas",
                        onAction = { showPicker = true },
                    )
                }
            }
            itemsIndexed(local, key = { _, entry -> entry.entryId }) { index, entry ->
                ReorderableItem(reorder, key = entry.entryId) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "drag")
                    Surface(shadowElevation = elevation, color = colors.background) {
                        SongRow(
                            song = entry.song,
                            current = entry.song.id == state.current?.id,
                            playing = state.isPlaying,
                            onClick = { if (!editing) vm.play(playlist.songs, index) },
                            trailing = if (editing) {
                                {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { vm.removeFromPlaylist(playlist.id, listOf(entry.entryId)) }
                                        ) {
                                            Icon(
                                                Icons.Rounded.RemoveCircleOutline,
                                                contentDescription = "Remover da playlist",
                                                tint = colors.error,
                                            )
                                        }
                                        Icon(
                                            Icons.Rounded.DragHandle,
                                            contentDescription = "Arrastar para reordenar",
                                            tint = colors.onSurfaceVariant,
                                            modifier = Modifier
                                                .draggableHandle(
                                                    onDragStarted = { dragging = true },
                                                    onDragStopped = {
                                                        dragging = false
                                                        vm.reorderPlaylist(playlist.id, local.map { it.entryId })
                                                    },
                                                )
                                                .padding(12.dp)
                                                .size(24.dp),
                                        )
                                    }
                                }
                            } else null,
                        )
                    }
                }
            }
        }
    }

    if (showRename) {
        NameDialog("Renomear playlist", playlist.name, "Salvar", onDismiss = { showRename = false }) { name ->
            showRename = false
            vm.renamePlaylist(playlist.id, name)
        }
    }
    if (showDelete) {
        ConfirmDialog(
            title = "Excluir playlist?",
            message = "“${playlist.name}” será excluída. As músicas continuam no aparelho.",
            confirmLabel = "Excluir",
            onDismiss = { showDelete = false },
        ) {
            showDelete = false
            nav.popBackStack()
            vm.deletePlaylist(playlist.id, playlist.name)
        }
    }
    if (showPicker) {
        SongPickerSheet(
            songs = allSongs,
            alreadyIn = inPlaylist,
            onDismiss = { showPicker = false },
            onConfirm = { picked -> vm.addToPlaylist(playlist.id, playlist.name, picked) },
        )
    }
}

@Composable
private fun PlaylistHeader(playlist: Playlist) {
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
            PlaylistCover(playlist.coverSongs, Modifier.size(200.dp), quality = 480, shape = RoundedCornerShape(12.dp))
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                playlist.name,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (playlist.entries.isEmpty()) "Vazia"
                else "${songsCount(playlist.entries.size)}, ${formatLongDuration(playlist.durationMs)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
