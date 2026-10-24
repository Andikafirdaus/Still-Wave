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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.still_wave_music_player.data.local.getSongKey
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
import com.example.still_wave_music_player.viewmodels.HomeViewModel
import com.example.still_wave_music_player.viewmodels.LibraryUiState

/**
 * Layar Beranda:
 * - Header Beranda (Judul + Pengaturan)
 * - "Baru diputar" (maksimal 10 lagu unik dari riwayat pemutaran Room)
 * - "Untuk kamu" (rekomendasi berbasis artis 30 hari terakhir dengan peluruhan nilai)
 * - "Suasana" (baris chip suasana: Sedih, Galau, Santai, Semangat, Fokus, Romantis, Pesta, Tidur)
 * - "Sedang populer" (hasil online lagu populer Indonesia)
 * - "Baru ditambahkan" & "Lagu di perangkat" (jika lagu lokal tersedia)
 */
@Composable
fun HomeScreen(
    libraryState: LibraryUiState,
    requiredPermission: String,
    onPermissionGranted: () -> Unit,
    recentlyPlayed: List<Song> = emptyList(),
    onPlaySong: (Song, List<Song>) -> Unit = { _, _ -> },
    onOpenAddToPlaylist: (Song) -> Unit = {},
    onSelectMood: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    homeViewModel: HomeViewModel = viewModel()
) {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // 10 lagu unik dari riwayat
    val uniqueRecentlyPlayed = remember(recentlyPlayed) {
        recentlyPlayed.distinctBy { it.getSongKey() }.take(10)
    }

    val forYouSongs by homeViewModel.forYouSongs.collectAsState()
    val trendingSongs by homeViewModel.trendingSongs.collectAsState()

    val moodList = listOf("Sedih", "Galau", "Santai", "Semangat", "Fokus", "Romantis", "Pesta", "Tidur")

    val localSongs = (libraryState as? LibraryUiState.Berisi)?.daftar ?: emptyList()
    val recentlyAdded = remember(localSongs) {
        localSongs.sortedByDescending { it.dateAdded }.take(8)
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
        // ── 1. Judul Utama & Tombol Pengaturan ───────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Beranda",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Settings,
                        contentDescription = "Pengaturan",
                        tint = TextSecondary
                    )
                }
            }
        }

        // ── 2. Bagian: Baru diputar (Maksimal 10 lagu unik) ───────────────────
        if (uniqueRecentlyPlayed.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Baru diputar",
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
                        items(uniqueRecentlyPlayed, key = { "recent_${it.getSongKey()}" }) { song ->
                            SongSquareCard(
                                song = song,
                                onClick = { onPlaySong(song, uniqueRecentlyPlayed) }
                            )
                        }
                    }
                }
            }
        }

        // ── 3. Bagian: Untuk kamu (Rekomendasi Cerdas) ────────────────────────
        if (forYouSongs.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Untuk kamu",
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
                        items(forYouSongs, key = { "foryou_${it.getSongKey()}" }) { song ->
                            SongSquareCard(
                                song = song,
                                onClick = { onPlaySong(song, emptyList()) }
                            )
                        }
                    }
                }
            }
        }

        // ── 4. Bagian: Suasana (Baris Chip) ──────────────────────────────────
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Suasana",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(moodList) { mood ->
                        MoodChipItem(
                            label = mood,
                            onClick = { onSelectMood(mood) }
                        )
                    }
                }
            }
        }

        // ── 5. Bagian: Sedang populer (Online Trending) ───────────────────────
        if (trendingSongs.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Sedang populer",
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
                        items(trendingSongs, key = { "trending_${it.getSongKey()}" }) { song ->
                            SongSquareCard(
                                song = song,
                                onClick = { onPlaySong(song, emptyList()) }
                            )
                        }
                    }
                }
            }
        }

        // ── 6. Bagian Lagu Lokal: Baru Ditambahkan ────────────────────────────
        if (recentlyAdded.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                        items(recentlyAdded, key = { "local_recent_${it.id}" }) { song ->
                            SongSquareCard(
                                song = song,
                                onClick = { onPlaySong(song, recentlyAdded) }
                            )
                        }
                    }
                }
            }
        }

        // ── 7. Bagian Lagu Lokal: Daftar Vertikal ────────────────────────────
        if (localSongs.isNotEmpty()) {
            item {
                Text(
                    text = "Lagu di perangkat",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            items(localSongs, key = { "all_local_${it.id}" }) { song ->
                TrackRowItem(
                    song = song,
                    onClick = { onPlaySong(song, localSongs) },
                    onMoreClick = { onOpenAddToPlaylist(song) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else if (libraryState !is LibraryUiState.Berisi) {
            // Tampilan info jika izin belum ada atau lagu lokal kosong
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
 * Kartu lagu persegi untuk baris horizontal (Baru diputar, Untuk kamu, Sedang populer).
 */
@Composable
private fun SongSquareCard(
    song: Song,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
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
 * Chip suasana dengan warna solid dan sudut melengkung.
 */
@Composable
private fun MoodChipItem(
    label: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .border(width = 1.dp, color = Border, shape = shape)
            .background(SurfaceHigh)
            .clickable { onClick() }
            .sizeIn(minHeight = 38.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        )
    }
}

/**
 * Baris item lagu untuk daftar vertikal dengan tombol MoreVert.
 */
@Composable
fun TrackRowItem(
    song: Song,
    onClick: () -> Unit = {},
    onMoreClick: (() -> Unit)? = null,
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
            onClick = { onMoreClick?.invoke() },
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
