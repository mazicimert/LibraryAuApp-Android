package com.mehmetmertmazici.libraryauapp.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmetmertmazici.libraryauapp.data.model.BookTemplate
import com.mehmetmertmazici.libraryauapp.data.model.Student
import com.mehmetmertmazici.libraryauapp.data.repository.FirebaseRepository
import com.mehmetmertmazici.libraryauapp.data.repository.NetworkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * TrashViewModel
 * Çöp kutusu (silinen kitaplar ve öğrenciler) yönetimi
 *
 * iOS Karşılığı: TrashViewModel.swift
 */
@HiltViewModel
class TrashViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val networkManager: NetworkManager
) : ViewModel() {

    // ── UI State ──
    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    // ── Network State ──
    val isOnline: StateFlow<Boolean> = networkManager.isOnline

    init {
        fetchAllTrash()
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Data Loading
    // ══════════════════════════════════════════════════════════════

    /**
     * Tüm silinen öğeleri paralel olarak yükle
     * iOS Karşılığı: fetchAllTrash() — Publishers.Zip
     */
    fun fetchAllTrash() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                // Paralel yükleme — iOS'taki Publishers.Zip karşılığı
                val booksDeferred = async { firebaseRepository.fetchDeletedBookTemplates() }
                val studentsDeferred = async { firebaseRepository.fetchDeletedStudents() }

                val booksResult = booksDeferred.await()
                val studentsResult = studentsDeferred.await()

                // Öğrencileri güncelle
                studentsResult
                    .onSuccess { students ->
                        _uiState.update { it.copy(deletedStudents = students) }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Öğrenciler yüklenirken hata oluştu"
                            )
                        }
                        println("❌ Silinen öğrenciler yükleme hatası: ${error.message}")
                        return@launch
                    }

                // Kitapları yükle ve kopya sayılarını getir
                booksResult
                    .onSuccess { books ->
                        fetchCopyCountsForBooks(books)
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Kitaplar yüklenirken hata oluştu"
                            )
                        }
                        println("❌ Silinen kitaplar yükleme hatası: ${error.message}")
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Bilinmeyen hata"
                    )
                }
                println("❌ Çöp kutusu yükleme hatası: ${e.message}")
            }
        }
    }

    /**
     * Her kitap için kopya sayısını getir
     * iOS Karşılığı: fetchCopyCountsForBooks — Publishers.MergeMany
     */
    private suspend fun fetchCopyCountsForBooks(books: List<BookTemplate>) {
        if (books.isEmpty()) {
            _uiState.update { it.copy(deletedBooks = emptyList(), isLoading = false) }
            return
        }

        try {
            // Her kitap için async kopya sayısı sorgusu
            val trashItems = books.map { book ->
                viewModelScope.async {
                    val countResult = firebaseRepository.fetchDeletedBookCopyCount(book.id ?: "")
                    val copyCount = countResult.getOrDefault(0)
                    TrashBookItem(book = book, copyCount = copyCount)
                }
            }.awaitAll()

            // Tarihe göre sırala (en son silinen en üstte)
            val sortedItems = trashItems.sortedByDescending { item ->
                item.book.deletedAt?.toDate()?.time ?: 0L
            }

            _uiState.update { it.copy(deletedBooks = sortedItems, isLoading = false) }
            println("✅ ${sortedItems.size} silinen kitap yüklendi")
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false) }
            println("❌ Kopya sayıları yükleme hatası: ${e.message}")
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Restore Operations
    // ══════════════════════════════════════════════════════════════

    fun restoreBook(item: TrashBookItem) {
        val id = item.book.id ?: return

        viewModelScope.launch {
            firebaseRepository.restoreBookTemplate(id)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            deletedBooks = state.deletedBooks.filter { it.id != id }
                        )
                    }
                    println("✅ Kitap geri yüklendi: ${item.book.title}")
                }
                .onFailure { error ->
                    showAlert("Hata", "Kitap geri yüklenirken hata oluştu: ${error.message}")
                    println("❌ Kitap geri yükleme hatası: ${error.message}")
                }
        }
    }

    fun restoreStudent(student: Student) {
        val id = student.id ?: return

        viewModelScope.launch {
            firebaseRepository.restoreStudent(id)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            deletedStudents = state.deletedStudents.filter { it.id != id }
                        )
                    }
                    println("✅ Öğrenci geri yüklendi: ${student.fullName}")
                }
                .onFailure { error ->
                    showAlert("Hata", "Öğrenci geri yüklenirken hata oluştu: ${error.message}")
                    println("❌ Öğrenci geri yükleme hatası: ${error.message}")
                }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Permanent Delete Operations
    // ══════════════════════════════════════════════════════════════

    fun deleteBookPermanently(item: TrashBookItem) {
        val id = item.book.id ?: return

        viewModelScope.launch {
            firebaseRepository.permanentlyDeleteBookTemplate(id)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            deletedBooks = state.deletedBooks.filter { it.id != id }
                        )
                    }
                    println("✅ Kitap kalıcı olarak silindi: ${item.book.title}")
                }
                .onFailure { error ->
                    showAlert("Hata", "Kitap silinirken hata oluştu: ${error.message}")
                    println("❌ Kalıcı silme hatası: ${error.message}")
                }
        }
    }

    fun deleteStudentPermanently(student: Student) {
        val id = student.id ?: return

        viewModelScope.launch {
            firebaseRepository.permanentlyDeleteStudent(id)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            deletedStudents = state.deletedStudents.filter { it.id != id }
                        )
                    }
                    println("✅ Öğrenci kalıcı olarak silindi: ${student.fullName}")
                }
                .onFailure { error ->
                    showAlert("Hata", "Öğrenci silinirken hata oluştu: ${error.message}")
                    println("❌ Kalıcı silme hatası: ${error.message}")
                }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Tab Management
    // ══════════════════════════════════════════════════════════════

    fun updateSelectedTab(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
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
 * Çöp kutusundaki kitap öğesi
 * iOS Karşılığı: TrashBookItem
 */
data class TrashBookItem(
    val book: BookTemplate,
    val copyCount: Int
) {
    val id: String? get() = book.id
}

/**
 * Trash UI State
 */
data class TrashUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val deletedBooks: List<TrashBookItem> = emptyList(),
    val deletedStudents: List<Student> = emptyList(),
    val selectedTab: Int = 0,
    // Alert
    val showAlert: Boolean = false,
    val alertTitle: String = "",
    val alertMessage: String = ""
)
