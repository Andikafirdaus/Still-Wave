package com.example.still_wave_music_player

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.still_wave_music_player.ui.AppRoot
import com.example.still_wave_music_player.ui.theme.StillwavemusicplayerTheme

/**
 * Entry point utama aplikasi Still Wave.
 * Mengaktifkan tampilan edge-to-edge dengan bilah sistem transparan
 * dan ikon bilah sistem terang di atas tema gelap.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            StillwavemusicplayerTheme {
                AppRoot()
            }
        }
    }
}