package com.example.still_wave_music_player.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.still_wave_music_player.data.model.Song
import com.example.still_wave_music_player.together.ListenTogetherManager
import com.example.still_wave_music_player.together.TogetherStatus
import com.example.still_wave_music_player.ui.components.AlbumArt
import com.example.still_wave_music_player.ui.theme.Accent
import com.example.still_wave_music_player.ui.theme.Motion
import com.example.still_wave_music_player.ui.theme.TextPrimary

/**
 * Komponen Mini Player kapsul melayang.
 *
 * Menggunakan FloatingGlass dengan tinggi 56dp, margin samping 16dp,
 * ditempatkan di atas FloatingNavBar dengan jarak 8dp.
 * Menampilkan sampul 40dp (sudut 12dp), judul dan artis satu baris elipsis,
 * tombol putar/jeda terpisah di kanan, serta garis progres tipis 2dp warna Accent.
 */
@Composable
fun MiniPlayer(
    song: Song?,
    isPlaying: Boolean,
    progress: Float,
    isBuffering: Boolean = false,
    onOpenPlayer: () -> Unit,
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = song != null,
        enter = slideInVertically(
            animationSpec = spring(
                dampingRatio = Motion.Standard.dampingRatio,
                stiffness = Motion.Standard.stiffness
            ),
            initialOffsetY = { it }
        ) + fadeIn(
            animationSpec = spring(
                dampingRatio = Motion.Standard.dampingRatio,
                stiffness = Motion.Standard.stiffness
            )
        ),
        exit = slideOutVertically(
            animationSpec = spring(
                dampingRatio = Motion.Standard.dampingRatio,
                stiffness = Motion.Standard.stiffness
            ),
            targetOffsetY = { it }
        ) + fadeOut(
            animationSpec = spring(
                dampingRatio = Motion.Standard.dampingRatio,
                stiffness = Motion.Standard.stiffness
            )
        ),
        modifier = modifier
    ) {
        if (song == null) return@AnimatedVisibility

        FloatingGlass(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            // Kontainer utama yang dapat diketuk untuk membuka layar pemutar penuh
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onOpenPlayer() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 8.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sampul lagu 40dp dengan sudut 12dp
                    AlbumArt(
                        song = song,
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        sizeDp = 40.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    val togetherState by ListenTogetherManager.uiState.collectAsState()
                    val isTogetherConnected = togetherState.status == TogetherStatus.Terhubung

                    // Judul dan Artis satu baris terpotong dengan elipsis
                    Text(
                        text = "${song.title} · ${song.artist}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (isTogetherConnected) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Accent)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Bareng",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Tombol Putar / Jeda bulat mandiri di kanan (tidak ikut membuka layar penuh)
                    MiniPlayPauseButton(
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        onClick = onTogglePlayPause
                    )
                }

                // Garis progres tipis (2dp, warna Accent) di tepi bawah kapsul
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                        .height(2.dp)
                        .background(Accent)
                )
            }
        }
    }
}

/**
 * Tombol putar / jeda bulat mandiri pada Mini Player dengan animasi spring scale.
 */
@Composable
private fun MiniPlayPauseButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.82f else 1f,
        animationSpec = spring(
            dampingRatio = Motion.Bouncy.dampingRatio,
            stiffness = Motion.Bouncy.stiffness
        ),
        label = "mini_play_scale"
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .scale(scale)
            .clip(CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        onClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = TextPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = if (isPlaying) AppIcons.Pause else AppIcons.Play,
                contentDescription = if (isPlaying) "Jeda musik" else "Putar musik",
                tint = TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
