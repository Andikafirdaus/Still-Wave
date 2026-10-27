package com.example.still_wave_music_player.ui.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.still_wave_music_player.data.model.Song
import com.example.still_wave_music_player.ui.theme.Accent
import com.example.still_wave_music_player.ui.theme.Surface
import com.example.still_wave_music_player.ui.theme.TextPrimary
import com.example.still_wave_music_player.ui.theme.TextSecondary
import com.example.still_wave_music_player.viewmodels.PlayerViewModel
import com.example.still_wave_music_player.viewmodels.QueueSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsSheet(
    song: Song,
    playerViewModel: PlayerViewModel,
    onDismissRequest: () -> Unit,
    onAddToPlaylistClick: (Song) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToAlbum: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val isFavorite by playerViewModel.isFavorite(song).collectAsState(initial = false)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Surface,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                AlbumArt(
                    song = song,
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    sizeDp = 56.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = song.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OptionRow(icon = AppIcons.SkipNext, title = "Putar berikutnya") {
                playerViewModel.putarBerikutnya(song)
                onDismissRequest()
            }
            OptionRow(icon = AppIcons.QueueMusic, title = "Tambah ke antrean") {
                playerViewModel.tambahKeAntrean(song)
                onDismissRequest()
            }
            OptionRow(icon = AppIcons.PlaylistAdd, title = "Tambah ke daftar putar") {
                onAddToPlaylistClick(song)
                onDismissRequest()
            }
            OptionRow(
                icon = if (isFavorite) AppIcons.FavoriteFilled else AppIcons.Favorite,
                title = if (isFavorite) "Hapus dari favorit" else "Favoritkan",
                tint = if (isFavorite) Accent else TextPrimary
            ) {
                playerViewModel.toggleFavorite(song)
                onDismissRequest()
            }

            if (!song.videoId.isNullOrBlank()) {
                OptionRow(icon = AppIcons.ArrowDown, title = "Unduh lagu") {
                    Toast.makeText(context, "Fitur unduh segera hadir!", Toast.LENGTH_SHORT).show()
                }
                OptionRow(icon = AppIcons.Explore, title = "Mulai Radio") {
                    playerViewModel.putar(song, listOf(song), QueueSource.SINGLE_ONLINE)
                    onDismissRequest()
                }
                if (!song.artistBrowseId.isNullOrBlank()) {
                    OptionRow(icon = AppIcons.Group, title = "Kunjungi Artis") {
                        onNavigateToArtist(song.artistBrowseId)
                        onDismissRequest()
                    }
                }
                if (!song.albumBrowseId.isNullOrBlank()) {
                    OptionRow(icon = AppIcons.Library, title = "Kunjungi Album") {
                        onNavigateToAlbum(song.albumBrowseId)
                        onDismissRequest()
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionRow(
    icon: ImageVector,
    title: String,
    tint: Color = TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            color = tint
        )
    }
}