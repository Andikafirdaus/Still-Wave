package com.example.still_wave_music_player.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.still_wave_music_player.data.model.Song
import com.example.still_wave_music_player.ui.components.AppIcons
import com.example.still_wave_music_player.ui.components.LibraryStateView
import com.example.still_wave_music_player.ui.theme.Accent
import com.example.still_wave_music_player.ui.theme.Background
import com.example.still_wave_music_player.ui.theme.Border
import com.example.still_wave_music_player.ui.theme.Motion
import com.example.still_wave_music_player.ui.theme.SurfaceHigh
import com.example.still_wave_music_player.ui.theme.TextMuted
import com.example.still_wave_music_player.ui.theme.TextPrimary
import com.example.still_wave_music_player.ui.theme.TextSecondary
import com.example.still_wave_music_player.viewmodels.LibraryUiState
import com.example.still_wave_music_player.viewmodels.OnlineSearchUiState
import com.example.still_wave_music_player.viewmodels.OnlineSearchViewModel

/**
 * Layar Cari:
 * - Dua chip penyaring sumber: "Di HP" dan "Online" (solid, chip aktif warna aksen, animasi spring).
 * - "Di HP": mencari dari lagu lokal perangkat.
 * - "Online": mencari lagu dari YouTube Music dengan debounce 500 ms.
 */
@Composable
fun SearchScreen(
    libraryState: LibraryUiState,
    requiredPermission: String,
    onPermissionGranted: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit = { _, _ -> },
    onlineSearchViewModel: OnlineSearchViewModel = viewModel()
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableStateOf("Di HP") }

    val onlineState by onlineSearchViewModel.uiState.collectAsStateWithLifecycle()

    // Tutup keyboard saat daftar digulir
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            keyboardController?.hide()
        }
    }

    // Penyaringan lagu lokal
    val allLocalSongs = (libraryState as? LibraryUiState.Berisi)?.daftar ?: emptyList()
    val filteredLocalSongs = remember(searchQuery, allLocalSongs) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            allLocalSongs.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true) ||
                    (it.album?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = 164.dp + bottomInset
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Bagian Header: Judul, Input Cari, dan Chip Sumber ───────────────────
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Cari",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                SearchBarField(
                    query = searchQuery,
                    onQueryChange = { newQuery ->
                        searchQuery = newQuery
                        if (selectedTab == "Online") {
                            onlineSearchViewModel.onQueryChanged(newQuery)
                        }
                    },
                    onSearch = {
                        keyboardController?.hide()
                    }
                )

                // Chip pilihan sumber pencarian
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SearchSourceChip(
                        label = "Di HP",
                        isSelected = selectedTab == "Di HP",
                        onClick = {
                            selectedTab = "Di HP"
                        }
                    )

                    SearchSourceChip(
                        label = "Online",
                        isSelected = selectedTab == "Online",
                        onClick = {
                            selectedTab = "Online"
                            if (searchQuery.isNotBlank()) {
                                onlineSearchViewModel.onQueryChanged(searchQuery)
                            }
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // ── Bagian Hasil: Berdasarkan Tab yang Dipilih ─────────────────────────
        if (selectedTab == "Di HP") {
            when (libraryState) {
                is LibraryUiState.Berisi -> {
                    if (searchQuery.isBlank()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 96.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Belum ada pencarian",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 15.sp,
                                        color = TextMuted
                                    )
                                )
                            }
                        }
                    } else if (filteredLocalSongs.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 96.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tidak ada hasil pencarian",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 15.sp,
                                        color = TextMuted
                                    )
                                )
                            }
                        }
                    } else {
                        items(filteredLocalSongs, key = { it.id }) { song ->
                            TrackRowItem(
                                song = song,
                                onClick = { onPlaySong(song, filteredLocalSongs) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
                else -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LibraryStateView(
                                state = libraryState,
                                requiredPermission = requiredPermission,
                                onPermissionGranted = onPermissionGranted
                            )
                        }
                    }
                }
            }
        } else {
            // Mode "Online"
            when (val state = onlineState) {
                is OnlineSearchUiState.Kosong -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 96.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Ketik judul lagu atau nama artis",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 15.sp,
                                    color = TextMuted
                                )
                            )
                        }
                    }
                }
                is OnlineSearchUiState.Memuat -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 96.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = Accent,
                                strokeWidth = 2.5.dp
                            )
                        }
                    }
                }
                is OnlineSearchUiState.TidakAda -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 96.dp, start = 16.dp, end = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tidak ada hasil untuk \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 15.sp,
                                    color = TextMuted
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                is OnlineSearchUiState.Galat -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 80.dp, start = 16.dp, end = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = state.pesan,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 15.sp,
                                    color = TextSecondary
                                ),
                                textAlign = TextAlign.Center
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceHigh)
                                    .clickable { onlineSearchViewModel.cobaLagi() }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Coba lagi",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Accent
                                    )
                                )
                            }
                        }
                    }
                }
                is OnlineSearchUiState.Berisi -> {
                    items(state.daftar, key = { it.id }) { song ->
                        TrackRowItem(
                            song = song,
                            onClick = {
                                onPlaySong(song, state.daftar)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Chip penyaring sumber pencarian dengan transisi spring Motion.kt.
 */
@Composable
private fun SearchSourceChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val targetBgColor = if (isSelected) Accent else SurfaceHigh
    val targetTextColor = if (isSelected) TextPrimary else TextSecondary

    val bgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = spring(
            dampingRatio = Motion.Standard.dampingRatio,
            stiffness = Motion.Standard.stiffness
        ),
        label = "chip_bg_${label}"
    )

    val textColor by animateColorAsState(
        targetValue = targetTextColor,
        animationSpec = spring(
            dampingRatio = Motion.Standard.dampingRatio,
            stiffness = Motion.Standard.stiffness
        ),
        label = "chip_text_${label}"
    )

    Box(
        modifier = Modifier
            .clip(shape)
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = textColor
            )
        )
    }
}

/**
 * Input pencarian dengan latar SurfaceHigh solid, sudut membulat penuh (CircleShape),
 * dan garis tepi halus 1dp Border.
 */
@Composable
private fun SearchBarField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape)
            .background(SurfaceHigh)
            .border(width = 1.dp, color = Border, shape = CircleShape)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = AppIcons.Search,
                contentDescription = "Ikon cari",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Cari lagu, artis, atau album",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            color = TextMuted
                        )
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        color = TextPrimary
                    ),
                    cursorBrush = SolidColor(TextPrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
