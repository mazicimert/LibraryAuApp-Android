package com.mehmetmertmazici.libraryauapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * BookCopy Model
 * Fiziksel kitap kopyası - Her kitabın fiziksel örneğini temsil eder
 *
 * iOS Karşılığı: BookCopy.swift
 */
data class BookCopy(
    @DocumentId
    val id: String? = null,
    val bookId: Int = 0,
    val barcode: String = "",
    val copyNumber: Int = 0,
    val isAvailable: Boolean = true,
    val bookTemplateId: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val isDeleted: Boolean = false,
    val deletedAt: Timestamp? = null
) {
    /**
     * Yeni kopya oluşturma için secondary constructor
     */
    constructor(
        bookId: Int,
        barcode: String,
        copyNumber: Int,
        bookTemplateId: String
    ) : this(
        id = null,
        bookId = bookId,
        barcode = barcode,
        copyNumber = copyNumber,
        isAvailable = true,
        bookTemplateId = bookTemplateId,
        createdAt = Timestamp.now(),
        isDeleted = false,
        deletedAt = null
    )

    // ── Computed Properties ──

    /** Kopyanın durumunu string olarak döndürür */
    val statusText: String
        get() = if (isAvailable) "Müsait" else "Ödünçte"

    /** Durum rengi için */
    val statusColor: String
        get() = if (isAvailable) "green" else "red"

    /** Barkod tipini belirler (ISBN mi LIB formatı mı?) */
    val barcodeType: BarcodeType
        get() = if (barcode.startsWith("LIB")) BarcodeType.LIB else BarcodeType.ISBN

    /** Barkod görüntüleme formatı */
    val displayBarcode: String
        get() = if (barcodeType == BarcodeType.LIB) barcode else "ISBN: $barcode"
}

// ══════════════════════════════════════════════════════════════
// MARK: - BarcodeType Enum
// ══════════════════════════════════════════════════════════════

enum class BarcodeType {
    ISBN,  // Orijinal ISBN barkodu
    LIB;   // Sistem tarafından oluşturulan LIB barkodu

    val description: String
        get() = when (this) {
            ISBN -> "ISBN Barkodu"
            LIB -> "Kütüphane Barkodu"
        }
}