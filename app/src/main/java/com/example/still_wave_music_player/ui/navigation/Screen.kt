package com.example.still_wave_music_player.ui.navigation

/**
 * Representasi rute navigasi aplikasi Still Wave.
 */
sealed class Screen(val route: String, val title: String) {
    data object Beranda : Screen("beranda", "Beranda")
    data object Jelajah : Screen("jelajah", "Jelajah")
    data object Pustaka : Screen("pustaka", "Pustaka")
    data object Cari    : Screen("cari", "Cari")
}
