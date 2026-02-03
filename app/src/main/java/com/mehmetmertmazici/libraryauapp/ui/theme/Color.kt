package com.mehmetmertmazici.libraryauapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Ankara Üniversitesi Renk Paleti
 *
 * iOS Karşılığı: Extensions.swift → Color extension
 */
object AnkaraColors {

    // ── Ana Renkler ──
    val Blue = Color(0xFF003882)           // #003882 - Ana mavi
    val LightBlue = Color(0xFF3378CC)      // #3378CC - Açık mavi
    val Gold = Color(0xFFCC9E1C)           // #CC9E1C - Altın

    // ── Yardımcı Renkler ──
    val Success = Color(0xFF00A64F)        // #00A64F - Başarı yeşili
    val Warning = Color(0xFFF29C12)        // #F29C12 - Uyarı turuncusu
    val Danger = Color(0xFFD93333)         // #D93333 - Tehlike kırmızısı

    // ── Arka Plan Renkleri (Light Mode) ──
    val Background = Color(0xFFFAFBFC)     // #FAFBFC - Ana arka plan
    val CardBackground = Color.White       // Kart arka planı
    val SectionBackground = Color(0xFFF2F4F7)  // #F2F4F7 - Bölüm arka planı

    // ── Arka Plan Renkleri (Dark Mode) ──
    val BackgroundDark = Color(0xFF121212)
    val CardBackgroundDark = Color(0xFF1E1E1E)
    val SectionBackgroundDark = Color(0xFF2C2C2C)

    // ── Metin Renkleri ──
    val OnPrimary = Color.White
    val OnBackground = Color(0xFF1C1B1F)
    val OnBackgroundDark = Color(0xFFE6E1E5)
    val Secondary = Color(0xFF6B7280)      // Gri metin

    /**
     * Durum rengini döndürür
     * iOS Karşılığı: Color.statusColor(for:)
     */
    fun statusColor(status: String): Color {
        return when (status.lowercase()) {
            "müsait", "available", "active" -> Success
            "ödünçte", "borrowed" -> Danger
            "gecikmiş", "overdue" -> Danger
            "beklemede", "pending" -> Warning
            else -> Color.Gray
        }
    }
}

// ── Uyumluluk için top-level değişkenler ──
val AnkaraBlue = AnkaraColors.Blue
val AnkaraLightBlue = AnkaraColors.LightBlue
val AnkaraGold = AnkaraColors.Gold
val AnkaraSuccess = AnkaraColors.Success
val AnkaraWarning = AnkaraColors.Warning
val AnkaraDanger = AnkaraColors.Danger
val AnkaraBackground = AnkaraColors.Background
val AnkaraCardBackground = AnkaraColors.CardBackground
val AnkaraSectionBackground = AnkaraColors.SectionBackground