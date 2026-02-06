package com.mehmetmertmazici.libraryauapp.ui.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmetmertmazici.libraryauapp.data.model.BookTemplate
import com.mehmetmertmazici.libraryauapp.data.repository.FirebaseRepository
import com.mehmetmertmazici.libraryauapp.data.repository.NetworkManager
import com.mehmetmertmazici.libraryauapp.domain.util.trimmed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BookEditViewModel
 * Kitap düzenleme işlemlerini yöneten ViewModel
 *
 * iOS Karşılığı: BookEditViewModel.swift
 */
@HiltViewModel
class BookEditViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val networkManager: NetworkManager
) : ViewModel() {

    // ── UI State ──
    private val _uiState = MutableStateFlow(BookEditUiState())
    val uiState: StateFlow<BookEditUiState> = _uiState.asStateFlow()

    // ══════════════════════════════════════════════════════════════
    // MARK: - Initialize
    // ══════════════════════════════════════════════════════════════

    fun initialize(book: BookTemplate) {
        _uiState.update {
            it.copy(
                originalBookId = book.id,
                title = book.title,
                author = book.author,
                isbn = book.isbn,
                publisher = book.publisher,
                editor = book.editor,
                category = book.category,
                description = book.description,
                isLoadingBook = false
            )
        }
        validateForm()
    }

    fun loadBookById(bookId: String) {
        if (bookId.isEmpty()) return

        _uiState.update { it.copy(isLoadingBook = true) }

        viewModelScope.launch {
            firebaseRepository.fetchBookTemplateById(bookId)
                .onSuccess { book ->
                    initialize(book)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoadingBook = false) }
                    showAlert("Hata", "Kitap bilgisi yüklenirken hata: ${error.message}")
                }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Form Field Updates
    // ══════════════════════════════════════════════════════════════

    fun updateTitle(value: String) {
        _uiState.update { it.copy(title = value) }
        validateForm()
    }

    fun updateAuthor(value: String) {
        _uiState.update { it.copy(author = value) }
        validateForm()
    }

    fun updateIsbn(value: String) {
        _uiState.update { it.copy(isbn = value) }
        validateForm()
    }

    fun updatePublisher(value: String) {
        _uiState.update { it.copy(publisher = value) }
        validateForm()
    }

    fun updateEditor(value: String) {
        _uiState.update { it.copy(editor = value) }
    }

    fun updateCategory(value: String) {
        _uiState.update { it.copy(category = value) }
    }

    fun updateDescription(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Validation
    // ══════════════════════════════════════════════════════════════

    private fun validateForm() {
        val state = _uiState.value
        val isValid = state.title.trimmed.isNotEmpty() &&
                state.author.trimmed.isNotEmpty() &&
                state.isbn.trimmed.isNotEmpty() &&
                state.publisher.trimmed.isNotEmpty()

        _uiState.update { it.copy(isFormValid = isValid) }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Update Book
    // ══════════════════════════════════════════════════════════════

    fun updateBook(onSuccess: (BookTemplate) -> Unit) {
        val state = _uiState.value

        if (!state.isFormValid) return

        if (!networkManager.isOnline.value) {
            showAlert("Bağlantı Hatası", "Düzenleme için internet bağlantısı gereklidir")
            return
        }

        val bookId = state.originalBookId
        if (bookId.isNullOrEmpty()) {
            showAlert("Hata", "Kitap ID'si bulunamadı")
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        val updatedBook = BookTemplate(
            id = bookId,
            title = state.title.trimmed,
            author = state.author.trimmed,
            isbn = state.isbn.trimmed,
            publisher = state.publisher.trimmed,
            editor = state.editor.trimmed,
            category = state.category.trimmed,
            description = state.description.trimmed
        )

        viewModelScope.launch {
            firebaseRepository.updateBookTemplate(updatedBook)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess(updatedBook)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    showAlert("Güncelleme Hatası", error.message ?: "Bilinmeyen hata")
                }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Alert
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
 * Book Edit UI State
 */
data class BookEditUiState(
    val originalBookId: String? = null,

    // Form fields
    val title: String = "",
    val author: String = "",
    val isbn: String = "",
    val publisher: String = "",
    val editor: String = "",
    val category: String = "",
    val description: String = "",

    // UI state
    val isLoadingBook: Boolean = false,
    val isLoading: Boolean = false,
    val isFormValid: Boolean = false,

    // Alert
    val showAlert: Boolean = false,
    val alertTitle: String = "",
    val alertMessage: String = ""
)