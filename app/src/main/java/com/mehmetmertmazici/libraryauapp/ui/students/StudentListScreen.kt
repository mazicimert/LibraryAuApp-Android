package com.mehmetmertmazici.libraryauapp.ui.students

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mehmetmertmazici.libraryauapp.data.model.Student
import com.mehmetmertmazici.libraryauapp.ui.components.EmptyStateView
import com.mehmetmertmazici.libraryauapp.ui.components.LoadingOverlay
import com.mehmetmertmazici.libraryauapp.ui.theme.*

/**
 * StudentListScreen Öğrenci listesi ana ekranı
 *
 * iOS Karşılığı: StudentListView.swift
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentListScreen(
    viewModel: StudentListViewModel = hiltViewModel(),
    onStudentClick: (Student) -> Unit,
    onAddStudent: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val filteredStudents by viewModel.filteredStudents.collectAsState()
    val searchText by viewModel.searchText.collectAsState()

    val focusManager = LocalFocusManager.current
    val colorScheme = MaterialTheme.colorScheme

    // Delete confirmation dialog
    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteStudent() },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AnkaraDanger.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = AnkaraDanger,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Öğrenciyi Sil",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "'${uiState.pendingDeleteStudentName}' isimli öğrenciyi silmek istediğinize emin misiniz? Bu işlem geri alınamaz.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteStudent() },
                    colors = ButtonDefaults.buttonColors(containerColor = AnkaraDanger),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sil", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.cancelDeleteStudent() },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text("İptal", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Export confirmation dialog
    if (uiState.showExportSheet) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissExportSheet() },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Öğrenci Listesi Export",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "Öğrenci listesini CSV olarak dışa aktarmak istiyor musunuz?",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissExportSheet()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CSV olarak Export Et", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissExportSheet() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("İptal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Alert dialog
    if (uiState.showAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAlert() },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = uiState.alertTitle,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = uiState.alertMessage,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissAlert() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tamam", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    AnkaraBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Header Section ──
                HeaderSection(
                    onExport = { viewModel.requestExport() },
                    onAddStudent = onAddStudent,
                    canAddStudents = viewModel.canAddStudents,
                    hasStudents = filteredStudents.isNotEmpty()
                )

                // ── Search Section ──
                SearchSection(
                    searchText = searchText,
                    onSearchTextChange = { viewModel.updateSearchText(it) },
                    onClearSearch = { viewModel.clearSearch() },
                    onDone = { focusManager.clearFocus() }
                )

                // ── Statistics Section ──
                if (filteredStudents.isNotEmpty()) {
                    StatisticsSection(
                        statisticsText = viewModel.statisticsText,
                        isOnline = isOnline
                    )
                }

                // ── Student List Section ──
                StudentListSection(
                    students = filteredStudents,
                    isLoading = uiState.isLoading,
                    showEmptyState = viewModel.showEmptyState,
                    emptyStateMessage = viewModel.emptyStateMessage,
                    canAddStudents = viewModel.canAddStudents,
                    onStudentClick = onStudentClick,
                    onDeleteStudent = { viewModel.requestDeleteStudent(it) },
                    onRefresh = { viewModel.refreshStudents() },
                    onAddStudent = onAddStudent
                )
            }

            // Loading overlay
            if (uiState.isLoading) {
                LoadingOverlay(message = "Yükleniyor...")
            }
        }
    }
}

@Composable
private fun HeaderSection(
    onExport: () -> Unit,
    onAddStudent: () -> Unit,
    canAddStudents: Boolean,
    hasStudents: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Öğrenciler",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onBackground
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Export button
            if (hasStudents) {
                IconButton(onClick = onExport) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Export",
                        tint = AnkaraBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Add student button
            if (canAddStudents) {
                IconButton(onClick = onAddStudent) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Öğrenci Ekle",
                        tint = AnkaraBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchSection(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onDone: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            placeholder = {
                Text(
                    "Öğrenci ara (ad, soyad, no)...",
                    color = colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Temizle",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AnkaraBlue,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor =
                        colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor =
                        colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
        )
    }
}

@Composable
private fun StatisticsSection(statisticsText: String, isOnline: Boolean) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = statisticsText, fontSize = 12.sp, color = colorScheme.onSurfaceVariant)

        if (!isOnline) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.WifiOff,
                    contentDescription = null,
                    tint = AnkaraWarning,
                    modifier = Modifier.size(14.dp)
                )
                Text(text = "Çevrimdışı", fontSize = 12.sp, color = AnkaraWarning)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentListSection(
    students: List<Student>,
    isLoading: Boolean,
    showEmptyState: Boolean,
    emptyStateMessage: String,
    canAddStudents: Boolean,
    onStudentClick: (Student) -> Unit,
    onDeleteStudent: (Student) -> Unit,
    onRefresh: () -> Unit,
    onAddStudent: () -> Unit
) {
    if (showEmptyState) {
        EmptyStateView(
            icon = Icons.Filled.People,
            title = "Öğrenci Bulunamadı",
            message = emptyStateMessage,
            actionTitle = if (canAddStudents) "İlk Öğrenciyi Ekle" else null,
            onAction = onAddStudent
        )
    } else {
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(students, key = { it.id ?: it.hashCode() }) { student ->
                    StudentRowView(
                        student = student,
                        onClick = { onStudentClick(student) },
                        onDelete = { onDeleteStudent(student) },
                        canDelete = canAddStudents
                    )
                }
            }
        }
    }
}

@Composable
fun StudentRowView(
    student: Student,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = colorScheme.surface.copy(alpha = 0.9f)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Student avatar
            Box(
                modifier =
                    Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            brush =
                                Brush.linearGradient(
                                    colors =
                                        listOf(
                                            AnkaraSuccess,
                                            AnkaraSuccess.copy(
                                                alpha = 0.7f
                                            )
                                        )
                                )
                        ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = student.name.take(1).uppercase(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Student info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = student.fullName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tag,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = student.studentNumber,
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = student.email,
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Detail chevron
                Box(
                    modifier =
                        Modifier
                            .size(32.dp)
                            .background(
                                color = AnkaraLightBlue.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = AnkaraLightBlue
                    )
                }

                // Delete button
                if (canDelete) {
                    Box(
                        modifier =
                            Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AnkaraDanger)
                                .clickable { onDelete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Sil",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
