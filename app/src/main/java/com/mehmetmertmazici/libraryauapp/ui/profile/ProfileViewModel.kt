package com.mehmetmertmazici.libraryauapp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmetmertmazici.libraryauapp.data.model.AdminUser
import com.mehmetmertmazici.libraryauapp.data.repository.AuthRepository
import com.mehmetmertmazici.libraryauapp.data.repository.FirebaseRepository
import com.mehmetmertmazici.libraryauapp.data.repository.NetworkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ProfileViewModel
 * Profil ekranı işlemlerini yöneten ViewModel
 *
 * iOS Karşılığı: AuthenticationViewModel.swift (profil bölümü)
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val authRepository: AuthRepository,
    private val networkManager: NetworkManager
) : ViewModel() {

    // ── UI State ──
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // ── Current User ──
    val currentAdminUser: StateFlow<AdminUser?> = authRepository.currentAdminUser

    // ── Network State ──
    val isOnline: StateFlow<Boolean> = networkManager.isOnline

    // ── Computed Properties ──
    val isSuperAdmin: Boolean
        get() = authRepository.isSuperAdmin

    val syncStatusInfo: String
        get() = networkManager.syncStatusInfo

    // ══════════════════════════════════════════════════════════════
    // MARK: - Logout
    // ══════════════════════════════════════════════════════════════

    fun showLogoutConfirmation() {
        _uiState.update { it.copy(showLogoutConfirmation = true) }
    }

    fun dismissLogoutConfirmation() {
        _uiState.update { it.copy(showLogoutConfirmation = false) }
    }

    fun performLogout() {
        _uiState.update { it.copy(isLoggingOut = true, showLogoutConfirmation = false) }
        authRepository.signOut()
        _uiState.update { ProfileUiState() }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Delete Account
    // ══════════════════════════════════════════════════════════════

    fun checkAndShowDeleteConfirmation() {
        if (!networkManager.isOnline.value) return

        _uiState.update { it.copy(isCheckingDeletion = true) }

        viewModelScope.launch {
            firebaseRepository.fetchSuperAdminCount()
                .onSuccess { count ->
                    val canDelete = if (authRepository.isSuperAdmin) count > 1 else true
                    _uiState.update {
                        it.copy(
                            isCheckingDeletion = false,
                            canDeleteAccount = canDelete,
                            showDeleteConfirmation = true
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isCheckingDeletion = false) }
                    showAlert("Hata", "Silme kontrolü sırasında hata oluştu: ${error.message}")
                    println("❌ Silme kontrolü hatası: ${error.message}")
                }
        }
    }

    fun dismissDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun showPasswordPrompt() {
        _uiState.update { it.copy(showDeleteConfirmation = false, showPasswordPrompt = true) }
    }

    fun dismissPasswordPrompt() {
        _uiState.update { it.copy(showPasswordPrompt = false, deleteAccountPassword = "") }
    }

    fun updateDeletePassword(password: String) {
        _uiState.update { it.copy(deleteAccountPassword = password) }
    }

    fun performDeleteAccount() {
        val password = _uiState.value.deleteAccountPassword
        if (password.isEmpty()) return

        _uiState.update { it.copy(showPasswordPrompt = false, isLoggingOut = true) }

        viewModelScope.launch {
            authRepository.deleteAccount(password)
                .onSuccess {
                    _uiState.update { ProfileUiState() }
                    println("✅ Hesap başarıyla silindi")
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoggingOut = false, deleteAccountPassword = "")
                    }
                    showAlert("Hata", "Hesap silinirken hata oluştu. Şifrenizi kontrol edin.")
                    println("❌ Hesap silme hatası: ${error.message}")
                }
        }
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
}

/**
 * Profile UI State
 */
data class ProfileUiState(
    val isLoggingOut: Boolean = false,
    val isCheckingDeletion: Boolean = false,
    val canDeleteAccount: Boolean = true,
    val showLogoutConfirmation: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val showPasswordPrompt: Boolean = false,
    val deleteAccountPassword: String = "",
    // Alert
    val showAlert: Boolean = false,
    val alertTitle: String = "",
    val alertMessage: String = ""
)
