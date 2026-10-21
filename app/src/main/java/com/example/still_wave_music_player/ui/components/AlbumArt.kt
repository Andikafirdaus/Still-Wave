package com.example.still_wave_music_player.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.still_wave_music_player.data.coverColor
import com.example.still_wave_music_player.data.model.Song
import com.example.still_wave_music_player.utils.AlbumArtLoader
import com.example.still_wave_music_player.utils.loadAlbumArt
import kotlin.math.roundToInt

/**
 * Komponen sampul album lagu.
 * Menggantikan kotak warna solid dengan gambar sampul asli jika ada,
 * dengan warna solid song.coverColor sebagai cadangan.
 *
 * Jika song.thumbnailUrl diawali dengan "http", gambar dimuat secara online menggunakan Coil
 * dengan transisi fade 180 ms dan fallback kotak warna solid song.coverColor.
 * Jika tidak, gambar dimuat secara lokal lewat AlbumArtLoader.
 */
@Composable
fun AlbumArt(
    song: Song,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    sizeDp: Dp = 52.dp,
    customSizePx: Int? = null
) {
    val density = LocalDensity.current
    val sizePx = remember(sizeDp, density, customSizePx) {
        if (customSizePx != null) {
            customSizePx
        } else {
            val rawPx = with(density) { sizeDp.toPx() }
            val rounded = (rawPx / 64f).roundToInt() * 64
            maxOf(rounded, 96)
        }
    }

    val context = LocalContext.current
    val isOnlineThumb = song.thumbnailUrl?.startsWith("http") == true

    Box(
        modifier = modifier
            .clip(shape)
            .background(song.coverColor),
        contentAlignment = Alignment.Center
    ) {
        if (isOnlineThumb) {
            var isLoaded by remember(song.thumbnailUrl) { mutableStateOf(false) }
            val alpha by animateFloatAsState(
                targetValue = if (isLoaded) 1f else 0f,
                animationSpec = tween(durationMillis = 180),
                label = "OnlineArtFade"
            )

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(song.thumbnailUrl)
                    .crossfade(180)
                    .build(),
                contentDescription = "Sampul ${song.title}",
                contentScale = ContentScale.Crop,
                onSuccess = { isLoaded = true },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .graphicsLayer { this.alpha = alpha }
            )
        } else {
            key(song.id, sizePx) {
                // Cek cache memori terlebih dahulu agar gambar yang sudah ada langsung tampil tanpa berkedip
                val cachedBitmap = remember(song.id, sizePx) {
                    AlbumArtLoader.getCached(song.id, sizePx)
                }

                val imageBitmap by produceState<ImageBitmap?>(
                    initialValue = cachedBitmap,
                    key1 = song.id,
                    key2 = sizePx
                ) {
                    if (value == null) {
                        value = loadAlbumArt(context, song, sizePx)
                    }
                }

                val currentBitmap = imageBitmap
                val alpha by animateFloatAsState(
                    targetValue = if (currentBitmap != null) 1f else 0f,
                    animationSpec = tween(durationMillis = 180),
                    label = "AlbumArtFade"
                )

                if (currentBitmap != null) {
                    Image(
                        bitmap = currentBitmap,
                        contentDescription = "Sampul ${song.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(shape)
                            .graphicsLayer { this.alpha = alpha }
                    )
                }
            }
        }
    }
}
