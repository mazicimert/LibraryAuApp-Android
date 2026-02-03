package com.mehmetmertmazici.libraryauapp.domain.util

import com.mehmetmertmazici.libraryauapp.data.model.BookCopy

/**
 * BarcodeGenerator
 * Akıllı barkod oluşturma sistemi
 *
 * iOS Karşılığı: BarcodeGenerator.swift
 */
object BarcodeGenerator {

    /**
     * Kitap kopyaları için akıllı barkod oluşturur
     *
     * @param bookId Kitap şablonu ID'si (numerik kısım için)
     * @param isbn Orijinal ISBN numarası
     * @param copyCount Oluşturulacak kopya sayısı
     * @param existingCopies Var olan kopyalar (kopya numarası belirlemek için)
     * @param bookTemplateId Kitap şablonunun Firestore ID'si
     * @return Oluşturulan BookCopy listesi
     */
    fun createBookCopies(
        bookId: Int,
        isbn: String,
        copyCount: Int,
        existingCopies: List<BookCopy> = emptyList(),
        bookTemplateId: String
    ): List<BookCopy> {
        val copies = mutableListOf<BookCopy>()
        val startingCopyNumber = getNextCopyNumber(existingCopies)

        for (i in 0 until copyCount) {
            val copyNumber = startingCopyNumber + i

            // Format: LIB + 001 (Kitap ID) + 001 (Kopya No) -> Örn: LIB001001
            val barcode = generateLIBBarcode(bookId, copyNumber)

            val copy = BookCopy(
                bookId = bookId,
                barcode = barcode,
                copyNumber = copyNumber,
                bookTemplateId = bookTemplateId
            )

            copies.add(copy)
        }

        return copies
    }

    /**
     * LIB formatında barkod oluşturur: LIB[KitapID(3 haneli)][KopyaNumarası(3 haneli)]
     */
    fun generateLIBBarcode(bookId: Int, copyNumber: Int): String {
        return "LIB${bookId.toString().padStart(3, '0')}${copyNumber.toString().padStart(3, '0')}"
    }

    /**
     * Var olan kopyalardan sonraki kopya numarasını belirler
     */
    fun getNextCopyNumber(copies: List<BookCopy>): Int {
        val maxCopyNumber = copies.maxOfOrNull { it.copyNumber } ?: 0
        return maxCopyNumber + 1
    }

    /**
     * Barkodun geçerli olup olmadığını kontrol eder
     */
    fun isValidBarcode(barcode: String): Boolean {
        // ISBN formatı kontrol
        if (isValidISBN(barcode)) {
            return true
        }

        // LIB formatı kontrol: LIB + 3 haneli kitap ID + 3 haneli kopya numarası = 9 karakter
        if (barcode.startsWith("LIB") && barcode.length == 9) {
            val numericPart = barcode.drop(3)
            return numericPart.all { it.isDigit() } && numericPart.length == 6
        }

        return false
    }

    /**
     * ISBN formatını kontrol eder (ISBN-10 ve ISBN-13 desteklenir)
     */
    private fun isValidISBN(isbn: String): Boolean {
        // ISBN'den tire ve boşlukları temizle
        val cleanISBN = isbn.replace("-", "").replace(" ", "")

        // ISBN-13 kontrolü (978 veya 979 ile başlar)
        if (cleanISBN.length == 13) {
            return cleanISBN.startsWith("978") || cleanISBN.startsWith("979")
        }

        // ISBN-10 kontrolü (10 haneli)
        if (cleanISBN.length == 10) {
            return cleanISBN.all { it.isDigit() || it == 'X' }
        }

        return false
    }

    /**
     * Barkoddan kitap ID ve kopya numarası çıkarır (LIB formatı için)
     */
    fun parseLIBBarcode(barcode: String): Pair<Int, Int>? {
        if (!barcode.startsWith("LIB") || barcode.length != 9) return null

        val numbers = barcode.drop(3)
        val bookId = numbers.take(3).toIntOrNull() ?: return null
        val copyNumber = numbers.takeLast(3).toIntOrNull() ?: return null

        return Pair(bookId, copyNumber)
    }

    /**
     * Test amaçlı örnek barkodlar oluşturur
     */
    fun generateSampleBarcodes(): List<String> {
        return listOf(
            "LIB001001",
            "LIB001002",
            "LIB002001",
            "LIB050001"
        )
    }
}

/**
 * Barkod Hataları
 */
sealed class BarcodeError(override val message: String) : Exception(message) {
    data object InvalidFormat : BarcodeError("Geçersiz barkod formatı")
    data object DuplicateBarcode : BarcodeError("Bu barkod zaten mevcut")
    data object InvalidISBN : BarcodeError("Geçersiz ISBN numarası")
    data object InvalidCopyCount : BarcodeError("Kopya sayısı 1'den az olamaz")
}