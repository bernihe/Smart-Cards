package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = BrightViolet,
    secondary = ElectricBlue,
    tertiary = DeepPink,
    background = CharcoalBg,
    surface = CharcoalSurface,
    onPrimary = OnDarkTextPrimary,
    onSecondary = OnDarkTextPrimary,
    onTertiary = OnDarkTextPrimary,
    onBackground = OnDarkTextPrimary,
    onSurface = OnDarkTextPrimary,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = OnDarkTextSecondary
  )

private val LightColorScheme = DarkColorScheme // Force dark theme vibe for a consistent premium aesthetic

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme
  dynamicColor: Boolean = false, // Disable dynamic light schemes for a cohesive custom look
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
