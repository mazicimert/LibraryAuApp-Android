package com.mehmetmertmazici.libraryauapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Student Model
 * Öğrenci bilgilerini temsil eder
 *
 * iOS Karşılığı: Student.swift
 */
data class Student(
    @DocumentId
    val id: String? = null,
    val name: String = "",
    val surname: String = "",
    val studentNumber: String = "",
    val email: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val isDeleted: Boolean = false,
    val deletedAt: Timestamp? = null
) {
    /**
     * Yeni öğrenci oluşturma için secondary constructor
     */
    constructor(
        name: String,
        surname: String,
        studentNumber: String,
        email: String
    ) : this(
        id = null,
        name = name,
        surname = surname,
        studentNumber = studentNumber,
        email = email,
        createdAt = Timestamp.now(),
        isDeleted = false,
        deletedAt = null
    )

    // ── Computed Properties ──

    /** Tam adı döndürür */
    val fullName: String
        get() = "$name $surname"

    /** Validasyon kontrolü */
    val isValid: Boolean
        get() = name.isNotBlank() &&
                surname.isNotBlank() &&
                studentNumber.isNotBlank() &&
                studentNumber.length >= 3 &&
                email.isNotBlank()

    /** Görüntüleme için formatted text */
    val displayText: String
        get() = "$fullName - $studentNumber"

    // ── Helper Functions ──

    /** Öğrenci arama için (isim, soyisim, numara bazlı) */
    fun matches(searchTerm: String): Boolean {
        val term = searchTerm.lowercase()
        return name.lowercase().contains(term) ||
                surname.lowercase().contains(term) ||
                studentNumber.lowercase().contains(term) ||
                fullName.lowercase().contains(term) ||
                email.lowercase().contains(term)
    }
}