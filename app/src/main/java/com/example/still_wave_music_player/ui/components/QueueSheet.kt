package com.example.still_wave_music_player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.still_wave_music_player.data.model.Song
import com.example.still_wave_music_player.utils.formatDurasi
import com.example.still_wave_music_player.ui.theme.Accent
import com.example.still_wave_music_player.ui.theme.Surface
import com.example.still_wave_music_player.ui.theme.SurfaceHigh
import com.example.still_wave_music_player.ui.theme.TextMuted
import com.example.still_wave_music_player.ui.theme.TextPrimary
import com.example.still_wave_music_player.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    currentSong: Song?,
    queue: List<Song>,
    isRadioQueue: Boolean,
    onJumpTo: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClearQueue: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentIndex = if (currentSong != null) {
        queue.indexOfFirst {
            (it.id == currentSong.id && it.id.isNotBlank()) ||
                (!it.videoId.isNullOrBlank() && it.videoId == currentSong.videoId)
        }.takeIf { it != -1 } ?: 0
    } else {
        0
    }

    // Lagu-lagu berikutnya setelah currentIndex
    val upcomingItems = if (currentIndex in queue.indices && currentIndex + 1 < queue.size) {
        queue.subList(currentIndex + 1, queue.size).mapIndexed { offset, song ->
            val actualIndex = currentIndex + 1 + offset
            actualIndex to song
        }
    } else {
        emptyList()
    }

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
            // Header: Judul "Antrean", badge "Radio", tombol "Bersihkan antrean"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Antrean",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    if (isRadioQueue) {
                        Box(
                            modifier = Modifier
                                .background(SurfaceHigh, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Radio",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Accent
                            )
                        }
                    }
                }

                TextButton(
                    onClick = onClearQueue,
                    enabled = upcomingItems.isNotEmpty()
                ) {
                    Text(
                        text = "Bersihkan antrean",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (upcomingItems.isNotEmpty()) Accent else TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bagian Sedang Diputar
            if (currentSong != null) {
                Text(
                    text = "Sedang diputar",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                QueueSongRow(
                    song = currentSong,
                    isCurrent = true,
                    onItemClick = {},
                    onRemove = null
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bagian Berikutnya
            Text(
                text = "Berikutnya (${upcomingItems.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            if (upcomingItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada lagu berikutnya dalam antrean",
                        fontSize = 14.sp,
                        color = TextMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(upcomingItems, key = { _, (actualIndex, song) ->
                        song.id.ifBlank { song.videoId ?: "$actualIndex" } + "_$actualIndex"
                    }) { _, (actualIndex, song) ->
                        QueueSongRow(
                            song = song,
                            isCurrent = false,
                            onItemClick = { onJumpTo(actualIndex) },
                            onRemove = { onRemove(actualIndex) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueSongRow(
    song: Song,
    isCurrent: Boolean,
    onItemClick: () -> Unit,
    onRemove: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isCurrent) SurfaceHigh else Surface)
            .clickable(onClick = onItemClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sampul
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceHigh),
            contentAlignment = Alignment.Center
        ) {
            val thumb = song.thumbnailUrl
            if (!thumb.isNullOrBlank()) {
                AsyncImage(
                    model = thumb,
                    contentDescription = song.title,
                    modifier = Modifier.size(44.dp),
                    contentScale = ContentScale.Crop
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
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = if (isCurrent) Accent else TextPrimary,
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
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        // Tombol Hapus per baris
        if (onRemove != null) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = AppIcons.Delete,
                    contentDescription = "Hapus dari antrean",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

