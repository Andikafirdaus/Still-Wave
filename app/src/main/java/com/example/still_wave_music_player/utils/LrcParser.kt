package com.example.still_wave_music_player.utils

import com.example.still_wave_music_player.data.model.LyricLine

/**
 * Parser format lirik tersinkron (.lrc).
 * Mendukung stempel waktu format [mm:ss.xx] dan [mm:ss.xxx],
 * satu baris dengan banyak stempel waktu, mengabaikan tag metadata seperti [ar:],
 * dan mengembalikan daftar LyricLine terurut berdasarkan waktu.
 */
object LrcParser {

    private val TIMESTAMP_REGEX = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\]""")
    private val TAG_REGEX = Regex("""^\[[a-zA-Z]+:.*\]""")

    fun parse(lrcContent: String?): List<LyricLine> {
        if (lrcContent.isNullOrBlank()) return emptyList()

        val result = mutableListOf<LyricLine>()
        val lines = lrcContent.lines()

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            // Abaikan tag metadata seperti [ar:Artis], [ti:Judul], [offset:0], dll.
            if (TAG_REGEX.matches(line)) continue

            val matchResults = TIMESTAMP_REGEX.findAll(line).toList()
            if (matchResults.isEmpty()) continue

            // Bersihkan teks dari stempel waktu
            val text = line.replace(TIMESTAMP_REGEX, "").trim()

            for (match in matchResults) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val fracStr = match.groupValues.getOrNull(3).orEmpty()
                val ms = if (fracStr.isNotEmpty()) {
                    fracStr.padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                } else {
                    0L
                }
                val totalMs = (min * 60L + sec) * 1000L + ms
                result.add(LyricLine(timeMs = totalMs, text = text))
            }
        }

        return result.sortedBy { it.timeMs }
    }
}
