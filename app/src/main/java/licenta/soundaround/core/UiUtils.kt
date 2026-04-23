package licenta.soundaround.core

import androidx.compose.ui.graphics.Color

private val avatarPalette = listOf(
    Color(0xFF6750A4), // purple
    Color(0xFF2E7D32), // forest green
    Color(0xFF1565C0), // deep blue
    Color(0xFFAD1457), // pink
    Color(0xFF00695C), // teal
    Color(0xFFF57F17), // amber
)

fun avatarColor(seed: String): Color {
    if (seed.isBlank()) return avatarPalette[0]
    val hash = seed.fold(0) { acc, c -> acc * 31 + c.code }
    return avatarPalette[(hash and Int.MAX_VALUE) % avatarPalette.size]
}
