package licenta.soundaround.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppDarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = NeonPurpleOnDark,
    primaryContainer = NeonPurpleDim,
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Teal,
    onSecondary = TealDim,
    secondaryContainer = Color(0xFF004D3A),
    onSecondaryContainer = Color(0xFFB3F5E6),
    tertiary = HotPink,
    onTertiary = HotPinkDim,
    tertiaryContainer = Color(0xFF5C1040),
    onTertiaryContainer = Color(0xFFFFD8EC),
    background = DarkBg,
    onBackground = LightOnDark,
    surface = DarkSurface,
    onSurface = LightOnDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = SubtleOnDark,
    outline = OutlineDark,
    outlineVariant = Color(0xFF3A3550),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF1C1B1F),
    inversePrimary = Purple40,
)

@Composable
fun SoundAroundTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppDarkColorScheme,
        typography = Typography,
        content = content
    )
}
