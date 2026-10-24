package com.example.still_wave_music_player.ui.navigation

/**
 * Representasi rute navigasi aplikasi Still Wave.
 */
sealed class Screen(val route: String, val title: String) {
    data object Beranda : Screen("beranda", "Beranda")
    data object Jelajah : Screen("jelajah", "Jelajah")
    data object Pustaka : Screen("pustaka", "Pustaka")
    data object Cari    : Screen("cari", "Cari")

    data object Artis : Screen("artis/{browseId}", "Artis") {
        fun createRoute(browseId: String): String = "artis/$browseId"
    }

    data object Album : Screen("album/{browseId}", "Album") {
        fun createRoute(browseId: String): String = "album/$browseId"
    }

    data object DaftarPutar : Screen("daftarputar/{id}", "Daftar Putar") {
        fun createRoute(id: Long): String = "daftarputar/$id"
    }
}
