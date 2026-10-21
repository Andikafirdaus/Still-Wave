package com.example.still_wave_music_player.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec

/**
 * Kumpulan spring animation spec siap pakai untuk seluruh aplikasi Still Wave.
 * Menggunakan spring (fisika) bukan tween agar animasi terasa alami dan responsif.
 */
object Motion {

    /**
     * Spring LEMBUT – untuk transisi halaman, panel bawah yang terbuka, dan elemen besar.
     * Tidak memantul; berhenti mulus di posisi akhir.
     */
    val Gentle = SpringSpec<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,   // 1.0 – tidak memantul sama sekali
        stiffness    = Spring.StiffnessLow            // gerakan lebih lambat dan halus
    )

    /**
     * Spring STANDAR – untuk animasi kartu, ikon, dan komponen interaktif umum.
     * Sedikit memantul agar terasa hidup, tapi tidak berlebihan.
     */
    val Standard = SpringSpec<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,  // 0.5 – memantul sedang
        stiffness    = Spring.StiffnessMedium             // kecepatan seimbang
    )

    /**
     * Spring MEMANTUL (Bouncy) – untuk tombol yang ditekan, konfirmasi aksi,
     * dan elemen yang perlu umpan balik visual mencolok.
     * Memantul kuat untuk kesan responsif dan playful.
     */
    val Bouncy = SpringSpec<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,  // 0.3 – memantul cukup kuat
        stiffness    = Spring.StiffnessMediumLow      // cukup cepat tapi tidak kaku
    )

    /**
     * Spring CEPAT – untuk tooltip, snackbar, atau overlay kecil yang harus
     * muncul/hilang dengan responsif tanpa delay terasa.
     */
    val Snappy = SpringSpec<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness    = Spring.StiffnessHigh           // sangat cepat, langsung
    )
}
