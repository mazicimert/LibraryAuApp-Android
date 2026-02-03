package com.mehmetmertmazici.libraryauapp.data.repository

import android.util.Patterns
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.mehmetmertmazici.libraryauapp.data.model.AdminUser
import com.mehmetmertmazici.libraryauapp.data.model.Permission
import com.mehmetmertmazici.libraryauapp.data.model.UserRole
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AuthRepository
 * Firebase Authentication ile kullanıcı yönetimi
 *
 * iOS Karşılığı: AuthenticationService.swift
 * iOS: Combine Publisher → Android: Flow/suspend function
 * iOS: @Published → Android: StateFlow
 */
@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    // ── State Flows ──
    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _currentAdminUser = MutableStateFlow<AdminUser?>(null)
    val currentAdminUser: StateFlow<AdminUser?> = _currentAdminUser.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // ── Collection Names ──
    private object Collections {
        const val ADMIN_USERS = "adminUsers"
    }

    init {
        setupAuthStateListener()
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Auth State Listener
    // ══════════════════════════════════════════════════════════════

    private fun setupAuthStateListener() {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            _isSignedIn.value = user != null

            if (user != null) {
                // Admin verilerini yükle (coroutine scope'da çağrılmalı)
                // Bu ViewModel'den tetiklenecek
            } else {
                _currentAdminUser.value = null
                _authState.value = AuthState.SignedOut
            }
        }
    }

    /**
     * Auth state değişikliklerini Flow olarak dinle
     */
    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        this@AuthRepository.auth.addAuthStateListener(listener)
        awaitClose { this@AuthRepository.auth.removeAuthStateListener(listener) }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Load Admin User Data
    // ══════════════════════════════════════════════════════════════

    /**
     * Admin kullanıcı verilerini yükle
     */
    suspend fun loadAdminUserData(userId: String): Result<AdminUser?> {
        return try {
            val document = firestore.collection(Collections.ADMIN_USERS)
                .document(userId)
                .get()
                .await()

            val adminUser = document.toObject(AdminUser::class.java)
            _currentAdminUser.value = adminUser

            if (adminUser != null) {
                _authState.value = if (adminUser.isActive) {
                    AuthState.SignedIn
                } else {
                    AuthState.PendingApproval
                }
            } else {
                println("⚠️ Admin kullanıcı verisi bulunamadı")
                _authState.value = AuthState.SignedOut
            }

            Result.success(adminUser)
        } catch (e: Exception) {
            println("⚠️ Admin kullanıcı verisi yüklenemedi: ${e.message}")
            _authState.value = AuthState.SignedOut
            Result.failure(e)
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Authentication Methods
    // ══════════════════════════════════════════════════════════════

    /**
     * Kullanıcı giriş işlemi
     */
    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw AuthError.UserNotFound

            // Admin verilerini yükle
            loadAdminUserData(user.uid)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Kullanıcı kayıt işlemi
     */
    suspend fun signUp(
        email: String,
        password: String,
        displayName: String
    ): Result<Unit> {
        return try {
            // Firebase Auth'da kullanıcı oluştur
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw AuthError.UserCreationFailed

            // Profil güncelle
            val profileUpdates = userProfileChangeRequest {
                this.displayName = displayName
            }
            user.updateProfile(profileUpdates).await()

            // Admin kullanıcı kaydı oluştur (onay bekliyor)
            val adminUser = AdminUser(
                email = email,
                displayName = displayName,
                isSuperAdmin = false
            )

            saveAdminUserWithId(adminUser, user.uid)

            _authState.value = AuthState.PendingApproval
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Çıkış işlemi
     */
    fun signOut() {
        auth.signOut()
        _currentAdminUser.value = null
        _authState.value = AuthState.SignedOut
    }

    /**
     * Şifre sıfırlama e-postası gönder
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * E-posta doğrulama gönder
     */
    suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw AuthError.UserNotFound
            user.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Kullanıcı hesabını sil
     */
    suspend fun deleteAccount(password: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw AuthError.UserNotFound
            val email = user.email ?: throw AuthError.UserNotFound

            // Yeniden doğrula
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()

            val userId = user.uid

            // Firestore'dan sil
            try {
                firestore.collection(Collections.ADMIN_USERS)
                    .document(userId)
                    .delete()
                    .await()
            } catch (e: Exception) {
                println("❌ Firestore kullanıcı kaydı silinemedi: ${e.message}")
            }

            // Auth'dan sil
            user.delete().await()

            _currentUser.value = null
            _currentAdminUser.value = null
            _isSignedIn.value = false
            _authState.value = AuthState.SignedOut

            println("✅ Kullanıcı hesabı başarıyla silindi")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AuthError.InvalidCredentials)
        }
    }

    /**
     * Admin kullanıcıyı ID ile kaydet
     */
    private suspend fun saveAdminUserWithId(adminUser: AdminUser, userId: String) {
        firestore.collection(Collections.ADMIN_USERS)
            .document(userId)
            .set(adminUser)
            .await()
    }

    /**
     * Süper Admin tarafından yeni admin oluştur
     */
    suspend fun createAdminBySuperAdmin(
        email: String,
        password: String,
        displayName: String,
        superAdminPassword: String,
        newAdminIsSuperAdmin: Boolean
    ): Result<String> {
        return try {
            val currentUser = auth.currentUser ?: throw AuthError.UserNotFound
            val superAdminEmail = currentUser.email ?: throw AuthError.UserNotFound

            // Süper admin şifresini doğrula
            val credential = EmailAuthProvider.getCredential(superAdminEmail, superAdminPassword)
            currentUser.reauthenticate(credential).await()

            println("✅ Şifre doğrulandı, yeni admin oluşturuluyor...")

            // Yeni kullanıcı oluştur
            // Not: Android'de ikincil Firebase App kullanmak yerine
            // Firebase Admin SDK (backend) kullanılması önerilir
            // Şimdilik aynı auth instance kullanıyoruz
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val newUser = result.user ?: throw AuthError.UserCreationFailed
            val newUserId = newUser.uid

            // Profil güncelle
            val profileUpdates = userProfileChangeRequest {
                this.displayName = displayName
            }
            newUser.updateProfile(profileUpdates).await()

            // Admin kaydı oluştur (otomatik onaylı)
            val adminUser = AdminUser(
                email = email,
                displayName = displayName,
                isSuperAdmin = newAdminIsSuperAdmin
            ).copy(isApproved = true) // Süper admin tarafından oluşturulanlar otomatik onaylı

            saveAdminUserWithId(adminUser, newUserId)

            // Süper admin hesabına geri dön
            auth.signInWithEmailAndPassword(superAdminEmail, superAdminPassword).await()

            println("✅ Yeni admin oluşturuldu: $newUserId")
            Result.success(newUserId)
        } catch (e: Exception) {
            println("❌ Admin oluşturma hatası: ${e.message}")
            Result.failure(e)
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Permission Checks
    // ══════════════════════════════════════════════════════════════

    /** Süper admin yetkisi var mı? */
    val isSuperAdmin: Boolean
        get() = _currentAdminUser.value?.isSuperAdmin == true

    /** Admin yetkisi var mı? (onaylanmış) */
    val isApprovedAdmin: Boolean
        get() = _currentAdminUser.value?.isActive == true

    /** Belirli bir izin var mı kontrol et */
    fun hasPermission(permission: Permission): Boolean {
        val adminUser = _currentAdminUser.value ?: return false
        if (!adminUser.isActive) return false

        val userRole = if (adminUser.isSuperAdmin) UserRole.SUPER_ADMIN else UserRole.ADMIN
        return permission in userRole.permissions
    }

    /** Birden fazla izin var mı kontrol et */
    fun hasPermissions(permissions: List<Permission>): Boolean {
        return permissions.all { hasPermission(it) }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Validation Helpers
    // ══════════════════════════════════════════════════════════════

    /** E-posta validasyonu */
    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /** Şifre validasyonu */
    fun validatePassword(password: String): PasswordValidationResult {
        if (password.length < 6) {
            return PasswordValidationResult(false, "Şifre en az 6 karakter olmalıdır")
        }

        if (password.length > 50) {
            return PasswordValidationResult(false, "Şifre 50 karakterden uzun olamaz")
        }

        val hasLetter = password.any { it.isLetter() }
        val hasNumber = password.any { it.isDigit() }

        if (!hasLetter || !hasNumber) {
            return PasswordValidationResult(false, "Şifre en az bir harf ve bir rakam içermelidir")
        }

        return PasswordValidationResult(true, null)
    }

    /** Form validasyonu (kayıt için) */
    fun validateRegistrationForm(
        email: String,
        password: String,
        displayName: String
    ): RegistrationValidationResult {
        val errors = mutableListOf<String>()

        if (displayName.trim().isEmpty()) {
            errors.add("Ad soyad boş olamaz")
        }

        if (!isValidEmail(email)) {
            errors.add("Geçerli bir e-posta adresi giriniz")
        }

        val passwordValidation = validatePassword(password)
        if (!passwordValidation.isValid) {
            passwordValidation.message?.let { errors.add(it) }
        }

        return RegistrationValidationResult(errors.isEmpty(), errors)
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - Auth State Enum
// ══════════════════════════════════════════════════════════════

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data object SignedIn : AuthState()
    data object SignedOut : AuthState()
    data object PendingApproval : AuthState()
    data class Error(val message: String) : AuthState()

    val isLoading: Boolean
        get() = this is Loading

    val errorMessage: String?
        get() = (this as? Error)?.message
}

// ══════════════════════════════════════════════════════════════
// MARK: - Auth Errors
// ══════════════════════════════════════════════════════════════

sealed class AuthError(override val message: String) : Exception(message) {
    data object UserCreationFailed : AuthError("Kullanıcı oluşturulamadı")
    data object UserNotFound : AuthError("Kullanıcı bulunamadı")
    data object InvalidCredentials : AuthError("Geçersiz kullanıcı bilgileri veya şifre")
    data object NetworkError : AuthError("Ağ bağlantı hatası")
    data object UserNotApproved : AuthError("Kullanıcı henüz onaylanmamış")
}

// ══════════════════════════════════════════════════════════════
// MARK: - Validation Results
// ══════════════════════════════════════════════════════════════

data class PasswordValidationResult(
    val isValid: Boolean,
    val message: String?
)

data class RegistrationValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)