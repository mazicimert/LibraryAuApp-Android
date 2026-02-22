package com.mehmetmertmazici.libraryauapp.ui.borrowing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmetmertmazici.libraryauapp.data.model.BookCopy
import com.mehmetmertmazici.libraryauapp.data.model.BookTemplate
import com.mehmetmertmazici.libraryauapp.data.model.BorrowedBook
import com.mehmetmertmazici.libraryauapp.data.model.BorrowingRule
import com.mehmetmertmazici.libraryauapp.data.model.LoadingState
import com.mehmetmertmazici.libraryauapp.data.model.Permission
import com.mehmetmertmazici.libraryauapp.data.model.Student
import com.mehmetmertmazici.libraryauapp.data.repository.AuthRepository
import com.mehmetmertmazici.libraryauapp.data.repository.FirebaseRepository
import com.mehmetmertmazici.libraryauapp.data.repository.NetworkManager
import com.mehmetmertmazici.libraryauapp.domain.util.searchNormalized
import com.mehmetmertmazici.libraryauapp.domain.util.trimmed
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * BorrowingViewModel Kitap odunc verme ve iade islemlerini yoneten ViewModel
 *
 * iOS Karsiligi: BorrowingViewModel.swift
 */
@HiltViewModel
class BorrowingViewModel
@Inject
constructor(
    private val firebaseRepository: FirebaseRepository,
    private val authRepository: AuthRepository,
    private val networkManager: NetworkManager
) : ViewModel() {

    // ── UI State ──
    private val _uiState = MutableStateFlow(BorrowingUiState())
    val uiState: StateFlow<BorrowingUiState> = _uiState.asStateFlow()

    // ── Network State ──
    val isOnline: StateFlow<Boolean> = networkManager.isOnline

    // ── Data Flows ──
    private val _borrowedBooks = MutableStateFlow<List<BorrowedBook>>(emptyList())
    private val _students = MutableStateFlow<List<Student>>(emptyList())
    private val _bookCopies = MutableStateFlow<List<BookCopy>>(emptyList())
    private val _bookTemplates = MutableStateFlow<List<BookTemplate>>(emptyList())
    private val _overdueBooks = MutableStateFlow<List<BorrowedBook>>(emptyList())

    // ── Search & Filter ──
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _filterOption = MutableStateFlow(BorrowFilterOption.ALL)
    val filterOption: StateFlow<BorrowFilterOption> = _filterOption.asStateFlow()

    // ── Borrow Form State ──
    private val _borrowStudentNumber = MutableStateFlow("")
    val borrowStudentNumber: StateFlow<String> = _borrowStudentNumber.asStateFlow()

    private val _borrowBookBarcode = MutableStateFlow("")
    val borrowBookBarcode: StateFlow<String> = _borrowBookBarcode.asStateFlow()

    private val _selectedStudent = MutableStateFlow<Student?>(null)
    val selectedStudent: StateFlow<Student?> = _selectedStudent.asStateFlow()

    private val _selectedBookCopy = MutableStateFlow<BookCopy?>(null)
    val selectedBookCopy: StateFlow<BookCopy?> = _selectedBookCopy.asStateFlow()

    private val _selectedBookTemplate = MutableStateFlow<BookTemplate?>(null)
    val selectedBookTemplate: StateFlow<BookTemplate?> = _selectedBookTemplate.asStateFlow()

    // ── Caches ──
    private var studentCache: Map<String, Student> = emptyMap()
    private var bookCopyCache: Map<String, BookCopy> = emptyMap()
    private var bookTemplateCache: Map<String, BookTemplate> = emptyMap()

    // ── Filtered Books (Reactive) ──
    val filteredBorrowedBooks: StateFlow<List<BorrowedBook>> =
        combine(_borrowedBooks, _searchText, _filterOption) { books, search, filter ->
            filterBorrowedBooks(books, search, filter)
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ══════════════════════════════════════════════════════════════
    // MARK: - Computed Properties
    // ══════════════════════════════════════════════════════════════

    val canManageBorrowing: Boolean
        get() =
            authRepository.hasPermission(Permission.MANAGE_BORROWING) &&
                    networkManager.isOnline.value

    val overdueCount: Int
        get() = _overdueBooks.value.size

    val totalBorrowedCount: Int
        get() = _borrowedBooks.value.count { !it.isReturned }

    val isBorrowFormValid: Boolean
        get() {
            val studentNumber = _borrowStudentNumber.value.trimmed
            val bookBarcode = _borrowBookBarcode.value.trimmed
            val student = _selectedStudent.value
            val bookCopy = _selectedBookCopy.value

            if (studentNumber.isEmpty() ||
                bookBarcode.isEmpty() ||
                student == null ||
                bookCopy == null
            ) {
                return false
            }

            // Limit kontrolu
            val activeBorrows =
                _borrowedBooks.value.filter { it.studentId == student.id && !it.isReturned }
            if (activeBorrows.size >= BorrowingRule.MAX_BOOKS_PER_STUDENT) {
                return false
            }

            return true
        }

    val showLoadingIndicator: Boolean
        get() = _uiState.value.isLoading || _uiState.value.loadingState.isLoading

    val showEmptyState: Boolean
        get() =
            !_uiState.value.isLoading &&
                    filteredBorrowedBooks.value.isEmpty() &&
                    _uiState.value.loadingState !is LoadingState.Loading

    val emptyStateMessage: String
        get() =
            when (_filterOption.value) {
                BorrowFilterOption.ALL ->
                    if (_searchText.value.isEmpty()) "Henüz ödünç verilen kitap yok"
                    else "Arama kriterlerinize uygun kayıt bulunamadı"

                BorrowFilterOption.ACTIVE -> "Şu anda ödünçte olan kitap yok"
                BorrowFilterOption.RETURNED -> "İade edilmiş kitap bulunamadı"
                BorrowFilterOption.OVERDUE -> "Süresi geçmiş kitap yok"
            }

    val hasOverdueWarning: Boolean
        get() = _overdueBooks.value.isNotEmpty()

    val overdueWarningMessage: String
        get() {
            val count = _overdueBooks.value.size
            return if (count == 1) "1 kitabın süresi geçmiş" else "$count kitabın süresi geçmiş"
        }

    val statisticsInfo: BorrowingStatistics
        get() {
            val all = _borrowedBooks.value
            val total = all.size
            val active = all.count { !it.isReturned }
            val returned = all.count { it.isReturned }
            val overdue = _overdueBooks.value.size
            return BorrowingStatistics(
                total = total,
                active = active,
                returned = returned,
                overdue = overdue
            )
        }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Init
    // ══════════════════════════════════════════════════════════════

    init {
        loadData()
        observeNetworkChanges()
    }

    private fun observeNetworkChanges() {
        viewModelScope.launch {
            networkManager.isOnline.collect { isOnline ->
                if (isOnline &&
                    _borrowedBooks.value.isEmpty() &&
                    _uiState.value.loadingState !is LoadingState.Loading
                ) {
                    loadData()
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Data Loading
    // ══════════════════════════════════════════════════════════════

    fun loadData() {
        if (!networkManager.isOnline.value) {
            _uiState.update {
                it.copy(loadingState = LoadingState.Error("Internet baglantisi gereklidir"))
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, loadingState = LoadingState.Loading) }

        viewModelScope.launch {
            val loadErrors = mutableMapOf<String, String>()

            // Paralel yukleme
            val borrowedDeferred = async { firebaseRepository.fetchBorrowedBooks() }
            val studentsDeferred = async { firebaseRepository.fetchStudents() }
            val bookCopiesDeferred = async { firebaseRepository.fetchAllBookCopies() }
            val bookTemplatesDeferred = async { firebaseRepository.fetchBookTemplates() }

            // Paralel yuklemeleri bekle
            val borrowedResult = borrowedDeferred.await()
            val studentsResult = studentsDeferred.await()
            val bookCopiesResult = bookCopiesDeferred.await()
            val bookTemplatesResult = bookTemplatesDeferred.await()

            // 1. Students
            studentsResult
                .onSuccess { students ->
                    _students.value = students
                    println("✅ ${students.size} ogrenci yuklendi")
                }
                .onFailure { error ->
                    loadErrors["students"] = error.message ?: "Bilinmeyen hata"
                    println("❌ Students yuklenemedi: ${error.message}")
                }

            // 2. Book Copies
            bookCopiesResult
                .onSuccess { copies ->
                    _bookCopies.value = copies
                    println("✅ ${copies.size} kitap kopyasi yuklendi")
                }
                .onFailure { error ->
                    loadErrors["bookCopies"] = error.message ?: "Bilinmeyen hata"
                    println("❌ Book Copies yuklenemedi: ${error.message}")
                }

            // 3. Book Templates
            bookTemplatesResult
                .onSuccess { templates ->
                    _bookTemplates.value = templates
                    println("✅ ${templates.size} kitap sablonu yuklendi")
                }
                .onFailure { error ->
                    loadErrors["bookTemplates"] = error.message ?: "Bilinmeyen hata"
                    println("❌ Book Templates yuklenemedi: ${error.message}")
                }

            // Veriler hazir, onbellekleri olustur
            buildCaches()

            // 4. Borrowed Books (En son yapiliyor ki UI recompose oldugunda diger veriler hazir
            // olsun)
            borrowedResult
                .onSuccess { books ->
                    _borrowedBooks.value = books
                    println("✅ ${books.size} odunc kaydi yuklendi")
                }
                .onFailure { error ->
                    loadErrors["borrowedBooks"] = error.message ?: "Bilinmeyen hata"
                    println("❌ Borrowed Books yuklenemedi: ${error.message}")
                }

            // Yukleme tamamlandi
            _uiState.update { it.copy(isLoading = false) }
            checkOverdueBooks()

            val totalDataSources = 4
            val failedCount = loadErrors.size

            when {
                failedCount == totalDataSources -> {
                    _uiState.update {
                        it.copy(loadingState = LoadingState.Error("Veriler yuklenemedi"))
                    }
                    showAlert(
                        "Yukleme Hatasi",
                        "Hicbir veri yuklenemedi. Lutfen internet baglantinizi kontrol edin ve tekrar deneyin."
                    )
                }

                failedCount > 0 -> {
                    _uiState.update { it.copy(loadingState = LoadingState.Success) }
                    val failedDataNames = loadErrors.keys.joinToString(", ")
                    showAlert(
                        "Kismi Yukleme",
                        "Bazi veriler yuklenemedi: $failedDataNames. Uygulama sinirli ozelliklerle calisabilir."
                    )
                }

                else -> {
                    _uiState.update { it.copy(loadingState = LoadingState.Success) }
                    println("✅ Tum veriler basariyla yuklendi")
                }
            }
        }
    }

    private fun buildCaches() {
        studentCache =
            _students
                .value
                .mapNotNull { student -> student.id?.let { id -> id to student } }
                .toMap()

        bookCopyCache =
            _bookCopies.value.mapNotNull { copy -> copy.id?.let { id -> id to copy } }.toMap()

        bookTemplateCache =
            _bookTemplates
                .value
                .mapNotNull { template -> template.id?.let { id -> id to template } }
                .toMap()
    }

    fun refreshData() {
        loadData()
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Filtering & Search
    // ══════════════════════════════════════════════════════════════

    private fun filterBorrowedBooks(
        books: List<BorrowedBook>,
        search: String,
        filter: BorrowFilterOption
    ): List<BorrowedBook> {
        var filtered = books

        // Durum filtresi
        when (filter) {
            BorrowFilterOption.ALL -> {
                /* Tumunu goster */
            }

            BorrowFilterOption.ACTIVE -> filtered = filtered.filter { !it.isReturned }
            BorrowFilterOption.RETURNED -> filtered = filtered.filter { it.isReturned }
            BorrowFilterOption.OVERDUE -> filtered = filtered.filter { it.isOverdue }
        }

        // Arama filtresi
        val trimmedSearch = search.trimmed
        if (trimmedSearch.isNotEmpty()) {
            val searchTerm = trimmedSearch.searchNormalized
            val students = _students.value
            val bookCopies = _bookCopies.value
            val bookTemplates = _bookTemplates.value

            filtered =
                filtered.filter { borrowRecord ->
                    // Ogrenci bilgilerinde ara
                    val student = students.firstOrNull { it.id == borrowRecord.studentId }
                    val studentMatch =
                        student?.name?.searchNormalized?.contains(searchTerm) == true ||
                                student?.surname?.searchNormalized?.contains(searchTerm) ==
                                true ||
                                student?.studentNumber?.contains(searchTerm) == true

                    // Kitap bilgilerinde ara
                    val bookCopy = bookCopies.firstOrNull { it.id == borrowRecord.bookCopyId }
                    val bookTemplate =
                        bookTemplates.firstOrNull { it.id == bookCopy?.bookTemplateId }
                    val bookMatch =
                        bookTemplate?.title?.searchNormalized?.contains(searchTerm) ==
                                true ||
                                bookTemplate?.author?.searchNormalized?.contains(
                                    searchTerm
                                ) == true ||
                                bookCopy?.barcode?.contains(searchTerm) == true

                    studentMatch || bookMatch
                }
        }

        // Tarihe gore sirala (en yeni once)
        return filtered.sortedByDescending { it.borrowDate.toDate() }
    }

    fun updateSearchText(text: String) {
        _searchText.value = text
    }

    fun clearSearch() {
        _searchText.value = ""
    }

    fun updateFilterOption(option: BorrowFilterOption) {
        _filterOption.value = option
    }

    fun clearFilter() {
        _filterOption.value = BorrowFilterOption.ALL
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Borrow Book
    // ══════════════════════════════════════════════════════════════

    fun updateBorrowStudentNumber(number: String) {
        // Sadece rakam ve max 8 hane
        val filtered = number.filter { it.isDigit() }.take(8)
        _borrowStudentNumber.value = filtered

        if (filtered.length == 8) {
            searchStudent()
        } else {
            _selectedStudent.value = null
        }
    }

    fun updateBorrowBookBarcode(barcode: String) {
        _borrowBookBarcode.value = barcode
    }

    fun searchStudent() {
        val studentNumber = _borrowStudentNumber.value.trimmed
        if (studentNumber.isEmpty()) {
            _selectedStudent.value = null
            return
        }

        val student = _students.value.firstOrNull { it.studentNumber == studentNumber }
        if (student != null) {
            _selectedStudent.value = student
            checkStudentBorrowLimit(student)
        } else {
            _selectedStudent.value = null
            showAlert("Ogrenci Bulunamadi", "Bu numaraya sahip ogrenci bulunamadi: $studentNumber")
        }
    }

    fun searchBookByBarcode() {
        val barcode = _borrowBookBarcode.value.trimmed
        if (barcode.isEmpty()) {
            _selectedBookCopy.value = null
            _selectedBookTemplate.value = null
            return
        }

        val bookCopy = _bookCopies.value.firstOrNull { it.barcode == barcode }
        if (bookCopy != null) {
            _selectedBookCopy.value = bookCopy
            val bookTemplate = _bookTemplates.value.firstOrNull { it.id == bookCopy.bookTemplateId }
            if (bookTemplate != null) {
                _selectedBookTemplate.value = bookTemplate
                checkBookAvailability(bookCopy)
            }
        } else {
            _selectedBookCopy.value = null
            _selectedBookTemplate.value = null
            showAlert("Kitap Bulunamadi", "Bu barkoda sahip kitap bulunamadi: $barcode")
        }
    }

    private fun checkStudentBorrowLimit(student: Student) {
        val activeBorrows =
            _borrowedBooks.value.filter { it.studentId == student.id && !it.isReturned }
        if (activeBorrows.size >= BorrowingRule.MAX_BOOKS_PER_STUDENT) {
            showAlert(
                "Limit Asildi",
                "${student.fullName} ogrencisi maksimum kitap limitine ulasti (${activeBorrows.size}/${BorrowingRule.MAX_BOOKS_PER_STUDENT})"
            )
        }
    }

    private fun checkBookAvailability(bookCopy: BookCopy) {
        if (!bookCopy.isAvailable) {
            showAlert("Kitap Musait Degil", "Bu kitap su anda oduncte. Barkod: ${bookCopy.barcode}")
        }
    }

    fun selectBookFromTemplate(bookTemplate: BookTemplate) {
        val templateId = bookTemplate.id
        if (templateId == null) {
            showAlert("Hata", "Gecersiz kitap bilgisi")
            return
        }

        val availableCopies =
            _bookCopies.value.filter { it.bookTemplateId == templateId && it.isAvailable }

        val firstAvailableCopy = availableCopies.firstOrNull()
        if (firstAvailableCopy == null) {
            showAlert(
                "Musait Kopya Yok",
                "'${bookTemplate.title}' kitabinin tum kopyalari oduncte. Lutfen baska bir kitap secin."
            )
            return
        }

        _selectedBookTemplate.value = bookTemplate
        _selectedBookCopy.value = firstAvailableCopy
        _borrowBookBarcode.value = firstAvailableCopy.barcode
        checkBookAvailability(firstAvailableCopy)
    }

    fun borrowBook() {
        if (!networkManager.isOnline.value) {
            showAlert("Baglanti Hatasi", "Odunc verme icin internet baglantisi gereklidir")
            return
        }

        if (!canManageBorrowing) {
            showAlert("Yetki Hatasi", "Odunc verme yetkiniz yok")
            return
        }

        val student = _selectedStudent.value
        val bookCopy = _selectedBookCopy.value
        val bookTemplate = _selectedBookTemplate.value

        if (student == null || bookCopy == null || bookTemplate == null) {
            showAlert("Eksik Bilgi", "Lutfen ogrenci ve kitap bilgilerini kontrol edin")
            return
        }

        // Final validasyonlar
        val validationResult = validateBorrowOperation(student, bookCopy, bookTemplate)
        if (!validationResult.first) {
            showAlert("Odunc Verilemez", validationResult.second)
            return
        }

        val bookCopyId = bookCopy.id
        val studentId = student.id
        if (bookCopyId == null || studentId == null) {
            showAlert("Hata", "Gecersiz veri")
            return
        }

        _uiState.update { it.copy(isBorrowingBook = true) }

        val borrowRecord =
            BorrowedBook(
                bookCopyId = bookCopyId,
                studentId = studentId,
                borrowDays = BorrowingRule.DEFAULT_BORROW_DAYS
            )

        viewModelScope.launch {
            // Odunc kaydi olustur
            firebaseRepository
                .createBorrowRecord(borrowRecord)
                .onSuccess {
                    // Kitap kopyasinin durumunu guncelle
                    val updatedBookCopy = bookCopy.copy(isAvailable = false)
                    firebaseRepository
                        .updateBookCopy(updatedBookCopy)
                        .onSuccess {
                            _uiState.update {
                                it.copy(
                                    isBorrowingBook = false,
                                    showBorrowBookSheet = false
                                )
                            }
                            clearBorrowForm()
                            loadData()
                            showAlert(
                                "Basarili",
                                "'${bookTemplate.title}' kitabi ${student.fullName} ogrencisine odunc verildi"
                            )
                            println(
                                "✅ Kitap odunc verildi: ${bookTemplate.title} -> ${student.fullName}"
                            )
                        }
                        .onFailure { error ->
                            _uiState.update { it.copy(isBorrowingBook = false) }
                            showAlert(
                                "Odunc Verme Hatasi",
                                "Kitap odunc verilirken hata olustu: ${error.message}"
                            )
                        }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isBorrowingBook = false) }
                    showAlert(
                        "Odunc Verme Hatasi",
                        "Kitap odunc verilirken hata olustu: ${error.message}"
                    )
                }
        }
    }

    private fun validateBorrowOperation(
        student: Student,
        bookCopy: BookCopy,
        bookTemplate: BookTemplate
    ): Pair<Boolean, String> {
        if (student.id == null) {
            return Pair(false, "Gecersiz ogrenci bilgisi")
        }

        if (!bookCopy.isAvailable) {
            return Pair(false, "Bu kitap su anda musait degil")
        }

        val studentActiveBorrows =
            _borrowedBooks.value.filter { it.studentId == student.id && !it.isReturned }

        if (!BorrowingRule.canBorrowBook(studentActiveBorrows.size)) {
            return Pair(
                false,
                "Ogrenci maksimum kitap limitine ulasti (${studentActiveBorrows.size}/${BorrowingRule.MAX_BOOKS_PER_STUDENT})"
            )
        }

        val templateId = bookTemplate.id
        if (templateId == null) {
            return Pair(false, "Kitap kimligi bulunamadi")
        }

        if (!BorrowingRule.canBorrowSameBook(
                studentBorrowedBooks = studentActiveBorrows,
                targetBookTemplateId = templateId,
                allBookCopies = _bookCopies.value
            )
        ) {
            return Pair(false, "Ogrenci zaten bu kitaptan odunc almis")
        }

        return Pair(true, "")
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Return Book
    // ══════════════════════════════════════════════════════════════

    fun returnBook(borrowRecord: BorrowedBook) {
        if (!networkManager.isOnline.value) {
            showAlert("Baglanti Hatasi", "Iade islemi icin internet baglantisi gereklidir")
            return
        }

        if (!canManageBorrowing) {
            showAlert("Yetki Hatasi", "Iade islemi yetkiniz yok")
            return
        }

        if (borrowRecord.isReturned) {
            showAlert("Zaten Iade Edilmis", "Bu kitap zaten iade edilmis")
            return
        }

        val borrowedBookId = borrowRecord.id
        if (borrowedBookId == null) {
            showAlert("Hata", "Gecersiz odunc kaydi")
            return
        }

        _uiState.update { it.copy(isReturningBook = true) }

        viewModelScope.launch {
            firebaseRepository
                .returnBook(borrowedBookId)
                .onSuccess {
                    // Kitap kopyasinin durumunu guncelle
                    val bookCopy =
                        _bookCopies.value.firstOrNull { it.id == borrowRecord.bookCopyId }
                    if (bookCopy != null) {
                        val updatedBookCopy = bookCopy.copy(isAvailable = true)
                        firebaseRepository.updateBookCopy(updatedBookCopy)
                    }

                    _uiState.update { it.copy(isReturningBook = false) }
                    loadData()

                    val student =
                        _students.value.firstOrNull { it.id == borrowRecord.studentId }
                    val bookCopyInfo =
                        _bookCopies.value.firstOrNull { it.id == borrowRecord.bookCopyId }
                    val bookTemplate =
                        _bookTemplates.value.firstOrNull {
                            it.id == bookCopyInfo?.bookTemplateId
                        }

                    showAlert(
                        "Iade Basarili",
                        "'${bookTemplate?.title ?: "Kitap"}' ${student?.fullName ?: "ogrenci"}'den iade alindi"
                    )
                    println(
                        "✅ Kitap iade edildi: ${bookTemplate?.title ?: "Unknown"} <- ${student?.fullName ?: "Unknown"}"
                    )
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isReturningBook = false) }
                    showAlert(
                        "Iade Hatasi",
                        "Kitap iade edilirken hata olustu: ${error.message}"
                    )
                }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Search Helpers (for Student/Book Search Sheets)
    // ══════════════════════════════════════════════════════════════

    fun getFilteredStudents(searchText: String): List<Student> {
        if (searchText.isEmpty()) return _students.value
        val searchTerm = searchText.searchNormalized
        return _students.value.filter { student ->
            student.name.searchNormalized.contains(searchTerm) ||
                    student.surname.searchNormalized.contains(searchTerm) ||
                    student.studentNumber.contains(searchTerm)
        }
    }

    fun getFilteredBookTemplates(searchText: String): List<BookTemplate> {
        if (searchText.isEmpty()) return _bookTemplates.value
        val searchTerm = searchText.searchNormalized
        return _bookTemplates.value.filter { template ->
            template.title.searchNormalized.contains(searchTerm) ||
                    template.author.searchNormalized.contains(searchTerm)
        }
    }

    fun directSelectStudent(student: Student) {
        _selectedStudent.value = student
        _borrowStudentNumber.value = student.studentNumber
        checkStudentBorrowLimit(student)
    }

    fun directSetBorrowBarcode(barcode: String) {
        _borrowBookBarcode.value = barcode
        searchBookByBarcode()
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Helper Methods
    // ══════════════════════════════════════════════════════════════

    fun clearBorrowForm() {
        _borrowStudentNumber.value = ""
        _borrowBookBarcode.value = ""
        _selectedStudent.value = null
        _selectedBookCopy.value = null
        _selectedBookTemplate.value = null
    }

    fun openBorrowBookForm() {
        clearBorrowForm()
        _uiState.update { it.copy(showBorrowBookSheet = true) }
    }

    fun closeBorrowBookForm() {
        _uiState.update { it.copy(showBorrowBookSheet = false) }
    }

    private fun showAlert(title: String, message: String) {
        _uiState.update { it.copy(showAlert = true, alertTitle = title, alertMessage = message) }
    }

    fun dismissAlert() {
        _uiState.update { it.copy(showAlert = false, alertTitle = "", alertMessage = "") }
    }

    fun getStudentInfo(borrowRecord: BorrowedBook): Student? {
        return studentCache[borrowRecord.studentId]
            ?: _students.value.firstOrNull { it.id == borrowRecord.studentId }
    }

    fun getBookInfo(borrowRecord: BorrowedBook): Pair<BookCopy?, BookTemplate?> {
        val bookCopy =
            bookCopyCache[borrowRecord.bookCopyId]
                ?: _bookCopies.value.firstOrNull { it.id == borrowRecord.bookCopyId }
        val bookTemplate =
            bookCopy?.bookTemplateId?.let { templateId ->
                bookTemplateCache[templateId]
                    ?: _bookTemplates.value.firstOrNull { it.id == templateId }
            }
        return Pair(bookCopy, bookTemplate)
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Overdue Management
    // ══════════════════════════════════════════════════════════════

    fun checkOverdueBooks() {
        val currentOverdue = _borrowedBooks.value.filter { it.isOverdue }
        _overdueBooks.value = currentOverdue
        if (currentOverdue.isNotEmpty()) {
            println("⚠️ ${currentOverdue.size} kitap suresi gecmis")
        }
    }

    fun getUpcomingDueBooks(days: Int = 2): List<BorrowedBook> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, days)
        val warningDate = calendar.time

        return _borrowedBooks.value.filter { borrowRecord ->
            !borrowRecord.isReturned &&
                    !borrowRecord.isOverdue &&
                    borrowRecord.dueDate.toDate() <= warningDate
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Reports & Analytics
    // ══════════════════════════════════════════════════════════════

    fun getMonthlyBorrowReport(): MonthlyBorrowReport {
        val calendar = Calendar.getInstance()
        val thisMonth = calendar.get(Calendar.MONTH)
        val thisYear = calendar.get(Calendar.YEAR)

        val thisMonthBorrows =
            _borrowedBooks.value.filter { borrowRecord ->
                val borrowDate = borrowRecord.borrowDate.toDate()
                val cal = Calendar.getInstance().apply { time = borrowDate }
                cal.get(Calendar.MONTH) == thisMonth && cal.get(Calendar.YEAR) == thisYear
            }

        val thisMonthReturns =
            _borrowedBooks.value.filter { borrowRecord ->
                val returnDate = borrowRecord.returnDate?.toDate() ?: return@filter false
                val cal = Calendar.getInstance().apply { time = returnDate }
                cal.get(Calendar.MONTH) == thisMonth && cal.get(Calendar.YEAR) == thisYear
            }

        return MonthlyBorrowReport(
            month = thisMonth,
            year = thisYear,
            totalBorrows = thisMonthBorrows.size,
            totalReturns = thisMonthReturns.size,
            activeBorrows = thisMonthBorrows.count { !it.isReturned }
        )
    }

    fun getMostBorrowedBooks(limit: Int = 5): List<Pair<BookTemplate, Int>> {
        val bookBorrowCounts = mutableMapOf<String, Int>()

        for (borrowRecord in _borrowedBooks.value) {
            val bookCopy = _bookCopies.value.firstOrNull { it.id == borrowRecord.bookCopyId }
            if (bookCopy != null) {
                bookBorrowCounts[bookCopy.bookTemplateId] =
                    (bookBorrowCounts[bookCopy.bookTemplateId] ?: 0) + 1
            }
        }

        return bookBorrowCounts.entries.sortedByDescending { it.value }.take(limit)
            .mapNotNull { (templateId, count) ->
                val template = _bookTemplates.value.firstOrNull { it.id == templateId }
                template?.let { Pair(it, count) }
            }
    }

    fun getMostActiveStudents(limit: Int = 5): List<Pair<Student, Int>> {
        val studentBorrowCounts = mutableMapOf<String, Int>()

        for (borrowRecord in _borrowedBooks.value) {
            studentBorrowCounts[borrowRecord.studentId] =
                (studentBorrowCounts[borrowRecord.studentId] ?: 0) + 1
        }

        return studentBorrowCounts.entries.sortedByDescending { it.value }.take(limit)
            .mapNotNull { (studentId, count) ->
                val student = _students.value.firstOrNull { it.id == studentId }
                student?.let { Pair(it, count) }
            }
    }

    fun getFilterCount(option: BorrowFilterOption): Int? {
        return when (option) {
            BorrowFilterOption.ALL -> null
            BorrowFilterOption.ACTIVE -> statisticsInfo.active
            BorrowFilterOption.RETURNED -> statisticsInfo.returned
            BorrowFilterOption.OVERDUE -> statisticsInfo.overdue
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - UI State
// ══════════════════════════════════════════════════════════════

data class BorrowingUiState(
    val isLoading: Boolean = false,
    val loadingState: LoadingState = LoadingState.Idle,
    val isBorrowingBook: Boolean = false,
    val isReturningBook: Boolean = false,
    val showBorrowBookSheet: Boolean = false,

    // Alert
    val showAlert: Boolean = false,
    val alertTitle: String = "",
    val alertMessage: String = ""
)

// ══════════════════════════════════════════════════════════════
// MARK: - Supporting Types
// ══════════════════════════════════════════════════════════════

enum class BorrowFilterOption(val displayName: String) {
    ALL("Tümü"),
    ACTIVE("Ödünçte"),
    RETURNED("İade Edildi"),
    OVERDUE("Süresi Geçti");

    val icon: String
        get() =
            when (this) {
                ALL -> "list"
                ACTIVE -> "book"
                RETURNED -> "check_circle"
                OVERDUE -> "warning"
            }
}

data class BorrowingStatistics(
    val total: Int,
    val active: Int,
    val returned: Int,
    val overdue: Int
) {
    val returnRate: Double
        get() = if (total > 0) returned.toDouble() / total * 100 else 0.0

    val overdueRate: Double
        get() = if (active > 0) overdue.toDouble() / active * 100 else 0.0
}

data class MonthlyBorrowReport(
    val month: Int,
    val year: Int,
    val totalBorrows: Int,
    val totalReturns: Int,
    val activeBorrows: Int
) {
    val monthName: String
        get() {
            val formatter = SimpleDateFormat("MMMM", Locale("tr", "TR"))
            val calendar = Calendar.getInstance().apply { set(Calendar.MONTH, month) }
            return formatter.format(calendar.time)
        }

    val displayText: String
        get() = "$monthName $year: $totalBorrows odunc, $totalReturns iade"
}
