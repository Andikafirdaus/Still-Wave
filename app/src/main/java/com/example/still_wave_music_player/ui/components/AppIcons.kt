package com.example.still_wave_music_player.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Koleksi ImageVector kustom untuk seluruh ikon aplikasi Still Wave.
 * Dibuat secara mandiri tanpa membutuhkan dependensi material-icons tambahan.
 */
object AppIcons {

    /**
     * Ikon rumah untuk tab Beranda.
     */
    val Home: ImageVector by lazy {
        ImageVector.Builder(
            name = "Home",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(10f, 20f)
                verticalLineTo(14f)
                horizontalLineTo(14f)
                verticalLineTo(20f)
                horizontalLineTo(19f)
                verticalLineTo(12f)
                horizontalLineTo(22f)
                lineTo(12f, 3f)
                lineTo(2f, 12f)
                horizontalLineTo(5f)
                verticalLineTo(20f)
                close()
            }
        }.build()
    }

    /**
     * Ikon kompas untuk tab Jelajah.
     */
    val Explore: ImageVector by lazy {
        ImageVector.Builder(
            name = "Explore",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Lingkaran luar kompas
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
                close()
                moveTo(12f, 20f)
                curveTo(7.59f, 20f, 4f, 16.41f, 4f, 12f)
                curveTo(4f, 7.59f, 7.59f, 4f, 12f, 4f)
                curveTo(16.41f, 4f, 20f, 7.59f, 20f, 12f)
                curveTo(20f, 16.41f, 16.41f, 20f, 12f, 20f)
                close()
            }
            // Jarum kompas
            path(fill = SolidColor(Color.White)) {
                moveTo(6.5f, 17.5f)
                lineTo(10f, 10f)
                lineTo(17.5f, 6.5f)
                lineTo(14f, 14f)
                close()
                moveTo(12f, 10.5f)
                curveTo(11.17f, 10.5f, 10.5f, 11.17f, 10.5f, 12f)
                curveTo(10.5f, 12.83f, 11.17f, 13.5f, 12f, 13.5f)
                curveTo(12.83f, 13.5f, 13.5f, 12.83f, 13.5f, 12f)
                curveTo(13.5f, 11.17f, 12.83f, 10.5f, 12f, 10.5f)
                close()
            }
        }.build()
    }

    /**
     * Ikon pustaka / koleksi musik untuk tab Pustaka.
     */
    val Library: ImageVector by lazy {
        ImageVector.Builder(
            name = "Library",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Latar kartu bertumpuk di belakang
            path(fill = SolidColor(Color.White)) {
                moveTo(4f, 6f)
                lineTo(2f, 6f)
                verticalLineTo(20f)
                curveTo(2f, 21.1f, 2.9f, 22f, 4f, 22f)
                horizontalLineTo(18f)
                verticalLineTo(20f)
                horizontalLineTo(4f)
                verticalLineTo(6f)
                close()
            }
            // Kartu utama di depan
            path(fill = SolidColor(Color.White)) {
                moveTo(20f, 2f)
                horizontalLineTo(8f)
                curveTo(6.9f, 2f, 6f, 2.9f, 6f, 4f)
                verticalLineTo(16f)
                curveTo(6f, 17.1f, 6.9f, 18f, 8f, 18f)
                horizontalLineTo(20f)
                curveTo(21.1f, 18f, 22f, 17.1f, 22f, 16f)
                verticalLineTo(4f)
                curveTo(22f, 2.9f, 21.1f, 2f, 20f, 2f)
                close()
                moveTo(20f, 16f)
                horizontalLineTo(8f)
                verticalLineTo(4f)
                horizontalLineTo(20f)
                verticalLineTo(16f)
                close()
            }
            // Not musik di dalam kartu depan
            path(fill = SolidColor(Color.White)) {
                moveTo(13f, 5.5f)
                verticalLineTo(11f)
                curveTo(12.5f, 10.7f, 11.9f, 10.5f, 11.25f, 10.5f)
                curveTo(9.7f, 10.5f, 8.5f, 11.7f, 8.5f, 13.25f)
                curveTo(8.5f, 14.8f, 9.7f, 16f, 11.25f, 16f)
                curveTo(12.8f, 16f, 14f, 14.8f, 14f, 13.25f)
                verticalLineTo(7.5f)
                horizontalLineTo(17.5f)
                verticalLineTo(5.5f)
                horizontalLineTo(13f)
                close()
            }
        }.build()
    }

    /**
     * Ikon kaca pembesar untuk tab Cari dan kolom pencarian.
     */
    val Search: ImageVector by lazy {
        ImageVector.Builder(
            name = "Search",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(15.5f, 14f)
                horizontalLineTo(14.71f)
                lineTo(14.43f, 13.73f)
                curveTo(15.41f, 12.59f, 16f, 11.11f, 16f, 9.5f)
                curveTo(16f, 5.91f, 13.09f, 3f, 9.5f, 3f)
                curveTo(5.91f, 3f, 3f, 5.91f, 3f, 9.5f)
                curveTo(3f, 13.09f, 5.91f, 16f, 9.5f, 16f)
                curveTo(11.11f, 16f, 12.59f, 15.41f, 13.73f, 14.43f)
                lineTo(14f, 14.71f)
                verticalLineTo(15.5f)
                lineTo(19f, 20.49f)
                lineTo(20.49f, 19f)
                lineTo(15.5f, 14f)
                close()
                moveTo(9.5f, 14f)
                curveTo(7.01f, 14f, 5f, 11.99f, 5f, 9.5f)
                curveTo(5f, 7.01f, 7.01f, 5f, 9.5f, 5f)
                curveTo(11.99f, 5f, 14f, 7.01f, 14f, 9.5f)
                curveTo(14f, 11.99f, 11.99f, 14f, 9.5f, 14f)
                close()
            }
        }.build()
    }

    /**
     * Ikon tiga titik vertikal untuk opsi menu lagu.
     */
    val MoreVert: ImageVector by lazy {
        ImageVector.Builder(
            name = "MoreVert",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 8f)
                curveTo(13.1f, 8f, 14f, 7.1f, 14f, 6f)
                curveTo(14f, 4.9f, 13.1f, 4f, 12f, 4f)
                curveTo(10.9f, 4f, 10f, 4.9f, 10f, 6f)
                curveTo(10f, 7.1f, 10.9f, 8f, 12f, 8f)
                close()
                moveTo(12f, 10f)
                curveTo(10.9f, 10f, 10f, 10.9f, 10f, 12f)
                curveTo(10f, 13.1f, 10.9f, 14f, 12f, 14f)
                curveTo(13.1f, 14f, 14f, 13.1f, 14f, 12f)
                curveTo(14f, 10.9f, 13.1f, 10f, 12f, 10f)
                close()
                moveTo(12f, 16f)
                curveTo(10.9f, 16f, 10f, 16.9f, 10f, 18f)
                curveTo(10f, 19.1f, 10.9f, 20f, 12f, 20f)
                curveTo(13.1f, 20f, 14f, 19.1f, 14f, 18f)
                curveTo(14f, 16.9f, 13.1f, 16f, 12f, 16f)
                close()
            }
        }.build()
    }

    /**
     * Ikon segitiga putar (Play).
     */
    val Play: ImageVector by lazy {
        ImageVector.Builder(
            name = "Play",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(8f, 5f)
                verticalLineTo(19f)
                lineTo(19f, 12f)
                close()
            }
        }.build()
    }

    /**
     * Ikon dua garis vertikal jeda (Pause).
     */
    val Pause: ImageVector by lazy {
        ImageVector.Builder(
            name = "Pause",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(6f, 19f)
                horizontalLineTo(10f)
                verticalLineTo(5f)
                horizontalLineTo(6f)
                verticalLineTo(19f)
                close()
                moveTo(14f, 5f)
                verticalLineTo(19f)
                horizontalLineTo(18f)
                verticalLineTo(5f)
                horizontalLineTo(14f)
                close()
            }
        }.build()
    }

    /**
     * Ikon lewati ke lagu berikutnya (Skip Next).
     */
    val SkipNext: ImageVector by lazy {
        ImageVector.Builder(
            name = "SkipNext",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(6f, 18f)
                lineTo(14.5f, 12f)
                lineTo(6f, 6f)
                verticalLineTo(18f)
                close()
                moveTo(16f, 6f)
                verticalLineTo(18f)
                horizontalLineTo(18f)
                verticalLineTo(6f)
                horizontalLineTo(16f)
                close()
            }
        }.build()
    }

    /**
     * Ikon kembali ke lagu sebelumnya (Skip Previous).
     */
    val SkipPrevious: ImageVector by lazy {
        ImageVector.Builder(
            name = "SkipPrevious",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(6f, 6f)
                horizontalLineTo(8f)
                verticalLineTo(18f)
                horizontalLineTo(6f)
                close()
                moveTo(9.5f, 12f)
                lineTo(18f, 18f)
                verticalLineTo(6f)
                close()
            }
        }.build()
    }

    /**
     * Ikon acak urutan lagu (Shuffle).
     */
    val Shuffle: ImageVector by lazy {
        ImageVector.Builder(
            name = "Shuffle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(10.59f, 9.17f)
                lineTo(5.41f, 4f)
                lineTo(4f, 5.41f)
                lineTo(9.17f, 10.59f)
                lineTo(10.59f, 9.17f)
                close()
                moveTo(14.5f, 4f)
                lineTo(16.54f, 6.04f)
                lineTo(4f, 18.59f)
                lineTo(5.41f, 20f)
                lineTo(17.96f, 7.46f)
                lineTo(20f, 9.5f)
                verticalLineTo(4f)
                horizontalLineTo(14.5f)
                close()
                moveTo(14.83f, 13.41f)
                lineTo(13.42f, 14.82f)
                lineTo(16.55f, 17.95f)
                lineTo(14.5f, 20f)
                horizontalLineTo(20f)
                verticalLineTo(14.5f)
                lineTo(17.96f, 16.54f)
                lineTo(14.83f, 13.41f)
                close()
            }
        }.build()
    }

    /**
     * Ikon ulang lagu / daftar putar (Repeat).
     */
    val Repeat: ImageVector by lazy {
        ImageVector.Builder(
            name = "Repeat",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(7f, 7f)
                horizontalLineTo(17f)
                verticalLineTo(10f)
                lineTo(21f, 6f)
                lineTo(17f, 2f)
                verticalLineTo(5f)
                horizontalLineTo(5f)
                verticalLineTo(11f)
                horizontalLineTo(7f)
                verticalLineTo(7f)
                close()
                moveTo(17f, 17f)
                horizontalLineTo(7f)
                verticalLineTo(14f)
                lineTo(3f, 18f)
                lineTo(7f, 22f)
                verticalLineTo(19f)
                horizontalLineTo(19f)
                verticalLineTo(13f)
                horizontalLineTo(17f)
                verticalLineTo(17f)
                close()
            }
        }.build()
    }

    /**
     * Ikon panah turun untuk menutup layar pemutar.
     */
    val ArrowDown: ImageVector by lazy {
        ImageVector.Builder(
            name = "ArrowDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(7.41f, 8.59f)
                lineTo(12f, 13.17f)
                lineTo(16.59f, 8.59f)
                lineTo(18f, 10f)
                lineTo(12f, 16f)
                lineTo(6f, 10f)
                close()
            }
        }.build()
    }

    /**
     * Ikon tombol kembali ke halaman sebelumnya.
     */
    val ArrowBack: ImageVector by lazy {
        ImageVector.Builder(
            name = "ArrowBack",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(20f, 11f)
                horizontalLineTo(7.83f)
                lineTo(13.42f, 5.41f)
                lineTo(12f, 4f)
                lineTo(4f, 12f)
                lineTo(12f, 20f)
                lineTo(13.41f, 18.59f)
                lineTo(7.83f, 13f)
                horizontalLineTo(20f)
                verticalLineTo(11f)
                close()
            }
        }.build()
    }

    /**
     * Ikon lirik lagu.
     */
    val Lyrics: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lyrics",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(20f, 4f)
                horizontalLineTo(4f)
                curveTo(2.9f, 4f, 2f, 4.9f, 2f, 6f)
                verticalLineTo(18f)
                curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f)
                horizontalLineTo(20f)
                curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
                verticalLineTo(6f)
                curveTo(22f, 4.9f, 21.1f, 4f, 20f, 4f)
                close()
                moveTo(20f, 18f)
                horizontalLineTo(4f)
                verticalLineTo(6f)
                horizontalLineTo(20f)
                verticalLineTo(18f)
                close()
                moveTo(6f, 8f)
                horizontalLineTo(18f)
                verticalLineTo(10f)
                horizontalLineTo(6f)
                close()
                moveTo(6f, 11f)
                horizontalLineTo(15f)
                verticalLineTo(13f)
                horizontalLineTo(6f)
                close()
                moveTo(6f, 14f)
                horizontalLineTo(11f)
                verticalLineTo(16f)
                horizontalLineTo(6f)
                close()
            }
        }.build()
    }

    /**
     * Ikon roda gigi untuk Pengaturan.
     */
    val Settings: ImageVector by lazy {
        ImageVector.Builder(
            name = "Settings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(19.14f, 12.94f)
                curveTo(19.18f, 12.63f, 19.21f, 12.32f, 19.21f, 12f)
                curveTo(19.21f, 11.68f, 19.18f, 11.37f, 19.14f, 11.06f)
                lineTo(21.41f, 9.29f)
                curveTo(21.62f, 9.13f, 21.67f, 8.84f, 21.54f, 8.61f)
                lineTo(19.39f, 4.89f)
                curveTo(19.26f, 4.66f, 18.98f, 4.57f, 18.73f, 4.67f)
                lineTo(16.06f, 5.75f)
                curveTo(15.5f, 5.32f, 14.9f, 4.97f, 14.24f, 4.69f)
                lineTo(13.84f, 1.85f)
                curveTo(13.8f, 1.6f, 13.58f, 1.42f, 13.33f, 1.42f)
                horizontalLineTo(9.07f)
                curveTo(8.82f, 1.42f, 8.6f, 1.6f, 8.56f, 1.85f)
                lineTo(8.16f, 4.69f)
                curveTo(7.5f, 4.97f, 6.9f, 5.32f, 6.34f, 5.75f)
                lineTo(3.67f, 4.67f)
                curveTo(3.42f, 4.57f, 3.14f, 4.66f, 3.01f, 4.89f)
                lineTo(0.86f, 8.61f)
                curveTo(0.73f, 8.84f, 0.78f, 9.13f, 0.99f, 9.29f)
                lineTo(3.26f, 11.06f)
                curveTo(3.22f, 11.37f, 3.19f, 11.68f, 3.19f, 12f)
                curveTo(3.19f, 12.32f, 3.22f, 12.63f, 3.26f, 12.94f)
                lineTo(0.99f, 14.71f)
                curveTo(0.78f, 14.87f, 0.73f, 15.16f, 0.86f, 15.39f)
                lineTo(3.01f, 19.11f)
                curveTo(3.14f, 19.34f, 3.42f, 19.43f, 3.67f, 19.33f)
                lineTo(6.34f, 18.25f)
                curveTo(6.9f, 18.68f, 7.5f, 19.03f, 8.16f, 19.31f)
                lineTo(8.56f, 22.15f)
                curveTo(8.6f, 22.4f, 8.82f, 22.58f, 9.07f, 22.58f)
                horizontalLineTo(13.33f)
                curveTo(13.58f, 22.58f, 13.8f, 22.4f, 13.84f, 22.15f)
                lineTo(14.24f, 19.31f)
                curveTo(14.9f, 19.03f, 15.5f, 18.68f, 16.06f, 18.25f)
                lineTo(18.73f, 19.33f)
                curveTo(18.98f, 19.43f, 19.26f, 19.34f, 19.39f, 19.11f)
                lineTo(21.54f, 15.39f)
                curveTo(21.67f, 15.16f, 21.62f, 14.87f, 21.41f, 14.71f)
                lineTo(19.14f, 12.94f)
                close()
                moveTo(11.2f, 15.5f)
                curveTo(9.27f, 15.5f, 7.7f, 13.93f, 7.7f, 12f)
                curveTo(7.7f, 10.07f, 9.27f, 8.5f, 11.2f, 8.5f)
                curveTo(13.13f, 8.5f, 14.7f, 10.07f, 14.7f, 12f)
                curveTo(14.7f, 13.93f, 13.13f, 15.5f, 11.2f, 15.5f)
                close()
            }
        }.build()
    }

    /**
     * Ikon tambah ke daftar putar (PlaylistAdd).
     */
    val PlaylistAdd: ImageVector by lazy {
        ImageVector.Builder(
            name = "PlaylistAdd",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(14f, 10f)
                horizontalLineTo(2f)
                verticalLineTo(12f)
                horizontalLineTo(14f)
                verticalLineTo(10f)
                close()
                moveTo(14f, 6f)
                horizontalLineTo(2f)
                verticalLineTo(8f)
                horizontalLineTo(14f)
                verticalLineTo(6f)
                close()
                moveTo(18f, 14f)
                verticalLineTo(10f)
                horizontalLineTo(16f)
                verticalLineTo(14f)
                horizontalLineTo(12f)
                verticalLineTo(16f)
                horizontalLineTo(16f)
                verticalLineTo(20f)
                horizontalLineTo(18f)
                verticalLineTo(16f)
                horizontalLineTo(22f)
                verticalLineTo(14f)
                horizontalLineTo(18f)
                close()
                moveTo(2f, 16f)
                horizontalLineTo(10f)
                verticalLineTo(14f)
                horizontalLineTo(2f)
                verticalLineTo(16f)
                close()
            }
        }.build()
    }

    /**
     * Ikon hati (outline) untuk favorit nonaktif.
     */
    val Favorite: ImageVector by lazy {
        ImageVector.Builder(
            name = "Favorite",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(16.5f, 3f)
                curveTo(14.76f, 3f, 13.09f, 3.81f, 12f, 5.09f)
                curveTo(10.91f, 3.81f, 9.24f, 3f, 7.5f, 3f)
                curveTo(4.42f, 3f, 2f, 5.42f, 2f, 8.5f)
                curveTo(2f, 12.28f, 5.4f, 15.36f, 10.55f, 20.04f)
                lineTo(12f, 21.35f)
                lineTo(13.45f, 20.03f)
                curveTo(18.6f, 15.36f, 22f, 12.28f, 22f, 8.5f)
                curveTo(22f, 5.42f, 19.58f, 3f, 16.5f, 3f)
                close()
                moveTo(12.1f, 18.55f)
                lineTo(12f, 18.65f)
                lineTo(11.9f, 18.55f)
                curveTo(7.14f, 14.24f, 4f, 11.39f, 4f, 8.5f)
                curveTo(4f, 6.5f, 5.5f, 5f, 7.5f, 5f)
                curveTo(9.04f, 5f, 10.54f, 5.99f, 11.07f, 7.36f)
                horizontalLineTo(12.94f)
                curveTo(13.46f, 5.99f, 14.96f, 5f, 16.5f, 5f)
                curveTo(18.5f, 5f, 20f, 6.5f, 20f, 8.5f)
                curveTo(20f, 11.39f, 16.86f, 14.24f, 12.1f, 18.55f)
                close()
            }
        }.build()
    }

    /**
     * Ikon hati (padat) untuk favorit aktif.
     */
    val FavoriteFilled: ImageVector by lazy {
        ImageVector.Builder(
            name = "FavoriteFilled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 21.35f)
                lineTo(10.55f, 20.03f)
                curveTo(5.4f, 15.36f, 2f, 12.28f, 2f, 8.5f)
                curveTo(2f, 5.42f, 4.42f, 3f, 7.5f, 3f)
                curveTo(9.24f, 3f, 10.91f, 3.81f, 12f, 5.09f)
                curveTo(13.09f, 3.81f, 14.76f, 3f, 16.5f, 3f)
                curveTo(19.58f, 3f, 22f, 5.42f, 22f, 8.5f)
                curveTo(22f, 12.28f, 18.6f, 15.36f, 13.45f, 20.03f)
                lineTo(12f, 21.35f)
                close()
            }
        }.build()
    }

    /**
     * Ikon jam putar-balik untuk riwayat pemutaran.
     */
    val History: ImageVector by lazy {
        ImageVector.Builder(
            name = "History",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(13f, 3f)
                curveTo(8.03f, 3f, 4f, 7.03f, 4f, 12f)
                horizontalLineTo(1f)
                lineTo(4.89f, 15.89f)
                lineTo(4.96f, 16.03f)
                lineTo(9f, 12f)
                horizontalLineTo(6f)
                curveTo(6f, 8.13f, 9.13f, 5f, 13f, 5f)
                curveTo(16.87f, 5f, 20f, 8.13f, 20f, 12f)
                curveTo(20f, 15.87f, 16.87f, 19f, 13f, 19f)
                curveTo(11.07f, 19f, 9.32f, 18.21f, 8.06f, 16.94f)
                lineTo(6.64f, 18.36f)
                curveTo(8.27f, 19.99f, 10.51f, 21f, 13f, 21f)
                curveTo(17.97f, 21f, 22f, 16.97f, 22f, 12f)
                curveTo(22f, 7.03f, 17.97f, 3f, 13f, 3f)
                close()
                moveTo(12f, 8f)
                verticalLineTo(13f)
                lineTo(16.25f, 15.52f)
                lineTo(17f, 14.33f)
                lineTo(13.5f, 12.25f)
                verticalLineTo(8f)
                horizontalLineTo(12f)
                close()
            }
        }.build()
    }

    /**
     * Ikon tambah (+) untuk membuat daftar putar baru.
     */
    val Add: ImageVector by lazy {
        ImageVector.Builder(
            name = "Add",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(19f, 13f)
                horizontalLineTo(13f)
                verticalLineTo(19f)
                horizontalLineTo(11f)
                verticalLineTo(13f)
                horizontalLineTo(5f)
                verticalLineTo(11f)
                horizontalLineTo(11f)
                verticalLineTo(5f)
                horizontalLineTo(13f)
                verticalLineTo(11f)
                horizontalLineTo(19f)
                verticalLineTo(13f)
                close()
            }
        }.build()
    }

    /**
     * Ikon delete / hapus (tempat sampah).
     */
    val Delete: ImageVector by lazy {
        ImageVector.Builder(
            name = "Delete",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(6f, 19f)
                curveTo(6f, 20.1f, 6.9f, 21f, 8f, 21f)
                horizontalLineTo(16f)
                curveTo(17.1f, 21f, 18f, 20.1f, 18f, 19f)
                verticalLineTo(7f)
                horizontalLineTo(6f)
                verticalLineTo(19f)
                close()
                moveTo(8f, 9f)
                horizontalLineTo(16f)
                verticalLineTo(19f)
                horizontalLineTo(8f)
                verticalLineTo(9f)
                close()
                moveTo(15.5f, 4f)
                lineTo(14.5f, 3f)
                horizontalLineTo(9.5f)
                lineTo(8.5f, 4f)
                horizontalLineTo(5f)
                verticalLineTo(6f)
                horizontalLineTo(19f)
                verticalLineTo(4f)
                horizontalLineTo(15.5f)
                close()
            }
        }.build()
    }

    /**
     * Ikon pensil untuk mengganti nama.
     */
    val Edit: ImageVector by lazy {
        ImageVector.Builder(
            name = "Edit",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(3f, 17.25f)
                verticalLineTo(21f)
                horizontalLineTo(6.75f)
                lineTo(17.81f, 9.94f)
                lineTo(14.06f, 6.19f)
                lineTo(3f, 17.25f)
                close()
                moveTo(20.71f, 7.04f)
                curveTo(21.1f, 6.65f, 21.1f, 6.02f, 20.71f, 5.63f)
                lineTo(18.37f, 3.29f)
                curveTo(17.98f, 2.9f, 17.35f, 2.9f, 16.96f, 3.29f)
                lineTo(15.13f, 5.12f)
                lineTo(18.88f, 8.87f)
                lineTo(20.71f, 7.04f)
                close()
            }
        }.build()
    }

    /**
     * Ikon antrean musik (QueueMusic).
     */
    val QueueMusic: ImageVector by lazy {
        ImageVector.Builder(
            name = "QueueMusic",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(15f, 6f)
                horizontalLineTo(3f)
                verticalLineTo(8f)
                horizontalLineTo(15f)
                verticalLineTo(6f)
                close()
                moveTo(15f, 10f)
                horizontalLineTo(3f)
                verticalLineTo(12f)
                horizontalLineTo(15f)
                verticalLineTo(10f)
                close()
                moveTo(3f, 16f)
                horizontalLineTo(11f)
                verticalLineTo(14f)
                horizontalLineTo(3f)
                verticalLineTo(16f)
                close()
                moveTo(17f, 6f)
                verticalLineTo(14.18f)
                curveTo(16.69f, 14.07f, 16.35f, 14f, 16f, 14f)
                curveTo(14.34f, 14f, 13f, 15.34f, 13f, 17f)
                curveTo(13f, 18.66f, 14.34f, 20f, 16f, 20f)
                curveTo(17.66f, 20f, 19f, 18.66f, 19f, 17f)
                verticalLineTo(8f)
                horizontalLineTo(22f)
                verticalLineTo(6f)
                horizontalLineTo(17f)
                close()
            }
        }.build()
    }

    /**
     * Ikon centang (Check).
     */
    val Check: ImageVector by lazy {
        ImageVector.Builder(
            name = "Check",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(9f, 16.17f)
                lineTo(4.83f, 12f)
                lineTo(3.41f, 13.41f)
                lineTo(9f, 19f)
                lineTo(21f, 7f)
                lineTo(19.59f, 5.59f)
                close()
            }
        }.build()
    }
}

