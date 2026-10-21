package com.example.still_wave_music_player.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.still_wave_music_player.data.model.Song
import com.example.still_wave_music_player.ui.components.FloatingNavBar
import com.example.still_wave_music_player.ui.components.MiniPlayer
import com.example.still_wave_music_player.ui.navigation.Screen
import com.example.still_wave_music_player.ui.screens.ExploreScreen
import com.example.still_wave_music_player.ui.screens.HomeScreen
import com.example.still_wave_music_player.ui.screens.LibraryScreen
import com.example.still_wave_music_player.ui.screens.PlayerScreen
import com.example.still_wave_music_player.ui.screens.SearchScreen
import com.example.still_wave_music_player.ui.theme.Background
import com.example.still_wave_music_player.ui.theme.Motion
import com.example.still_wave_music_player.viewmodels.LibraryViewModel
import com.example.still_wave_music_player.viewmodels.PlayerViewModel

/**
 * Root composable aplikasi (AppRoot).
 * Berisi NavHost dengan 4 rute utama, FloatingNavBar melayang di bagian bawah,
 * MiniPlayer di atas FloatingNavBar, dan PlayerScreen sebagai lapisan penuh di atasnya.
 * Menyediakan satu instance LibraryViewModel dan PlayerViewModel yang dibagikan ke semua layar,
 * serta meminta izin POST_NOTIFICATIONS pada Android 13+ saat pertama kali memutar lagu.
 */
@Composable
fun AppRoot(
    playerViewModel: PlayerViewModel = viewModel(),
    libraryViewModel: LibraryViewModel = viewModel()
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Beranda.route

    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val lyricsState by playerViewModel.lyricsState.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    var isPlayerOpen by rememberSaveable { mutableStateOf(false) }

    // State dan launcher untuk izin POST_NOTIFICATIONS (Android 13+)
    var hasRequestedNotificationPermission by rememberSaveable { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Pemutaran tetap berjalan lancar sekalipun izin notifikasi ditolak
    }

    val requestNotificationPermissionIfNeeded: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasRequestedNotificationPermission) {
            hasRequestedNotificationPermission = true
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!isGranted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val handlePlaySong: (Song, List<Song>) -> Unit = { song, list ->
        requestNotificationPermissionIfNeeded()
        playerViewModel.putar(song, list)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // ── 1. NavHost: Konten Utama yang Dapat Digulir ────────────────────────
        NavHost(
            navController = navController,
            startDestination = Screen.Beranda.route,
            enterTransition = {
                fadeIn(animationSpec = tween(180)) +
                    scaleIn(initialScale = 0.98f, animationSpec = tween(180))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(180))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(180)) +
                    scaleIn(initialScale = 0.98f, animationSpec = tween(180))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(180))
            },
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Screen.Beranda.route) {
                HomeScreen(
                    libraryState = libraryState,
                    requiredPermission = libraryViewModel.getRequiredPermission(),
                    onPermissionGranted = libraryViewModel::muat,
                    onPlaySong = handlePlaySong
                )
            }
            composable(Screen.Jelajah.route) {
                ExploreScreen()
            }
            composable(Screen.Pustaka.route) {
                LibraryScreen(
                    libraryState = libraryState,
                    requiredPermission = libraryViewModel.getRequiredPermission(),
                    onPermissionGranted = libraryViewModel::muat,
                    onPlaySong = handlePlaySong
                )
            }
            composable(Screen.Cari.route) {
                SearchScreen(
                    libraryState = libraryState,
                    requiredPermission = libraryViewModel.getRequiredPermission(),
                    onPermissionGranted = libraryViewModel::muat,
                    onPlaySong = handlePlaySong
                )
            }
        }

        // ── 2. Mini Player Melayang (8dp di atas FloatingNavBar) ───────────────
        MiniPlayer(
            song = playerState.currentSong,
            isPlaying = playerState.isPlaying,
            progress = playerState.progress,
            isBuffering = playerState.isBuffering,
            onOpenPlayer = { isPlayerOpen = true },
            onTogglePlayPause = {
                requestNotificationPermissionIfNeeded()
                playerViewModel.togglePlayPause()
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 84.dp)
        )

        // ── 3. Bar Navigasi Bawah Melayang (FloatingNavBar) ───────────────────
        FloatingNavBar(
            currentRoute = currentRoute,
            onNavigateToTab = { screen ->
                if (currentRoute != screen.route) {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        )

        // ── 4. Layar Pemutar Penuh (PlayerScreen) sebagai Lapisan di Atas ──────
        AnimatedVisibility(
            visible = isPlayerOpen && playerState.currentSong != null,
            enter = slideInVertically(
                animationSpec = spring(
                    dampingRatio = Motion.Gentle.dampingRatio,
                    stiffness = Motion.Gentle.stiffness
                ),
                initialOffsetY = { it }
            ) + fadeIn(
                animationSpec = spring(
                    dampingRatio = Motion.Gentle.dampingRatio,
                    stiffness = Motion.Gentle.stiffness
                )
            ),
            exit = slideOutVertically(
                animationSpec = spring(
                    dampingRatio = Motion.Gentle.dampingRatio,
                    stiffness = Motion.Gentle.stiffness
                ),
                targetOffsetY = { it }
            ) + fadeOut(
                animationSpec = spring(
                    dampingRatio = Motion.Gentle.dampingRatio,
                    stiffness = Motion.Gentle.stiffness
                )
            )
        ) {
            val song = playerState.currentSong
            if (song != null) {
                PlayerScreen(
                    song = song,
                    isPlaying = playerState.isPlaying,
                    progress = playerState.progress,
                    currentPositionMs = playerState.currentPositionMs,
                    lyricsState = lyricsState,
                    isBuffering = playerState.isBuffering,
                    isShuffle = playerState.isShuffle,
                    repeatMode = playerState.repeatMode,
                    onClose = { isPlayerOpen = false },
                    onTogglePlayPause = {
                        requestNotificationPermissionIfNeeded()
                        playerViewModel.togglePlayPause()
                    },
                    onNext = playerViewModel::berikutnya,
                    onPrevious = playerViewModel::sebelumnya,
                    onSeek = playerViewModel::geserProgres,
                    onSeekToPosition = playerViewModel::seekToPosition,
                    onToggleShuffle = playerViewModel::toggleAcak,
                    onChangeRepeatMode = playerViewModel::gantiModeUlang
                )
            }
        }
    }
}
