package com.mehmetmertmazici.libraryauapp.ui.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mehmetmertmazici.libraryauapp.data.model.Student
import com.mehmetmertmazici.libraryauapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * TrashScreen
 * Çöp kutusu ekranı — silinen kitaplar ve öğrenciler
 *
 * iOS Karşılığı: TrashView.swift
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onBack: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Alert state management
    var showDeleteAlert by remember { mutableStateOf(false) }
    var showRestoreAlert by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var alertItemName by remember { mutableStateOf("") }
    var alertCopyCount by remember { mutableIntStateOf(0) }
    var alertIsBook by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Çöp Kutusu") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Segmented Tab ──
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SegmentedButton(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.updateSelectedTab(0) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Kitaplar")
                }
                SegmentedButton(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.updateSelectedTab(1) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Öğrenciler")
                }
            }

            // ── Content ──
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AnkaraBlue)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Yükleniyor...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                uiState.errorMessage != null -> {
                    TrashErrorView(message = uiState.errorMessage!!)
                }

                else -> {
                    when (uiState.selectedTab) {
                        0 -> BooksList(
                            deletedBooks = uiState.deletedBooks,
                            onRestore = { item ->
                                alertItemName = item.book.title
                                alertCopyCount = item.copyCount
                                alertIsBook = true
                                pendingAction = { viewModel.restoreBook(item) }
                                showRestoreAlert = true
                            },
                            onDelete = { item ->
                                alertItemName = item.book.title
                                alertCopyCount = item.copyCount
                                alertIsBook = true
                                pendingAction = { viewModel.deleteBookPermanently(item) }
                                showDeleteAlert = true
                            }
                        )

                        1 -> StudentsList(
                            deletedStudents = uiState.deletedStudents,
                            onRestore = { student ->
                                alertItemName = student.fullName
                                alertCopyCount = 0
                                alertIsBook = false
                                pendingAction = { viewModel.restoreStudent(student) }
                                showRestoreAlert = true
                            },
                            onDelete = { student ->
                                alertItemName = student.fullName
                                alertCopyCount = 0
                                alertIsBook = false
                                pendingAction = { viewModel.deleteStudentPermanently(student) }
                                showDeleteAlert = true
                            }
                        )
                    }
                }
            }
        }
    }

    // ── Delete Alert Dialog ──
    if (showDeleteAlert) {
        AlertDialog(
            onDismissRequest = {
                showDeleteAlert = false
                pendingAction = null
            },
            title = { Text("Kalıcı Olarak Sil") },
            text = {
                if (alertIsBook) {
                    val suffix = if (alertCopyCount > 0) " ve $alertCopyCount adet kopyası" else ""
                    Text("'$alertItemName' kitabı${suffix} KALICI OLARAK silinecek. Bu işlem geri alınamaz!")
                } else {
                    Text("'$alertItemName' öğrencisi kalıcı olarak silinecek. Bu işlem geri alınamaz!")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingAction?.invoke()
                        showDeleteAlert = false
                        pendingAction = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AnkaraDanger)
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteAlert = false
                    pendingAction = null
                }) {
                    Text("İptal")
                }
            }
        )
    }

    // ── Restore Alert Dialog ──
    if (showRestoreAlert) {
        AlertDialog(
            onDismissRequest = {
                showRestoreAlert = false
                pendingAction = null
            },
            title = { Text("Geri Yükle") },
            text = {
                if (alertIsBook) {
                    val suffix = if (alertCopyCount > 0) " ve $alertCopyCount adet kopyası" else ""
                    Text("'$alertItemName' kitabı${suffix} kütüphaneye geri eklenecek. Onaylıyor musunuz?")
                } else {
                    Text("'$alertItemName' öğrencisi sisteme geri eklenecek. Onaylıyor musunuz?")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingAction?.invoke()
                        showRestoreAlert = false
                        pendingAction = null
                    }
                ) {
                    Text("Geri Yükle")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreAlert = false
                    pendingAction = null
                }) {
                    Text("İptal")
                }
            }
        )
    }

    // ── Generic Alert ──
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
}

// ══════════════════════════════════════════════════════════════
// MARK: - Books List
// ══════════════════════════════════════════════════════════════

@Composable
private fun BooksList(
    deletedBooks: List<TrashBookItem>,
    onRestore: (TrashBookItem) -> Unit,
    onDelete: (TrashBookItem) -> Unit
) {
    if (deletedBooks.isEmpty()) {
        TrashEmptyState(
            icon = Icons.Filled.MenuBook,
            text = "Silinmiş kitap yok"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(deletedBooks, key = { it.id ?: "" }) { item ->
                BookTrashRow(
                    item = item,
                    onRestore = { onRestore(item) },
                    onDelete = { onDelete(item) }
                )
            }
        }
    }
}

@Composable
private fun BookTrashRow(
    item: TrashBookItem,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Kitap İkonu
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = AnkaraWarning.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Book,
                contentDescription = "Kitap",
                tint = AnkaraWarning,
                modifier = Modifier.size(20.dp)
            )
        }

        // Bilgiler
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.book.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Kopya sayısı rozeti
                if (item.copyCount > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(
                        modifier = Modifier
                            .background(
                                color = Color(0xFF2196F3).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Kopya",
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "${item.copyCount}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )
                    }
                }
            }

            Text(
                text = item.book.author,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Silinme tarihi
            item.book.deletedAt?.let { timestamp ->
                val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("tr"))
                val dateText = try {
                    dateFormat.format(timestamp.toDate())
                } catch (_: Exception) {
                    null
                }
                if (dateText != null) {
                    Text(
                        text = "Silindi: $dateText",
                        fontSize = 10.sp,
                        color = AnkaraDanger.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Aksiyon butonları
        TrashActionButtons(onRestore = onRestore, onDelete = onDelete)
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
}

// ══════════════════════════════════════════════════════════════
// MARK: - Students List
// ══════════════════════════════════════════════════════════════

@Composable
private fun StudentsList(
    deletedStudents: List<Student>,
    onRestore: (Student) -> Unit,
    onDelete: (Student) -> Unit
) {
    if (deletedStudents.isEmpty()) {
        TrashEmptyState(
            icon = Icons.Filled.PersonOff,
            text = "Silinmiş öğrenci yok"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(deletedStudents, key = { it.id ?: "" }) { student ->
                StudentTrashRow(
                    student = student,
                    onRestore = { onRestore(student) },
                    onDelete = { onDelete(student) }
                )
            }
        }
    }
}

@Composable
private fun StudentTrashRow(
    student: Student,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Öğrenci İkonu
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = Color(0xFF2196F3).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "Öğrenci",
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(20.dp)
            )
        }

        // Bilgiler
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = student.fullName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = student.studentNumber,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Silinme tarihi
            student.deletedAt?.let { timestamp ->
                val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("tr"))
                val dateText = try {
                    dateFormat.format(timestamp.toDate())
                } catch (_: Exception) {
                    null
                }
                if (dateText != null) {
                    Text(
                        text = "Silindi: $dateText",
                        fontSize = 10.sp,
                        color = AnkaraDanger.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Aksiyon butonları
        TrashActionButtons(onRestore = onRestore, onDelete = onDelete)
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
}

// ══════════════════════════════════════════════════════════════
// MARK: - Shared Components
// ══════════════════════════════════════════════════════════════

@Composable
private fun TrashActionButtons(
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Geri Yükle butonu
        IconButton(
            onClick = onRestore,
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = AnkaraSuccess.copy(alpha = 0.1f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Filled.Undo,
                contentDescription = "Geri Yükle",
                tint = AnkaraSuccess,
                modifier = Modifier.size(16.dp)
            )
        }

        // Kalıcı Sil butonu
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = AnkaraDanger.copy(alpha = 0.1f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Kalıcı Sil",
                tint = AnkaraDanger,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun TrashEmptyState(
    icon: ImageVector,
    text: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = Color.Gray.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TrashErrorView(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "Hata",
                tint = AnkaraWarning,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Hata: $message",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}
