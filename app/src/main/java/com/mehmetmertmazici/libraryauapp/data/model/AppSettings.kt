package com.mehmetmertmazici.libraryauapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * AppSettings Model
 * Uygulama genel ayarlarını ve iş kurallarını yönetir
 *
 * iOS Karşılığı: AppSettings.swift
 */
data class AppSettings(
    @DocumentId
    val id: String? = null,
    val maxBooksPerStudent: Int = 3,
    val defaultBorrowDays: Int = 14,
    val warningDaysBeforeDue: Int = 2,
    val allowSameBookTemplate: Boolean = false,
    val isFirstLaunch: Boolean = true,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    /** Ayarların geçerli olup olmadığını kontrol et */
    val isValid: Boolean
        get() = maxBooksPerStudent > 0 &&
                defaultBorrowDays > 0 &&
                warningDaysBeforeDue >= 0

    /** Ayar özetleri string olarak */
    val settingsSummary: String
        get() = """
            Maksimum Kitap: $maxBooksPerStudent
            Ödünç Süresi: $defaultBorrowDays gün
            Uyarı Eşiği: $warningDaysBeforeDue gün
            Aynı Kitap: ${if (allowSameBookTemplate) "İzin Verilir" else "İzin Verilmez"}
        """.trimIndent()

    /** Ayarları güncelle */
    fun withUpdatedSettings(
        maxBooks: Int? = null,
        borrowDays: Int? = null,
        warningDays: Int? = null,
        allowSameBook: Boolean? = null
    ): AppSettings {
        return copy(
            maxBooksPerStudent = maxBooks?.coerceAtLeast(1) ?: maxBooksPerStudent,
            defaultBorrowDays = borrowDays?.coerceAtLeast(1) ?: defaultBorrowDays,
            warningDaysBeforeDue = warningDays?.coerceAtLeast(0) ?: warningDaysBeforeDue,
            allowSameBookTemplate = allowSameBook ?: allowSameBookTemplate,
            updatedAt = Timestamp.now()
        )
    }

    /** İlk kurulum tamamlandı olarak işaretle */
    fun withFirstLaunchComplete(): AppSettings {
        return copy(
            isFirstLaunch = false,
            updatedAt = Timestamp.now()
        )
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - NetworkStatus - Ağ Durumu
// ══════════════════════════════════════════════════════════════

enum class NetworkStatus {
    CONNECTED,
    DISCONNECTED,
    CONNECTING;

    val description: String
        get() = when (this) {
            CONNECTED -> "Çevrimiçi"
            DISCONNECTED -> "Çevrimdışı"
            CONNECTING -> "Bağlanıyor"
        }

    val emoji: String
        get() = when (this) {
            CONNECTED -> "🟢"
            DISCONNECTED -> "🔴"
            CONNECTING -> "🟡"
        }
}

// ══════════════════════════════════════════════════════════════
// MARK: - LoadingState - Yükleme Durumu
// ══════════════════════════════════════════════════════════════

sealed class LoadingState {
    data object Idle : LoadingState()
    data object Loading : LoadingState()
    data object Success : LoadingState()
    data class Error(val message: String) : LoadingState()

    val isLoading: Boolean
        get() = this is Loading

    val errorMessage: String?
        get() = (this as? Error)?.message
}

// ══════════════════════════════════════════════════════════════
// MARK: - UserRole - Kullanıcı Rolleri
// ══════════════════════════════════════════════════════════════

enum class UserRole(val value: String) {
    ADMIN("admin"),
    SUPER_ADMIN("superAdmin");

    val displayName: String
        get() = when (this) {
            ADMIN -> "Admin"
            SUPER_ADMIN -> "Süper Admin"
        }

    val permissions: List<Permission>
        get() = when (this) {
            ADMIN -> listOf(
                Permission.MANAGE_BOOKS,
                Permission.MANAGE_STUDENTS,
                Permission.MANAGE_BORROWING
            )
            SUPER_ADMIN -> Permission.entries
        }
}

// ══════════════════════════════════════════════════════════════
// MARK: - Permission - İzinler
// ══════════════════════════════════════════════════════════════

enum class Permission(val value: String) {
    MANAGE_BOOKS("manageBooks"),
    MANAGE_STUDENTS("manageStudents"),
    MANAGE_BORROWING("manageBorrowing"),
    MANAGE_ADMINS("manageAdmins"),
    MANAGE_SETTINGS("manageSettings");

    val displayName: String
        get() = when (this) {
            MANAGE_BOOKS -> "Kitap Yönetimi"
            MANAGE_STUDENTS -> "Öğrenci Yönetimi"
            MANAGE_BORROWING -> "Ödünç İşlemleri"
            MANAGE_ADMINS -> "Admin Yönetimi"
            MANAGE_SETTINGS -> "Sistem Ayarları"
        }
}