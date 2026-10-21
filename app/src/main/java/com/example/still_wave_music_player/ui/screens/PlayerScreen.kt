package com.example.still_wave_music_player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.still_wave_music_player.data.model.Song
import com.example.still_wave_music_player.ui.components.AlbumArt
import com.example.still_wave_music_player.ui.components.AppIcons
import com.example.still_wave_music_player.ui.components.FloatingGlass
import com.example.still_wave_music_player.ui.theme.Accent
import com.example.still_wave_music_player.ui.theme.Background
import com.example.still_wave_music_player.ui.theme.Border
import com.example.still_wave_music_player.ui.theme.Motion
import com.example.still_wave_music_player.ui.theme.TextPrimary
import com.example.still_wave_music_player.ui.theme.TextSecondary
import com.example.still_wave_music_player.viewmodels.RepeatMode
import java.util.Locale

/**
 * Layar pemutar penuh (PlayerScreen).
 *
 * Ditampilkan sebagai lapisan di atas AppRoot (bukan rute NavHost),
 * dengan latar solid Background, animasi spring cover saat putar/jeda,
 * bilah progres kustom tanpa Slider bawaan Material, dan kontrol lengkap.
 */
@Composable
fun PlayerScreen(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    isBuffering: Boolean = false,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onChangeRepeatMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Tombol Back sistem menutup pemutar lebih dulu
    BackHandler(enabled = true) {
        onClose()
    }

    // Animasi skala sampul: mengecil sedikit saat jeda dan membesar saat memutar (spring)
    val coverScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.90f,
        animationSpec = spring(
            dampingRatio = Motion.Standard.dampingRatio,
            stiffness = Motion.Standard.stiffness
        ),
        label = "cover_scale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                // Deteksi geser turun untuk menutup layar pemutar
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 24f) {
                        onClose()
                    }
                }
            }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── 1. Bar Atas: Panah Turun, Teks Status, Titik Tiga ─────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BouncyIconButton(
                icon = AppIcons.ArrowDown,
                contentDescription = "Tutup layar pemutar",
                tint = TextPrimary,
                onClick = onClose
            )

            Text(
                text = "Sedang diputar",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            )

            BouncyIconButton(
                icon = AppIcons.MoreVert,
                contentDescription = "Menu opsi lagu",
                tint = TextSecondary,
                onClick = { /* Menu aksi di tahap berikutnya */ }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── 2. Sampul Persegi Besar (Sudut 28dp, Animasi Spring, Sekitar 640px) ──
        AlbumArt(
            song = song,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .scale(coverScale),
            shape = RoundedCornerShape(28.dp),
            sizeDp = 320.dp,
            customSizePx = 640
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── 3. Judul Lagu dan Nama Artis ─────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    color = TextSecondary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── 4. Bilah Progres Kustom Tanpa Slider Bawaan Material ──────────────
        CustomProgressBar(
            progress = progress,
            songDurationMs = song.durationMs,
            onSeek = onSeek
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── 5. Baris Kontrol: Acak, Sebelumnya, Putar/Jeda, Berikutnya, Ulang ───
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Tombol Acak (Shuffle)
            BouncyIconButton(
                icon = AppIcons.Shuffle,
                contentDescription = if (isShuffle) "Mode acak aktif" else "Mode acak nonaktif",
                tint = if (isShuffle) Accent else TextSecondary,
                onClick = onToggleShuffle
            )

            // Tombol Sebelumnya (Skip Previous)
            BouncyIconButton(
                icon = AppIcons.SkipPrevious,
                contentDescription = "Lagu sebelumnya",
                tint = TextPrimary,
                onClick = onPrevious
            )

            // Tombol Putar / Jeda Besar 72dp (FloatingGlass)
            BigPlayPauseButton(
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                onClick = onTogglePlayPause
            )

            // Tombol Berikutnya (Skip Next)
            BouncyIconButton(
                icon = AppIcons.SkipNext,
                contentDescription = "Lagu berikutnya",
                tint = TextPrimary,
                onClick = onNext
            )

            // Tombol Ulang (Repeat)
            BouncyIconButton(
                icon = AppIcons.Repeat,
                contentDescription = when (repeatMode) {
                    RepeatMode.OFF -> "Ulang nonaktif"
                    RepeatMode.ALL -> "Ulang semua lagu"
                    RepeatMode.ONE -> "Ulang satu lagu"
                },
                tint = if (repeatMode != RepeatMode.OFF) Accent else TextSecondary,
                onClick = onChangeRepeatMode
            )
        }

        // ── 6. Ruang Kosong Bawah ─────────────────────────────────────────────
        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Bilah progres kustom (tanpa Slider bawaan Material):
 * - Jalur 4dp warna Border
 * - Bagian terisi warna TextPrimary
 * - Bulatan penanda 12dp yang membesar menjadi 16dp saat disentuh/digeser
 * - Teks waktu berjalan dan sisa waktu format m:ss di bawahnya
 */
@Composable
private fun CustomProgressBar(
    progress: Float,
    songDurationMs: Long,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val activeProgress = if (isDragging) dragProgress else progress.coerceIn(0f, 1f)

    val thumbSize by animateDpAsState(
        targetValue = if (isDragging) 16.dp else 12.dp,
        animationSpec = spring(
            dampingRatio = Motion.Bouncy.dampingRatio,
            stiffness = Motion.Bouncy.stiffness
        ),
        label = "thumb_size"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            isDragging = true
                            dragProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val released = tryAwaitRelease()
                            if (released) {
                                onSeek(dragProgress)
                            }
                            isDragging = false
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            onSeek(dragProgress)
                            isDragging = false
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            dragProgress = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val widthPx = constraints.maxWidth.toFloat()
            val density = LocalDensity.current
            val thumbSizePx = with(density) { thumbSize.toPx() }

            // Jalur dasar 4dp warna Border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Border)
            )

            // Bagian terisi 4dp warna TextPrimary
            Box(
                modifier = Modifier
                    .fillMaxWidth(activeProgress)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TextPrimary)
            )

            // Bulatan penanda 12dp / 16dp
            val thumbOffset = ((widthPx * activeProgress) - (thumbSizePx / 2f))
                .coerceIn(0f, (widthPx - thumbSizePx).coerceAtLeast(0f))

            Box(
                modifier = Modifier
                    .offset { IntOffset(thumbOffset.toInt(), 0) }
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(TextPrimary)
            )
        }

        // Teks waktu berjalan dan sisa waktu (format m:ss)
        val currentMs = (activeProgress * songDurationMs).toLong().coerceAtLeast(0L)
        val remainingMs = ((1f - activeProgress) * songDurationMs).toLong().coerceAtLeast(0L)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(currentMs),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            )

            Text(
                text = "-${formatDuration(remainingMs)}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            )
        }
    }
}

/**
 * Tombol putar / jeda bulat besar 72dp memakai FloatingGlass,
 * dengan animasi skala saat pergantian ikon dan efek pegas saat ditekan.
 */
@Composable
private fun BigPlayPauseButton(
    isPlaying: Boolean,
    isBuffering: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Motion.Bouncy.dampingRatio,
            stiffness = Motion.Bouncy.stiffness
        ),
        label = "big_play_scale"
    )

    FloatingGlass(
        modifier = modifier
            .size(72.dp)
            .scale(scale)
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
        shape = CircleShape
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = TextPrimary,
                    strokeWidth = 3.dp
                )
            } else {
                AnimatedContent(
                    targetState = isPlaying,
                    transitionSpec = {
                        (scaleIn(animationSpec = tween(150)) + fadeIn(animationSpec = tween(150)))
                            .togetherWith(scaleOut(animationSpec = tween(150)) + fadeOut(animationSpec = tween(150)))
                    },
                    label = "play_pause_transition"
                ) { playing ->
                    Icon(
                        imageVector = if (playing) AppIcons.Pause else AppIcons.Play,
                        contentDescription = if (playing) "Jeda pemutaran" else "Lanjutkan pemutaran",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

/**
 * Tombol ikon dengan animasi spring saat ditekan dan target sentuh minimal 48dp.
 */
@Composable
private fun BouncyIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.80f else 1f,
        animationSpec = spring(
            dampingRatio = Motion.Bouncy.dampingRatio,
            stiffness = Motion.Bouncy.stiffness
        ),
        label = "icon_btn_scale"
    )

    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .scale(scale)
            .semantics { role = Role.Button }
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
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Format durasi milidetik menjadi m:ss.
 */
private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}
