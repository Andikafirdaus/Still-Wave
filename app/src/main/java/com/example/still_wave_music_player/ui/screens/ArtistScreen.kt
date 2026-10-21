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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.still_wave_music_player.data.model.AlbumSearchResult
import com.example.still_wave_music_player.data.model.Song
import com.example.still_wave_music_player.ui.components.AppIcons
import com.example.still_wave_music_player.ui.components.ThumbnailArt
import com.example.still_wave_music_player.ui.theme.Accent
import com.example.still_wave_music_player.ui.theme.Background
import com.example.still_wave_music_player.ui.theme.SurfaceHigh
import com.example.still_wave_music_player.ui.theme.TextMuted
import com.example.still_wave_music_player.ui.theme.TextPrimary
import com.example.still_wave_music_player.ui.theme.TextSecondary
import com.example.still_wave_music_player.viewmodels.ArtistUiState
import com.example.still_wave_music_player.viewmodels.ArtistViewModel

/**
 * Layar rincian Artis:
 * - Header: foto bundar artis, nama artis, jumlah pendengar/pelanggan.
 * - Tombol "Radio artis".
 * - Bagian lagu populer (tampilkan 5 lagu dengan tombol ekspansi).
 * - Bagian Album dan Single (daftar gulir horizontal).
 */
@Composable
fun ArtistScreen(
    browseId: String,
    onBack: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayRadio: (Song) -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    artistViewModel: ArtistViewModel = viewModel()
) {
    val uiState by artistViewModel.uiState.collectAsStateWithLifecycle()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var showAllTopSongs by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(browseId) {
        artistViewModel.loadArtist(browseId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        when (val state = uiState) {
            is ArtistUiState.Memuat -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Accent,
                        strokeWidth = 3.dp
                    )
                }
            }

            is ArtistUiState.Galat -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.pesan,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            color = TextSecondary
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceHigh)
                            .clickable { artistViewModel.cobaLagi() }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Coba lagi",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Accent
                            )
                        )
                    }
                }
            }

            is ArtistUiState.Sukses -> {
                val artist = state.artist
                val songsToDisplay = if (showAllTopSongs) {
                    artist.topSongs
                } else {
                    artist.topSongs.take(5)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = 120.dp + bottomInset
                    )
                ) {
                    // Header Artis
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ThumbnailArt(
                                thumbnailUrl = artist.thumbnailUrl,
                                contentDescription = "Foto ${artist.name}",
                                modifier = Modifier.size(136.dp),
                                shape = CircleShape
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = artist.name,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                ),
                                textAlign = TextAlign.Center
                            )

                            if (!artist.listeners.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = artist.listeners,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 13.sp,
                                        color = TextMuted
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }

                            if (artist.topSongs.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Accent)
                                        .clickable {
                                            onPlayRadio(artist.topSongs.first())
                                        }
                                        .padding(horizontal = 24.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Radio artis",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    // Bagian Lagu Populer
                    if (artist.topSongs.isNotEmpty()) {
                        item {
                            Text(
                                text = "Lagu populer",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                ),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }

                        itemsIndexed(songsToDisplay, key = { index, song -> song.id + index }) { index, song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onPlaySong(song, artist.topSongs)
                                    }
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextMuted
                                    ),
                                    modifier = Modifier.width(28.dp)
                                )

                                ThumbnailArt(
                                    thumbnailUrl = song.thumbnailUrl,
                                    contentDescription = "Sampul ${song.title}",
                                    modifier = Modifier.size(46.dp),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (!song.album.isNullOrBlank()) {
                                        Text(
                                            text = song.album,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (song.durationMs > 0) {
                                    val sec = (song.durationMs / 1000) % 60
                                    val min = (song.durationMs / 1000) / 60
                                    Text(
                                        text = String.format("%d:%02d", min, sec),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        ),
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                }
                            }
                        }

                        if (artist.topSongs.size > 5) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showAllTopSongs = !showAllTopSongs }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (showAllTopSongs) "Tampilkan lebih sedikit" else "Tampilkan semua",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Accent
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Bagian Album
                    if (artist.albums.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Album",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                ),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }

                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(artist.albums, key = { it.browseId }) { album ->
                                    ArtistReleaseCard(
                                        release = album,
                                        onClick = { onNavigateToAlbum(album.browseId) }
                                    )
                                }
                            }
                        }
                    }

                    // Bagian Single & EP
                    if (artist.singles.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Single & EP",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                ),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }

                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(artist.singles, key = { it.browseId }) { single ->
                                    ArtistReleaseCard(
                                        release = single,
                                        onClick = { onNavigateToAlbum(single.browseId) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tombol Kembali melayang di kiri atas
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(top = 4.dp, start = 8.dp)
                .size(44.dp)
        ) {
            Icon(
                imageVector = AppIcons.ArrowBack,
                contentDescription = "Kembali",
                tint = TextPrimary
            )
        }
    }
}

/**
 * Kartu album atau single dalam baris horizontal.
 */
@Composable
private fun ArtistReleaseCard(
    release: AlbumSearchResult,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        ThumbnailArt(
            thumbnailUrl = release.thumbnailUrl,
            contentDescription = "Sampul ${release.title}",
            modifier = Modifier.size(130.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = release.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (!release.year.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = release.year,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = TextSecondary
                ),
                maxLines = 1
            )
        }
    }
}
