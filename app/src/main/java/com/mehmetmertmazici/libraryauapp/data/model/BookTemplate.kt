package com.mehmetmertmazici.libraryauapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.gson.annotations.SerializedName

/**
 * BookTemplate Model
 * Kitap şablonu - Bir kitabın genel bilgilerini temsil eder
 *
 * iOS Karşılığı: BookTemplate.swift
 */
data class BookTemplate(
    @DocumentId
    val id: String? = null,
    val title: String = "",
    val author: String = "",
    val isbn: String = "",
    val publisher: String = "",
    val editor: String = "",
    val category: String = "",
    val description: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val isDeleted: Boolean = false,
    val deletedAt: Timestamp? = null
) {
    /**
     * Yeni kitap şablonu oluşturma için secondary constructor
     */
    constructor(
        title: String,
        author: String,
        isbn: String,
        publisher: String,
        editor: String = "",
        category: String = "",
        description: String = ""
    ) : this(
        id = null,
        title = title,
        author = author,
        isbn = isbn,
        publisher = publisher,
        editor = editor,
        category = category,
        description = description,
        createdAt = Timestamp.now(),
        isDeleted = false,
        deletedAt = null
    )
}

// ══════════════════════════════════════════════════════════════
// MARK: - BookData (JSON Parsing için)
// JSON dosyasından kitap verilerini almak için
// ══════════════════════════════════════════════════════════════

data class BookData(
    @SerializedName("BAŞLIK")
    val title: String,

    @SerializedName("YAZAR")
    val author: String,

    @SerializedName("ISBN NUMARASI")
    val isbn: String,

    @SerializedName("YAYINEVİ")
    val publisher: String,

    @SerializedName("EDİTÖR")
    val editor: String,

    @SerializedName("CATEGORY")
    val category: String,

    @SerializedName("DESCRIPTION")
    val description: String,

    @SerializedName("COPY_COUNT")
    val copyCount: Int
) {
    /** BookData'dan BookTemplate'e dönüştürme */
    fun toBookTemplate(): BookTemplate {
        return BookTemplate(
            title = title,
            author = author,
            isbn = isbn,
            publisher = publisher,
            editor = editor,
            category = category,
            description = description
        )
    }
}