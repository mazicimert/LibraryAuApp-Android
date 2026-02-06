package com.mehmetmertmazici.libraryauapp.ui.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmetmertmazici.libraryauapp.data.model.BookCopy
import com.mehmetmertmazici.libraryauapp.data.model.BookTemplate
import com.mehmetmertmazici.libraryauapp.data.repository.FirebaseRepository
import com.mehmetmertmazici.libraryauapp.data.repository.NetworkManager
import com.mehmetmertmazici.libraryauapp.domain.util.BarcodeGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AddCopyViewModel
 * Mevcut kitaba yeni kopya ekleme işlemlerini yöneten ViewModel
 *
 * iOS Karşılığı: AddCopyViewModel.swift
 */
@HiltViewModel
class AddCopyViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val networkManager: NetworkManager
) : ViewModel() {

    // ── UI State ──
    private val _uiState = MutableStateFlow(AddCopyUiState())
    val uiState: StateFlow<AddCopyUiState> = _uiState.asStateFlow()

    // ══════════════════════════════════════════════════════════════
    // MARK: - Setup
    // ══════════════════════════════════════════════════════════════

    fun setup(bookTemplate: BookTemplate, existingCopies: List<BookCopy>) {
        _uiState.update {
            it.copy(
                bookTemplate = bookTemplate,
                existingCopies = existingCopies,
                totalCopies = existingCopies.size,
                availableCopies = existingCopies.count { copy -> copy.isAvailable },
                borrowedCopies = existingCopies.count { copy -> !copy.isAvailable },
                isLoading = false
            )
        }
        updatePreviewBarcodes()
        validateForm()
    }

    fun loadBookById(bookId: String) {
        if (bookId.isEmpty()) return

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            // Load book template
            firebaseRepository.fetchBookTemplateById(bookId)
                .onSuccess { template ->
                    // Load existing copies
                    firebaseRepository.fetchBookCopies(bookId)
                        .onSuccess { copies ->
                            setup(template, copies)
                        }
                        .onFailure { error ->
                            _uiState.update { it.copy(isLoading = false) }
                            showAlert("Hata", "Kopyalar yüklenirken hata: ${error.message}")
                        }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    showAlert("Hata", "Kitap bilgisi yüklenirken hata: ${error.message}")
                }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Copy Count
    // ══════════════════════════════════════════════════════════════

    fun updateCopyCount(value: Int) {
        _uiState.update { it.copy(copyCount = value.coerceIn(1, 10)) }
        updatePreviewBarcodes()
        validateForm()
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Barcode Preview
    // ══════════════════════════════════════════════════════════════

    private fun updatePreviewBarcodes() {
        val state = _uiState.value
        val bookTemplate = state.bookTemplate ?: return
        val existingCopies = state.existingCopies

        // Get bookId from existing copies or use placeholder
        val bookId = existingCopies.firstOrNull()?.bookId ?: 0
        val startingCopyNumber = getNextCopyNumber()

        val previews = mutableListOf<BarcodePreviewItem>()

        for (i in 0 until state.copyCount) {
            val copyNumber = startingCopyNumber + i
            val barcode: String
            val barcodeType: BarcodeType

            val totalAfterAddition = existingCopies.size + state.copyCount

            if (existingCopies.isEmpty() && state.copyCount == 1) {
                // No copies exist and only 1 will be added: use ISBN
                barcode = bookTemplate.isbn
                barcodeType = BarcodeType.ISBN
            } else if (totalAfterAddition == 1) {
                // Single copy: use ISBN
                barcode = bookTemplate.isbn
                barcodeType = BarcodeType.ISBN
            } else {
                // Multiple copies: use LIB format
                if (bookId == 0) {
                    // Placeholder display
                    barcode = "LIB???${String.format("%03d", copyNumber)}"
                } else {
                    barcode =
                        "LIB${String.format("%03d", bookId)}${String.format("%03d", copyNumber)}"
                }
                barcodeType = BarcodeType.LIB
            }

            previews.add(
                BarcodePreviewItem(
                    barcode = barcode,
                    copyNumber = copyNumber,
                    barcodeType = barcodeType
                )
            )
        }

        _uiState.update { it.copy(previewBarcodes = previews) }
    }

    private fun getNextCopyNumber(): Int {
        val existingCopies = _uiState.value.existingCopies
        if (existingCopies.isEmpty()) return 1
        val maxCopyNumber = existingCopies.maxOfOrNull { it.copyNumber } ?: 0
        return maxCopyNumber + 1
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Validation
    // ══════════════════════════════════════════════════════════════

    private fun validateForm() {
        val state = _uiState.value
        val errors = mutableListOf<String>()

        if (state.copyCount <= 0) {
            errors.add("Kopya sayısı 1'den az olamaz")
        }

        if (state.copyCount > 10) {
            errors.add("Tek seferde en fazla 10 kopya ekleyebilirsiniz")
        }

        if (!networkManager.isOnline.value) {
            errors.add("İnternet bağlantısı gereklidir")
        }

        val bookTemplate = state.bookTemplate
        if (bookTemplate == null) {
            errors.add("Kitap bilgisi eksik")
        } else if (bookTemplate.isbn.isEmpty()) {
            errors.add("Kitabın ISBN bilgisi eksik")
        }

        // Check barcode conflicts
        val previewBarcodes = state.previewBarcodes.map { it.barcode }
        val existingBarcodes = state.existingCopies.map { it.barcode }

        for (barcode in previewBarcodes) {
            if (!barcode.contains("???") && existingBarcodes.contains(barcode)) {
                errors.add("Barkod çakışması: $barcode")
            }
        }

        _uiState.update {
            it.copy(
                validationErrors = errors,
                isFormValid = errors.isEmpty() && state.copyCount > 0 && networkManager.isOnline.value
            )
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Add Copies
    // ══════════════════════════════════════════════════════════════

    fun addCopies(onSuccess: () -> Unit) {
        val state = _uiState.value

        if (!state.isFormValid) return

        val bookTemplate = state.bookTemplate
        val bookTemplateId = bookTemplate?.id

        if (bookTemplateId.isNullOrEmpty()) {
            showAlert("Hata", "Kitap ID'si bulunamadı")
            return
        }

        _uiState.update { it.copy(isAddingCopies = true) }

        viewModelScope.launch {
            val existingCopies = state.existingCopies

            if (existingCopies.isEmpty()) {
                // Need to get new bookId
                firebaseRepository.getNextBookId()
                    .onSuccess { nextBookId ->
                        val copies = BarcodeGenerator.createBookCopies(
                            bookId = nextBookId,
                            isbn = bookTemplate.isbn,
                            copyCount = state.copyCount,
                            existingCopies = emptyList(),
                            bookTemplateId = bookTemplateId
                        )
                        addCopiesToFirebase(copies, onSuccess)
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(isAddingCopies = false) }
                        showAlert("Hata", "BookId alınamadı: ${error.message}")
                    }
            } else {
                // Use existing bookId
                val bookId = existingCopies.first().bookId

                val copies = BarcodeGenerator.createBookCopies(
                    bookId = bookId,
                    isbn = bookTemplate.isbn,
                    copyCount = state.copyCount,
                    existingCopies = existingCopies,
                    bookTemplateId = bookTemplateId
                )
                addCopiesToFirebase(copies, onSuccess)
            }
        }
    }

    private suspend fun addCopiesToFirebase(copies: List<BookCopy>, onSuccess: () -> Unit) {
        var successCount = 0
        var failureCount = 0

        copies.forEach { copy ->
            firebaseRepository.addBookCopy(copy)
                .onSuccess { successCount++ }
                .onFailure { failureCount++ }
        }

        _uiState.update { it.copy(isAddingCopies = false) }

        if (failureCount == 0) {
            showAlert("Başarılı", "$successCount kopya başarıyla eklendi")
            onSuccess()
        } else {
            showAlert("Kısmi Başarı", "$successCount kopya eklendi, $failureCount kopya eklenemedi")
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
 * Add Copy UI State
 */
data class AddCopyUiState(
    val bookTemplate: BookTemplate? = null,
    val existingCopies: List<BookCopy> = emptyList(),

    // Statistics
    val totalCopies: Int = 0,
    val availableCopies: Int = 0,
    val borrowedCopies: Int = 0,

    // Form
    val copyCount: Int = 1,
    val previewBarcodes: List<BarcodePreviewItem> = emptyList(),

    // State
    val isLoading: Boolean = false,
    val isAddingCopies: Boolean = false,
    val isFormValid: Boolean = false,

    // Validation
    val validationErrors: List<String> = emptyList(),

    // Alert
    val showAlert: Boolean = false,
    val alertTitle: String = "",
    val alertMessage: String = ""
)

/**
 * Barcode Preview Item
 */
data class BarcodePreviewItem(
    val barcode: String,
    val copyNumber: Int,
    val barcodeType: BarcodeType
)

/**
 * Barcode Type
 */
enum class BarcodeType {
    ISBN,
    LIB;

    val description: String
        get() = when (this) {
            ISBN -> "ISBN Barkodu"
            LIB -> "Kütüphane Barkodu"
        }
}