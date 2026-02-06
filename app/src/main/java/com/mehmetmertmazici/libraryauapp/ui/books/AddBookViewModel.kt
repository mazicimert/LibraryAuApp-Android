package com.mehmetmertmazici.libraryauapp.ui.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmetmertmazici.libraryauapp.data.model.BookTemplate
import com.mehmetmertmazici.libraryauapp.data.repository.FirebaseRepository
import com.mehmetmertmazici.libraryauapp.data.repository.ISBNLookupRepository
import com.mehmetmertmazici.libraryauapp.data.repository.NetworkManager
import com.mehmetmertmazici.libraryauapp.domain.util.BarcodeGenerator
import com.mehmetmertmazici.libraryauapp.domain.util.isValidISBN
import com.mehmetmertmazici.libraryauapp.domain.util.trimmed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AddBookViewModel
 * Yeni kitap ekleme işlemlerini yöneten ViewModel
 *
 * iOS Karşılığı: BookListViewModel.swift içindeki add book işlemleri
 */
@HiltViewModel
class AddBookViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val isbnLookupRepository: ISBNLookupRepository,
    private val networkManager: NetworkManager
) : ViewModel() {

    // ── UI State ──
    private val _uiState = MutableStateFlow(AddBookUiState())
    val uiState: StateFlow<AddBookUiState> = _uiState.asStateFlow()

    init {
        loadNextBookId()
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
        _uiState.update {
            it.copy(
                isbn = value,
                isValidIsbn = value.isValidISBN
            )
        }
        validateForm()

        // Auto-fetch book info when valid ISBN
        val cleanIsbn = value.replace("-", "")
        if ((cleanIsbn.length == 10 || cleanIsbn.length == 13) && _uiState.value.title.isEmpty()) {
            fetchBookInfoFromIsbn(value)
        }
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

    fun updateCopyCount(value: Int) {
        _uiState.update { it.copy(initialCopyCount = value.coerceIn(1, 10)) }
    }

    fun toggleAdvancedOptions() {
        _uiState.update { it.copy(showAdvancedOptions = !it.showAdvancedOptions) }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - ISBN Lookup
    // ══════════════════════════════════════════════════════════════

    private fun fetchBookInfoFromIsbn(isbn: String) {
        if (isbn.isEmpty()) return

        _uiState.update { it.copy(isFetchingBookInfo = true) }

        viewModelScope.launch {
            try {
                val bookInfo = isbnLookupRepository.fetchBookInfo(isbn)
                _uiState.update { state ->
                    state.copy(
                        isFetchingBookInfo = false,
                        title = bookInfo?.title ?: state.title,
                        author = bookInfo?.author ?: state.author,
                        publisher = bookInfo?.publisher ?: state.publisher,
                        description = bookInfo?.description ?: state.description,
                        category = bookInfo?.category ?: state.category
                    )
                }
                validateForm()
            } catch (e: Exception) {
                _uiState.update { it.copy(isFetchingBookInfo = false) }
            }
        }
    }

    fun onBarcodeScanned(barcode: String) {
        updateIsbn(barcode)
        fetchBookInfoFromIsbn(barcode)
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Load Next Book ID
    // ══════════════════════════════════════════════════════════════

    private fun loadNextBookId() {
        viewModelScope.launch {
            firebaseRepository.getNextBookId()
                .onSuccess { nextId ->
                    _uiState.update { it.copy(nextBookId = nextId) }
                }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Validation
    // ══════════════════════════════════════════════════════════════

    private fun validateForm() {
        val errors = mutableListOf<String>()
        val state = _uiState.value

        if (state.title.trimmed.isEmpty()) {
            errors.add("Kitap adı boş olamaz")
        }
        if (state.author.trimmed.isEmpty()) {
            errors.add("Yazar adı boş olamaz")
        }
        if (state.isbn.trimmed.isEmpty()) {
            errors.add("ISBN numarası boş olamaz")
        } else if (!state.isbn.isValidISBN) {
            errors.add("Geçersiz ISBN formatı")
        }
        if (state.publisher.trimmed.isEmpty()) {
            errors.add("Yayınevi boş olamaz")
        }

        _uiState.update {
            it.copy(
                validationErrors = errors,
                isFormValid = errors.isEmpty() && state.initialCopyCount > 0
            )
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Add Book
    // ══════════════════════════════════════════════════════════════

    fun addBookWithCopies(onSuccess: () -> Unit) {
        if (!_uiState.value.isFormValid) return

        if (!networkManager.isOnline.value) {
            showAlert("Bağlantı Hatası", "Kitap eklemek için internet bağlantısı gereklidir")
            return
        }

        _uiState.update { it.copy(isAddingBook = true) }

        viewModelScope.launch {
            val state = _uiState.value

            // Create book template
            val bookTemplate = BookTemplate(
                title = state.title.trimmed,
                author = state.author.trimmed,
                isbn = state.isbn.trimmed,
                publisher = state.publisher.trimmed,
                editor = state.editor.trimmed,
                category = state.category.trimmed,
                description = state.description.trimmed
            )

            // Add book template to Firebase
            firebaseRepository.addBookTemplate(bookTemplate)
                .onSuccess { bookTemplateId ->
                    // Get next book ID for copies
                    firebaseRepository.getNextBookId()
                        .onSuccess { bookId ->
                            // Create copies
                            val copies = BarcodeGenerator.createBookCopies(
                                bookId = bookId,
                                isbn = state.isbn.trimmed,
                                copyCount = state.initialCopyCount,
                                existingCopies = emptyList(),
                                bookTemplateId = bookTemplateId
                            )

                            // Add copies to Firebase
                            var successCount = 0
                            copies.forEach { copy ->
                                firebaseRepository.addBookCopy(copy)
                                    .onSuccess { successCount++ }
                            }

                            _uiState.update { it.copy(isAddingBook = false) }
                            showAlert(
                                "Başarılı",
                                "'${state.title}' kitabı ve $successCount kopya başarıyla eklendi"
                            )
                            onSuccess()
                        }
                        .onFailure { error ->
                            _uiState.update { it.copy(isAddingBook = false) }
                            showAlert("Hata", "Kopyalar eklenirken hata oluştu: ${error.message}")
                        }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isAddingBook = false) }
                    showAlert("Hata", "Kitap eklenirken hata oluştu: ${error.message}")
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

    // ══════════════════════════════════════════════════════════════
    // MARK: - Clear Form
    // ══════════════════════════════════════════════════════════════

    fun clearForm() {
        _uiState.update { AddBookUiState() }
        loadNextBookId()
    }
}

/**
 * Add Book UI State
 */
data class AddBookUiState(
    // Form fields
    val title: String = "",
    val author: String = "",
    val isbn: String = "",
    val publisher: String = "",
    val editor: String = "",
    val category: String = "",
    val description: String = "",

    // Copy settings
    val initialCopyCount: Int = 1,
    val nextBookId: Int = 1,

    // UI state
    val showAdvancedOptions: Boolean = false,
    val isFetchingBookInfo: Boolean = false,
    val isAddingBook: Boolean = false,
    val isValidIsbn: Boolean = false,
    val isFormValid: Boolean = false,

    // Validation
    val validationErrors: List<String> = emptyList(),

    // Alert
    val showAlert: Boolean = false,
    val alertTitle: String = "",
    val alertMessage: String = ""
)