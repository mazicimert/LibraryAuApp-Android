package com.mehmetmertmazici.libraryauapp.ui.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmetmertmazici.libraryauapp.data.model.LoadingState
import com.mehmetmertmazici.libraryauapp.data.model.Permission
import com.mehmetmertmazici.libraryauapp.data.model.Student
import com.mehmetmertmazici.libraryauapp.data.repository.AuthRepository
import com.mehmetmertmazici.libraryauapp.data.repository.FirebaseRepository
import com.mehmetmertmazici.libraryauapp.data.repository.NetworkManager
import com.mehmetmertmazici.libraryauapp.domain.util.searchNormalized
import com.mehmetmertmazici.libraryauapp.domain.util.trimmed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * StudentListViewModel
 * Öğrenci listesi ve öğrenci yönetimi işlemlerini yöneten ViewModel
 *
 * iOS Karşılığı: StudentListViewModel.swift
 */
@HiltViewModel
class StudentListViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val authRepository: AuthRepository,
    private val networkManager: NetworkManager
) : ViewModel() {

    // ── UI State ──
    private val _uiState = MutableStateFlow(StudentListUiState())
    val uiState: StateFlow<StudentListUiState> = _uiState.asStateFlow()

    // ── Network State ──
    val isOnline: StateFlow<Boolean> = networkManager.isOnline

    // ── Search ──
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    // ── All Students ──
    private val _students = MutableStateFlow<List<Student>>(emptyList())

    // ── Filtered Students ──
    val filteredStudents: StateFlow<List<Student>> = combine(
        _students,
        _searchText
    ) { students, search ->
        filterStudents(students, search)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ══════════════════════════════════════════════════════════════
    // MARK: - Computed Properties
    // ══════════════════════════════════════════════════════════════

    val canAddStudents: Boolean
        get() = authRepository.hasPermission(Permission.MANAGE_STUDENTS) &&
                networkManager.isOnline.value

    val showEmptyState: Boolean
        get() = !_uiState.value.isLoading && filteredStudents.value.isEmpty()

    val emptyStateMessage: String
        get() = when {
            _searchText.value.isNotEmpty() -> "Arama kriterlerinize uygun öğrenci bulunamadı"
            _students.value.isEmpty() -> "Henüz öğrenci eklenmemiş"
            else -> "Öğrenci bulunamadı"
        }

    val statisticsText: String
        get() {
            val totalCount = _students.value.size
            val filteredCount = filteredStudents.value.size
            return if (_searchText.value.isEmpty()) {
                "Toplam $totalCount öğrenci"
            } else {
                "$filteredCount/$totalCount öğrenci gösteriliyor"
            }
        }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Init
    // ══════════════════════════════════════════════════════════════

    init {
        loadStudents()
        observeNetworkChanges()
    }

    private fun observeNetworkChanges() {
        viewModelScope.launch {
            networkManager.isOnline.collect { isOnline ->
                if (isOnline && _students.value.isEmpty()) {
                    loadStudents()
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Data Loading
    // ══════════════════════════════════════════════════════════════

    fun loadStudents() {
        if (!networkManager.isOnline.value) {
            _uiState.update { it.copy(loadingState = LoadingState.Error("İnternet bağlantısı gereklidir")) }
            return
        }

        if (_uiState.value.isLoading) return

        _uiState.update { it.copy(isLoading = true, loadingState = LoadingState.Loading) }

        viewModelScope.launch {
            firebaseRepository.fetchStudents()
                .onSuccess { students ->
                    _students.value = students
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingState = LoadingState.Success
                        )
                    }
                    println("✅ ${students.size} öğrenci başarıyla yüklendi")
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingState = LoadingState.Error(error.message ?: "Bilinmeyen hata")
                        )
                    }
                    showAlert("Yükleme Hatası", "Öğrenciler yüklenirken hata oluştu: ${error.message}")
                }
        }
    }

    fun refreshStudents() {
        loadStudents()
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

    private fun filterStudents(students: List<Student>, search: String): List<Student> {
        val trimmedSearch = search.trimmed
        if (trimmedSearch.isEmpty()) {
            return students.sortedBy { it.name }
        }

        val searchTerm = trimmedSearch.searchNormalized
        return students.filter { student ->
            student.name.searchNormalized.contains(searchTerm) ||
                    student.surname.searchNormalized.contains(searchTerm) ||
                    student.fullName.searchNormalized.contains(searchTerm) ||
                    student.studentNumber.searchNormalized.contains(searchTerm) ||
                    student.email.searchNormalized.contains(searchTerm)
        }.sortedBy { it.name }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Delete Operations
    // ══════════════════════════════════════════════════════════════

    fun requestDeleteStudent(student: Student) {
        val studentId = student.id ?: return
        _uiState.update {
            it.copy(
                pendingDeleteStudentId = studentId,
                pendingDeleteStudentName = student.fullName,
                showDeleteConfirmation = true
            )
        }
    }

    fun confirmDeleteStudent() {
        val studentId = _uiState.value.pendingDeleteStudentId ?: return
        val studentName = _uiState.value.pendingDeleteStudentName ?: ""

        _uiState.update { it.copy(isLoading = true, showDeleteConfirmation = false) }

        viewModelScope.launch {
            firebaseRepository.deleteStudent(studentId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pendingDeleteStudentId = null,
                            pendingDeleteStudentName = null
                        )
                    }
                    loadStudents()
                    showAlert("Başarılı", "'$studentName' öğrencisi başarıyla silindi")
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pendingDeleteStudentId = null,
                            pendingDeleteStudentName = null
                        )
                    }
                    showAlert("Silme Hatası", "Öğrenci silinirken hata oluştu: ${error.message}")
                }
        }
    }

    fun cancelDeleteStudent() {
        _uiState.update {
            it.copy(
                pendingDeleteStudentId = null,
                pendingDeleteStudentName = null,
                showDeleteConfirmation = false
            )
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Export
    // ══════════════════════════════════════════════════════════════

    fun exportStudentsToCSV(): String {
        val csvContent = StringBuilder()
        csvContent.append("Ad,Soyad,Öğrenci Numarası,E-posta\n")

        _students.value.forEach { student ->
            csvContent.append("${student.name},${student.surname},${student.studentNumber},${student.email}\n")
        }

        return csvContent.toString()
    }

    fun requestExport() {
        _uiState.update { it.copy(showExportSheet = true) }
    }

    fun dismissExportSheet() {
        _uiState.update { it.copy(showExportSheet = false) }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Navigation
    // ══════════════════════════════════════════════════════════════

    fun openAddStudentForm() {
        _uiState.update { it.copy(showAddStudentSheet = true) }
    }

    fun closeAddStudentSheet() {
        _uiState.update { it.copy(showAddStudentSheet = false) }
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
 * Student List UI State
 */
data class StudentListUiState(
    val isLoading: Boolean = false,
    val loadingState: LoadingState = LoadingState.Idle,

    // Sheets
    val showAddStudentSheet: Boolean = false,
    val showExportSheet: Boolean = false,

    // Delete confirmation
    val showDeleteConfirmation: Boolean = false,
    val pendingDeleteStudentId: String? = null,
    val pendingDeleteStudentName: String? = null,

    // Alert
    val showAlert: Boolean = false,
    val alertTitle: String = "",
    val alertMessage: String = ""
)