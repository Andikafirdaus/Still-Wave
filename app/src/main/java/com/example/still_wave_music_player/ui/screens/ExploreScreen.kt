package com.example.still_wave_music_player.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.still_wave_music_player.data.model.Song
import com.example.still_wave_music_player.data.remote.YouTubeMusicApi
import com.example.still_wave_music_player.ui.components.AppIcons
import com.example.still_wave_music_player.ui.theme.Background
import com.example.still_wave_music_player.ui.theme.Primary
import com.example.still_wave_music_player.ui.theme.TextPrimary
import com.example.still_wave_music_player.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Model data kartu suasana dengan warna solid.
 */
data class MoodTile(
    val title: String,
    val color: Color
)

/**
 * 8 Suasana standar Still Wave dengan warna solid kalem tanpa gradien.
 */
val MoodTiles = listOf(
    MoodTile("Sedih", Color(0xFF252D38)),
    MoodTile("Galau", Color(0xFF332633)),
    MoodTile("Santai", Color(0xFF1E3330)),
    MoodTile("Semangat", Color(0xFF2D3528)),
    MoodTile("Fokus", Color(0xFF26272C)),
    MoodTile("Romantis", Color(0xFF382F2A)),
    MoodTile("Pesta", Color(0xFF2E2B3D)),
    MoodTile("Tidur", Color(0xFF1F2937))
)

/**
 * Layar Jelajah:
 * - Menampilkan grid kartu suasana dengan warna solid.
 * - Saat tile suasana diketuk, melakukan pencarian online "lagu <suasana>" (filter Lagu).
 * - Menampilkan daftar hasil pencarian, tombol "Putar semua" (radio tersambung di akhir),
 *   dan baris lagu (memutar online dengan radio berlanjut).
 */
@Composable
fun ExploreScreen(
    initialMood: String? = null,
    onMoodConsumed: () -> Unit = {},
    onPlaySong: (Song, List<Song>) -> Unit = { _, _ -> },
    onPlayAll: (List<Song>) -> Unit = {},
    onOpenAddToPlaylist: (Song) -> Unit = {}
) {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scope = rememberCoroutineScope()

    var selectedMood by rememberSaveable { mutableStateOf<String?>(null) }
    var moodSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun fetchSongsForMood(mood: String) {
        isLoading = true
        errorMessage = null
        moodSongs = emptyList()
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                YouTubeMusicApi.cari("lagu $mood")
            }
            isLoading = false
            result.fold(
                onSuccess = { list ->
                    moodSongs = list
                    if (list.isEmpty()) {
                        errorMessage = "Tidak ada lagu ditemukan untuk suasana ini"
                    }
                },
                onFailure = { error ->
                    errorMessage = error.message ?: "Gagal memuat lagu suasana"
                }
            )
        }
    }

    LaunchedEffect(initialMood) {
        if (!initialMood.isNullOrBlank()) {
            selectedMood = initialMood
            onMoodConsumed()
            fetchSongsForMood(initialMood)
        }
    }

    val activeMood = selectedMood

    if (activeMood != null) {
        BackHandler {
            selectedMood = null
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 164.dp + bottomInset
            )
        ) {
            // Header Layar Suasana
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { selectedMood = null }
                    ) {
                        Icon(
                            imageVector = AppIcons.ArrowBack,
                            contentDescription = "Kembali ke Jelajah",
                            tint = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Suasana: $activeMood",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
            }

            // Status Memuat
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = Primary,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Mencari lagu $activeMood...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            // Status Galat
            if (!isLoading && errorMessage != null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorMessage ?: "Terjadi kesalahan",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { fetchSongsForMood(activeMood) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(
                                text = "Coba lagi",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Status Berhasil & Tombol Putar Semua
            if (!isLoading && errorMessage == null && moodSongs.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onPlayAll(moodSongs) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Play,
                                contentDescription = "Putar semua",
                                modifier = Modifier.size(18.dp),
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Putar semua",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Daftar Lagu
                items(moodSongs) { song ->
                    TrackRowItem(
                        song = song,
                        onClick = {
                            // Perilaku satu-lagu-online: memutar lagu dan memicu radio kelanjutan
                            onPlaySong(song, emptyList())
                        },
                        onMoreClick = {
                            onOpenAddToPlaylist(song)
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    } else {
        // Tampilan Grid Jelajah Utama
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 164.dp + bottomInset
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = "Jelajah",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            items(MoodTiles) { tile ->
                MoodCard(
                    tile = tile,
                    onClick = {
                        selectedMood = tile.title
                        fetchSongsForMood(tile.title)
                    }
                )
            }
        }
    }
}

/**
 * Kartu suasana dengan warna solid kalem dan sudut 20dp.
 */
@Composable
private fun MoodCard(
    tile: MoodTile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(108.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(tile.color)
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Text(
            text = tile.title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        )
    }
}
