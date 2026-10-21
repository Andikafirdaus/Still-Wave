package com.example.still_wave_music_player.ui.components

import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.still_wave_music_player.ui.theme.GlassBorder
import com.example.still_wave_music_player.ui.theme.GlassHighlight
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur

/**
 * Permukaan kaca. Di Android 12+ memakai blur dari backdrop,
 * di bawahnya memakai isian semi-transparan tanpa blur.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    backdropState: Backdrop,
    shape: Shape = RectangleShape,
    blurRadiusDp: Dp = 20.dp,
    fillColor: Color = GlassHighlight,
    borderColor: Color = GlassBorder,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val supportsBlur = false

    if (supportsBlur) {
        val density = LocalDensity.current
        val blurRadiusPx = with(density) { blurRadiusDp.toPx() }

        Box(
            modifier = modifier
                .clip(shape)
                .drawBackdrop(
                    backdrop      = backdropState,
                    shape         = { shape },
                    effects       = {
                        blur(blurRadiusPx)
                    },
                    onDrawSurface = {
                        drawRect(fillColor)
                    }
                )
                .border(width = borderWidth, color = borderColor, shape = shape),
            content = content
        )
    } else {
        Box(
            modifier = modifier
                .clip(shape)
                .drawBehind { drawRect(fillColor) }
                .border(width = borderWidth, color = borderColor, shape = shape),
            content = content
        )
    }
}