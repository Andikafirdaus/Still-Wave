package com.example.still_wave_music_player.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.still_wave_music_player.data.local.Playlist
import com.example.still_wave_music_player.data.model.Song
import com.example.still_wave_music_player.ui.components.AlbumArt
import com.example.still_wave_music_player.ui.components.AppIcons
import com.example.still_wave_music_player.ui.theme.Accent
import com.example.still_wave_music_player.ui.theme.Background
import com.example.still_wave_music_player.ui.theme.Border
import com.example.still_wave_music_player.ui.theme.Surface
import com.example.still_wave_music_player.ui.theme.SurfaceHigh
import com.example.still_wave_music_player.ui.theme.TextPrimary
import com.example.still_wave_music_player.ui.theme.TextSecondary
import com.example.still_wave_music_player.viewmodels.PlayerViewModel
import com.example.still_wave_music_player.viewmodels.PlaylistViewModel

/**
 * Layar Pustaka:
 * - Chip tab: "Daftar putar" | "Favorit" | "Riwayat"
 * - Tab Daftar putar: daftar Room Playlist dengan tombol buat, ubah nama, hapus
 * - Tab Favorit: daftar FavoriteSong dari Room
 * - Tab Riwayat: daftar PlayHistory dari Room
 */
@Composable
fun LibraryScreen(
    playerViewModel: PlayerViewModel,
    playlistViewModel: PlaylistViewModel,
    onPlaySong: (Song, List<Song>) -> Unit = { _, _ -> },
    onOpenSettings: () -> Unit = {}
) {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val filterOptions = listOf("Daftar putar", "Favorit", "Riwayat")
    var selectedFilter by remember { mutableStateOf("Daftar putar") }

    // Ambil data dari Room via Flow
    val playlists by playlistViewModel.playlists.collectAsState(initial = emptyList())
    val favorites by playerViewModel.getFavorites().collectAsState(initial = emptyList())
    val history by playerViewModel.historyRepository.getRecent(100).collectAsState(initial = emptyList())

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

        // ── Chip Tab ────────────────────────────────────────────────────────────
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
            "Daftar putar" -> PlaylistTab(
                playlists = playlists,
                playerViewModel = playerViewModel,
                playlistViewModel = playlistViewModel,
                onPlaySong = onPlaySong,
                onCreateNew = { showCreateDialog = true },
                onRename = { showRenameDialog = it },
                onDelete = { showDeleteDialog = it },
                bottomInset = bottomInset.value
            )

            "Favorit" -> FavoritesTab(
                songs = favorites,
                onPlaySong = onPlaySong,
                onRemoveFavorite = { playerViewModel.toggleFavorite(it) },
                bottomInset = bottomInset.value
            )

            "Riwayat" -> HistoryTab(
                songs = history,
                onPlaySong = onPlaySong,
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
                    Text("Hapus", color = TextPrimary)
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
// Tab Daftar Putar
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlaylistTab(
    playlists: List<Playlist>,
    playerViewModel: PlayerViewModel,
    playlistViewModel: PlaylistViewModel,
    onPlaySong: (Song, List<Song>) -> Unit,
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
                    onPlaySong = onPlaySong,
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
    onPlaySong: (Song, List<Song>) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val songs by playlistViewModel.getSongsForPlaylist(playlist.id)
        .collectAsState(initial = emptyList())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                if (songs.isNotEmpty()) onPlaySong(songs.first(), songs)
            }
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
// Tab Favorit
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun FavoritesTab(
    songs: List<Song>,
    onPlaySong: (Song, List<Song>) -> Unit,
    onRemoveFavorite: (Song) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                TrackRowItemWithAction(
                    song = song,
                    onClick = { onPlaySong(song, songs) },
                    actionIcon = AppIcons.FavoriteFilled,
                    actionDescription = "Hapus dari favorit",
                    actionTint = Accent,
                    onAction = { onRemoveFavorite(song) }
                )
            }
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
                    modifier = Modifier
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Komponen bersama
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Baris item lagu dengan aksi kustom di sisi kanan (mis. hapus favorit).
 */
@Composable
private fun TrackRowItemWithAction(
    song: Song,
    onClick: () -> Unit,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
    actionDescription: String,
    actionTint: androidx.compose.ui.graphics.Color,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
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

        IconButton(onClick = onAction, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = actionIcon,
                contentDescription = actionDescription,
                tint = actionTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Chip penyaring dengan sudut 12dp dan warna solid.
 */
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
