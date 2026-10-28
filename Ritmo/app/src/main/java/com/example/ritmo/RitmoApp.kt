package com.example.ritmo

import android.app.Application
import com.example.ritmo.data.MusicRepository
import com.example.ritmo.data.Prefs
import com.example.ritmo.playback.PlayerController

class RitmoApp : Application() {
    lateinit var prefs: Prefs
        private set
    lateinit var repository: MusicRepository
        private set
    lateinit var player: PlayerController
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        repository = MusicRepository(this, prefs)
        player = PlayerController(this, repository, prefs)
    }
}
