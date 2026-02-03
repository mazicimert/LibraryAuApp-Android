package com.mehmetmertmazici.libraryauapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmetmertmazici.libraryauapp.data.repository.AuthRepository
import com.mehmetmertmazici.libraryauapp.data.repository.AuthState
import com.mehmetmertmazici.libraryauapp.data.repository.NetworkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AuthViewModel
 * Authentication işlemlerini yöneten ViewModel
 *
 * iOS Karşılığı: AuthenticationViewModel.swift
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val networkManager: NetworkManager
) : ViewModel() {

    // ── UI State ──
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // ── Auth State ──
    val authState: StateFlow<AuthState> = authRepository.authState

    // ── Network State ──
    val isOnline: StateFlow<Boolean> = networkManager.isOnline

    // ══════════════════════════════════════════════════════════════
    // MARK: - Login Methods
    // ══════════════════════════════════════════════════════════════

    fun updateLoginEmail(email: String) {
        _uiState.update { it.copy(loginEmail = email) }
        validateLoginForm()
    }

    fun updateLoginPassword(password: String) {
        _uiState.update { it.copy(loginPassword = password) }
        validateLoginForm()
    }

    private fun validateLoginForm() {
        val state = _uiState.value
        val trimmedEmail = state.loginEmail.trim()
        val isValid = trimmedEmail.isNotEmpty() &&
                state.loginPassword.isNotEmpty() &&
                authRepository.isValidEmail(trimmedEmail) &&
                state.loginPassword.length >= 6

        _uiState.update { it.copy(isLoginFormValid = isValid) }
    }

    fun performLogin() {
        if (!networkManager.isOnline.value) {
            showAlert("Bağlantı Hatası", "İnternet bağlantısı gereklidir")
            return
        }

        val state = _uiState.value
        val trimmedEmail = state.loginEmail.trim()

        if (trimmedEmail.isEmpty() || state.loginPassword.isEmpty()) {
            showAlert("Form Hatası", "E-posta ve şifre boş olamaz")
            return
        }

        if (!authRepository.isValidEmail(trimmedEmail)) {
            showAlert("Form Hatası", "Lütfen geçerli bir e-posta adresi giriniz")
            return
        }

        _uiState.update { it.copy(isLoginLoading = true) }

        viewModelScope.launch {
            authRepository.signIn(trimmedEmail, state.loginPassword)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoginLoading = false,
                            loginEmail = "",
                            loginPassword = ""
                        )
                    }
                    println("✅ Giriş başarılı")
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoginLoading = false) }
                    showAlert("Giriş Hatası", mapAuthError(error))
                    println("❌ Giriş hatası: ${error.message}")
                }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Register Methods
    // ══════════════════════════════════════════════════════════════

    fun updateRegisterDisplayName(name: String) {
        _uiState.update { it.copy(registerDisplayName = name) }
        validateRegisterForm()
    }

    fun updateRegisterEmail(email: String) {
        _uiState.update { it.copy(registerEmail = email) }
        validateRegisterForm()
    }

    fun updateRegisterPassword(password: String) {
        _uiState.update { it.copy(registerPassword = password) }
        validateRegisterForm()
    }

    fun updateRegisterConfirmPassword(password: String) {
        _uiState.update { it.copy(registerConfirmPassword = password) }
        validateRegisterForm()
    }

    private fun validateRegisterForm() {
        val state = _uiState.value
        val errors = mutableListOf<String>()

        val trimmedName = state.registerDisplayName.trim()
        val trimmedEmail = state.registerEmail.trim()

        // Name validation
        if (trimmedName.isNotEmpty() && trimmedName.length < 2) {
            errors.add("Ad soyad en az 2 karakter olmalıdır")
        }

        // Email validation
        if (trimmedEmail.isNotEmpty() && !authRepository.isValidEmail(trimmedEmail)) {
            errors.add("Geçerli bir e-posta adresi giriniz")
        }

        // Password validation
        if (state.registerPassword.isNotEmpty()) {
            val passwordResult = authRepository.validatePassword(state.registerPassword)
            if (!passwordResult.isValid) {
                passwordResult.message?.let { errors.add(it) }
            }
        }

        // Confirm password validation
        if (state.registerConfirmPassword.isNotEmpty() &&
            state.registerPassword != state.registerConfirmPassword
        ) {
            errors.add("Şifreler eşleşmiyor")
        }

        val isValid = errors.isEmpty() &&
                trimmedName.isNotEmpty() &&
                trimmedEmail.isNotEmpty() &&
                state.registerPassword.isNotEmpty() &&
                state.registerConfirmPassword.isNotEmpty()

        _uiState.update {
            it.copy(
                registerValidationErrors = errors,
                isRegisterFormValid = isValid
            )
        }
    }

    fun performRegistration() {
        if (!networkManager.isOnline.value) {
            showAlert("Bağlantı Hatası", "İnternet bağlantısı gereklidir")
            return
        }

        if (!_uiState.value.isRegisterFormValid) {
            showAlert("Form Hatası", "Lütfen tüm hataları düzeltin")
            return
        }

        _uiState.update { it.copy(isRegisterLoading = true) }

        val state = _uiState.value

        viewModelScope.launch {
            authRepository.signUp(
                email = state.registerEmail.trim(),
                password = state.registerPassword,
                displayName = state.registerDisplayName.trim()
            )
                .onSuccess {
                    _uiState.update { it.copy(isRegisterLoading = false) }
                    showAlert(
                        "Kayıt Başarılı",
                        "Hesabınız oluşturuldu. Süper admin onayından sonra giriş yapabilirsiniz."
                    )
                    clearRegisterForm()
                    println("✅ Kayıt başarılı")
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isRegisterLoading = false) }
                    showAlert("Kayıt Hatası", mapAuthError(error))
                    println("❌ Kayıt hatası: ${error.message}")
                }
        }
    }

    private fun clearRegisterForm() {
        _uiState.update {
            it.copy(
                registerDisplayName = "",
                registerEmail = "",
                registerPassword = "",
                registerConfirmPassword = "",
                registerValidationErrors = emptyList()
            )
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Password Reset
    // ══════════════════════════════════════════════════════════════

    fun updateResetEmail(email: String) {
        _uiState.update { it.copy(resetEmail = email) }
    }

    fun sendPasswordReset() {
        if (!networkManager.isOnline.value) {
            showAlert("Bağlantı Hatası", "İnternet bağlantısı gereklidir")
            return
        }

        val email = _uiState.value.resetEmail.trim()
        if (email.isEmpty() || !authRepository.isValidEmail(email)) {
            showAlert("Form Hatası", "Geçerli bir e-posta adresi giriniz")
            return
        }

        _uiState.update { it.copy(isResetLoading = true) }

        viewModelScope.launch {
            authRepository.sendPasswordResetEmail(email)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isResetLoading = false,
                            resetEmail = ""
                        )
                    }
                    showAlert(
                        "E-posta Gönderildi",
                        "Şifre sıfırlama bağlantısı e-posta adresinize gönderildi."
                    )
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isResetLoading = false) }
                    showAlert("Hata", mapAuthError(error))
                }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Logout
    // ══════════════════════════════════════════════════════════════

    fun performLogout() {
        authRepository.signOut()
        _uiState.update { AuthUiState() } // Reset all state
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Alert Handling
    // ══════════════════════════════════════════════════════════════

    private fun showAlert(title: String, message: String) {
        _uiState.update {
            it.copy(
                showAlert = true,
                alertTitle = title,
                alertMessage = message
            )
        }
    }

    fun dismissAlert() {
        _uiState.update {
            it.copy(
                showAlert = false,
                alertTitle = "",
                alertMessage = ""
            )
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Error Mapping
    // ══════════════════════════════════════════════════════════════

    private fun mapAuthError(error: Throwable): String {
        val message = error.message?.lowercase() ?: ""

        return when {
            message.contains("invalid") || message.contains("credential") ||
                    message.contains("user-not-found") || message.contains("wrong-password") ->
                "E-posta veya şifre hatalı. Lütfen bilgilerinizi kontrol edin"

            message.contains("email") && message.contains("format") ->
                "Geçersiz e-posta adresi formatı"

            message.contains("disabled") ->
                "Bu hesap devre dışı bırakılmış"

            message.contains("weak") ->
                "Şifre çok zayıf. En az 6 karakter, harf ve rakam içermelidir"

            message.contains("already") && message.contains("use") ->
                "Bu e-posta adresi zaten kullanımda"

            message.contains("network") ->
                "Ağ bağlantısı hatası. Lütfen internetinizi kontrol edin"

            message.contains("too-many") ->
                "Çok fazla başarısız deneme. Lütfen daha sonra tekrar deneyin"

            else -> "Bir hata oluştu. Lütfen tekrar deneyin"
        }
    }
}

/**
 * Auth UI State
 */
data class AuthUiState(
    // Login
    val loginEmail: String = "",
    val loginPassword: String = "",
    val isLoginLoading: Boolean = false,
    val isLoginFormValid: Boolean = false,

    // Register
    val registerDisplayName: String = "",
    val registerEmail: String = "",
    val registerPassword: String = "",
    val registerConfirmPassword: String = "",
    val isRegisterLoading: Boolean = false,
    val isRegisterFormValid: Boolean = false,
    val registerValidationErrors: List<String> = emptyList(),

    // Password Reset
    val resetEmail: String = "",
    val isResetLoading: Boolean = false,

    // Alert
    val showAlert: Boolean = false,
    val alertTitle: String = "",
    val alertMessage: String = ""
)