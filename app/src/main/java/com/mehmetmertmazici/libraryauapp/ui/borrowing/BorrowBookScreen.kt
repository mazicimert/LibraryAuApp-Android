package com.mehmetmertmazici.libraryauapp.ui.borrowing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mehmetmertmazici.libraryauapp.data.model.BookCopy
import com.mehmetmertmazici.libraryauapp.data.model.BookTemplate
import com.mehmetmertmazici.libraryauapp.data.model.BorrowingRule
import com.mehmetmertmazici.libraryauapp.data.model.Student
import com.mehmetmertmazici.libraryauapp.ui.components.LoadingOverlay
import com.mehmetmertmazici.libraryauapp.ui.theme.AnkaraBlue
import com.mehmetmertmazici.libraryauapp.ui.theme.AnkaraDanger
import com.mehmetmertmazici.libraryauapp.ui.theme.AnkaraLightBlue
import com.mehmetmertmazici.libraryauapp.ui.theme.AnkaraSuccess
import com.mehmetmertmazici.libraryauapp.ui.theme.AnkaraWarning
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * BorrowBookScreen
 * Kitap odunc verme ekrani
 *
 * iOS Karsiligi: BorrowBookView.swift
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorrowBookScreen(
    viewModel: BorrowingViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val borrowStudentNumber by viewModel.borrowStudentNumber.collectAsState()
    val borrowBookBarcode by viewModel.borrowBookBarcode.collectAsState()
    val selectedStudent by viewModel.selectedStudent.collectAsState()
    val selectedBookCopy by viewModel.selectedBookCopy.collectAsState()
    val selectedBookTemplate by viewModel.selectedBookTemplate.collectAsState()

    // Ekran acildiginda formu temizle ve verileri tazele
    LaunchedEffect(Unit) {
        viewModel.clearBorrowForm()
        viewModel.refreshData()
    }

    // General alert dialog
    if (uiState.showAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAlert() },
            title = { Text(uiState.alertTitle) },
            text = { Text(uiState.alertMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissAlert() }) {
                    Text("Tamam")
                }
            }
        )
    }

    LoadingOverlay(
        isLoading = uiState.isBorrowingBook,
        message = "Kitap odunc veriliyor..."
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Kitap Odunc Ver") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Iptal")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { viewModel.borrowBook() },
                            enabled = viewModel.isBorrowFormValid && !uiState.isBorrowingBook
                        ) {
                            Text(
                                text = "Odunc Ver",
                                fontWeight = FontWeight.Bold,
                                color = if (viewModel.isBorrowFormValid && !uiState.isBorrowingBook)
                                    AnkaraBlue
                                else
                                    Color.Gray
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Ogrenci Arama Bolumu
                StudentSearchSection(
                    studentNumber = borrowStudentNumber,
                    onStudentNumberChange = { viewModel.updateBorrowStudentNumber(it) },
                    onSearch = { viewModel.searchStudent() },
                    selectedStudent = selectedStudent
                )

                // Kitap Arama Bolumu
                BookSearchSection(
                    barcode = borrowBookBarcode,
                    onBarcodeChange = { viewModel.updateBorrowBookBarcode(it) },
                    onSearch = { viewModel.searchBookByBarcode() },
                    selectedBookTemplate = selectedBookTemplate,
                    selectedBookCopy = selectedBookCopy
                )

                // Secilen Bilgiler Ozeti
                if (selectedStudent != null || selectedBookTemplate != null) {
                    SelectedItemsSection(
                        student = selectedStudent,
                        bookTemplate = selectedBookTemplate,
                        bookCopy = selectedBookCopy
                    )
                }

                // Odunc Kurallari Bilgisi
                BorrowingRulesSection()

                // Validasyon ve Uyarilar
                val validationMessages = buildValidationMessages(
                    selectedStudent = selectedStudent,
                    selectedBookTemplate = selectedBookTemplate,
                    selectedBookCopy = selectedBookCopy
                )
                if (validationMessages.isNotEmpty()) {
                    ValidationSection(messages = validationMessages)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - Student Search Section
// ══════════════════════════════════════════════════════════════

@Composable
private fun StudentSearchSection(
    studentNumber: String,
    onStudentNumberChange: (String) -> Unit,
    onSearch: () -> Unit,
    selectedStudent: Student?
) {
    SectionCard(
        title = "Ogrenci Bilgileri",
        footer = "Ogrenci numarasi 8 haneli olmalidir"
    ) {
        // Ogrenci Numarasi Girisi
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = studentNumber,
                onValueChange = onStudentNumberChange,
                label = { Text("Ogrenci numarasi (8 hane)") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (studentNumber.length == 8) onSearch()
                    }
                ),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )

            Button(
                onClick = onSearch,
                enabled = studentNumber.length == 8,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AnkaraBlue
                )
            ) {
                Text("Ara")
            }
        }

        // Secilen Ogrenci Bilgisi
        if (selectedStudent != null) {
            Spacer(modifier = Modifier.height(8.dp))
            StudentSelectedView(student = selectedStudent)
        } else if (studentNumber.isNotEmpty() && studentNumber.length == 8) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = AnkaraWarning,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Ogrenci bulunamadi",
                    style = MaterialTheme.typography.labelSmall,
                    color = AnkaraWarning
                )
            }
        } else if (studentNumber.isNotEmpty() && studentNumber.length < 8) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = AnkaraLightBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Kalan: ${8 - studentNumber.length} hane",
                    style = MaterialTheme.typography.labelSmall,
                    color = AnkaraLightBlue
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - Book Search Section
// ══════════════════════════════════════════════════════════════

@Composable
private fun BookSearchSection(
    barcode: String,
    onBarcodeChange: (String) -> Unit,
    onSearch: () -> Unit,
    selectedBookTemplate: BookTemplate?,
    selectedBookCopy: BookCopy?
) {
    SectionCard(
        title = "Kitap Bilgileri",
        footer = "Barkod okutarak veya manuel arama yapabilirsiniz"
    ) {
        // Barkod Girisi
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = barcode,
                onValueChange = onBarcodeChange,
                label = { Text("Kitap barkodu") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { onSearch() }
                ),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )

            Button(
                onClick = onSearch,
                enabled = barcode.isNotEmpty(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AnkaraBlue
                )
            ) {
                Text("Ara")
            }
        }

        // Secilen Kitap Bilgisi
        if (selectedBookTemplate != null && selectedBookCopy != null) {
            Spacer(modifier = Modifier.height(8.dp))
            BookSelectedView(
                bookTemplate = selectedBookTemplate,
                bookCopy = selectedBookCopy
            )
        } else if (barcode.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Kitap bulunamadi veya musait degil",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - StudentSelectedView
// ══════════════════════════════════════════════════════════════

@Composable
private fun StudentSelectedView(student: Student) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AnkaraSuccess.copy(alpha = 0.05f))
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(AnkaraSuccess.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = student.name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AnkaraSuccess
            )
        }

        // Bilgiler
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = student.fullName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "No: ${student.studentNumber}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = student.email,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Secildi ikonu
        Column(horizontalAlignment = Alignment.End) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = AnkaraSuccess,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Secildi",
                style = MaterialTheme.typography.labelSmall,
                color = AnkaraSuccess
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - BookSelectedView
// ══════════════════════════════════════════════════════════════

@Composable
private fun BookSelectedView(
    bookTemplate: BookTemplate,
    bookCopy: BookCopy
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AnkaraLightBlue.copy(alpha = 0.05f))
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Kitap ikonu
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(AnkaraLightBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Book,
                contentDescription = null,
                tint = AnkaraLightBlue,
                modifier = Modifier.size(24.dp)
            )
        }

        // Bilgiler
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bookTemplate.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Yazar: ${bookTemplate.author}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Barkod: ${bookCopy.barcode}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Musait ikonu
        Column(horizontalAlignment = Alignment.End) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = AnkaraLightBlue,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Musait",
                style = MaterialTheme.typography.labelSmall,
                color = AnkaraLightBlue
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - Selected Items Section
// ══════════════════════════════════════════════════════════════

@Composable
private fun SelectedItemsSection(
    student: Student?,
    bookTemplate: BookTemplate?,
    bookCopy: BookCopy?
) {
    val dateFormatter = remember {
        SimpleDateFormat("d MMMM yyyy", Locale("tr", "TR"))
    }

    SectionCard(title = "Odunc Ozeti") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Ogrenci Ozeti
            if (student != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = AnkaraSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = student.fullName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "No: ${student.studentNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AnkaraSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Kitap Ozeti
            if (bookTemplate != null && bookCopy != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Book,
                        contentDescription = null,
                        tint = AnkaraLightBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bookTemplate.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Barkod: ${bookCopy.barcode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AnkaraSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Odunc Detaylari
            if (student != null && bookTemplate != null) {
                HorizontalDivider()

                val today = Date()
                val calendar = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, BorrowingRule.DEFAULT_BORROW_DAYS)
                }
                val dueDate = calendar.time

                SummaryRow("Odunc Tarihi:", dateFormatter.format(today))
                SummaryRow("Teslim Tarihi:", dateFormatter.format(dueDate))
                SummaryRow(
                    "Odunc Suresi:",
                    "${BorrowingRule.DEFAULT_BORROW_DAYS} gun",
                    valueColor = AnkaraLightBlue
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onBackground
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - Borrowing Rules Section
// ══════════════════════════════════════════════════════════════

@Composable
private fun BorrowingRulesSection() {
    SectionCard(
        title = "Odunc Kurallari",
        footer = "Bu kurallar sistem ayarlarindan degistirilebilir"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RuleRowView(
                icon = Icons.Default.Book,
                title = "Maksimum Kitap",
                value = "${BorrowingRule.MAX_BOOKS_PER_STUDENT} kitap/ogrenci"
            )
            RuleRowView(
                icon = Icons.Default.CalendarMonth,
                title = "Odunc Suresi",
                value = "${BorrowingRule.DEFAULT_BORROW_DAYS} gun"
            )
            RuleRowView(
                icon = Icons.Default.Warning,
                title = "Ayni Kitap",
                value = "Ogrenci ayni kitaptan alamaz"
            )
            RuleRowView(
                icon = Icons.Default.Info,
                title = "Gecikme",
                value = "Ceza yok, sadece uyari"
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - RuleRowView
// ══════════════════════════════════════════════════════════════

@Composable
private fun RuleRowView(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AnkaraLightBlue,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - Validation Section
// ══════════════════════════════════════════════════════════════

@Composable
private fun ValidationSection(messages: List<String>) {
    SectionCard(title = null) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = AnkaraDanger,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Kontrol Edilmesi Gerekenler",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AnkaraDanger
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            messages.forEach { message ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = AnkaraDanger,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.labelSmall,
                        color = AnkaraDanger
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - Section Card
// ══════════════════════════════════════════════════════════════

@Composable
private fun SectionCard(
    title: String?,
    footer: String? = null,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
        if (footer != null) {
            Text(
                text = footer,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - Helper Functions
// ══════════════════════════════════════════════════════════════

private fun buildValidationMessages(
    selectedStudent: Student?,
    selectedBookTemplate: BookTemplate?,
    selectedBookCopy: BookCopy?
): List<String> {
    val messages = mutableListOf<String>()

    if (selectedStudent == null) {
        messages.add("Ogrenci secilmedi")
    }
    if (selectedBookTemplate == null) {
        messages.add("Kitap secilmedi")
    }
    if (selectedBookCopy == null) {
        messages.add("Musait kopya bulunamadi")
    }

    return messages
}
