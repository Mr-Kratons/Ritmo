package com.example.ritmo.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.example.ritmo.RitmoViewModel
import com.example.ritmo.playback.PlayerState
import com.example.ritmo.playback.QueueItem
import com.example.ritmo.ui.components.CoverArt
import com.example.ritmo.ui.components.LocalAddToPlaylist
import com.example.ritmo.ui.components.OptionRow
import com.example.ritmo.ui.components.RitmoSheet
import com.example.ritmo.ui.components.SongRow
import com.example.ritmo.ui.components.rememberDominantColor
import com.example.ritmo.util.formatDuration
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun NowPlayingScreen(vm: RitmoViewModel, onClose: () -> Unit) {
    val state by vm.playerState.collectAsStateWithLifecycle()
    val favorites by vm.favoriteIds.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val dominant by rememberDominantColor(state.current, colors.surfaceContainerHigh)
    val tint by animateColorAsState(
        targetValue = if (dark) lerp(dominant, Color.Black, 0.35f) else lerp(dominant, Color.White, 0.45f),
        animationSpec = tween(600),
        label = "tint",
    )
    var dragY by remember { mutableFloatStateOf(0f) }
    var showQueue by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }
    val addToPlaylist = LocalAddToPlaylist.current

    val song = state.current ?: return
    val liked = song.id in favorites
    val background = colors.background

    Box(
        Modifier
            .fillMaxSize()
            .offset { IntOffset(0, dragY.roundToInt()) }
            .background(Brush.verticalGradient(0f to tint, 0.7f to background, 1f to background))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragY > 220f) onClose()
                        dragY = 0f
                    },
                    onDragCancel = { dragY = 0f },
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        dragY = (dragY + amount).coerceAtLeast(0f)
                    },
                )
            }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Minimizar", modifier = Modifier.size(32.dp))
                }
                Text(
                    "Tocando agora",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.size(48.dp))
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                CoverArt(
                    song,
                    Modifier
                        .aspectRatio(1f)
                        .shadow(24.dp, RoundedCornerShape(20.dp)),
                    quality = 720,
                    shape = RoundedCornerShape(20.dp),
                )
            }

            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(song.title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        song.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { vm.toggleFavorite(song) }) {
                    Icon(
                        if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (liked) "Remover das curtidas" else "Curtir",
                        tint = if (liked) colors.primary else colors.onSurface,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            SeekBar(vm, state.durationMs)

            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { vm.player.toggleShuffle() }) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = if (state.shuffle) "Desativar aleatório" else "Ativar aleatório",
                        tint = if (state.shuffle) colors.primary else colors.onSurface.copy(alpha = 0.7f),
                    )
                }
                IconButton(onClick = { vm.player.previous() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Anterior", modifier = Modifier.size(38.dp))
                }
                Surface(
                    onClick = { vm.player.togglePlay() },
                    shape = CircleShape,
                    color = colors.primary,
                    modifier = Modifier.size(72.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pausar" else "Tocar",
                            tint = colors.onPrimary,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
                IconButton(onClick = { vm.player.next() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Próxima", modifier = Modifier.size(38.dp))
                }
                IconButton(onClick = { vm.player.cycleRepeat() }) {
                    Icon(
                        if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = when (state.repeatMode) {
                            Player.REPEAT_MODE_ALL -> "Repetir tudo"
                            Player.REPEAT_MODE_ONE -> "Repetir esta música"
                            else -> "Repetição desativada"
                        },
                        tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) colors.primary else colors.onSurface.copy(alpha = 0.7f),
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { showQueue = true }) {
                    Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = "Fila", tint = colors.onSurface.copy(alpha = 0.8f))
                }
                IconButton(onClick = { showSleep = true }) {
                    Icon(
                        Icons.Rounded.Bedtime,
                        contentDescription = "Timer de sono",
                        tint = if (state.sleepEndsAt > 0) colors.primary else colors.onSurface.copy(alpha = 0.8f),
                    )
                }
                IconButton(onClick = { addToPlaylist(listOf(song)) }) {
                    Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = "Adicionar à playlist", tint = colors.onSurface.copy(alpha = 0.8f))
                }
            }
        }
    }

    if (showQueue) QueueSheet(vm, state, onDismiss = { showQueue = false })
    if (showSleep) SleepSheet(vm, state.sleepEndsAt, onDismiss = { showSleep = false })
}

@Composable
private fun SeekBar(vm: RitmoViewModel, durationMs: Long) {
    val position by vm.positionMs.collectAsStateWithLifecycle()
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val colors = MaterialTheme.colorScheme
    val maxValue = max(durationMs.toFloat(), 1f)
    val shown = (if (dragging) dragValue else position.toFloat()).coerceIn(0f, maxValue)

    Column(Modifier.fillMaxWidth()) {
        Slider(
            value = shown,
            onValueChange = {
                dragging = true
                dragValue = it
            },
            onValueChangeFinished = {
                vm.player.seekTo(dragValue.toLong())
                dragging = false
            },
            valueRange = 0f..maxValue,
            colors = SliderDefaults.colors(
                thumbColor = colors.onSurface,
                activeTrackColor = colors.onSurface,
                inactiveTrackColor = colors.onSurface.copy(alpha = 0.2f),
            ),
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Text(formatDuration(shown.toLong()), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text(formatDuration(durationMs), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun QueueSheet(vm: RitmoViewModel, state: PlayerState, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val upNext = remember(state.queue, state.currentWindow) {
        state.queue.filter { it.windowIndex > state.currentWindow }
    }
    val local = remember { mutableStateListOf<QueueItem>().apply { addAll(upNext) } }
    var draggedKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(upNext) {
        if (draggedKey == null) {
            local.clear()
            local.addAll(upNext)
        }
    }
    val lazyState = rememberLazyListState()
    val reorder = rememberReorderableLazyListState(lazyState) { from, to ->
        val f = local.indexOfFirst { it.key == from.key }
        val t = local.indexOfFirst { it.key == to.key }
        if (f >= 0 && t >= 0) local.add(t, local.removeAt(f))
    }

    RitmoSheet(onDismiss, fullHeight = true) { _ ->
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Fila", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (upNext.isNotEmpty()) {
                TextButton(onClick = { vm.player.clearUpNext() }) { Text("Limpar a seguir") }
            }
        }
        LazyColumn(state = lazyState, modifier = Modifier.weight(1f)) {
            val current = state.current
            if (current != null) {
                item(key = "current-title") {
                    Text(
                        "Tocando agora",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                }
                item(key = "current") {
                    SongRow(current, current = true, playing = state.isPlaying, onClick = {}, trailing = {})
                }
            }
            item(key = "next-title") {
                Text(
                    "A seguir",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
            if (local.isEmpty()) {
                item(key = "empty") {
                    Text(
                        "Não há mais músicas depois desta. Use “Tocar a seguir” ou “Adicionar à fila” nas músicas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                }
            }
            itemsIndexed(local, key = { _, item -> item.key }) { _, item ->
                ReorderableItem(reorder, key = item.key) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "drag")
                    Surface(shadowElevation = elevation, color = colors.surfaceContainerHigh) {
                        SongRow(
                            song = item.song,
                            onClick = { vm.player.skipTo(item.windowIndex) },
                            trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { vm.player.removeFromQueue(item.windowIndex) }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Remover da fila", tint = colors.onSurfaceVariant)
                                    }
                                    Icon(
                                        Icons.Rounded.DragHandle,
                                        contentDescription = "Arrastar para reordenar",
                                        tint = colors.onSurfaceVariant,
                                        modifier = Modifier
                                            .draggableHandle(
                                                onDragStarted = { draggedKey = item.key },
                                                onDragStopped = {
                                                    val key = draggedKey
                                                    draggedKey = null
                                                    if (key != null) {
                                                        val from = upNext.firstOrNull { it.key == key }?.windowIndex
                                                        val finalPos = local.indexOfFirst { it.key == key }
                                                        val to = upNext.getOrNull(finalPos)?.windowIndex
                                                        if (from != null && to != null) vm.player.moveInQueue(from, to)
                                                    }
                                                },
                                            )
                                            .padding(12.dp)
                                            .size(24.dp),
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepSheet(vm: RitmoViewModel, endsAt: Long, onDismiss: () -> Unit) {
    RitmoSheet(onDismiss) { close ->
        Text(
            "Timer de sono",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        if (endsAt > 0) {
            val time = remember(endsAt) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(endsAt)) }
            Text(
                "A música vai pausar às $time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
            OptionRow(Icons.Rounded.Close, "Desativar o timer") { vm.player.cancelSleepTimer(); close() }
        }
        listOf(5, 10, 15, 30, 45, 60).forEach { minutes ->
            OptionRow(Icons.Rounded.Timer, "$minutes minutos") { vm.player.startSleepTimer(minutes); close() }
        }
        Spacer(Modifier.navigationBarsPadding().height(16.dp))
    }
}
