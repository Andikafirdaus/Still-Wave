package com.example.still_wave_music_player.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.still_wave_music_player.data.model.Song
import com.example.still_wave_music_player.ui.components.AlbumArt
import com.example.still_wave_music_player.ui.components.AppIcons
import com.example.still_wave_music_player.ui.components.LibraryStateView
import com.example.still_wave_music_player.ui.theme.Background
import com.example.still_wave_music_player.ui.theme.TextPrimary
import com.example.still_wave_music_player.ui.theme.TextSecondary
import com.example.still_wave_music_player.viewmodels.LibraryUiState

/**
 * Layar Beranda:
 * - Menampilkan status perpustakaan (PermissionPrompt / Memuat / Kosong) jika belum Berisi
 * - Saat Berisi:
 *   - Bagian "Baru ditambahkan": 8 lagu terbaru berdasarkan dateAdded
 *   - Bagian "Untukmu": seluruh daftar lagu dari penyimpanan
 */
@Composable
fun HomeScreen(
    libraryState: LibraryUiState,
    requiredPermission: String,
    onPermissionGranted: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit = { _, _ -> }
) {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    when (libraryState) {
        is LibraryUiState.Berisi -> {
            val allSongs = libraryState.daftar
            val recentlyAdded = remember(allSongs) {
                allSongs.sortedByDescending { it.dateAdded }.take(8)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background)
                    .statusBarsPadding(),
                contentPadding = PaddingValues(
                    top = 16.dp,
                    bottom = 164.dp + bottomInset
                ),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // ── Judul Utama ───────────────────────────────────────────────────────
                item {
                    Text(
                        text = "Beranda",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // ── Bagian: Baru ditambahkan ──────────────────────────────────────────
                if (recentlyAdded.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "Baru ditambahkan",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(recentlyAdded, key = { it.id }) { song ->
                                    RecentlyAddedCard(
                                        song = song,
                                        onClick = { onPlaySong(song, recentlyAdded) }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Bagian: Untukmu ───────────────────────────────────────────────────
                item {
                    Text(
                        text = "Untukmu",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                items(allSongs, key = { it.id }) { song ->
                    TrackRowItem(
                        song = song,
                        onClick = { onPlaySong(song, allSongs) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background)
                    .statusBarsPadding()
                    .padding(
                        top = 16.dp,
                        bottom = 164.dp + bottomInset
                    )
            ) {
                Text(
                    text = "Beranda",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

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
}

/**
 * Kartu persegi untuk bagian Baru ditambahkan.
 * Sampul warna solid dengan sudut 12dp, judul dan artis di bawahnya.
 */
@Composable
private fun RecentlyAddedCard(
    song: Song,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(136.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        AlbumArt(
            song = song,
            modifier = Modifier.size(136.dp),
            shape = RoundedCornerShape(12.dp),
            sizeDp = 136.dp
        )

        Spacer(modifier = Modifier.height(8.dp))

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

        Spacer(modifier = Modifier.height(2.dp))

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
}

/**
 * Baris item lagu untuk daftar vertikal.
 * Sampul kecil 52dp sudut 12dp, judul 15sp, artis 12sp, dan tombol titik tiga di kanan.
 */
@Composable
fun TrackRowItem(
    song: Song,
    onClick: () -> Unit = {},
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

        IconButton(
            onClick = { /* Menu aksi lagu */ },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = AppIcons.MoreVert,
                contentDescription = "Menu opsi untuk ${song.title}",
                tint = TextSecondary
            )
        }
    }
}
