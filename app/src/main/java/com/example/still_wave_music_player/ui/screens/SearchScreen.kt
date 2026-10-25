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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.still_wave_music_player.data.model.AlbumSearchResult
import com.example.still_wave_music_player.data.model.ArtistSearchResult
import com.example.still_wave_music_player.data.model.SearchAllResult
import com.example.still_wave_music_player.data.model.Song
import com.example.still_wave_music_player.data.model.TopResult
import com.example.still_wave_music_player.data.model.TopResultType
import com.example.still_wave_music_player.ui.components.AppIcons
import com.example.still_wave_music_player.ui.components.LibraryStateView
import com.example.still_wave_music_player.ui.components.ThumbnailArt
import com.example.still_wave_music_player.ui.theme.Accent
import com.example.still_wave_music_player.ui.theme.Background
import com.example.still_wave_music_player.ui.theme.Border
import com.example.still_wave_music_player.ui.theme.Motion
import com.example.still_wave_music_player.ui.theme.SurfaceHigh
import com.example.still_wave_music_player.ui.theme.TextMuted
import com.example.still_wave_music_player.ui.theme.TextPrimary
import com.example.still_wave_music_player.ui.theme.TextSecondary
import com.example.still_wave_music_player.viewmodels.LibraryUiState
import com.example.still_wave_music_player.viewmodels.OnlineSearchCategory
import com.example.still_wave_music_player.viewmodels.OnlineSearchUiState
import com.example.still_wave_music_player.viewmodels.OnlineSearchViewModel

/**
 * Layar Cari:
 * - Dua chip penyaring sumber: "Di HP" dan "Online".
 * - Saat "Online" aktif: chip tab kategori "Lagu | Artis | Album".
 * - "Di HP": mencari dari lagu lokal perangkat.
 * - "Online": mencari lagu, artis, atau album dari YouTube Music.
 */
@Composable
fun SearchScreen(
    libraryState: LibraryUiState,
    requiredPermission: String,
    onPermissionGranted: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit = { _, _ -> },
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {},
    onOpenAddToPlaylist: (Song) -> Unit = {},
    onlineSearchViewModel: OnlineSearchViewModel = viewModel()
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableStateOf("Di HP") }

    val onlineState by onlineSearchViewModel.uiState.collectAsStateWithLifecycle()
    val selectedCategory by onlineSearchViewModel.selectedCategory.collectAsStateWithLifecycle()

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

                // Chip pilihan sumber pencarian: Di HP | Online
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

                // Jika mode Online, tampilkan chip kategori: Lagu | Artis | Album
                if (selectedTab == "Online") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OnlineSearchCategory.entries.forEach { cat ->
                            SearchCategoryChip(
                                label = cat.label,
                                isSelected = selectedCategory == cat,
                                onClick = {
                                    onlineSearchViewModel.setCategory(cat)
                                }
                            )
                        }
                    }
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
                                onMoreClick = { onOpenAddToPlaylist(song) },
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
                                text = "Ketik judul lagu, artis, atau album",
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
                is OnlineSearchUiState.BerisiSemua -> {
                    val hasil = state.hasil

                    // 1. Kartu Hasil Teratas
                    hasil.topResult?.let { top ->
                        item(key = "semua_top_result_${top.type}_${top.id}") {
                            TopResultCard(
                                topResult = top,
                                onPlaySong = onPlaySong,
                                onNavigateToArtist = onNavigateToArtist,
                                onNavigateToAlbum = onNavigateToAlbum,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // 2. Bagian Lagu (maksimal 4 baris + tombol "Lihat semua")
                    if (hasil.songs.isNotEmpty()) {
                        item(key = "semua_header_lagu") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Lagu",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "Lihat semua",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Accent
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            onlineSearchViewModel.setCategory(OnlineSearchCategory.LAGU)
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }

                        items(hasil.songs.take(4), key = { "semua_song_${it.id}" }) { song ->
                            TrackRowItem(
                                song = song,
                                onClick = { onPlaySong(song, emptyList()) },
                                onMoreClick = { onOpenAddToPlaylist(song) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    // 3. Bagian Artis (gulir horizontal)
                    if (hasil.artists.isNotEmpty()) {
                        item(key = "semua_header_artis") {
                            Text(
                                text = "Artis",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                ),
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
                            )
                        }

                        item(key = "semua_row_artis") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(hasil.artists, key = { "semua_art_${it.browseId}" }) { artist ->
                                    ArtistCircularItem(
                                        artist = artist,
                                        onClick = { onNavigateToArtist(artist.browseId) }
                                    )
                                }
                            }
                        }
                    }

                    // 4. Bagian Album (gulir horizontal)
                    if (hasil.albums.isNotEmpty()) {
                        item(key = "semua_header_album") {
                            Text(
                                text = "Album",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                ),
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
                            )
                        }

                        item(key = "semua_row_album") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(hasil.albums, key = { "semua_alb_${it.browseId}" }) { album ->
                                    AlbumSquareCard(
                                        album = album,
                                        onClick = { onNavigateToAlbum(album.browseId) }
                                    )
                                }
                            }
                        }
                    }

                    item(key = "semua_bottom_spacer") {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                is OnlineSearchUiState.Berisi -> {
                    items(state.daftar, key = { it.id }) { song ->
                        TrackRowItem(
                            song = song,
                            onClick = {
                                onPlaySong(song, state.daftar)
                            },
                            onMoreClick = { onOpenAddToPlaylist(song) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                is OnlineSearchUiState.BerisiArtis -> {
                    items(state.daftar, key = { it.browseId }) { artist ->
                        ArtistRowItem(
                            artist = artist,
                            onClick = { onNavigateToArtist(artist.browseId) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                is OnlineSearchUiState.BerisiAlbum -> {
                    items(state.daftar, key = { it.browseId }) { album ->
                        AlbumRowItem(
                            album = album,
                            onClick = { onNavigateToAlbum(album.browseId) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Baris hasil pencarian artis: foto bulat, nama, dan teks "Artis".
 */
@Composable
private fun ArtistRowItem(
    artist: ArtistSearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ThumbnailArt(
            thumbnailUrl = artist.thumbnailUrl,
            contentDescription = "Foto ${artist.name}",
            modifier = Modifier.size(52.dp),
            shape = CircleShape
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "Artis",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = TextSecondary
                ),
                maxLines = 1
            )
        }
    }
}

/**
 * Baris hasil pencarian album: sampul persegi, judul, artis dan tahun.
 */
@Composable
private fun AlbumRowItem(
    album: AlbumSearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ThumbnailArt(
            thumbnailUrl = album.thumbnailUrl,
            contentDescription = "Sampul ${album.title}",
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = album.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            val subtitle = if (!album.year.isNullOrBlank()) {
                "${album.artist} • ${album.year}"
            } else {
                album.artist
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = TextSecondary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
 * Chip kategori pencarian online (Lagu | Artis | Album).
 */
@Composable
private fun SearchCategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val targetBgColor = if (isSelected) Accent else SurfaceHigh
    val targetTextColor = if (isSelected) TextPrimary else TextSecondary

    val bgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = spring(
            dampingRatio = Motion.Standard.dampingRatio,
            stiffness = Motion.Standard.stiffness
        ),
        label = "cat_chip_bg_${label}"
    )

    val textColor by animateColorAsState(
        targetValue = targetTextColor,
        animationSpec = spring(
            dampingRatio = Motion.Standard.dampingRatio,
            stiffness = Motion.Standard.stiffness
        ),
        label = "cat_chip_text_${label}"
    )

    Box(
        modifier = Modifier
            .clip(shape)
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
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

/**
 * Kartu Hasil Teratas untuk tab "Semua".
 */
@Composable
private fun TopResultCard(
    topResult: TopResult,
    onPlaySong: (Song, List<Song>) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPlaySong by rememberUpdatedState(onPlaySong)
    val currentNavigateToArtist by rememberUpdatedState(onNavigateToArtist)
    val currentNavigateToAlbum by rememberUpdatedState(onNavigateToAlbum)
    val currentTopResult by rememberUpdatedState(topResult)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Hasil teratas",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceHigh)
                .border(width = 1.dp, color = Border, shape = RoundedCornerShape(16.dp))
                .clickable {
                    when (currentTopResult.type) {
                        TopResultType.LAGU -> {
                            currentTopResult.song?.let { currentPlaySong(it, emptyList()) }
                        }
                        TopResultType.ARTIS -> {
                            currentNavigateToArtist(currentTopResult.id)
                        }
                        TopResultType.ALBUM -> {
                            currentNavigateToAlbum(currentTopResult.id)
                        }
                    }
                }
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val isArtist = topResult.type == TopResultType.ARTIS
                ThumbnailArt(
                    thumbnailUrl = topResult.thumbnailUrl,
                    contentDescription = topResult.title,
                    modifier = Modifier.size(76.dp),
                    shape = if (isArtist) CircleShape else RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = topResult.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val subtitleText = topResult.subtitle ?: when (topResult.type) {
                        TopResultType.LAGU -> "Lagu"
                        TopResultType.ARTIS -> "Artis"
                        TopResultType.ALBUM -> "Album"
                    }

                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            color = TextSecondary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Item artis berfoto bulat dalam deretan horizontal tab "Semua".
 */
@Composable
private fun ArtistCircularItem(
    artist: ArtistSearchResult,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(88.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        ThumbnailArt(
            thumbnailUrl = artist.thumbnailUrl,
            contentDescription = artist.name,
            modifier = Modifier.size(72.dp),
            shape = CircleShape
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Artis",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = TextSecondary
            ),
            maxLines = 1
        )
    }
}

/**
 * Kartu album berformat sampul persegi dalam deretan horizontal tab "Semua".
 */
@Composable
private fun AlbumSquareCard(
    album: AlbumSearchResult,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        ThumbnailArt(
            thumbnailUrl = album.thumbnailUrl,
            contentDescription = album.title,
            modifier = Modifier.size(110.dp),
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        val sub = if (!album.year.isNullOrBlank()) "${album.artist} • ${album.year}" else album.artist
        Text(
            text = sub,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = TextSecondary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
