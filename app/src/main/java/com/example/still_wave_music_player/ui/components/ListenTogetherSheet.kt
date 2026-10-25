package com.example.still_wave_music_player.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.still_wave_music_player.together.ListenTogetherManager
import com.example.still_wave_music_player.together.TogetherPreferences
import com.example.still_wave_music_player.together.TogetherStatus
import com.example.still_wave_music_player.ui.theme.Accent
import com.example.still_wave_music_player.ui.theme.Border
import com.example.still_wave_music_player.ui.theme.Surface
import com.example.still_wave_music_player.ui.theme.SurfaceHigh
import com.example.still_wave_music_player.ui.theme.TextMuted
import com.example.still_wave_music_player.ui.theme.TextPrimary
import com.example.still_wave_music_player.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Bottom Sheet Listening Together:
 * - Dengar bareng berdua dengan Firebase Realtime Database lewat REST.
 * - Pengaturan server & nama panggilan dengan validasi.
 * - Buat ruang & Masuk dengan kode 6 karakter.
 * - Salin & Bagikan kode ruang saat terhubung, status pasangan online, dan keluar ruang.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListenTogetherSheet(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    val uiState by ListenTogetherManager.uiState.collectAsStateWithLifecycle()
    val prefs = remember(context) { TogetherPreferences(context) }

    var serverUrlInput by remember { mutableStateOf(prefs.serverUrl) }
    var nicknameInput by remember { mutableStateOf(prefs.nickname) }
    var joinCodeInput by remember { mutableStateOf("") }
    var settingsError by remember { mutableStateOf<String?>(null) }
    var settingsSuccess by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceHigh,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Border)
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Group,
                        contentDescription = "Ikon dengar bareng",
                        tint = Accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Dengar Bareng",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                if (uiState.status == TogetherStatus.Terhubung) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Accent)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Terhubung",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }

            // Pesan Galat Global
            if (uiState.pesanGalat != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF3B1E22))
                        .border(1.dp, Color(0xFF6B2B31), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = uiState.pesanGalat ?: "Terjadi kesalahan",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFFFB4AB),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // Catatan lagu lokal
            if (uiState.localOnlyNote != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface)
                        .border(1.dp, Border, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = uiState.localOnlyNote ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary
                        )
                    )
                }
            }

            when (uiState.status) {
                TogetherStatus.Menghubungkan -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Accent,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "Menghubungkan ke ruang...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary
                                )
                            )
                        }
                    }
                }

                TogetherStatus.Terhubung -> {
                    val code = uiState.kodeRuang ?: ""

                    // Tampilan Kode Ruang Besar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Surface)
                            .border(1.dp, Border, RoundedCornerShape(16.dp))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "KODE RUANG",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 2.sp
                            )
                        )

                        Text(
                            text = code,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                letterSpacing = 6.sp
                            )
                        )

                        // Status pasangan online
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isPartnerOnline) Color(0xFF4CAF50) else TextMuted)
                            )
                            val statusText = if (uiState.isPartnerOnline) {
                                val name = uiState.partnerName?.takeIf { it.isNotBlank() } ?: "Teman"
                                "$name terhubung"
                            } else {
                                "Menunggu pasangan"
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    color = if (uiState.isPartnerOnline) TextPrimary else TextSecondary,
                                    fontWeight = if (uiState.isPartnerOnline) FontWeight.SemiBold else FontWeight.Normal
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Tombol Salin dan Bagikan
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(code))
                                    Toast.makeText(context, "Kode ruang disalin", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SurfaceHigh,
                                    contentColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Salin",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Dengar bareng yuk! Kode ruang: $code"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Bagikan kode ruang"))
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Accent,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Bagikan",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Tombol Keluar dari Ruang
                    OutlinedButton(
                        onClick = {
                            ListenTogetherManager.leaveRoom(context)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFFB4AB)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = SolidColor(Color(0xFF6B2B31))
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Keluar dari ruang",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                TogetherStatus.TidakTerhubung -> {
                    val isServerConfigured = prefs.serverUrl.isNotBlank()

                    // Bagian Buat Ruang & Masuk Ruang
                    if (isServerConfigured) {
                        // Tombol Buat Ruang
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                ListenTogetherManager.createRoom(context)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Accent,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = "Buat ruang",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Pemisah "atau masuk dengan kode"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f).height(1.dp).background(Border))
                            Text(
                                text = "atau masuk dengan kode",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                            Box(modifier = Modifier.weight(1f).height(1.dp).background(Border))
                        }

                        // Kolom Kode Ruang & Tombol Masuk
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Surface)
                                    .border(1.dp, Border, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (joinCodeInput.isEmpty()) {
                                    Text(
                                        text = "6 KARAKTER",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextMuted,
                                            letterSpacing = 2.sp
                                        )
                                    )
                                }
                                BasicTextField(
                                    value = joinCodeInput,
                                    onValueChange = { newVal ->
                                        if (newVal.length <= 6) {
                                            joinCodeInput = newVal.uppercase(Locale.ROOT)
                                        }
                                    },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        letterSpacing = 3.sp
                                    ),
                                    cursorBrush = SolidColor(TextPrimary),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = {
                                        keyboardController?.hide()
                                        if (joinCodeInput.length == 6) {
                                            ListenTogetherManager.joinRoom(context, joinCodeInput)
                                        }
                                    }),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Button(
                                onClick = {
                                    keyboardController?.hide()
                                    ListenTogetherManager.joinRoom(context, joinCodeInput)
                                },
                                enabled = joinCodeInput.length == 6,
                                modifier = Modifier.height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Accent,
                                    contentColor = Color.White,
                                    disabledContainerColor = Surface,
                                    disabledContentColor = TextMuted
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Masuk",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        // Jika serverUrl kosong: nonaktifkan tombol dan tampilkan pesan
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Surface)
                                .border(1.dp, Border, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Isi alamat server dulu di bawah sebelum membuat atau masuk ruang.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // ── Bagian Pengaturan Server ──────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Surface)
                            .border(1.dp, Border, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Pengaturan server",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )

                        // Input Alamat Server
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Alamat server (Firebase Realtime Database)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceHigh)
                                    .border(1.dp, Border, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (serverUrlInput.isEmpty()) {
                                    Text(
                                        text = "https://proyek.firebasedatabase.app",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                    )
                                }
                                BasicTextField(
                                    value = serverUrlInput,
                                    onValueChange = {
                                        serverUrlInput = it
                                        settingsSuccess = false
                                        settingsError = null
                                    },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    ),
                                    cursorBrush = SolidColor(TextPrimary),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Input Nama Panggilan
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Nama panggilan",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceHigh)
                                    .border(1.dp, Border, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (nicknameInput.isEmpty()) {
                                    Text(
                                        text = "Aku",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                    )
                                }
                                BasicTextField(
                                    value = nicknameInput,
                                    onValueChange = {
                                        nicknameInput = it
                                        settingsSuccess = false
                                    },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    ),
                                    cursorBrush = SolidColor(TextPrimary),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        if (settingsError != null) {
                            Text(
                                text = settingsError ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFFFB4AB),
                                    fontSize = 12.sp
                                )
                            )
                        }

                        if (settingsSuccess) {
                            Text(
                                text = "Pengaturan berhasil disimpan",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF81C784),
                                    fontSize = 12.sp
                                )
                            )
                        }

                        Button(
                            onClick = {
                                val validation = TogetherPreferences.validateServerUrl(serverUrlInput)
                                if (validation.isSuccess) {
                                    val validUrl = validation.getOrThrow()
                                    prefs.serverUrl = validUrl
                                    prefs.nickname = nicknameInput.ifBlank { "Aku" }
                                    settingsError = null
                                    settingsSuccess = true
                                    keyboardController?.hide()
                                } else {
                                    settingsError = validation.exceptionOrNull()?.message ?: "Alamat server tidak valid"
                                    settingsSuccess = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceHigh,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Simpan pengaturan",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
