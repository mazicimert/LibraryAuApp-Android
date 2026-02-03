package com.mehmetmertmazici.libraryauapp.domain.util

import android.util.Patterns
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Uygulama genelinde kullanılan extension fonksiyonlar
 *
 * iOS Karşılığı: Extensions.swift
 */

// ══════════════════════════════════════════════════════════════
// MARK: - String Extensions
// ══════════════════════════════════════════════════════════════

/**
 * String'i temizle (boşlukları kaldır)
 */
val String.trimmed: String
    get() = this.trim()

/**
 * Email formatı geçerli mi?
 */
val String.isValidEmail: Boolean
    get() = Patterns.EMAIL_ADDRESS.matcher(this).matches()

/**
 * Sadece rakamları içeriyor mu?
 */
val String.isNumeric: Boolean
    get() = this.isNotEmpty() && this.all { it.isDigit() }

/**
 * Türkçe karakterleri normalize et
 */
val String.turkishNormalized: String
    get() = this
        .replace("ı", "i")
        .replace("İ", "I")
        .replace("ş", "s")
        .replace("Ş", "S")
        .replace("ğ", "g")
        .replace("Ğ", "G")
        .replace("ü", "u")
        .replace("Ü", "U")
        .replace("ö", "o")
        .replace("Ö", "O")
        .replace("ç", "c")
        .replace("Ç", "C")

/**
 * Arama için normalize et (küçük harf + türkçe karakter temizliği)
 */
val String.searchNormalized: String
    get() = this.lowercase().turkishNormalized

/**
 * Belirtilen uzunlukta sıfırlarla doldur
 */
fun String.padded(toLength: Int, withPad: Char = '0'): String {
    val padding = withPad.toString().repeat(maxOf(0, toLength - this.length))
    return padding + this
}

// ══════════════════════════════════════════════════════════════
// MARK: - Date Extensions
// ══════════════════════════════════════════════════════════════

private val turkishLocale = Locale("tr", "TR")

/**
 * Türkçe tarih formatı (örn: 15 Ocak 2025)
 */
val Date.turkishDateString: String
    get() {
        val formatter = SimpleDateFormat("d MMMM yyyy", turkishLocale)
        return formatter.format(this)
    }

/**
 * Kısa tarih formatı (gün/ay/yıl)
 */
val Date.shortDateString: String
    get() {
        val formatter = SimpleDateFormat("dd/MM/yyyy", turkishLocale)
        return formatter.format(this)
    }

/**
 * Saat formatı
 */
val Date.timeString: String
    get() {
        val formatter = SimpleDateFormat("HH:mm", turkishLocale)
        return formatter.format(this)
    }

/**
 * İki tarih arasındaki gün sayısı
 */
fun Date.daysBetween(other: Date): Int {
    val diffInMillis = other.time - this.time
    return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
}

/**
 * Tarih geçmiş mi?
 */
val Date.isPast: Boolean
    get() = this.before(Date())

/**
 * Tarihe gün ekle
 */
fun Date.addingDays(days: Int): Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar.add(Calendar.DAY_OF_YEAR, days)
    return calendar.time
}

// ══════════════════════════════════════════════════════════════
// MARK: - List Extensions
// ══════════════════════════════════════════════════════════════

/**
 * Element'i ID ile bul (Identifiable gibi çalışır)
 */
inline fun <T, ID> List<T>.firstWithId(id: ID, idSelector: (T) -> ID): T? {
    return this.firstOrNull { idSelector(it) == id }
}