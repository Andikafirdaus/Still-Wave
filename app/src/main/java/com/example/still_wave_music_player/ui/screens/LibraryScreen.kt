package com.example.still_wave_music_player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.still_wave_music_player.data.model.Song
import com.example.still_wave_music_player.ui.components.LibraryStateView
import com.example.still_wave_music_player.ui.theme.Background
import com.example.still_wave_music_player.ui.theme.Border
import com.example.still_wave_music_player.ui.theme.Surface
import com.example.still_wave_music_player.ui.theme.SurfaceHigh
import com.example.still_wave_music_player.ui.theme.TextPrimary
import com.example.still_wave_music_player.ui.theme.TextSecondary
import com.example.still_wave_music_player.viewmodels.LibraryUiState

/**
 * Layar Pustaka:
 * - Judul besar "Pustaka" (30sp bold)
 * - Jumlah lagu di bawah judul (misalnya "128 lagu")
 * - Baris filter chip: "Daftar putar", "Artis", "Album"
 * - Daftar lagu asli dari penyimpanan HP
 * - Menampilkan status perpustakaan (PermissionPrompt / Memuat / Kosong) jika belum Berisi
 */
@Composable
fun LibraryScreen(
    libraryState: LibraryUiState,
    requiredPermission: String,
    onPermissionGranted: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit = { _, _ -> }
) {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val filterOptions = listOf("Daftar putar", "Artis", "Album")
    var selectedFilter by remember { mutableStateOf("Daftar putar") }

    when (libraryState) {
        is LibraryUiState.Berisi -> {
            val songs = libraryState.daftar

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background)
                    .statusBarsPadding(),
                contentPadding = PaddingValues(
                    top = 16.dp,
                    bottom = 164.dp + bottomInset
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Judul Utama dan Jumlah Lagu
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Pustaka",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )

                        Text(
                            text = "${songs.size} lagu",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                color = TextSecondary
                            )
                        )
                    }
                }

                // Baris Chip Penyaring
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Daftar Baris Lagu Asli
                items(songs, key = { it.id }) { song ->
                    TrackRowItem(
                        song = song,
                        onClick = { onPlaySong(song, songs) },
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
                    text = "Pustaka",
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
