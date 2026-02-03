package com.mehmetmertmazici.libraryauapp.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmetmertmazici.libraryauapp.data.repository.AuthRepository
import com.mehmetmertmazici.libraryauapp.data.repository.AuthState
import com.mehmetmertmazici.libraryauapp.data.repository.FirebaseRepository
import com.mehmetmertmazici.libraryauapp.data.repository.NetworkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AppNavigationViewModel
 * Ana navigasyon için ViewModel - Auth ve network durumunu yönetir
 *
 * iOS Karşılığı: ContentView.swift + MainTabView.swift state yönetimi
 */
@HiltViewModel
class AppNavigationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firebaseRepository: FirebaseRepository,
    private val networkManager: NetworkManager
) : ViewModel() {

    // ── Auth State ──
    val authState: StateFlow<AuthState> = authRepository.authState

    // ── Network State ──
    val isOnline: StateFlow<Boolean> = networkManager.isOnline

    // ── Super Admin Status ──
    private val _isSuperAdmin = MutableStateFlow(false)
    val isSuperAdmin: StateFlow<Boolean> = _isSuperAdmin.asStateFlow()

    // ── Pending Admin Count ──
    private val _pendingAdminCount = MutableStateFlow(0)
    val pendingAdminCount: StateFlow<Int> = _pendingAdminCount.asStateFlow()

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                when (state) {
                    is AuthState.SignedIn -> {
                        _isSuperAdmin.value = authRepository.isSuperAdmin

                        // Süper admin ise pending admins'i dinle
                        if (authRepository.isSuperAdmin) {
                            observePendingAdmins()
                        }

                        // İlk veri yükleme kontrolü
                        checkAndLoadInitialData()
                    }
                    else -> {
                        _isSuperAdmin.value = false
                        _pendingAdminCount.value = 0
                    }
                }
            }
        }
    }

    private fun observePendingAdmins() {
        viewModelScope.launch {
            firebaseRepository.listenToPendingAdmins().collect { admins ->
                _pendingAdminCount.value = admins.size
            }
        }
    }

    private fun checkAndLoadInitialData() {
        viewModelScope.launch {
            if (!networkManager.isOnline.value) {
                println("⚠️ İnternet yok, veri kontrolü yapılamadı.")
                return@launch
            }

            println("🔍 Firebase'de kitap kontrolü yapılıyor...")

            firebaseRepository.fetchBookTemplates()
                .onSuccess { books ->
                    if (books.isEmpty()) {
                        println("📚 Database boş, JSON'dan kitaplar yükleniyor...")
                        loadBooksFromJSON()
                    } else {
                        println("✅ Database'de ${books.size} kitap mevcut")
                    }
                }
                .onFailure { error ->
                    println("❌ Kitap kontrolü hatası: ${error.message}")
                }
        }
    }

    private fun loadBooksFromJSON() {
        viewModelScope.launch {
            firebaseRepository.loadBooksFromJSON()
                .onSuccess {
                    println("✅ Kitaplar JSON'dan başarıyla yüklendi")
                    firebaseRepository.completeFirstLaunchSetup()
                }
                .onFailure { error ->
                    println("❌ JSON yükleme hatası: ${error.message}")
                }
        }
    }

    fun retry() {
        viewModelScope.launch {
            val currentUser = authRepository.currentUser.value
            if (currentUser != null) {
                authRepository.loadAdminUserData(currentUser.uid)
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}