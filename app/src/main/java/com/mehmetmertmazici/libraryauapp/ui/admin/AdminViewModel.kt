package com.mehmetmertmazici.libraryauapp.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmetmertmazici.libraryauapp.data.model.AdminUser
import com.mehmetmertmazici.libraryauapp.data.model.LoadingState
import com.mehmetmertmazici.libraryauapp.data.model.Permission
import com.mehmetmertmazici.libraryauapp.data.repository.AuthRepository
import com.mehmetmertmazici.libraryauapp.data.repository.FirebaseRepository
import com.mehmetmertmazici.libraryauapp.data.repository.NetworkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * AdminViewModel
 * Admin kullanıcı yönetimi işlemlerini yöneten ViewModel (Sadece Süper Admin için)
 *
 * iOS Karşılığı: AdminViewModel.swift
 */
@HiltViewModel
class AdminViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val authRepository: AuthRepository,
    private val networkManager: NetworkManager
) : ViewModel() {

    // ── UI State ──
    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    // ── Network State ──
    val isOnline: StateFlow<Boolean> = networkManager.isOnline

    // ══════════════════════════════════════════════════════════════
    // MARK: - Computed Properties
    // ══════════════════════════════════════════════════════════════

    val currentUserId: String?
        get() = authRepository.currentUser.value?.uid

    val canManageAdmins: Boolean
        get() = authRepository.isSuperAdmin && networkManager.isOnline.value

    val pendingAdminCount: Int
        get() = _uiState.value.pendingAdmins.size

    val hasPendingApprovals: Boolean
        get() = _uiState.value.pendingAdmins.isNotEmpty()

    val isAddAdminFormValid: Boolean
        get() {
            val state = _uiState.value
            return state.newAdminEmail.trim().isNotEmpty() &&
                    state.newAdminDisplayName.trim().isNotEmpty() &&
                    state.newAdminPassword.trim().isNotEmpty() &&
                    authRepository.isValidEmail(state.newAdminEmail) &&
                    state.newAdminPassword.length >= 6
        }

    val addAdminFormErrors: List<String>
        get() {
            val state = _uiState.value
            val errors = mutableListOf<String>()

            if (state.newAdminDisplayName.trim().isEmpty()) {
                errors.add("Ad soyad boş olamaz")
            }

            if (!authRepository.isValidEmail(state.newAdminEmail)) {
                errors.add("Geçerli bir e-posta adresi giriniz")
            }

            val passwordValidation = authRepository.validatePassword(state.newAdminPassword)
            if (!passwordValidation.isValid) {
                passwordValidation.message?.let { errors.add(it) }
            }

            return errors
        }

    val showLoadingIndicator: Boolean
        get() = _uiState.value.isLoading || _uiState.value.loadingState.isLoading

    val showEmptyState: Boolean
        get() {
            val state = _uiState.value
            return !state.isLoading && state.adminUsers.isEmpty() && state.loadingState != LoadingState.Loading
        }

    val emptyStateMessage: String
        get() = "Henüz admin kullanıcı eklenmemiş"

    val pendingApprovalNotification: String?
        get() {
            val count = pendingAdminCount
            if (count <= 0) return null
            return if (count == 1) "1 admin onay bekliyor" else "$count admin onay bekliyor"
        }

    val adminStatistics: AdminStatistics
        get() {
            val state = _uiState.value
            val total = state.adminUsers.size
            val pending = state.pendingAdmins.size
            val approved = state.approvedAdmins.size
            val superAdmins = state.adminUsers.count { it.isSuperAdmin }
            val regularAdmins = approved - superAdmins
            return AdminStatistics(
                total = total,
                pending = pending,
                approved = approved,
                superAdmins = superAdmins,
                regularAdmins = regularAdmins
            )
        }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Init
    // ══════════════════════════════════════════════════════════════

    init {
        if (authRepository.isSuperAdmin) {
            startListeningToPendingAdmins()
            loadAdminUsers()
        }
        observeNetworkChanges()
    }

    private fun observeNetworkChanges() {
        viewModelScope.launch {
            networkManager.isOnline.collect { isOnline ->
                if (isOnline && _uiState.value.adminUsers.isEmpty() && authRepository.isSuperAdmin) {
                    loadAdminUsers()
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Pending Admins Listener
    // ══════════════════════════════════════════════════════════════

    private fun startListeningToPendingAdmins() {
        if (!authRepository.isSuperAdmin) return

        viewModelScope.launch {
            firebaseRepository.listenToPendingAdmins().collect { admins ->
                _uiState.update { it.copy(pendingAdmins = admins) }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Data Loading
    // ══════════════════════════════════════════════════════════════

    fun loadAdminUsers() {
        if (!networkManager.isOnline.value) {
            _uiState.update { it.copy(loadingState = LoadingState.Error("İnternet bağlantısı gereklidir")) }
            return
        }

        if (!authRepository.isSuperAdmin) {
            _uiState.update {
                it.copy(loadingState = LoadingState.Error("Sadece süper adminler bu bilgileri görüntüleyebilir"))
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, loadingState = LoadingState.Loading) }

        viewModelScope.launch {
            // Paralel yükleme
            val allAdminsDeferred = async { firebaseRepository.fetchAdminUsers() }
            val pendingAdminsDeferred = async { firebaseRepository.fetchPendingAdmins() }

            val allAdminsResult = allAdminsDeferred.await()
            val pendingAdminsResult = pendingAdminsDeferred.await()

            if (allAdminsResult.isSuccess && pendingAdminsResult.isSuccess) {
                val allAdmins = allAdminsResult.getOrDefault(emptyList())
                val pendingAdmins = pendingAdminsResult.getOrDefault(emptyList())
                val approvedAdmins = allAdmins.filter { it.isApproved }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingState = LoadingState.Success,
                        adminUsers = allAdmins,
                        pendingAdmins = pendingAdmins,
                        approvedAdmins = approvedAdmins
                    )
                }
                println("✅ Admin verileri yüklendi: ${allAdmins.size} admin, ${pendingAdmins.size} onay bekliyor")
            } else {
                val error =
                    allAdminsResult.exceptionOrNull() ?: pendingAdminsResult.exceptionOrNull()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingState = LoadingState.Error(error?.message ?: "Bilinmeyen hata")
                    )
                }
                showAlert(
                    "Yükleme Hatası",
                    "Admin bilgileri yüklenirken hata oluştu: ${error?.message}"
                )
            }
        }
    }

    fun refreshAdminUsers() {
        loadAdminUsers()
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Admin Approval Management
    // ══════════════════════════════════════════════════════════════

    fun approveAdmin(admin: AdminUser) {
        if (!networkManager.isOnline.value) {
            showAlert("Bağlantı Hatası", "Onaylama için internet bağlantısı gereklidir")
            return
        }

        if (!authRepository.isSuperAdmin) {
            showAlert("Yetki Hatası", "Sadece süper adminler onaylama yapabilir")
            return
        }

        val adminId = admin.id
        if (adminId == null) {
            showAlert("Hata", "Admin ID'si bulunamadı")
            return
        }

        viewModelScope.launch {
            firebaseRepository.approveAdmin(adminId)
                .onSuccess {
                    loadAdminUsers()
                    showAlert("Onaylama Başarılı", "${admin.displayName} başarıyla onaylandı")
                    println("✅ Admin onaylandı: ${admin.displayName}")
                }
                .onFailure { error ->
                    showAlert(
                        "Onaylama Hatası",
                        "Admin onaylanırken hata oluştu: ${error.message}"
                    )
                }
        }
    }

    fun performRejectAdmin(admin: AdminUser) {
        val adminId = admin.id
        if (adminId == null) {
            showAlert("Hata", "Admin ID'si bulunamadı")
            return
        }

        viewModelScope.launch {
            firebaseRepository.deleteAdminUser(adminId)
                .onSuccess {
                    loadAdminUsers()
                    showAlert("Başvuru Reddedildi", "${admin.displayName} reddedildi ve silindi")
                    println("✅ Admin reddedildi: ${admin.displayName}")
                }
                .onFailure { error ->
                    showAlert(
                        "Silme Hatası",
                        "Admin silinirken hata oluştu: ${error.message}"
                    )
                }
        }
    }

    fun performBulkApproval() {
        val pending = _uiState.value.pendingAdmins
        if (pending.isEmpty()) {
            showAlert("Onay Bekleyen Yok", "Onay bekleyen admin bulunmuyor")
            return
        }

        viewModelScope.launch {
            val results = pending.mapNotNull { admin ->
                val adminId = admin.id ?: return@mapNotNull null
                async { firebaseRepository.approveAdmin(adminId) }
            }.awaitAll()

            val failureCount = results.count { it.isFailure }
            if (failureCount > 0) {
                showAlert(
                    "Toplu Onaylama Hatası",
                    "Bazı adminler onaylanırken hata oluştu"
                )
            } else {
                val count = pending.size
                showAlert("Toplu Onaylama Başarılı", "$count admin başarıyla onaylandı")
            }
            loadAdminUsers()
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Admin Role Update
    // ══════════════════════════════════════════════════════════════

    fun updateAdminRole(admin: AdminUser, isSuperAdmin: Boolean) {
        if (!networkManager.isOnline.value) {
            showAlert("Bağlantı Hatası", "Güncelleme için internet bağlantısı gereklidir")
            return
        }

        if (!authRepository.isSuperAdmin) {
            showAlert("Yetki Hatası", "Sadece süper adminler rol değişikliği yapabilir")
            return
        }

        val updatedAdmin = admin.withUpdatedRole(isSuperAdmin)

        viewModelScope.launch {
            firebaseRepository.saveAdminUser(updatedAdmin)
                .onSuccess {
                    loadAdminUsers()
                    val newRole = if (isSuperAdmin) "Süper Admin" else "Admin"
                    showAlert("Rol Güncellendi", "${admin.displayName} artık $newRole")
                }
                .onFailure { error ->
                    showAlert(
                        "Güncelleme Hatası",
                        "Admin rolü güncellenirken hata oluştu: ${error.message}"
                    )
                }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Add New Admin
    // ══════════════════════════════════════════════════════════════

    fun openAddAdminForm() {
        clearAddAdminForm()
        _uiState.update { it.copy(showAddAdminSheet = true) }
    }

    fun closeAddAdminSheet() {
        _uiState.update { it.copy(showAddAdminSheet = false) }
    }

    fun initiateAddNewAdmin() {
        if (!networkManager.isOnline.value) {
            showAlert("Bağlantı Hatası", "Admin eklemek için internet bağlantısı gereklidir")
            return
        }

        if (!authRepository.isSuperAdmin) {
            showAlert("Yetki Hatası", "Sadece süper adminler yeni admin ekleyebilir")
            return
        }

        if (!isAddAdminFormValid) {
            val errors = addAdminFormErrors
            showAlert("Form Hatası", errors.joinToString("\n"))
            return
        }

        // Email benzersizliği kontrolü
        val state = _uiState.value
        if (state.adminUsers.any { it.email == state.newAdminEmail.trim() }) {
            showAlert("Email Zaten Kullanımda", "Bu e-posta adresi zaten kayıtlı")
            return
        }

        // Şifre doğrulama dialogu göster
        _uiState.update { it.copy(superAdminPassword = "", showPasswordVerification = true) }
    }

    fun addNewAdmin() {
        val state = _uiState.value
        if (state.superAdminPassword.isEmpty()) {
            showAlert("Şifre Gerekli", "Lütfen şifrenizi girin")
            return
        }

        _uiState.update { it.copy(isAddingAdmin = true) }

        viewModelScope.launch {
            authRepository.createAdminBySuperAdmin(
                email = state.newAdminEmail.trim(),
                password = state.newAdminPassword,
                displayName = state.newAdminDisplayName.trim(),
                superAdminPassword = state.superAdminPassword,
                newAdminIsSuperAdmin = state.newAdminIsSuperAdmin
            )
                .onSuccess { newAdminId ->
                    val adminDisplayName = state.newAdminDisplayName.trim()
                    val adminType = if (state.newAdminIsSuperAdmin) "Süper Admin" else "Admin"

                    clearAddAdminForm()
                    _uiState.update {
                        it.copy(
                            isAddingAdmin = false,
                            showPasswordVerification = false,
                            showAddAdminSheet = false
                        )
                    }

                    // Liste güncellenene kadar kısa bir gecikme
                    delay(500)
                    loadAdminUsers()

                    delay(300)
                    showAlert(
                        "Ekleme Başarılı",
                        "$adminDisplayName ($adminType) başarıyla eklendi"
                    )

                    println("✅ Yeni admin oluşturuldu: $newAdminId")
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isAddingAdmin = false,
                            showPasswordVerification = false
                        )
                    }
                    showAlert("Ekleme Hatası", error.message ?: "Bilinmeyen hata")
                }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Form Management
    // ══════════════════════════════════════════════════════════════

    fun clearAddAdminForm() {
        _uiState.update {
            it.copy(
                newAdminEmail = "",
                newAdminDisplayName = "",
                newAdminPassword = "",
                newAdminIsSuperAdmin = false,
                superAdminPassword = ""
            )
        }
    }

    fun updateNewAdminEmail(email: String) {
        _uiState.update { it.copy(newAdminEmail = email) }
    }

    fun updateNewAdminDisplayName(name: String) {
        _uiState.update { it.copy(newAdminDisplayName = name) }
    }

    fun updateNewAdminPassword(password: String) {
        _uiState.update { it.copy(newAdminPassword = password) }
    }

    fun updateNewAdminIsSuperAdmin(isSuperAdmin: Boolean) {
        _uiState.update { it.copy(newAdminIsSuperAdmin = isSuperAdmin) }
    }

    fun updateSuperAdminPassword(password: String) {
        _uiState.update { it.copy(superAdminPassword = password) }
    }

    fun dismissPasswordVerification() {
        _uiState.update { it.copy(showPasswordVerification = false, superAdminPassword = "") }
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

// ══════════════════════════════════════════════════════════════
// MARK: - UI State
// ══════════════════════════════════════════════════════════════

data class AdminUiState(
    val isLoading: Boolean = false,
    val loadingState: LoadingState = LoadingState.Idle,
    val adminUsers: List<AdminUser> = emptyList(),
    val pendingAdmins: List<AdminUser> = emptyList(),
    val approvedAdmins: List<AdminUser> = emptyList(),
    // Add Admin form
    val showAddAdminSheet: Boolean = false,
    val newAdminEmail: String = "",
    val newAdminDisplayName: String = "",
    val newAdminPassword: String = "",
    val newAdminIsSuperAdmin: Boolean = false,
    val isAddingAdmin: Boolean = false,
    // Password verification
    val superAdminPassword: String = "",
    val showPasswordVerification: Boolean = false,
    // Alert
    val showAlert: Boolean = false,
    val alertTitle: String = "",
    val alertMessage: String = ""
)

// ══════════════════════════════════════════════════════════════
// MARK: - Admin Statistics
// ══════════════════════════════════════════════════════════════

data class AdminStatistics(
    val total: Int,
    val pending: Int,
    val approved: Int,
    val superAdmins: Int,
    val regularAdmins: Int
) {
    val approvalRate: Double
        get() = if (total > 0) approved.toDouble() / total * 100 else 0.0

    val pendingRate: Double
        get() = if (total > 0) pending.toDouble() / total * 100 else 0.0

    val summaryText: String
        get() = "Toplam: $total, Onaylı: $approved, Bekleyen: $pending"
}
