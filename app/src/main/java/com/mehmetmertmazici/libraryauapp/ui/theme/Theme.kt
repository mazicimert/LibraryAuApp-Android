package com.mehmetmertmazici.libraryauapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * LibraryAuTheme
 * Ankara Üniversitesi renk paleti ile Material3 tema
 *
 * iOS Karşılığı: Extensions.swift → Color extension + AnkaraBackgroundModifier
 *
 * NOT: Bu dosya Adım 3'te tam olarak doldurulacak.
 * Şimdilik temel yapı ve ana renkler tanımlı.
 */

// ── Ankara Üniversitesi Ana Renkleri ──
// iOS: Extensions.swift'ten birebir taşındı
val AnkaraBlue = Color(0xFF003882)
val AnkaraLightBlue = Color(0xFF3378CC)
val AnkaraGold = Color(0xFFCC9E1C)
val AnkaraSuccess = Color(0xFF00A64F)
val AnkaraWarning = Color(0xFFF29C12)
val AnkaraDanger = Color(0xFFD93333)
val AnkaraBackground = Color(0xFFFAFBFC)
val AnkaraCardBackground = Color.White
val AnkaraSectionBackground = Color(0xFFF2F4F7)

// ── Light Color Scheme ──
private val LightColorScheme = lightColorScheme(
    primary = AnkaraBlue,
    secondary = AnkaraLightBlue,
    tertiary = AnkaraGold,
    background = AnkaraBackground,
    surface = AnkaraCardBackground,
    error = AnkaraDanger,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    onError = Color.White,
)

// ── Dark Color Scheme ──
private val DarkColorScheme = darkColorScheme(
    primary = AnkaraLightBlue,
    secondary = AnkaraBlue,
    tertiary = AnkaraGold,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    error = Color(0xFFEF5350),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
    onError = Color.White,
)

@Composable
fun LibraryAuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        // typography = LibraryAuTypography,  // Adım 3'te eklenecek
        content = content
    )
}