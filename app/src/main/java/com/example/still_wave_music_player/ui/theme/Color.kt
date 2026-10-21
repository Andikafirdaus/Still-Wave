package com.example.still_wave_music_player.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Background & Surface ────────────────────────────────────────────────────
/** Latar utama aplikasi: gelap solid #0C0C0E */
val Background    = Color(0xFF0C0C0E)
/** Permukaan level 1: abu-abu sangat gelap solid #151518 */
val Surface       = Color(0xFF151518)
/** Permukaan level 2: abu-abu gelap solid #1E1E22 */
val SurfaceHigh   = Color(0xFF1E1E22)

// ─── Border ──────────────────────────────────────────────────────────────────
/** Garis tepi pembatas: putih 10% */
val Border        = Color(0x1AFFFFFF)

// ─── Teks ────────────────────────────────────────────────────────────────────
/** Teks utama: putih pudar #F5F5F7 untuk keterbacaan tinggi */
val TextPrimary   = Color(0xFFF5F5F7)
/** Teks sekunder: abu-abu netral #9A9AA3 untuk metadata dan subtitle */
val TextSecondary = Color(0xFF9A9AA3)
/** Teks muted: abu-abu redup #6B6B73 untuk petunjuk atau placeholder */
val TextMuted     = Color(0xFF6B6B73)

// ─── Aksen ───────────────────────────────────────────────────────────────────
/** Aksen tunggal: merah koral solid #FF4D5E */
val Accent        = Color(0xFFFF4D5E)

// ─── Kompatibilitas GlassSurface lama ─────────────────────────────────────────
// GlassSurface.kt tetap dipertahankan tanpa perubahan sesuai instruksi;
// variabel berikut disediakan agar file tersebut tetap terkompilasi.
val GlassBorder    = Border
val GlassHighlight = Color(0x14FFFFFF)