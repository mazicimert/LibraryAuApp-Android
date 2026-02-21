package com.mehmetmertmazici.libraryauapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * LibraryAuTheme Ankara Üniversitesi renk paleti ile Material3 tema
 *
 * iOS Karşılığı: Extensions.swift → AnkaraBackgroundModifier
 */

// ── Light Color Scheme ──
private val LightColorScheme =
        lightColorScheme(
                primary = AnkaraBlue,
                onPrimary = Color.White,
                primaryContainer = AnkaraLightBlue,
                onPrimaryContainer = Color.White,
                secondary = AnkaraLightBlue,
                onSecondary = Color.White,
                secondaryContainer = AnkaraColors.SectionBackground,
                onSecondaryContainer = AnkaraColors.OnBackground,
                tertiary = AnkaraGold,
                onTertiary = Color.White,
                background = AnkaraBackground,
                onBackground = AnkaraColors.OnBackground,
                surface = AnkaraCardBackground,
                onSurface = AnkaraColors.OnBackground,
                surfaceVariant = AnkaraColors.SectionBackground,
                onSurfaceVariant = AnkaraColors.Secondary,
                error = AnkaraDanger,
                onError = Color.White,
                outline = Color(0xFFE0E0E0),
                outlineVariant = Color(0xFFF0F0F0)
        )

// ── Dark Color Scheme ──
private val DarkColorScheme =
        darkColorScheme(
                primary = AnkaraLightBlue,
                onPrimary = Color.White,
                primaryContainer = AnkaraBlue,
                onPrimaryContainer = Color.White,
                secondary = AnkaraBlue,
                onSecondary = Color.White,
                secondaryContainer = AnkaraColors.SectionBackgroundDark,
                onSecondaryContainer = AnkaraColors.OnBackgroundDark,
                tertiary = AnkaraGold,
                onTertiary = Color.White,
                background = AnkaraColors.BackgroundDark,
                onBackground = AnkaraColors.OnBackgroundDark,
                surface = AnkaraColors.CardBackgroundDark,
                onSurface = AnkaraColors.OnBackgroundDark,
                surfaceVariant = AnkaraColors.SectionBackgroundDark,
                onSurfaceVariant = Color(0xFFAAAAAA),
                error = Color(0xFFEF5350),
                onError = Color.White,
                outline = Color(0xFF444444),
                outlineVariant = Color(0xFF333333)
        )

@Composable
fun LibraryAuTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Status bar ve navigation bar renklerini ayarla
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = LibraryAuTypography, content = content)
}

/** Ankara Üniversitesi gradient arka planı iOS Karşılığı: AnkaraBackgroundModifier */
@Composable
fun AnkaraBackground(
        modifier: Modifier = Modifier,
        darkTheme: Boolean = isSystemInDarkTheme(),
        content: @Composable () -> Unit
) {
    // Fully opaque colors to avoid status bar bleed-through with transparent window
    val gradientColors =
            if (darkTheme) {
                listOf(Color(0xFF1A1A1A), Color(0xFF001C44))
            } else {
                listOf(Color(0xFFF5F7FA), Color(0xFFB8D4ED))
            }

    Box(
            modifier =
                    modifier.fillMaxSize()
                            .background(brush = Brush.linearGradient(colors = gradientColors))
    ) { content() }
}

/** Modifier extension for Ankara background */
fun Modifier.ankaraBackground(darkTheme: Boolean): Modifier {
    // Fully opaque colors to avoid status bar bleed-through with transparent window
    val gradientColors =
            if (darkTheme) {
                listOf(Color(0xFF1A1A1A), Color(0xFF001C44))
            } else {
                listOf(Color(0xFFF5F7FA), Color(0xFFB8D4ED))
            }

    return this.background(brush = Brush.linearGradient(colors = gradientColors))
}
