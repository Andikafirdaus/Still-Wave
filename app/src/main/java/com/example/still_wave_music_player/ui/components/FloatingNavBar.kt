package com.example.still_wave_music_player.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.still_wave_music_player.ui.navigation.Screen
import com.example.still_wave_music_player.ui.theme.Motion
import com.example.still_wave_music_player.ui.theme.SurfaceHigh
import com.example.still_wave_music_player.ui.theme.TextPrimary
import com.example.still_wave_music_player.ui.theme.TextSecondary

/**
 * Bar navigasi bawah melayang bergaya "liquid glass".
 * Struktur: Kapsul (Beranda, Pustaka, Bersama) + Tombol Bulat (Cari).
 */
@Composable
fun FloatingNavBar(
    currentRoute: String,
    onNavigateToTab: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val capsuleTabs = listOf(
        Pair(Screen.Beranda, AppIcons.Home),
        Pair(Screen.Pustaka, AppIcons.Library),
        Pair(Screen.Together, AppIcons.Group) // Tab Baru
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Kapsul Navigasi Utama (Beranda, Pustaka, Bersama) ───────────────
        FloatingGlass(
            modifier = Modifier
                .weight(1f)
                .height(64.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                // Karena sekarang ada 3 tab, lebar dibagi 3
                val tabWidth = maxWidth / 3
                val pillHeight = 44.dp
                val pillWidth = (tabWidth - 12.dp).coerceAtLeast(44.dp)

                val selectedIndex = when (currentRoute) {
                    Screen.Beranda.route -> 0
                    Screen.Pustaka.route -> 1
                    Screen.Together.route -> 2
                    else -> -1
                }

                // Pil penanda aktif
                if (selectedIndex >= 0) {
                    val animatedIndex by animateFloatAsState(
                        targetValue = selectedIndex.toFloat(),
                        animationSpec = spring(
                            dampingRatio = Motion.Standard.dampingRatio,
                            stiffness = Motion.Standard.stiffness
                        ),
                        label = "pill_translation"
                    )
                    val pillOffsetX = (tabWidth * animatedIndex) + (tabWidth - pillWidth) / 2

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = pillOffsetX)
                            .size(width = pillWidth, height = pillHeight)
                            .background(SurfaceHigh, RoundedCornerShape(20.dp))
                    )
                }

                // Baris ikon
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    capsuleTabs.forEach { (screen, icon) ->
                        val isSelected = currentRoute == screen.route
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            BouncyTabButton(
                                icon = icon,
                                contentDescription = screen.title,
                                isSelected = isSelected,
                                onClick = { onNavigateToTab(screen) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // ── Tombol Bulat Terpisah untuk Tab Cari ──────────────────────────────
        val isSearchSelected = currentRoute == Screen.Cari.route

        FloatingGlass(
            modifier = Modifier.size(64.dp),
            shape = CircleShape
        ) {
            if (isSearchSelected) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .align(Alignment.Center)
                        .background(SurfaceHigh, CircleShape)
                )
            }

            BouncyTabButton(
                icon = AppIcons.Search,
                contentDescription = Screen.Cari.title,
                isSelected = isSearchSelected,
                onClick = { onNavigateToTab(Screen.Cari) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun BouncyTabButton(
    icon: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
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
        label = "tab_button_scale"
    )

    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .scale(scale)
            .semantics {
                role = Role.Tab
                selected = isSelected
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isSelected) TextPrimary else TextSecondary,
            modifier = Modifier.size(24.dp)
        )
    }
}