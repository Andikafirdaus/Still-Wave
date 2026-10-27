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
import androidx.compose.runtime.remember
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
import com.example.still_wave_music_player.data.local.getSongKey
import com.example.still_wave_music_player.data.model.Song
import com.example.still_wave_music_player.ui.components.AddToPlaylistSheet
import com.example.still_wave_music_player.ui.components.FloatingNavBar
import com.example.still_wave_music_player.ui.components.MiniPlayer
import com.example.still_wave_music_player.ui.components.SettingsSheet
import com.example.still_wave_music_player.ui.components.SongOptionsSheet
import com.example.still_wave_music_player.ui.navigation.Screen
import com.example.still_wave_music_player.ui.screens.AlbumScreen
import com.example.still_wave_music_player.ui.screens.ArtistScreen
import com.example.still_wave_music_player.ui.screens.HomeScreen
import com.example.still_wave_music_player.ui.screens.LibraryScreen
import com.example.still_wave_music_player.ui.screens.PlayerScreen
import com.example.still_wave_music_player.ui.screens.PlaylistScreen
import com.example.still_wave_music_player.ui.screens.SearchScreen
import com.example.still_wave_music_player.ui.screens.TogetherScreen // Import layar baru
import com.example.still_wave_music_player.ui.theme.Background
import com.example.still_wave_music_player.ui.theme.Motion
import com.example.still_wave_music_player.viewmodels.LibraryViewModel
import com.example.still_wave_music_player.viewmodels.PlayerViewModel
import com.example.still_wave_music_player.viewmodels.PlaylistViewModel
import com.example.still_wave_music_player.viewmodels.QueueSource

@Composable
fun AppRoot(
    playerViewModel: PlayerViewModel = viewModel(),
    libraryViewModel: LibraryViewModel = viewModel(),
    playlistViewModel: PlaylistViewModel = viewModel()
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Beranda.route

    // Menambahkan Screen.Together ke daftar tab utama
    val isMainTab = currentRoute in listOf(
        Screen.Beranda.route,
        Screen.Pustaka.route,
        Screen.Together.route,
        Screen.Cari.route
    )

    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val lyricsState by playerViewModel.lyricsState.collectAsStateWithLifecycle()
    val isAutoRadioEnabled by playerViewModel.isAutoRadioEnabled.collectAsStateWithLifecycle()
    val crossfadeSeconds by playerViewModel.crossfadeSeconds.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val isFavorite by playerViewModel.isFavorite(
        playerState.currentSong ?: Song(id = "", title = "", artist = "")
    ).collectAsStateWithLifecycle(initialValue = false)

    val rawRecentHistory by playerViewModel.historyRepository.getRecent(50)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val recentlyPlayed = remember(rawRecentHistory) {
        rawRecentHistory.distinctBy { it.getSongKey() }.take(10)
    }

    var isPlayerOpen by rememberSaveable { mutableStateOf(false) }
    var isSettingsOpen by rememberSaveable { mutableStateOf(false) }
    var songForOptions by remember { mutableStateOf<Song?>(null) }
    var songForAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var pendingMood by remember { mutableStateOf<String?>(null) }

    var hasRequestedNotificationPermission by rememberSaveable { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

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
        NavHost(
            navController = navController,
            startDestination = Screen.Beranda.route,
            enterTransition = { fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.98f, animationSpec = tween(180)) },
            exitTransition = { fadeOut(animationSpec = tween(180)) },
            popEnterTransition = { fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.98f, animationSpec = tween(180)) },
            popExitTransition = { fadeOut(animationSpec = tween(180)) },
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Screen.Beranda.route) {
                HomeScreen(
                    recentlyPlayed = recentlyPlayed,
                    onPlaySong = handlePlaySong,
                    onOpenAddToPlaylist = { songForOptions = it },
                    onSelectMood = { mood ->
                        pendingMood = mood
                        navController.navigate(Screen.Cari.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenSettings = { isSettingsOpen = true }
                )
            }

            composable(Screen.Pustaka.route) {
                LibraryScreen(
                    libraryState = libraryState,
                    requiredPermission = libraryViewModel.getRequiredPermission(),
                    onPermissionGranted = libraryViewModel::muat,
                    playerViewModel = playerViewModel,
                    playlistViewModel = playlistViewModel,
                    onPlaySong = handlePlaySong,
                    onNavigateToPlaylist = { playlistId ->
                        navController.navigate(Screen.DaftarPutar.createRoute(playlistId))
                    },
                    onOpenAddToPlaylist = { songForOptions = it },
                    onOpenSettings = { isSettingsOpen = true }
                )
            }

            // ── LAYAR LISTENING TOGETHER ─────────────────────────────────────
            composable(Screen.Together.route) {
                TogetherScreen() // Memanggil layar Together yang baru dibuat
            }

            composable(Screen.Cari.route) {
                SearchScreen(
                    initialMood = pendingMood,
                    onMoodConsumed = { pendingMood = null },
                    onPlaySong = handlePlaySong,
                    onNavigateToArtist = { browseId ->
                        navController.navigate(Screen.Artis.createRoute(browseId))
                    },
                    onNavigateToAlbum = { browseId ->
                        navController.navigate(Screen.Album.createRoute(browseId))
                    },
                    onOpenAddToPlaylist = { songForOptions = it }
                )
            }

            composable(Screen.Artis.route) { backStackEntry ->
                val browseId = backStackEntry.arguments?.getString("browseId") ?: return@composable
                ArtistScreen(
                    browseId = browseId,
                    onBack = { navController.popBackStack() },
                    onPlaySong = { song, list ->
                        requestNotificationPermissionIfNeeded()
                        playerViewModel.putar(song, list, QueueSource.LIST)
                    },
                    onPlayRadio = { song ->
                        requestNotificationPermissionIfNeeded()
                        playerViewModel.putar(song, listOf(song), QueueSource.RADIO)
                    },
                    onNavigateToAlbum = { albumBrowseId ->
                        navController.navigate(Screen.Album.createRoute(albumBrowseId))
                    },
                    onOpenAddToPlaylist = { songForOptions = it }
                )
            }

            composable(Screen.Album.route) { backStackEntry ->
                val browseId = backStackEntry.arguments?.getString("browseId") ?: return@composable
                AlbumScreen(
                    browseId = browseId,
                    onBack = { navController.popBackStack() },
                    onPlaySongs = { songs, index ->
                        if (songs.isNotEmpty()) {
                            requestNotificationPermissionIfNeeded()
                            val targetSong = songs[index.coerceIn(0, songs.lastIndex)]
                            playerViewModel.putar(targetSong, songs, QueueSource.LIST)
                        }
                    },
                    onNavigateToArtist = { artistBrowseId ->
                        navController.navigate(Screen.Artis.createRoute(artistBrowseId))
                    },
                    onOpenAddToPlaylist = { songForOptions = it }
                )
            }

            composable(Screen.DaftarPutar.route) { backStackEntry ->
                val idStr = backStackEntry.arguments?.getString("id")
                val playlistId = idStr?.toLongOrNull() ?: return@composable
                PlaylistScreen(
                    playlistId = playlistId,
                    playlistViewModel = playlistViewModel,
                    onBack = { navController.popBackStack() },
                    onPlaySong = { song, list ->
                        requestNotificationPermissionIfNeeded()
                        playerViewModel.putar(song, list, QueueSource.LIST)
                    },
                    onOpenAddToPlaylist = { songForOptions = it }
                )
            }
        }

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
                .padding(bottom = if (isMainTab) 84.dp else 12.dp)
        )

        if (isMainTab) {
            FloatingNavBar(
                currentRoute = currentRoute,
                onNavigateToTab = { screen ->
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
        }

        AnimatedVisibility(
            visible = isPlayerOpen && playerState.currentSong != null,
            enter = slideInVertically(animationSpec = spring(dampingRatio = Motion.Gentle.dampingRatio, stiffness = Motion.Gentle.stiffness), initialOffsetY = { it }) + fadeIn(animationSpec = spring(dampingRatio = Motion.Gentle.dampingRatio, stiffness = Motion.Gentle.stiffness)),
            exit = slideOutVertically(animationSpec = spring(dampingRatio = Motion.Gentle.dampingRatio, stiffness = Motion.Gentle.stiffness), targetOffsetY = { it }) + fadeOut(animationSpec = spring(dampingRatio = Motion.Gentle.dampingRatio, stiffness = Motion.Gentle.stiffness))
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
                    isFavorite = isFavorite,
                    queue = playerState.queue,
                    isRadioQueue = playerState.isRadioQueue,
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
                    onChangeRepeatMode = playerViewModel::gantiModeUlang,
                    onToggleFavorite = { playerViewModel.toggleFavorite(song) },
                    onJumpToQueue = playerViewModel::lompatKeAntrean,
                    onRemoveFromQueue = playerViewModel::hapusDariAntrean,
                    onClearQueue = playerViewModel::bersihkanAntrean,
                    onOpenAddToPlaylist = { songForOptions = song },
                    onNavigateToArtist = { browseId ->
                        isPlayerOpen = false
                        navController.navigate(Screen.Artis.createRoute(browseId))
                    },
                    onNavigateToAlbum = { browseId ->
                        isPlayerOpen = false
                        navController.navigate(Screen.Album.createRoute(browseId))
                    }
                )
            }
        }

        if (isSettingsOpen) {
            SettingsSheet(
                isAutoRadioEnabled = isAutoRadioEnabled,
                onAutoRadioChanged = playerViewModel::setAutoRadioEnabled,
                crossfadeSeconds = crossfadeSeconds,
                onCrossfadeChanged = playerViewModel::setCrossfadeSeconds,
                onDismiss = { isSettingsOpen = false }
            )
        }

        songForOptions?.let { targetSong ->
            SongOptionsSheet(
                song = targetSong,
                playerViewModel = playerViewModel,
                onDismissRequest = { songForOptions = null },
                onAddToPlaylistClick = { song ->
                    songForOptions = null
                    songForAddToPlaylist = song
                },
                onNavigateToArtist = { browseId ->
                    songForOptions = null
                    navController.navigate(Screen.Artis.createRoute(browseId))
                },
                onNavigateToAlbum = { browseId ->
                    songForOptions = null
                    navController.navigate(Screen.Album.createRoute(browseId))
                }
            )
        }

        songForAddToPlaylist?.let { targetSong ->
            AddToPlaylistSheet(
                song = targetSong,
                playlistViewModel = playlistViewModel,
                onDismissRequest = { songForAddToPlaylist = null }
            )
        }
    }
}