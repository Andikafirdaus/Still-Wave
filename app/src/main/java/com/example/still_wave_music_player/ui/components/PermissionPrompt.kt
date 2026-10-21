package com.example.still_wave_music_player.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.still_wave_music_player.ui.theme.Accent
import com.example.still_wave_music_player.ui.theme.Motion
import com.example.still_wave_music_player.ui.theme.TextMuted
import com.example.still_wave_music_player.ui.theme.TextPrimary
import com.example.still_wave_music_player.ui.theme.TextSecondary
import com.example.still_wave_music_player.viewmodels.LibraryUiState

/**
 * Tampilan untuk meminta izin akses audio perangkat.
 * Menggunakan ActivityResultContracts.RequestPermission() untuk meluncurkan dialog izin.
 * Jika ditolak, teks berganti dan tombol berubah menjadi "Buka pengaturan aplikasi".
 */
@Composable
fun PermissionPrompt(
    requiredPermission: String,
    onPermissionGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isDenied = false
            onPermissionGranted()
        } else {
            isDenied = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isDenied) "Izin akses musik ditolak" else "Izinkan akses musik",
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isDenied) {
                "Aplikasi tidak dapat membaca lagu tanpa izin akses penyimpanan. Buka pengaturan aplikasi untuk memberikan izin."
            } else {
                "Still Wave membutuhkan izin membaca penyimpanan audio agar dapat memindai dan memutar koleksi musik di perangkatmu."
            },
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                color = TextSecondary
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isDenied) {
            SolidAccentButton(
                text = "Buka pengaturan aplikasi",
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                }
            )
        } else {
            SolidAccentButton(
                text = "Izinkan",
                onClick = {
                    permissionLauncher.launch(requiredPermission)
                }
            )
        }
    }
}

/**
 * Tampilan kosong ketika izin sudah ada tetapi tidak ditemukan file musik di perangkat.
 */
@Composable
fun EmptyLibraryPrompt(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Tidak ada lagu ditemukan",
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Salin file musik (.mp3, .m4a, dll.) ke penyimpanan perangkat untuk mulai mendengarkan.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                color = TextSecondary
            ),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Tampilan sederhana saat memuat lagu dari MediaStore.
 */
@Composable
fun LoadingPrompt(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Memuat lagu...",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                color = TextMuted
            ),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Komponen terpadu untuk menampilkan status non-Berisi (BelumIzin, Memuat, Kosong).
 */
@Composable
fun LibraryStateView(
    state: LibraryUiState,
    requiredPermission: String,
    onPermissionGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        is LibraryUiState.BelumIzin -> {
            PermissionPrompt(
                requiredPermission = requiredPermission,
                onPermissionGranted = onPermissionGranted,
                modifier = modifier
            )
        }
        is LibraryUiState.Memuat -> {
            LoadingPrompt(modifier = modifier)
        }
        is LibraryUiState.Kosong -> {
            EmptyLibraryPrompt(modifier = modifier)
        }
        is LibraryUiState.Berisi -> {
            // Ditangani secara spesifik oleh masing-masing layar
        }
    }
}

/**
 * Tombol solid beraksen Accent dengan efek pegas spring saat ditekan.
 */
@Composable
private fun SolidAccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Motion.Bouncy.dampingRatio,
            stiffness = Motion.Bouncy.stiffness
        ),
        label = "btn_accent_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(Accent)
            .sizeIn(minWidth = 140.dp, minHeight = 48.dp)
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
            }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        )
    }
}
