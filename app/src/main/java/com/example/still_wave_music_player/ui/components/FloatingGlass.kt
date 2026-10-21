package com.example.still_wave_music_player.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Permukaan "kaca" melayang tanpa blur dan tanpa library backdrop.
 *
 * CATATAN PENGGANTI SEMENTARA:
 * Komponen ini merupakan pengganti sementara sampai blur nyata (RenderEffect / Backdrop)
 * dapat digunakan tanpa risiko crash. Efek blur dari library backdrop sebelumnya
 * menyebabkan crash SIGSEGV pada RenderThread di perangkat target.
 *
 * Karakter visual liquid glass disimulasikan menggunakan:
 * 1. Isi semi-transparan (putih ~9% di atas latar gelap)
 * 2. Garis tepi 1dp putih ~12%
 * 3. Garis highlight tipis di tepi atas (putih ~20%)
 */
@Composable
fun FloatingGlass(
    shape: Shape = RoundedCornerShape(28.dp),
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val fillColor = Color.White.copy(alpha = 0.09f)
    val borderColor = Color.White.copy(alpha = 0.12f)
    val highlightColor = Color.White.copy(alpha = 0.20f)

    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                drawRect(color = fillColor)
            }
            .border(width = 1.dp, color = borderColor, shape = shape)
            .drawWithContent {
                drawContent()
                // Garis highlight tipis di tepi atas
                drawLine(
                    color = highlightColor,
                    start = Offset(x = 0f, y = 0.5f),
                    end = Offset(x = size.width, y = 0.5f),
                    strokeWidth = 1f
                )
            },
        content = content
    )
}
