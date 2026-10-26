package com.example.still_wave_music_player.ui.screens

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import coil.compose.AsyncImage
import com.example.still_wave_music_player.data.local.Playlist
import com.example.still_wave_music_player.data.model.Song
import com.example.still_wave_music_player.utils.formatDurasi
import com.example.still_wave_music_player.ui.components.AppIcons
import com.example.still_wave_music_player.ui.theme.Accent
import com.example.still_wave_music_player.ui.theme.Background
import com.example.still_wave_music_player.ui.theme.Border
import com.example.still_wave_music_player.ui.theme.Surface
import com.example.still_wave_music_player.ui.theme.SurfaceHigh
import com.example.still_wave_music_player.ui.theme.TextMuted
import com.example.still_wave_music_player.ui.theme.TextPrimary
import com.example.still_wave_music_player.ui.theme.TextSecondary
import com.example.still_wave_music_player.viewmodels.PlaylistViewModel
import java.util.Locale

@Composable
fun PlaylistScreen(
    playlistId: Long,
    playlistViewModel: PlaylistViewModel,
    onBack: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onOpenAddToPlaylist: (Song) -> Unit = {}
) {
    val context = LocalContext.current
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var playlist by remember { mutableStateOf<Playlist?>(null) }
    val rawSongs by playlistViewModel.getSongsForPlaylist(playlistId).collectAsState(initial = emptyList())

    // Filter lagu lokal yang filenya masih tersedia di perangkat
    val songs = remember(rawSongs, context) {
        filterAvailableSongs(context, rawSongs)
    }

    // Ambil data playlist
    LaunchedEffect(playlistId) {
        playlist = playlistViewModel.getPlaylistById(playlistId)
    }

    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Bar Atas: Tombol Kembali + Menu Titik Tiga ──────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = AppIcons.ArrowBack,
                        contentDescription = "Kembali",
                        tint = TextPrimary
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = AppIcons.MoreVert,
                            contentDescription = "Opsi daftar putar",
                            tint = TextSecondary
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ganti nama", color = TextPrimary) },
                            onClick = {
                                showMenu = false
                                newName = playlist?.name.orEmpty()
                                showRenameDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Hapus daftar putar", color = Accent) },
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }

            // ── Konten Daftar Lagu ──────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 120.dp + bottomInset
                )
            ) {
                // Header Rincian Daftar Putar
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    ) {
                        Text(
                            text = playlist?.name ?: "Daftar Putar",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${songs.size} lagu",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Tombol Putar & Acak
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Tombol Putar
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Accent)
                                    .clickable(enabled = songs.isNotEmpty()) {
                                        songs.firstOrNull()?.let { firstSong ->
                                            onPlaySong(firstSong, songs)
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
                                    .clickable(enabled = songs.isNotEmpty()) {
                                        val shuffled = songs.shuffled()
                                        shuffled.firstOrNull()?.let { firstSong ->
                                            onPlaySong(firstSong, shuffled)
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
                }

                // Daftar Lagu
                if (songs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Daftar putar ini masih kosong",
                                fontSize = 14.sp,
                                color = TextMuted
                            )
                        }
                    }
                } else {
                    itemsIndexed(songs, key = { index, song ->
                        (song.id.ifBlank { song.videoId ?: "$index" }) + "_$index"
                    }) { _, song ->
                        PlaylistSongRow(
                            song = song,
                            onClick = { onPlaySong(song, songs) },
                            onRemove = {
                                playlistViewModel.removeSongFromPlaylist(playlistId, song)
                            },
                            onOpenAddToPlaylist = { onOpenAddToPlaylist(song) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }

    // Dialog Ganti Nama
    if (showRenameDialog && playlist != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Ganti nama daftar putar", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Border
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = newName.trim()
                        if (trimmed.isNotEmpty()) {
                            playlist?.let {
                                playlistViewModel.renamePlaylist(it, trimmed)
                                playlist = it.copy(name = trimmed)
                            }
                            showRenameDialog = false
                        }
                    }
                ) {
                    Text("Simpan", color = Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            },
            containerColor = Surface
        )
    }

    // Dialog Konfirmasi Hapus
    if (showDeleteDialog && playlist != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus daftar putar?", color = TextPrimary) },
            text = {
                Text(
                    "Daftar putar \"${playlist?.name}\" akan dihapus secara permanen.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        playlist?.let { playlistViewModel.deletePlaylist(it) }
                        showDeleteDialog = false
                        onBack()
                    }
                ) {
                    Text("Hapus", color = Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            },
            containerColor = Surface
        )
    }
}

@Composable
private fun PlaylistSongRow(
    song: Song,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onOpenAddToPlaylist: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sampul
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceHigh),
            contentAlignment = Alignment.Center
        ) {
            val thumb = song.thumbnailUrl
            if (!thumb.isNullOrBlank()) {
                AsyncImage(
                    model = thumb,
                    contentDescription = song.title,
                    modifier = Modifier.size(46.dp)
                )
            } else {
                Text(
                    text = song.title.take(1).uppercase(Locale.ROOT),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Judul & Artis
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.artist,
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Durasi
        if (song.durationMs > 0L) {
            Text(
                text = formatDurasi(song.durationMs),
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Tombol Tambah ke playlist lain (MoreVert)
        IconButton(onClick = onOpenAddToPlaylist, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = AppIcons.PlaylistAdd,
                contentDescription = "Tambah ke daftar putar lain",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        // Tombol Hapus per lagu
        IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = AppIcons.Delete,
                contentDescription = "Hapus dari daftar putar",
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
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
