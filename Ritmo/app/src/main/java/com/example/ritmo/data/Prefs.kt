package com.example.ritmo.data

import android.content.Context
import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Preferências simples + última fila tocada (para continuar de onde parou). */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("ritmo_prefs", Context.MODE_PRIVATE)

    private inline fun <reified E : Enum<E>> readEnum(key: String, default: E): E =
        runCatching { enumValueOf<E>(sp.getString(key, null) ?: default.name) }.getOrDefault(default)

    private val _themeMode = MutableStateFlow(readEnum("theme", ThemeMode.SYSTEM))
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()
    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        sp.edit().putString("theme", mode.name).apply()
    }

    private val _songSort = MutableStateFlow(readEnum("sort", SongSort.TITLE))
    val songSort: StateFlow<SongSort> = _songSort.asStateFlow()
    fun setSongSort(sort: SongSort) {
        _songSort.value = sort
        sp.edit().putString("sort", sort.name).apply()
    }

    private val _minDurationSec = MutableStateFlow(sp.getInt("min_duration", 30))
    val minDurationSec: StateFlow<Int> = _minDurationSec.asStateFlow()
    fun setMinDurationSec(sec: Int) {
        _minDurationSec.value = sec
        sp.edit().putInt("min_duration", sec).apply()
    }

    private val _musicOnly = MutableStateFlow(sp.getBoolean("music_only", true))
    val musicOnly: StateFlow<Boolean> = _musicOnly.asStateFlow()
    fun setMusicOnly(value: Boolean) {
        _musicOnly.value = value
        sp.edit().putBoolean("music_only", value).apply()
    }

    // ---- sessão do player ----
    var shuffle: Boolean
        get() = sp.getBoolean("shuffle", false)
        set(value) { sp.edit().putBoolean("shuffle", value).apply() }

    var repeatMode: Int
        get() = sp.getInt("repeat", Player.REPEAT_MODE_OFF)
        set(value) { sp.edit().putInt("repeat", value).apply() }

    fun saveQueue(songIds: List<Long>, index: Int, positionMs: Long) {
        sp.edit()
            .putString("queue", songIds.joinToString(","))
            .putInt("queue_index", index)
            .putLong("queue_pos", positionMs)
            .apply()
    }

    val savedQueue: List<Long>
        get() = sp.getString("queue", "").orEmpty().split(',').mapNotNull { it.toLongOrNull() }
    val savedIndex: Int get() = sp.getInt("queue_index", 0)
    val savedPosition: Long get() = sp.getLong("queue_pos", 0L)
}
