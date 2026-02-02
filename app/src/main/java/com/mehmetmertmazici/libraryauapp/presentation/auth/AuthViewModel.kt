package com.mehmetmertmazici.libraryauapp.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmetmertmazici.libraryauapp.data.repository.AuthRepository
import com.mehmetmertmazici.libraryauapp.domain.model.AdminUser
import com.mehmetmertmazici.libraryauapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AuthViewModel
 * 
 * Manages authentication state and operations.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.getCurrentAdminUser().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        val adminUser = result.data
                        val authState = when {
                            adminUser == null -> AuthState.SignedOut
                            adminUser.isActive -> AuthState.SignedIn(adminUser)
                            else -> AuthState.PendingApproval(adminUser)
                        }
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                authState = authState,
                                currentUser = adminUser
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                authState = AuthState.SignedOut,
                                error = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    // region Form State Updates

    fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
    }

    fun updateConfirmPassword(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, confirmPasswordError = null) }
    }

    fun updateDisplayName(displayName: String) {
        _uiState.update { it.copy(displayName = displayName, displayNameError = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // endregion

    // region Authentication Operations

    fun signIn() {
        val state = _uiState.value
        
        // Validation
        var hasError = false
        
        if (state.email.isBlank()) {
            _uiState.update { it.copy(emailError = "E-posta adresi gerekli") }
            hasError = true
        } else if (!isValidEmail(state.email)) {
            _uiState.update { it.copy(emailError = "Geçerli bir e-posta adresi girin") }
            hasError = true
        }
        
        if (state.password.isBlank()) {
            _uiState.update { it.copy(passwordError = "Şifre gerekli") }
            hasError = true
        }
        
        if (hasError) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            when (val result = authRepository.signIn(state.email, state.password)) {
                is Resource.Success -> {
                    // Auth state will be updated by observeAuthState
                    _uiState.update { it.copy(isLoading = false) }
                }
                is Resource.Error -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                is Resource.Loading -> { /* Handled above */ }
            }
        }
    }

    fun signUp() {
        val state = _uiState.value
        
        // Validation
        var hasError = false
        
        if (state.displayName.isBlank()) {
            _uiState.update { it.copy(displayNameError = "Ad soyad gerekli") }
            hasError = true
        }
        
        if (state.email.isBlank()) {
            _uiState.update { it.copy(emailError = "E-posta adresi gerekli") }
            hasError = true
        } else if (!isValidEmail(state.email)) {
            _uiState.update { it.copy(emailError = "Geçerli bir e-posta adresi girin") }
            hasError = true
        }
        
        val passwordValidation = validatePassword(state.password)
        if (!passwordValidation.isValid) {
            _uiState.update { it.copy(passwordError = passwordValidation.message) }
            hasError = true
        }
        
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(confirmPasswordError = "Şifreler eşleşmiyor") }
            hasError = true
        }
        
        if (hasError) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            when (val result = authRepository.signUp(state.email, state.password, state.displayName)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
                is Resource.Error -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                is Resource.Loading -> { /* Handled above */ }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            when (val result = authRepository.signOut()) {
                is Resource.Success -> {
                    _uiState.update { 
                        AuthUiState() // Reset to initial state
                    }
                }
                is Resource.Error -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                is Resource.Loading -> { /* Handled above */ }
            }
        }
    }

    fun resetPassword() {
        val email = _uiState.value.email
        
        if (email.isBlank()) {
            _uiState.update { it.copy(emailError = "E-posta adresi gerekli") }
            return
        }
        
        if (!isValidEmail(email)) {
            _uiState.update { it.copy(emailError = "Geçerli bir e-posta adresi girin") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            when (val result = authRepository.resetPassword(email)) {
                is Resource.Success -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            successMessage = "Şifre sıfırlama e-postası gönderildi"
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                is Resource.Loading -> { /* Handled above */ }
            }
        }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    // endregion

    // region Validation Helpers

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}")
        return emailRegex.matches(email)
    }

    private fun validatePassword(password: String): PasswordValidation {
        if (password.length < 6) {
            return PasswordValidation(false, "Şifre en az 6 karakter olmalı")
        }
        if (password.length > 50) {
            return PasswordValidation(false, "Şifre 50 karakterden uzun olamaz")
        }
        if (!password.any { it.isLetter() }) {
            return PasswordValidation(false, "Şifre en az bir harf içermeli")
        }
        if (!password.any { it.isDigit() }) {
            return PasswordValidation(false, "Şifre en az bir rakam içermeli")
        }
        return PasswordValidation(true, null)
    }

    // endregion
}

// region UI State

data class AuthUiState(
    val isLoading: Boolean = true, // Start with loading to check auth state
    val authState: AuthState = AuthState.SignedOut,
    val currentUser: AdminUser? = null,
    
    // Form fields
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val displayName: String = "",
    
    // Field errors
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val displayNameError: String? = null,
    
    // General messages
    val error: String? = null,
    val successMessage: String? = null
)

sealed class AuthState {
    object SignedOut : AuthState()
    data class SignedIn(val user: AdminUser) : AuthState()
    data class PendingApproval(val user: AdminUser) : AuthState()
}

private data class PasswordValidation(
    val isValid: Boolean,
    val message: String?
)

// endregion
