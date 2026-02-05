package com.mehmetmertmazici.libraryauapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * AdminUser Model Admin ve Süper Admin kullanıcılarını temsil eder
 *
 * iOS Karşılığı: AdminUser.swift
 */
data class AdminUser(
        @DocumentId val id: String? = null,
        val email: String = "",
        val displayName: String = "",
        val createdAt: Timestamp = Timestamp.now(),
        @get:PropertyName("isApproved")
        @set:PropertyName("isApproved")
        var isApproved: Boolean = false,
        @get:PropertyName("isSuperAdmin")
        @set:PropertyName("isSuperAdmin")
        var isSuperAdmin: Boolean = false
) {
    /** İlk kayıt için secondary constructor */
    constructor(
            email: String,
            displayName: String,
            isSuperAdmin: Boolean = false
    ) : this(
            id = null,
            email = email,
            displayName = displayName,
            createdAt = Timestamp.now(),
            isApproved = isSuperAdmin, // Süper adminler otomatik onaylı
            isSuperAdmin = isSuperAdmin
    )

    // ── Computed Properties ──

    /** Kullanıcı rolü string olarak */
    val roleText: String
        get() = if (isSuperAdmin) "Süper Admin" else "Admin"

    /** Onay durumu string olarak */
    val approvalStatusText: String
        get() =
                when {
                    isSuperAdmin -> "Aktif"
                    isApproved -> "Onaylandı"
                    else -> "Onay Bekliyor"
                }

    /** Aktif kullanıcı mı? */
    val isActive: Boolean
        get() = isSuperAdmin || isApproved

    /** Onay bekleyen admin mi? */
    val isPendingApproval: Boolean
        get() = !isSuperAdmin && !isApproved

    /** Email validasyonu */
    val hasValidEmail: Boolean
        get() = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    /** Görüntüleme metni */
    val displayText: String
        get() = "$displayName ($roleText)"

    // ── Helper Functions ──

    /** Rol değiştirme için yardımcı fonksiyon */
    fun withUpdatedRole(newIsSuperAdmin: Boolean): AdminUser {
        return copy(
                isSuperAdmin = newIsSuperAdmin,
                isApproved = if (newIsSuperAdmin) true else isApproved
        )
    }
}
