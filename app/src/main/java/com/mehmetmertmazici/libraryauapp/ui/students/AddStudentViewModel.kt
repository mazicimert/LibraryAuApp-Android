package com.mehmetmertmazici.libraryauapp.ui.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmetmertmazici.libraryauapp.data.model.Student
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
 * AddStudentViewModel
 * Yeni öğrenci ekleme işlemlerini yöneten ViewModel
 *
 * iOS Karşılığı: StudentListViewModel.swift içindeki add student işlemleri
 */
@HiltViewModel
class AddStudentViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val networkManager: NetworkManager
) : ViewModel() {

    // ── UI State ──
    private val _uiState = MutableStateFlow(AddStudentUiState())
    val uiState: StateFlow<AddStudentUiState> = _uiState.asStateFlow()

    // ── Existing Students (for uniqueness check) ──
    private val _existingStudents = MutableStateFlow<List<Student>>(emptyList())

    init {
        loadExistingStudents()
    }

    private fun loadExistingStudents() {
        viewModelScope.launch {
            firebaseRepository.fetchStudents()
                .onSuccess { students ->
                    _existingStudents.value = students
                }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Form Field Updates
    // ══════════════════════════════════════════════════════════════

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value) }
        validateForm()
    }

    fun updateSurname(value: String) {
        _uiState.update { it.copy(surname = value) }
        validateForm()
    }

    fun updateStudentNumber(value: String) {
        // Sadece rakamları al ve maksimum 8 hane
        val digits = value.filter { it.isDigit() }.take(8)
        _uiState.update {
            it.copy(
                studentNumber = digits,
                isStudentNumberValid = digits.length == 8 && isStudentNumberUnique(digits),
                isStudentNumberUnique = isStudentNumberUnique(digits)
            )
        }
        validateForm()
    }

    fun updateEmail(value: String) {
        _uiState.update {
            it.copy(
                email = value,
                isEmailValid = isValidEmail(value)
            )
        }
        validateForm()
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Validation
    // ══════════════════════════════════════════════════════════════

    private fun validateForm() {
        val errors = mutableListOf<String>()
        val state = _uiState.value

        // Name validation
        if (state.name.trimmed.isEmpty()) {
            errors.add("Öğrenci adı boş olamaz")
        } else if (state.name.trimmed.length < 2) {
            errors.add("Öğrenci adı en az 2 karakter olmalıdır")
        }

        // Surname validation
        if (state.surname.trimmed.isEmpty()) {
            errors.add("Öğrenci soyadı boş olamaz")
        } else if (state.surname.trimmed.length < 2) {
            errors.add("Öğrenci soyadı en az 2 karakter olmalıdır")
        }

        // Student number validation
        if (state.studentNumber.trimmed.isEmpty()) {
            errors.add("Öğrenci numarası boş olamaz")
        } else if (state.studentNumber.trimmed.length < 8) {
            errors.add("Öğrenci numarası 8 haneli olmalıdır (şu an: ${state.studentNumber.trimmed.length} hane)")
        } else if (state.studentNumber.trimmed.length > 8) {
            errors.add("Öğrenci numarası en fazla 8 hane olabilir")
        } else if (!isStudentNumberUnique(state.studentNumber.trimmed)) {
            errors.add("Bu öğrenci numarası zaten kullanılıyor")
        }

        // Email validation
        if (state.email.trimmed.isEmpty()) {
            errors.add("E-posta adresi boş olamaz")
        } else if (!isValidEmail(state.email)) {
            errors.add("Geçerli bir e-posta adresi giriniz")
        }

        val isFormValid = errors.isEmpty() &&
                state.name.trimmed.length >= 2 &&
                state.surname.trimmed.length >= 2 &&
                state.studentNumber.trimmed.length == 8 &&
                isStudentNumberUnique(state.studentNumber.trimmed) &&
                isValidEmail(state.email)

        _uiState.update {
            it.copy(
                validationErrors = errors,
                isFormValid = isFormValid
            )
        }
    }

    fun isStudentNumberUnique(studentNumber: String, excludingId: String? = null): Boolean {
        return !_existingStudents.value.any { student ->
            student.studentNumber == studentNumber && student.id != excludingId
        }
    }

    fun isValidEmail(email: String): Boolean {
        val emailRegex = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}".toRegex()
        return emailRegex.matches(email)
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Add Student
    // ══════════════════════════════════════════════════════════════

    fun addStudent(onSuccess: () -> Unit) {
        val state = _uiState.value

        if (!state.isFormValid) return

        if (!networkManager.isOnline.value) {
            showAlert("Bağlantı Hatası", "Öğrenci eklemek için internet bağlantısı gereklidir")
            return
        }

        // Tekrar uniqueness kontrolü
        if (!isStudentNumberUnique(state.studentNumber.trimmed)) {
            showAlert("Tekrar Eden Numara", "Bu öğrenci numarası zaten kullanılıyor")
            return
        }

        _uiState.update { it.copy(isAddingStudent = true) }

        val newStudent = Student(
            name = state.name.trimmed,
            surname = state.surname.trimmed,
            studentNumber = state.studentNumber.trimmed,
            email = state.email.trimmed.lowercase()
        )

        viewModelScope.launch {
            firebaseRepository.addStudent(newStudent)
                .onSuccess { studentId ->
                    _uiState.update { it.copy(isAddingStudent = false) }
                    showAlert("Başarılı", "'${newStudent.fullName}' öğrencisi başarıyla eklendi")
                    println("✅ Öğrenci eklendi: $studentId")
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isAddingStudent = false) }
                    showAlert("Ekleme Hatası", "Öğrenci eklenirken hata oluştu: ${error.message}")
                    println("❌ Öğrenci ekleme hatası: ${error.message}")
                }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MARK: - Help Sheet
    // ══════════════════════════════════════════════════════════════

    fun showHelpSheet() {
        _uiState.update { it.copy(showHelpSheet = true) }
    }

    fun dismissHelpSheet() {
        _uiState.update { it.copy(showHelpSheet = false) }
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
        _uiState.update { AddStudentUiState() }
    }
}

/**
 * Add Student UI State
 */
data class AddStudentUiState(
    // Form fields
    val name: String = "",
    val surname: String = "",
    val studentNumber: String = "",
    val email: String = "",

    // Validation status
    val isStudentNumberValid: Boolean = false,
    val isStudentNumberUnique: Boolean = true,
    val isEmailValid: Boolean = false,
    val isFormValid: Boolean = false,
    val validationErrors: List<String> = emptyList(),

    // UI state
    val isAddingStudent: Boolean = false,
    val showHelpSheet: Boolean = false,

    // Alert
    val showAlert: Boolean = false,
    val alertTitle: String = "",
    val alertMessage: String = ""
)