package com.mehmetmertmazici.libraryauapp.domain.util

import android.util.Patterns
import java.text.Normalizer
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
 * String boş veya sadece boşluk mu kontrol eder
 */
val String.isBlankOrEmpty: Boolean
    get() = this.isBlank()

/**
 * Email formatı geçerli mi?
 */
val String.isValidEmail: Boolean
    get() = Patterns.EMAIL_ADDRESS.matcher(this.trim()).matches()

/**
 * Sadece rakamları içeriyor mu?
 */
val String.isNumeric: Boolean
    get() = this.isNotEmpty() && this.all { it.isDigit() }

/**
 * Türkçe karakterleri normalize et (ASCII karşılıklarına çevir)
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
 * Arama için normalize et
 * - Küçük harfe çevirir
 * - Türkçe karakterleri ASCII'ye çevirir
 * - Unicode aksanları temizler
 * - Fazla boşlukları temizler
 */
val String.searchNormalized: String
    get() {
        // Küçük harfe çevir (Türkçe locale ile)
        var result = this.lowercase(Locale.forLanguageTag("tr-TR"))

        // Türkçe karakterleri ASCII karşılıklarına çevir
        result = result.turkishNormalized.lowercase()

        // Unicode normalization (aksanlı karakterleri temizle)
        result = Normalizer.normalize(result, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

        // Fazla boşlukları temizle
        result = result.replace(Regex("\\s+"), " ").trim()

        return result
    }

/**
 * Belirtilen uzunlukta karakterle doldur (varsayılan: sıfır)
 */
fun String.padded(toLength: Int, withPad: Char = '0'): String {
    val padding = withPad.toString().repeat(maxOf(0, toLength - this.length))
    return padding + this
}

/**
 * String'i telefon numarası formatına çevirir
 * Örnek: 5321234567 -> (532) 123 45 67
 */
fun String.toPhoneFormat(): String {
    val digits = this.filter { it.isDigit() }
    return when {
        digits.length >= 10 -> {
            val formatted = StringBuilder()
            formatted.append("(")
            formatted.append(digits.substring(0, 3))
            formatted.append(") ")
            formatted.append(digits.substring(3, 6))
            formatted.append(" ")
            formatted.append(digits.substring(6, 8))
            formatted.append(" ")
            formatted.append(digits.substring(8, minOf(10, digits.length)))
            formatted.toString()
        }
        else -> this
    }
}

/**
 * İlk harfi büyük yapar
 */
fun String.capitalizeFirst(): String {
    return if (this.isNotEmpty()) {
        this[0].uppercaseChar() + this.substring(1).lowercase()
    } else {
        this
    }
}

/**
 * Her kelimenin ilk harfini büyük yapar
 */
fun String.capitalizeWords(): String {
    return this.split(" ").joinToString(" ") { it.capitalizeFirst() }
}

/**
 * String'den sadece rakamları al
 */
val String.digitsOnly: String
    get() = this.filter { it.isDigit() }

/**
 * Geçerli bir ISBN mi? (10 veya 13 haneli)
 */
val String.isValidISBN: Boolean
    get() {
        val digits = this.digitsOnly
        return digits.length == 10 || digits.length == 13
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
 * Uzun tarih formatı (gün ay yıl, saat:dakika)
 */
val Date.longDateString: String
    get() {
        val formatter = SimpleDateFormat("d MMMM yyyy, HH:mm", turkishLocale)
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
 * Tarih bugün mü?
 */
val Date.isToday: Boolean
    get() {
        val today = Calendar.getInstance()
        val dateCalendar = Calendar.getInstance().apply { time = this@isToday }
        return today.get(Calendar.YEAR) == dateCalendar.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == dateCalendar.get(Calendar.DAY_OF_YEAR)
    }

/**
 * Tarihe gün ekle
 */
fun Date.addingDays(days: Int): Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar.add(Calendar.DAY_OF_YEAR, days)
    return calendar.time
}

/**
 * Günün başlangıcını al (00:00:00)
 */
val Date.startOfDay: Date
    get() {
        val calendar = Calendar.getInstance()
        calendar.time = this
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

/**
 * Günün sonunu al (23:59:59)
 */
val Date.endOfDay: Date
    get() {
        val calendar = Calendar.getInstance()
        calendar.time = this
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
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

/**
 * Güvenli index erişimi
 */
fun <T> List<T>.getOrNull(index: Int): T? {
    return if (index in indices) this[index] else null
}

// ══════════════════════════════════════════════════════════════
// MARK: - Int Extensions
// ══════════════════════════════════════════════════════════════

/**
 * Sayıyı belirli uzunlukta string'e çevir (sıfırla doldur)
 */
fun Int.toPaddedString(length: Int): String {
    return this.toString().padStart(length, '0')
}