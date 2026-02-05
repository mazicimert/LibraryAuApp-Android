package com.mehmetmertmazici.libraryauapp.ui.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmetmertmazici.libraryauapp.data.model.BookTemplate
import com.mehmetmertmazici.libraryauapp.data.model.LoadingState
import com.mehmetmertmazici.libraryauapp.data.repository.AuthRepository
import com.mehmetmertmazici.libraryauapp.data.repository.FirebaseRepository
import com.mehmetmertmazici.libraryauapp.data.repository.NetworkManager
import com.mehmetmertmazici.libraryauapp.domain.util.BarcodeGenerator
import com.mehmetmertmazici.libraryauapp.domain.util.searchNormalized
import com.mehmetmertmazici.libraryauapp.domain.util.trimmed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BookListViewModel
 * Kitap listesi ve kitap yönetimi işlemlerini yöneten ViewModel
 *
 * iOS Karşılığı: BookListViewModel.swift
 */
@HiltViewModel
class BookListViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val authRepository: AuthRepository,
    private val networkManager: NetworkManager
) : ViewModel() {

    // ── UI State ──
    private val _uiState = MutableStateFlow(BookListUiState())
    val uiState: StateFlow<BookListUiState> = _uiState.asStateFlow()

    // ── Network State ──
    val isOnline: StateFlow<Boolean> = networkManager.isOnline

    // ── Search & Filter ──
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Tümü")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // ── All Books ──
    private val _bookTemplates = MutableStateFlow<List<BookTemplate>>(emptyList())

    // ── Filtered Books ──
    val filteredBooks: StateFlow<List<BookTemplate>> = combine(
        _bookTemplates,
        _searchText,
        _selectedCategory
    ) { books, search, category ->
        filterBooks(books, search, category)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Categories ──
    val categories: StateFlow<List<String>> = _bookTemplates.map { books ->
        val bookCategories = books
            .mapNotNull { it.category }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
        listOf("Tümü") + bookCategories
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Tümü"))

    // ══════════════════════════════════════════════════════════════
    // MARK: - Computed Properties
    // ══════════════════════════════════════════════════════════════

    val canAddBooks: Boolean
        get() = authRepository.hasPermission(com.mehmetmertmazici.libraryauapp.data.model.Permission.MANAGE_BOOKS) &&
                networkManager.isOnline.value

    val showEmptyState: Boolean
        get() = !_uiState.value.isLoading && filteredBooks.value.isEmpty()

    val emptyStateMessage: String
        get() = when {
            _searchText.value.isNotEmpty() || _selectedCategory.value != "Tümü" ->
                "Arama kriterlerinize uygun kitap bulunamadı"
            _bookTemplates.value.isEmpty() ->
                "Henüz kitap eklenmemiş"
            else ->
                "Kitap bulunamadı"
        }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Init
    // ══════════════════════════════════════════════════════════════

    init {
        loadBooks()
        observeNetworkChanges()
    }

    private fun observeNetworkChanges() {
        viewModelScope.launch {
            networkManager.isOnline.collect { isOnline ->
                if (isOnline && _bookTemplates.value.isEmpty()) {
                    loadBooks()
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Data Loading
    // ══════════════════════════════════════════════════════════════

    fun loadBooks() {
        if (!networkManager.isOnline.value) {
            println("❌ İnternet bağlantısı yok")
            _uiState.update { it.copy(loadingState = LoadingState.Error("İnternet bağlantısı gereklidir")) }
            return
        }

        println("📚 Kitaplar yükleniyor...")
        _uiState.update { it.copy(isLoading = true, loadingState = LoadingState.Loading) }

        viewModelScope.launch {
            firebaseRepository.fetchBookTemplates()
                .onSuccess { books ->
                    _bookTemplates.value = books
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingState = LoadingState.Success
                        )
                    }
                    println("✅ ${books.size} kitap başarıyla yüklendi")
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingState = LoadingState.Error(error.message ?: "Bilinmeyen hata")
                        )
                    }
                    showAlert("Yükleme Hatası", "Kitaplar yüklenirken hata oluştu: ${error.message}")
                    println("❌ Yükleme hatası: ${error.message}")
                }
        }
    }

    fun refreshBooks() {
        loadBooks()
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Search & Filter
    // ══════════════════════════════════════════════════════════════

    fun updateSearchText(text: String) {
        _searchText.value = text
    }

    fun clearSearch() {
        _searchText.value = ""
    }

    fun updateSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun clearCategoryFilter() {
        _selectedCategory.value = "Tümü"
    }

    private fun filterBooks(
        books: List<BookTemplate>,
        search: String,
        category: String
    ): List<BookTemplate> {
        var filtered = books

        // Kategori filtresi
        if (category != "Tümü") {
            filtered = filtered.filter { it.category == category }
        }

        // Arama filtresi
        val trimmedSearch = search.trimmed
        if (trimmedSearch.isNotEmpty()) {
            val searchTerm = trimmedSearch.searchNormalized
            filtered = filtered.filter { book ->
                book.title.searchNormalized.contains(searchTerm) ||
                        book.author.searchNormalized.contains(searchTerm) ||
                        book.isbn.searchNormalized.contains(searchTerm) ||
                        book.publisher.searchNormalized.contains(searchTerm)
            }
        }

        return filtered.sortedBy { it.title }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Delete Operations
    // ══════════════════════════════════════════════════════════════

    fun requestDeleteBook(book: BookTemplate) {
        val bookId = book.id ?: return
        _uiState.update {
            it.copy(
                pendingDeleteBookId = bookId,
                pendingDeleteBookTitle = book.title,
                showAlert = true,
                alertTitle = "Kitabı Sil",
                alertMessage = "'${book.title}' kitabını ve tüm kopyalarını silmek istediğinizden emin misiniz?\n\nBu işlem geri alınamaz."
            )
        }
    }

    fun confirmDeleteBookWithAllCopies() {
        val bookId = _uiState.value.pendingDeleteBookId ?: return
        val bookTitle = _uiState.value.pendingDeleteBookTitle ?: ""

        _uiState.update { it.copy(isLoading = true, showAlert = false) }

        viewModelScope.launch {
            // 1. Önce kitaba ait kopyaları getir
            firebaseRepository.fetchBookCopies(bookId)
                .onSuccess { copies ->
                    // 2. Kopyaları sil
                    copies.forEach { copy ->
                        copy.id?.let { copyId ->
                            firebaseRepository.deleteBookCopy(copyId)
                        }
                    }

                    // 3. Kitap template'ini sil
                    firebaseRepository.deleteBookTemplate(bookId)
                        .onSuccess {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    pendingDeleteBookId = null,
                                    pendingDeleteBookTitle = null
                                )
                            }
                            loadBooks()
                            showAlert("Başarılı", "'$bookTitle' kitabı ve tüm kopyaları başarıyla silindi.")
                            println("✅ Kitap ve tüm kopyaları silindi: $bookId")
                        }
                        .onFailure { error ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    pendingDeleteBookId = null,
                                    pendingDeleteBookTitle = null
                                )
                            }
                            showAlert("Silme Hatası", "Kitap silinirken bir hata oluştu. Lütfen tekrar deneyin.")
                        }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pendingDeleteBookId = null,
                            pendingDeleteBookTitle = null
                        )
                    }
                    showAlert("Silme Hatası", "Kopyalar alınırken bir hata oluştu.")
                }
        }
    }

    fun cancelDeleteBook() {
        _uiState.update {
            it.copy(
                pendingDeleteBookId = null,
                pendingDeleteBookTitle = null,
                showAlert = false
            )
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Barcode Search
    // ══════════════════════════════════════════════════════════════

    fun searchByBarcode(barcode: String) {
        if (!networkManager.isOnline.value) {
            showAlert("Bağlantı Hatası", "Barkod arama için internet bağlantısı gereklidir")
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            firebaseRepository.findBookCopy(barcode)
                .onSuccess { bookCopy ->
                    if (bookCopy != null) {
                        // BookCopy bulundu, BookTemplate'i bul
                        val bookTemplate = _bookTemplates.value.find { it.id == bookCopy.bookTemplateId }
                        if (bookTemplate != null) {
                            _searchText.value = bookTemplate.title
                            _uiState.update { it.copy(isLoading = false) }
                            showAlert("Kitap Bulundu", "'${bookTemplate.title}' kitabı listelendi")
                        } else {
                            _uiState.update { it.copy(isLoading = false) }
                            showAlert("Kitap Bulunamadı", "Bu barkoda sahip kitap bulunamadı")
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                        showAlert("Kitap Bulunamadı", "Bu barkoda sahip kitap bulunamadı")
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    showAlert("Arama Hatası", "Barkod arama sırasında hata oluştu: ${error.message}")
                }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Navigation
    // ══════════════════════════════════════════════════════════════

    fun openAddBookForm() {
        _uiState.update { it.copy(showAddBookSheet = true) }
    }

    fun closeAddBookSheet() {
        _uiState.update { it.copy(showAddBookSheet = false) }
    }

    fun openBarcodeScanner() {
        _uiState.update { it.copy(showBarcodeScannerSheet = true) }
    }

    fun closeBarcodeScanner() {
        _uiState.update { it.copy(showBarcodeScannerSheet = false) }
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
                alertMessage = "",
                pendingDeleteBookId = null,
                pendingDeleteBookTitle = null
            )
        }
    }
}

/**
 * Book List UI State
 */
data class BookListUiState(
    val isLoading: Boolean = false,
    val loadingState: LoadingState = LoadingState.Idle,

    // Sheets
    val showAddBookSheet: Boolean = false,
    val showBarcodeScannerSheet: Boolean = false,

    // Delete confirmation
    val pendingDeleteBookId: String? = null,
    val pendingDeleteBookTitle: String? = null,

    // Alert
    val showAlert: Boolean = false,
    val alertTitle: String = "",
    val alertMessage: String = ""
)