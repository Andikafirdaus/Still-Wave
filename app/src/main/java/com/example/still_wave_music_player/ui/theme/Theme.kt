package com.example.still_wave_music_player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Skema warna gelap satu-satunya untuk Still Wave.
 * Semua warna solid, tanpa gradien, sesuai arahan desain TAHAP 2.2a.
 */

// Pastikan Color_Error dideklarasikan sebelum dipakai
private val Color_Error = Color(0xFFCF6679)

private val StillWaveDarkColorScheme = darkColorScheme(
    primary              = Accent,
    onPrimary            = TextPrimary,
    primaryContainer     = SurfaceHigh,
    onPrimaryContainer   = TextPrimary,

    secondary            = Accent,
    onSecondary          = Background,
    secondaryContainer   = Surface,
    onSecondaryContainer = TextPrimary,

    tertiary             = SurfaceHigh,
    onTertiary           = TextPrimary,

    background           = Background,
    onBackground         = TextPrimary,

    surface              = Surface,
    onSurface            = TextPrimary,
    surfaceVariant       = SurfaceHigh,
    onSurfaceVariant     = TextSecondary,

    outline              = TextMuted,
    outlineVariant       = Border,

    error                = Color_Error,
    onError              = TextPrimary,
)

/**
 * Tema utama aplikasi Still Wave – selalu gelap, tanpa dynamic color.
 */
@Composable
fun StillwavemusicplayerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StillWaveDarkColorScheme,
        typography  = Typography,
        content     = content
    )
}