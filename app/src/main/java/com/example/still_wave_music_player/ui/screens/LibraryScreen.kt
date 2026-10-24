package com.example.still_wave_music_player.ui.screens

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.still_wave_music_player.data.local.Playlist
import com.example.still_wave_music_player.data.model.Song
import com.example.still_wave_music_player.ui.components.AlbumArt
import com.example.still_wave_music_player.ui.components.AppIcons
import com.example.still_wave_music_player.ui.components.LibraryStateView
import com.example.still_wave_music_player.ui.theme.Accent
import com.example.still_wave_music_player.ui.theme.Background
import com.example.still_wave_music_player.ui.theme.Border
import com.example.still_wave_music_player.ui.theme.Surface
import com.example.still_wave_music_player.ui.theme.SurfaceHigh
import com.example.still_wave_music_player.ui.theme.TextMuted
import com.example.still_wave_music_player.ui.theme.TextPrimary
import com.example.still_wave_music_player.ui.theme.TextSecondary
import com.example.still_wave_music_player.viewmodels.LibraryUiState
import com.example.still_wave_music_player.viewmodels.PlayerViewModel
import com.example.still_wave_music_player.viewmodels.PlaylistViewModel

/**
 * Layar Pustaka:
 * - 4 tab yang dapat digulir mendatar: "Di HP | Favorit | Daftar putar | Riwayat"
 * - Tab "Di HP": menampilkan lagu lokal dari penyimpanan perangkat
 * - Tab "Favorit": daftar lagu favorit dengan tombol Putar dan Acak
 * - Tab "Daftar putar": daftar Room Playlist dengan aksi buat, ubah nama, hapus, dan klik membuka PlaylistScreen
 * - Tab "Riwayat": daftar riwayat pemutaran
 */
@Composable
fun LibraryScreen(
    libraryState: LibraryUiState,
    requiredPermission: String,
    onPermissionGranted: () -> Unit,
    playerViewModel: PlayerViewModel,
    playlistViewModel: PlaylistViewModel,
    onPlaySong: (Song, List<Song>) -> Unit = { _, _ -> },
    onNavigateToPlaylist: (Long) -> Unit = {},
    onOpenAddToPlaylist: (Song) -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val filterOptions = listOf("Di HP", "Favorit", "Daftar putar", "Riwayat")
    var selectedFilter by remember { mutableStateOf("Di HP") }

    // Ambil data Room via Flow
    val playlists by playlistViewModel.playlists.collectAsState(initial = emptyList())
    val rawFavorites by playerViewModel.getFavorites().collectAsState(initial = emptyList())
    val rawHistory by playerViewModel.historyRepository.getRecent(100).collectAsState(initial = emptyList())

    val favorites = remember(rawFavorites, context) { filterAvailableSongs(context, rawFavorites) }
    val history = remember(rawHistory, context) { filterAvailableSongs(context, rawHistory) }

    // Dialog state
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<Playlist?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Playlist?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        // ── Header: Judul + Pengaturan ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pustaka",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = AppIcons.Settings,
                    contentDescription = "Pengaturan",
                    tint = TextSecondary
                )
            }
        }

        // ── Chip Tab (4 Tab Gulir Mendatar) ─────────────────────────────────────
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            items(filterOptions) { filter ->
                val isSelected = filter == selectedFilter
                FilterChipItem(
                    label = filter,
                    isSelected = isSelected,
                    onClick = { selectedFilter = filter }
                )
            }
        }

        // ── Konten Tab ──────────────────────────────────────────────────────────
        when (selectedFilter) {
            "Di HP" -> LocalDeviceTab(
                libraryState = libraryState,
                requiredPermission = requiredPermission,
                onPermissionGranted = onPermissionGranted,
                onPlaySong = onPlaySong,
                onOpenAddToPlaylist = onOpenAddToPlaylist,
                bottomInset = bottomInset.value
            )

            "Favorit" -> FavoritesTab(
                songs = favorites,
                onPlaySong = onPlaySong,
                onRemoveFavorite = { playerViewModel.toggleFavorite(it) },
                onOpenAddToPlaylist = onOpenAddToPlaylist,
                bottomInset = bottomInset.value
            )

            "Daftar putar" -> PlaylistTab(
                playlists = playlists,
                playlistViewModel = playlistViewModel,
                onNavigateToPlaylist = onNavigateToPlaylist,
                onCreateNew = { showCreateDialog = true },
                onRename = { showRenameDialog = it },
                onDelete = { showDeleteDialog = it },
                bottomInset = bottomInset.value
            )

            "Riwayat" -> HistoryTab(
                songs = history,
                onPlaySong = onPlaySong,
                onOpenAddToPlaylist = onOpenAddToPlaylist,
                bottomInset = bottomInset.value
            )
        }
    }

    // ── Dialog Buat Daftar Putar ─────────────────────────────────────────────────
    if (showCreateDialog) {
        var nameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = Surface,
            title = {
                Text(
                    text = "Daftar putar baru",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )
            },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = {
                        Text("Nama daftar putar", color = TextSecondary)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Border,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = Accent
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            playlistViewModel.createPlaylist(nameInput)
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Buat", color = Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            }
        )
    }

    // ── Dialog Ubah Nama Daftar Putar ────────────────────────────────────────────
    showRenameDialog?.let { playlist ->
        var nameInput by remember { mutableStateOf(playlist.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            containerColor = Surface,
            title = {
                Text(
                    text = "Ubah nama",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )
            },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Border,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = Accent
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            playlistViewModel.renamePlaylist(playlist, nameInput)
                            showRenameDialog = null
                        }
                    }
                ) {
                    Text("Simpan", color = Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) {
                    Text("Batal", color = TextSecondary)
                }
            }
        )
    }

    // ── Dialog Konfirmasi Hapus Daftar Putar ─────────────────────────────────────
    showDeleteDialog?.let { playlist ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            containerColor = Surface,
            title = {
                Text(
                    text = "Hapus daftar putar?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )
            },
            text = {
                Text(
                    text = "\"${playlist.name}\" akan dihapus permanen.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        playlistViewModel.deletePlaylist(playlist)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Hapus", color = Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Batal", color = TextSecondary)
                }
            }
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Tab Di HP (Lagu Lokal)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun LocalDeviceTab(
    libraryState: LibraryUiState,
    requiredPermission: String,
    onPermissionGranted: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onOpenAddToPlaylist: (Song) -> Unit,
    bottomInset: Float
) {
    val context = LocalContext.current

    when (libraryState) {
        is LibraryUiState.Berisi -> {
            val allSongs = libraryState.daftar
            val availableSongs = remember(allSongs, context) {
                filterAvailableSongs(context, allSongs)
            }

            if (availableSongs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada lagu di penyimpanan perangkat",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            color = TextSecondary
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp,
                        bottom = (148 + bottomInset).dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(availableSongs, key = { it.id }) { song ->
                        TrackRowItem(
                            song = song,
                            onClick = { onPlaySong(song, availableSongs) },
                            onMoreClick = { onOpenAddToPlaylist(song) }
                        )
                    }
                }
            }
        }
        else -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                LibraryStateView(
                    state = libraryState,
                    requiredPermission = requiredPermission,
                    onPermissionGranted = onPermissionGranted
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Tab Favorit (dengan tombol Putar & Acak)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun FavoritesTab(
    songs: List<Song>,
    onPlaySong: (Song, List<Song>) -> Unit,
    onRemoveFavorite: (Song) -> Unit,
    onOpenAddToPlaylist: (Song) -> Unit,
    bottomInset: Float
) {
    if (songs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Belum ada favorit",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    color = TextSecondary
                )
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                bottom = (148 + bottomInset).dp
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Tombol Putar & Acak
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tombol Putar
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Accent)
                            .clickable {
                                songs.firstOrNull()?.let { first ->
                                    onPlaySong(first, songs)
                                }
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.Play,
                            contentDescription = "Putar",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Putar",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    // Tombol Acak
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceHigh)
                            .clickable {
                                val shuffled = songs.shuffled()
                                shuffled.firstOrNull()?.let { first ->
                                    onPlaySong(first, shuffled)
                                }
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.Shuffle,
                            contentDescription = "Acak",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Acak",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }

            items(songs, key = { it.id }) { song ->
                FavoriteSongRow(
                    song = song,
                    onClick = { onPlaySong(song, songs) },
                    onRemove = { onRemoveFavorite(song) },
                    onOpenAddToPlaylist = { onOpenAddToPlaylist(song) }
                )
            }
        }
    }
}

@Composable
private fun FavoriteSongRow(
    song: Song,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onOpenAddToPlaylist: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArt(
            song = song,
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(12.dp),
            sizeDp = 52.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = TextSecondary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onOpenAddToPlaylist, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = AppIcons.MoreVert,
                contentDescription = "Menu opsi lagu",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = AppIcons.FavoriteFilled,
                contentDescription = "Hapus dari favorit",
                tint = Accent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Tab Daftar Putar (Membuka PlaylistScreen saat diketuk)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlaylistTab(
    playlists: List<Playlist>,
    playlistViewModel: PlaylistViewModel,
    onNavigateToPlaylist: (Long) -> Unit,
    onCreateNew: () -> Unit,
    onRename: (Playlist) -> Unit,
    onDelete: (Playlist) -> Unit,
    bottomInset: Float
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            bottom = (148 + bottomInset).dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Tombol buat daftar putar baru
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceHigh)
                    .clickable { onCreateNew() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Add,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Buat daftar putar baru",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Accent
                    )
                )
            }
        }

        if (playlists.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada daftar putar",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            color = TextSecondary
                        )
                    )
                }
            }
        } else {
            items(playlists, key = { it.id }) { playlist ->
                PlaylistRowItem(
                    playlist = playlist,
                    playlistViewModel = playlistViewModel,
                    onClick = { onNavigateToPlaylist(playlist.id) },
                    onRename = { onRename(playlist) },
                    onDelete = { onDelete(playlist) }
                )
            }
        }
    }
}

@Composable
private fun PlaylistRowItem(
    playlist: Playlist,
    playlistViewModel: PlaylistViewModel,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val songs by playlistViewModel.getSongsForPlaylist(playlist.id)
        .collectAsState(initial = emptyList())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ikon daftar putar
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceHigh),
            contentAlignment = Alignment.Center
        ) {
            if (songs.isNotEmpty()) {
                AlbumArt(
                    song = songs.first(),
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    sizeDp = 52.dp
                )
            } else {
                Icon(
                    imageVector = AppIcons.Library,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${songs.size} lagu",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            )
        }

        // Tombol ubah nama
        IconButton(onClick = onRename, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = AppIcons.Edit,
                contentDescription = "Ubah nama ${playlist.name}",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        // Tombol hapus
        IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = AppIcons.Delete,
                contentDescription = "Hapus ${playlist.name}",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Tab Riwayat
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun HistoryTab(
    songs: List<Song>,
    onPlaySong: (Song, List<Song>) -> Unit,
    onOpenAddToPlaylist: (Song) -> Unit,
    bottomInset: Float
) {
    if (songs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Belum ada riwayat pemutaran",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    color = TextSecondary
                )
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                bottom = (148 + bottomInset).dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(songs, key = { it.id + it.hashCode() }) { song ->
                TrackRowItem(
                    song = song,
                    onClick = { onPlaySong(song, songs) },
                    onMoreClick = { onOpenAddToPlaylist(song) },
                    modifier = Modifier
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Komponen bersama
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val backgroundColor = if (isSelected) SurfaceHigh else Surface
    val textColor = if (isSelected) TextPrimary else TextSecondary
    val borderModifier = if (isSelected) {
        Modifier.border(width = 1.dp, color = Border, shape = shape)
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .clip(shape)
            .then(borderModifier)
            .background(backgroundColor)
            .clickable { onClick() }
            .sizeIn(minHeight = 40.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor
            )
        )
    }
}

/**
 * Memeriksa apakah file audio lokal masih dapat dibuka; jika tidak ada, dilewati dengan aman.
 */
private fun filterAvailableSongs(context: Context, songs: List<Song>): List<Song> {
    return songs.filter { song ->
        if (!song.videoId.isNullOrBlank()) return@filter true
        val uriStr = song.contentUri ?: return@filter false
        try {
            val uri = Uri.parse(uriStr)
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
