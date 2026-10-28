package com.example.ritmo.util

import java.text.Normalizer
import java.util.Locale

private val DiacriticsRegex = Regex("\\p{InCombiningDiacriticalMarks}+")

/** Minúsculas e sem acentos: usado em busca e ordenação. */
fun String.normalizeForSearch(): String =
    DiacriticsRegex.replace(Normalizer.normalize(this, Normalizer.Form.NFD), "")
        .lowercase(Locale.ROOT)
        .trim()

fun formatDuration(ms: Long): String {
    val total = ms.coerceAtLeast(0L) / 1000L
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%d:%02d", m, s)
}

fun formatLongDuration(ms: Long): String {
    val minutes = ms / 60_000L
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "$h h $m min"
        h > 0 -> "$h h"
        else -> "${maxOf(m, 1L)} min"
    }
}

fun songsCount(n: Int): String = if (n == 1) "1 música" else "$n músicas"
fun albumsCount(n: Int): String = if (n == 1) "1 álbum" else "$n álbuns"
