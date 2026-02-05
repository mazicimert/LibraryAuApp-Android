package com.mehmetmertmazici.libraryauapp.ui.books

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmetmertmazici.libraryauapp.data.model.BookCopy
import com.mehmetmertmazici.libraryauapp.data.model.BookTemplate
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
 * BookDetailViewModel
 * Kitap detay ve kopya yönetimi işlemleri
 *
 * iOS Karşılığı: BookDetailViewModel.swift
 */
@HiltViewModel
class BookDetailViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val authRepository: AuthRepository,
    private val networkManager: NetworkManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // ── UI State ──
    private val _uiState = MutableStateFlow(BookDetailUiState())
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    // ── Book Copies ──
    private val _bookCopies = MutableStateFlow<List<BookCopy>>(emptyList())
    val bookCopies: StateFlow<List<BookCopy>> = _bookCopies.asStateFlow()

    // ── Network State ──
    val isOnline: StateFlow<Boolean> = networkManager.isOnline

    // ══════════════════════════════════════════════════════════════
    // MARK: - Computed Properties
    // ══════════════════════════════════════════════════════════════

    val availableCopiesCount: Int
        get() = _bookCopies.value.count { it.isAvailable }

    val borrowedCopiesCount: Int
        get() = _bookCopies.value.count { !it.isAvailable }

    val canManageBooks: Boolean
        get() = authRepository.hasPermission(com.mehmetmertmazici.libraryauapp.data.model.Permission.MANAGE_BOOKS)

    // ══════════════════════════════════════════════════════════════
    // MARK: - Load Book Copies
    // ══════════════════════════════════════════════════════════════

    fun loadBookCopies(bookTemplateId: String) {
        if (!networkManager.isOnline.value) {
            // Offline durumunda cache'ten yükle
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            firebaseRepository.fetchBookCopies(bookTemplateId)
                .onSuccess { copies ->
                    _bookCopies.value = copies.sortedBy { it.copyNumber }
                    _uiState.update { it.copy(isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    showAlert("Yükleme Hatası", "Kopya bilgileri yüklenirken hata oluştu: ${error.message}")
                }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Delete Copy
    // ══════════════════════════════════════════════════════════════

    fun requestDeleteCopy(copy: BookCopy) {
        _uiState.update {
            it.copy(
                copyToDelete = copy,
                showDeleteConfirmation = true
            )
        }
    }

    fun confirmDeleteCopy() {
        val copy = _uiState.value.copyToDelete ?: return
        val copyId = copy.id ?: return

        _uiState.update { it.copy(showDeleteConfirmation = false) }

        viewModelScope.launch {
            firebaseRepository.deleteBookCopy(copyId)
                .onSuccess {
                    // Başarılı silme - listeyi güncelle
                    _bookCopies.update { copies ->
                        copies.filter { it.id != copyId }
                    }
                    _uiState.update { it.copy(copyToDelete = null) }
                    showAlert("Başarılı", "Kopya başarıyla silindi")
                }
                .onFailure { error ->
                    _uiState.update { it.copy(copyToDelete = null) }
                    showAlert("Silme Hatası", "Kopya silinirken hata oluştu: ${error.message}")
                }
        }
    }

    fun cancelDeleteCopy() {
        _uiState.update {
            it.copy(
                copyToDelete = null,
                showDeleteConfirmation = false
            )
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Navigation / Sheets
    // ══════════════════════════════════════════════════════════════

    fun openAddCopySheet() {
        _uiState.update { it.copy(showAddCopySheet = true) }
    }

    fun closeAddCopySheet() {
        _uiState.update { it.copy(showAddCopySheet = false) }
    }

    fun openEditSheet() {
        _uiState.update { it.copy(showEditSheet = true) }
    }

    fun closeEditSheet() {
        _uiState.update { it.copy(showEditSheet = false) }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Update Book
    // ══════════════════════════════════════════════════════════════

    fun updateCurrentBook(updatedBook: BookTemplate) {
        _uiState.update { it.copy(currentBook = updatedBook) }
    }

    fun setInitialBook(book: BookTemplate) {
        _uiState.update { it.copy(currentBook = book) }
        book.id?.let { loadBookCopies(it) }
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
 * Book Detail UI State
 */
data class BookDetailUiState(
    val isLoading: Boolean = false,
    val currentBook: BookTemplate? = null,

    // Sheets
    val showAddCopySheet: Boolean = false,
    val showEditSheet: Boolean = false,

    // Delete confirmation
    val showDeleteConfirmation: Boolean = false,
    val copyToDelete: BookCopy? = null,

    // Description expand
    val isDescriptionExpanded: Boolean = false,
    val isTitleExpanded: Boolean = false,

    // Alert
    val showAlert: Boolean = false,
    val alertTitle: String = "",
    val alertMessage: String = ""
)