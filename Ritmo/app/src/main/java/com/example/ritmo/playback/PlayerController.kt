package com.example.ritmo.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.ritmo.data.MusicRepository
import com.example.ritmo.data.Prefs
import com.example.ritmo.data.Song
import com.example.ritmo.data.albumArtUri
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val MAX_QUEUE = 1000

/** Uma posição da fila. `key` é único mesmo se a mesma música aparecer duas vezes. */
data class QueueItem(val key: String, val windowIndex: Int, val song: Song)

data class PlayerState(
    val current: Song? = null,
    val currentKey: String? = null,
    val currentWindow: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val queue: List<QueueItem> = emptyList(),
    val sleepEndsAt: Long = 0L,
) {
    val hasQueue: Boolean get() = current != null
}

/**
 * Ponte entre a interface e o serviço de reprodução.
 *
 * O "aleatório" é feito de verdade na fila (embaralha o que vem a seguir e guarda a ordem
 * original para restaurar), assim a fila exibida é sempre a ordem real e pode ser editada.
 */
class PlayerController(
    private val context: Context,
    private val repo: MusicRepository,
    private val prefs: Prefs,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: MediaController? = null
    private var future: ListenableFuture<MediaController>? = null

    private val _state = MutableStateFlow(
        PlayerState(shuffle = prefs.shuffle, repeatMode = prefs.repeatMode)
    )
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private var counter = 0L
    private var originalKeys: List<String> = emptyList()
    private var lastCountedKey: String? = null
    private var tickerJob: Job? = null
    private var sleepJob: Job? = null
    private var restored = false

    // ---------------- Conexão ----------------

    fun connect() {
        if (future != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val f = MediaController.Builder(context, token).buildAsync()
        future = f
        f.addListener(
            {
                val c = try {
                    f.get()
                } catch (e: Exception) {
                    future = null
                    return@addListener
                }
                controller = c
                c.addListener(listener)
                c.shuffleModeEnabled = false // o aleatório é feito na própria fila
                if (c.mediaItemCount == 0) c.repeatMode = prefs.repeatMode
                publish(rebuildQueue = true)
                _positionMs.value = c.currentPosition.coerceAtLeast(0L)
                if (c.isPlaying) onPlayingChanged(true)
                restoreSession()
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(rebuildQueue = events.contains(Player.EVENT_TIMELINE_CHANGED))

            if (events.containsAny(
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_POSITION_DISCONTINUITY,
                )
            ) {
                _positionMs.value = player.currentPosition.coerceAtLeast(0L)
            }
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) onPlayingChanged(player.isPlaying)
            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) && player.playWhenReady) countPlayIfNeeded()
            if (events.containsAny(
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_TIMELINE_CHANGED,
                )
            ) {
                saveSession()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val c = controller ?: return
            _errors.tryEmit("Não foi possível tocar esta música")
            if (c.hasNextMediaItem()) {
                c.seekToNextMediaItem()
                c.prepare()
                c.play()
            }
        }
    }

    private fun publish(rebuildQueue: Boolean) {
        val c = controller ?: return
        val songMap = repo.songById.value
        val item = c.currentMediaItem
        val song = item?.let { songMap[songIdOf(it.mediaId)] }
        val queue = if (rebuildQueue) buildQueue(c, songMap) else _state.value.queue
        val duration = c.duration
        _state.value = _state.value.copy(
            current = song,
            currentKey = item?.mediaId,
            currentWindow = if (item == null) -1 else c.currentMediaItemIndex,
            isPlaying = c.isPlaying,
            isBuffering = c.playbackState == Player.STATE_BUFFERING,
            durationMs = if (duration == C.TIME_UNSET || duration < 0) song?.durationMs ?: 0L else duration,
            repeatMode = c.repeatMode,
            queue = queue,
        )
    }

    private fun buildQueue(c: MediaController, songMap: Map<Long, Song>): List<QueueItem> {
        val out = ArrayList<QueueItem>(c.mediaItemCount)
        for (i in 0 until c.mediaItemCount) {
            val item = c.getMediaItemAt(i)
            val song = songMap[songIdOf(item.mediaId)] ?: continue
            out.add(QueueItem(item.mediaId, i, song))
        }
        return out
    }

    private fun onPlayingChanged(playing: Boolean) {
        tickerJob?.cancel()
        if (playing) {
            tickerJob = scope.launch {
                while (isActive) {
                    controller?.let { _positionMs.value = it.currentPosition.coerceAtLeast(0L) }
                    delay(300)
                }
            }
            countPlayIfNeeded()
        } else {
            controller?.let { _positionMs.value = it.currentPosition.coerceAtLeast(0L) }
        }
    }

    /** Conta uma reprodução quando uma música nova começa (alimenta "tocadas recentemente"). */
    private fun countPlayIfNeeded() {
        val key = controller?.currentMediaItem?.mediaId ?: return
        if (key == lastCountedKey) return
        lastCountedKey = key
        val id = songIdOf(key)
        if (id > 0) repo.registerPlay(id)
    }

    private fun songIdOf(mediaId: String): Long = mediaId.substringBefore('#').toLongOrNull() ?: -1L

    private fun mediaItem(song: Song): MediaItem = MediaItem.Builder()
        .setMediaId("${song.id}#${counter++}")
        .setUri(song.uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .setArtworkUri(song.albumArtUri)
                .build()
        )
        .build()

    // ---------------- Tocar / fila ----------------

    /** Toca [songs] a partir de [startIndex]. Com aleatório ligado, a música escolhida vem primeiro. */
    fun playSongs(songs: List<Song>, startIndex: Int = 0, forceShuffle: Boolean = false) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        val shuffle = forceShuffle || _state.value.shuffle

        // Bibliotecas enormes viram uma fila de até MAX_QUEUE músicas (limite de dados entre app e serviço).
        var list = songs
        var start = startIndex.coerceIn(songs.indices)
        if (list.size > MAX_QUEUE) {
            if (forceShuffle) {
                list = list.shuffled().take(MAX_QUEUE)
                start = 0
            } else {
                val from = (start - 100).coerceIn(0, list.size - MAX_QUEUE)
                list = list.subList(from, from + MAX_QUEUE)
                start -= from
            }
        }

        val items = list.map(::mediaItem)
        originalKeys = items.map { it.mediaId }

        if (shuffle) {
            val first = if (forceShuffle) items.indices.random() else start
            val ordered = listOf(items[first]) + items.filterIndexed { i, _ -> i != first }.shuffled()
            c.setMediaItems(ordered, 0, 0L)
        } else {
            c.setMediaItems(items, start, 0L)
        }
        setShuffleFlag(shuffle)
        c.prepare()
        c.play()
    }

    fun playNext(songs: List<Song>) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        if (c.mediaItemCount == 0) {
            playSongs(songs)
            return
        }
        c.addMediaItems(c.currentMediaItemIndex + 1, songs.map(::mediaItem))
    }

    fun enqueue(songs: List<Song>) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        if (c.mediaItemCount == 0) {
            playSongs(songs)
            return
        }
        c.addMediaItems(songs.map(::mediaItem))
    }

    fun removeFromQueue(windowIndex: Int) {
        controller?.removeMediaItem(windowIndex)
    }

    fun moveInQueue(from: Int, to: Int) {
        if (from == to) return
        controller?.moveMediaItem(from, to)
    }

    fun skipTo(windowIndex: Int) {
        val c = controller ?: return
        c.seekTo(windowIndex, 0L)
        c.play()
    }

    fun clearUpNext() {
        val c = controller ?: return
        val from = c.currentMediaItemIndex + 1
        if (from < c.mediaItemCount) c.removeMediaItems(from, c.mediaItemCount)
    }

    // ---------------- Controles ----------------

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            if (c.playbackState == Player.STATE_ENDED) c.seekTo(0, 0L)
            c.play()
        }
    }

    fun next() {
        controller?.seekToNext()
    }

    /** Depois de 3 s volta ao início da música; antes disso, vai para a anterior. */
    fun previous() {
        controller?.seekToPrevious()
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        _positionMs.value = ms
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        prefs.repeatMode = c.repeatMode
    }

    fun toggleShuffle() {
        val c = controller
        val turnOn = !_state.value.shuffle
        if (c != null && c.mediaItemCount > 1) {
            val cur = c.currentMediaItemIndex
            if (turnOn) {
                originalKeys = (0 until c.mediaItemCount).map { c.getMediaItemAt(it).mediaId }
                val after = (cur + 1 until c.mediaItemCount).map { c.getMediaItemAt(it) }
                if (after.isNotEmpty()) {
                    c.removeMediaItems(cur + 1, c.mediaItemCount)
                    c.addMediaItems(after.shuffled())
                }
            } else {
                restoreOriginalOrder(c)
            }
        }
        setShuffleFlag(turnOn)
    }

    private fun restoreOriginalOrder(c: MediaController) {
        val count = c.mediaItemCount
        val cur = c.currentMediaItemIndex
        val items = (0 until count).map { c.getMediaItemAt(it) }
        val currentKey = items[cur].mediaId
        val byKey = items.associateBy { it.mediaId }
        val originalSet = originalKeys.toHashSet()
        val desired = originalKeys.mapNotNull { byKey[it] } + items.filter { it.mediaId !in originalSet }
        val curPos = desired.indexOfFirst { it.mediaId == currentKey }
        if (curPos < 0) return
        val before = desired.subList(0, curPos)
        val after = desired.subList(curPos + 1, desired.size)

        if (cur + 1 < count) c.removeMediaItems(cur + 1, count)
        if (cur > 0) c.removeMediaItems(0, cur)
        if (before.isNotEmpty()) c.addMediaItems(0, before)
        if (after.isNotEmpty()) c.addMediaItems(after)
    }

    private fun setShuffleFlag(on: Boolean) {
        prefs.shuffle = on
        _state.value = _state.value.copy(shuffle = on)
    }

    // ---------------- Timer de sono ----------------

    fun startSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        val millis = minutes * 60_000L
        _state.value = _state.value.copy(sleepEndsAt = System.currentTimeMillis() + millis)
        sleepJob = scope.launch {
            delay(millis)
            controller?.pause()
            _state.value = _state.value.copy(sleepEndsAt = 0L)
        }
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel()
        sleepJob = null
        _state.value = _state.value.copy(sleepEndsAt = 0L)
    }

    // ---------------- Continuar de onde parou ----------------

    fun persist() = saveSession()

    private fun saveSession() {
        val c = controller ?: return
        if (c.mediaItemCount == 0) return
        val ids = ArrayList<Long>(c.mediaItemCount)
        for (i in 0 until c.mediaItemCount) ids.add(songIdOf(c.getMediaItemAt(i).mediaId))
        prefs.saveQueue(ids, c.currentMediaItemIndex, c.currentPosition.coerceAtLeast(0L))
    }

    private fun restoreSession() {
        if (restored) return
        restored = true
        scope.launch {
            repo.loaded.first { it }
            val c = controller ?: return@launch
            if (c.mediaItemCount > 0) return@launch // já está tocando (serviço continuou vivo)
            val saved = prefs.savedQueue
            if (saved.isEmpty()) return@launch
            val songMap = repo.songById.value
            val kept = saved.withIndex().filter { songMap.containsKey(it.value) }
            if (kept.isEmpty()) return@launch
            val savedIndex = prefs.savedIndex
            val index = kept.indexOfFirst { it.index >= savedIndex }.let { if (it < 0) kept.lastIndex else it }
            val items = kept.map { mediaItem(songMap.getValue(it.value)) }
            originalKeys = items.map { it.mediaId }
            val position = if (kept[index].index == savedIndex) prefs.savedPosition else 0L
            c.setMediaItems(items, index, position)
            c.prepare() // pronto para continuar, mas sem tocar sozinho
        }
    }
}
