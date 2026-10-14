package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TrensurbBlueLight,
    onPrimary = Color.White,
    primaryContainer = TrensurbBlueDark,
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = RailGreenLight,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF00532A),
    onSecondaryContainer = Color(0xFF8CF8A8),
    tertiary = WarningAmberLight,
    onTertiary = Color.Black,
    background = SurfaceDarkNavy,
    onBackground = TextPrimaryDark,
    surface = SurfaceDarkCard,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceDarkCardElevated,
    onSurfaceVariant = TextSecondaryDark,
    error = AlertRed
)

private val LightColorScheme = lightColorScheme(
    primary = TrensurbBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = TrensurbBlueDark,
    secondary = RailGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB9F6CA),
    onSecondaryContainer = Color(0xFF00391A),
    tertiary = WarningAmber,
    onTertiary = Color.White,
    background = SurfaceLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLightCard,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = TextSecondaryLight,
    error = AlertRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent Trensurb branding
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
