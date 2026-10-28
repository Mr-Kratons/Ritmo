package com.example.ritmo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ritmo.ui.RitmoRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { RitmoRoot() }
    }

    override fun onStop() {
        super.onStop()
        (application as RitmoApp).player.persist()
    }
}
