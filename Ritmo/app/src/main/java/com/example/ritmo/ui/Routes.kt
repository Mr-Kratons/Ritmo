package com.example.ritmo.ui

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"
    const val PLAYLIST = "playlist/{id}"
    const val ALBUM = "album/{id}"
    const val ARTIST = "artist/{id}"

    fun playlist(id: Long) = "playlist/$id"
    fun album(id: Long) = "album/$id"
    fun artist(id: Long) = "artist/$id"
}
