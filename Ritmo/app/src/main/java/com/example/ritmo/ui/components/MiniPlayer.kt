package com.example.ritmo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ritmo.RitmoViewModel
import com.example.ritmo.playback.PlayerState

@Composable
fun MiniPlayer(
    vm: RitmoViewModel,
    state: PlayerState,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val song = state.current ?: return
    val favorites by vm.favoriteIds.collectAsStateWithLifecycle()
    val liked = song.id in favorites
    val colors = MaterialTheme.colorScheme

    Surface(
        onClick = onOpen,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .height(60.dp),
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceContainerHigh,
        shadowElevation = 6.dp,
    ) {
        Box {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoverArt(song, Modifier.size(44.dp), quality = 160)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { vm.toggleFavorite(song) }) {
                    Icon(
                        if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (liked) "Remover das curtidas" else "Curtir",
                        tint = if (liked) colors.primary else colors.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { vm.player.togglePlay() }) {
                    Icon(
                        if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pausar" else "Tocar",
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
            MiniProgress(
                vm,
                state.durationMs,
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp),
            )
        }
    }
}

@Composable
private fun MiniProgress(vm: RitmoViewModel, durationMs: Long, modifier: Modifier = Modifier) {
    val position by vm.positionMs.collectAsStateWithLifecycle()
    val fraction = if (durationMs > 0) (position.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(colors.onSurfaceVariant.copy(alpha = 0.25f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(colors.primary)
        )
    }
}
