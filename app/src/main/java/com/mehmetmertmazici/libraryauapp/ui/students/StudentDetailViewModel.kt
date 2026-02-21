package com.mehmetmertmazici.libraryauapp.ui.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmetmertmazici.libraryauapp.data.model.BookCopy
import com.mehmetmertmazici.libraryauapp.data.model.BookTemplate
import com.mehmetmertmazici.libraryauapp.data.model.BorrowedBook
import com.mehmetmertmazici.libraryauapp.data.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * StudentDetailViewModel Öğrenci detay ve ödünç geçmişi işlemlerini yöneten ViewModel
 *
 * iOS Karşılığı: StudentDetailViewModel (StudentDetailView.swift içinde)
 */
@HiltViewModel
class StudentDetailViewModel
@Inject
constructor(private val firebaseRepository: FirebaseRepository) : ViewModel() {

    // ── UI State ──
    private val _uiState = MutableStateFlow(StudentDetailUiState())
    val uiState: StateFlow<StudentDetailUiState> = _uiState.asStateFlow()

    // ── Borrow Data ──
    private val _activeBorrows = MutableStateFlow<List<BorrowInfoModel>>(emptyList())
    val activeBorrows: StateFlow<List<BorrowInfoModel>> = _activeBorrows.asStateFlow()

    private val _historyBorrows = MutableStateFlow<List<BorrowInfoModel>>(emptyList())
    val historyBorrows: StateFlow<List<BorrowInfoModel>> = _historyBorrows.asStateFlow()

    // ══════════════════════════════════════════════════════════════
    // MARK: - Load Data
    // ══════════════════════════════════════════════════════════════

    fun loadData(studentId: String) {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                // Fetch student details
                val studentResult = firebaseRepository.fetchStudentById(studentId)
                val student = studentResult.getOrNull()

                if (student != null) {
                    _uiState.update { it.copy(student = student) }
                }

                // Fetch all borrowed books
                val borrowsResult = firebaseRepository.fetchBorrowedBooks()
                val copiesResult = firebaseRepository.fetchAllBookCopies()
                val templatesResult = firebaseRepository.fetchBookTemplates()

                if (borrowsResult.isSuccess && copiesResult.isSuccess && templatesResult.isSuccess
                ) {
                    val allBorrows = borrowsResult.getOrNull() ?: emptyList()
                    val allCopies = copiesResult.getOrNull() ?: emptyList()
                    val allTemplates = templatesResult.getOrNull() ?: emptyList()

                    // Filter borrows for this student
                    val studentBorrows = allBorrows.filter { it.studentId == studentId }

                    processBorrows(studentBorrows, allCopies, allTemplates)
                    _uiState.update { it.copy(isLoading = false) }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                    println("❌ Detay yükleme hatası")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                println("❌ Detay yükleme hatası: ${e.message}")
            }
        }
    }

    private fun processBorrows(
            borrows: List<BorrowedBook>,
            copies: List<BookCopy>,
            templates: List<BookTemplate>
    ) {
        val active = mutableListOf<BorrowInfoModel>()
        val history = mutableListOf<BorrowInfoModel>()

        for (record in borrows) {
            // Find matching copy
            val copy = copies.find { it.id == record.bookCopyId }
            // Find matching template
            val template = templates.find { it.id == copy?.bookTemplateId }

            val infoModel =
                    BorrowInfoModel(borrowRecord = record, bookCopy = copy, bookTemplate = template)

            if (record.isReturned) {
                history.add(infoModel)
            } else {
                active.add(infoModel)
            }
        }

        // Sort by date (newest first for active, by return date for history)
        _activeBorrows.value = active.sortedBy { it.borrowRecord.dueDate?.toDate() }
        _historyBorrows.value = history.sortedByDescending { it.borrowRecord.returnDate?.toDate() }
    }
}

/** Student Detail UI State */
data class StudentDetailUiState(
        val isLoading: Boolean = false,
        val student: com.mehmetmertmazici.libraryauapp.data.model.Student? = null
)

/**
 * Ödünç kaydı ve Kitap detayını bir arada tutan yapı
 *
 * iOS Karşılığı: BorrowInfoModel (StudentDetailView.swift içinde)
 */
data class BorrowInfoModel(
        val id: String = UUID.randomUUID().toString(),
        val borrowRecord: BorrowedBook,
        val bookCopy: BookCopy?,
        val bookTemplate: BookTemplate?
) {
    val bookTitle: String
        get() = bookTemplate?.title ?: "Bilinmeyen Kitap"

    val bookAuthor: String
        get() = bookTemplate?.author ?: ""

    val barcode: String
        get() = bookCopy?.barcode ?: "-"
}
