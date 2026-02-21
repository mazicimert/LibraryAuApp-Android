package com.mehmetmertmazici.libraryauapp.ui.borrowing

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mehmetmertmazici.libraryauapp.data.model.BookCopy
import com.mehmetmertmazici.libraryauapp.data.model.BookTemplate
import com.mehmetmertmazici.libraryauapp.data.model.BorrowedBook
import com.mehmetmertmazici.libraryauapp.data.model.Student
import com.mehmetmertmazici.libraryauapp.ui.components.EmptyStateView
import com.mehmetmertmazici.libraryauapp.ui.components.LoadingOverlay
import com.mehmetmertmazici.libraryauapp.ui.theme.AnkaraBlue
import com.mehmetmertmazici.libraryauapp.ui.theme.AnkaraDanger
import com.mehmetmertmazici.libraryauapp.ui.theme.AnkaraLightBlue
import com.mehmetmertmazici.libraryauapp.ui.theme.AnkaraSuccess

/**
 * BorrowedBooksScreen
 * Odunc alinan kitaplar listesi ekrani
 *
 * iOS Karsiligi: BorrowedBooksView.swift
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorrowedBooksScreen(
    viewModel: BorrowingViewModel = hiltViewModel(),
    onBorrowBook: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredBooks by viewModel.filteredBorrowedBooks.collectAsState()
    val searchText by viewModel.searchText.collectAsState()
    val filterOption by viewModel.filterOption.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    var selectedBorrowRecord by remember { mutableStateOf<BorrowedBook?>(null) }
    var showReturnConfirmation by remember { mutableStateOf(false) }
    var showDetailSheet by remember { mutableStateOf(false) }
    var showMostBorrowedSheet by remember { mutableStateOf(false) }
    var showMonthlyReport by remember { mutableStateOf(false) }
    var monthlyReportData by remember { mutableStateOf<MonthlyBorrowReport?>(null) }
    var showReportMenu by remember { mutableStateOf(false) }

    // Return confirmation dialog
    if (showReturnConfirmation && selectedBorrowRecord != null) {
        val record = selectedBorrowRecord!!
        val studentInfo = viewModel.getStudentInfo(record)
        val bookInfo = viewModel.getBookInfo(record)

        AlertDialog(
            onDismissRequest = { showReturnConfirmation = false },
            title = { Text("Kitap Iade") },
            text = {
                Text(
                    "'${bookInfo.second?.title ?: "Kitap"}' kitabini ${studentInfo?.fullName ?: "ogrenci"}'den iade almak istediginizden emin misiniz?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.returnBook(record)
                        showReturnConfirmation = false
                    }
                ) {
                    Text("Iade Al", color = AnkaraDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReturnConfirmation = false }) {
                    Text("Iptal")
                }
            }
        )
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

    // Monthly report dialog
    if (showMonthlyReport && monthlyReportData != null) {
        val report = monthlyReportData!!
        AlertDialog(
            onDismissRequest = { showMonthlyReport = false },
            title = { Text("Aylik Rapor") },
            text = {
                Text(
                    "${report.monthName} ${report.year}\n\n" +
                            "Toplam Odunc: ${report.totalBorrows}\n" +
                            "Toplam Iade: ${report.totalReturns}\n" +
                            "Aktif Odunc: ${report.activeBorrows}"
                )
            },
            confirmButton = {
                TextButton(onClick = { showMonthlyReport = false }) {
                    Text("Tamam")
                }
            }
        )
    }

    // Detail bottom sheet
    if (showDetailSheet && selectedBorrowRecord != null) {
        val record = selectedBorrowRecord!!
        ModalBottomSheet(
            onDismissRequest = { showDetailSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            BorrowDetailSheet(
                borrowRecord = record,
                studentInfo = viewModel.getStudentInfo(record),
                bookInfo = viewModel.getBookInfo(record),
                onDismiss = { showDetailSheet = false }
            )
        }
    }

    // Most borrowed books bottom sheet
    if (showMostBorrowedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMostBorrowedSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            MostBorrowedBooksSheet(
                viewModel = viewModel,
                onDismiss = { showMostBorrowedSheet = false }
            )
        }
    }

    LoadingOverlay(
        isLoading = viewModel.showLoadingIndicator,
        message = "Yukleniyor..."
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Section
            HeaderSection()

            // Overdue Warning Banner
            if (viewModel.hasOverdueWarning) {
                OverdueWarningBanner(
                    message = viewModel.overdueWarningMessage,
                    onViewOverdue = { viewModel.updateFilterOption(BorrowFilterOption.OVERDUE) }
                )
            }

            // Filter & Search Section
            FilterSection(
                searchText = searchText,
                onSearchTextChange = { viewModel.updateSearchText(it) },
                onClearSearch = { viewModel.clearSearch() },
                filterOption = filterOption,
                onFilterOptionChange = { viewModel.updateFilterOption(it) },
                viewModel = viewModel
            )

            // Statistics Section
            StatisticsSection(statistics = viewModel.statisticsInfo)

            // Report Menu & Add Button Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rapor Menu
                Box {
                    IconButton(onClick = { showReportMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Raporlar",
                            tint = AnkaraLightBlue
                        )
                    }
                    DropdownMenu(
                        expanded = showReportMenu,
                        onDismissRequest = { showReportMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Aylik Rapor") },
                            onClick = {
                                showReportMenu = false
                                monthlyReportData = viewModel.getMonthlyBorrowReport()
                                showMonthlyReport = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Gecikmis Kitaplar") },
                            onClick = {
                                showReportMenu = false
                                viewModel.updateFilterOption(BorrowFilterOption.OVERDUE)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("En Cok Odunc Alinan") },
                            onClick = {
                                showReportMenu = false
                                showMostBorrowedSheet = true
                            }
                        )
                    }
                }

                // Yeni odunc verme butonu
                if (viewModel.canManageBorrowing) {
                    IconButton(onClick = onBorrowBook) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Kitap Odunc Ver",
                            tint = AnkaraLightBlue
                        )
                    }
                }
            }

            // Borrowed Books List
            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = { viewModel.refreshData() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (viewModel.showEmptyState) {
                    EmptyStateView(
                        icon = Icons.Default.Book,
                        title = "Kayit Bulunamadi",
                        message = viewModel.emptyStateMessage,
                        actionTitle = if (viewModel.canManageBorrowing) "Kitap Odunc Ver" else null,
                        onAction = if (viewModel.canManageBorrowing) onBorrowBook else null
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = filteredBooks,
                            key = { it.id ?: it.hashCode() }
                        ) { borrowRecord ->
                            BorrowedBookRowView(
                                borrowRecord = borrowRecord,
                                studentInfo = viewModel.getStudentInfo(borrowRecord),
                                bookInfo = viewModel.getBookInfo(borrowRecord),
                                onReturn = {
                                    selectedBorrowRecord = borrowRecord
                                    showReturnConfirmation = true
                                },
                                onViewDetails = {
                                    selectedBorrowRecord = borrowRecord
                                    showDetailSheet = true
                                },
                                canReturn = viewModel.canManageBorrowing && !borrowRecord.isReturned
                            )
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - Header Section
// ══════════════════════════════════════════════════════════════

@Composable
private fun HeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Odunc Kitaplar",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Icon(
            imageVector = Icons.Default.MenuBook,
            contentDescription = null,
            tint = AnkaraLightBlue,
            modifier = Modifier.size(36.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - Overdue Warning Banner
// ══════════════════════════════════════════════════════════════

@Composable
private fun OverdueWarningBanner(
    message: String,
    onViewOverdue: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AnkaraDanger.copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = AnkaraDanger,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = AnkaraDanger,
            modifier = Modifier.weight(1f)
        )
        TextButton(
            onClick = onViewOverdue
        ) {
            Text(
                text = "Goruntule",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .background(AnkaraDanger, RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - Filter Section
// ══════════════════════════════════════════════════════════════

@Composable
private fun FilterSection(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    filterOption: BorrowFilterOption,
    onFilterOptionChange: (BorrowFilterOption) -> Unit,
    viewModel: BorrowingViewModel
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Arama
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = searchText,
                onValueChange = onSearchTextChange,
                placeholder = { Text("Ogrenci veya kitap ara...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = onClearSearch) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Temizle",
                                tint = Color.Gray
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Filtre butonlari
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(BorrowFilterOption.entries) { option ->
                FilterChip(
                    title = option.displayName,
                    isSelected = filterOption == option,
                    count = viewModel.getFilterCount(option),
                    onClick = { onFilterOptionChange(option) }
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - FilterChip
// ══════════════════════════════════════════════════════════════

@Composable
private fun FilterChip(
    title: String,
    isSelected: Boolean,
    count: Int?,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) AnkaraBlue else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "chipBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "chipContent"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor
        )
        if (count != null) {
            Text(
                text = "($count)",
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - Statistics Section
// ══════════════════════════════════════════════════════════════

@Composable
private fun StatisticsSection(statistics: BorrowingStatistics) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        item {
            EnhancedStatCard(title = "Toplam", value = "${statistics.total}", color = Color.Gray, subtitle = "Islem")
        }
        item {
            EnhancedStatCard(title = "Aktif", value = "${statistics.active}", color = AnkaraLightBlue, subtitle = "Oduncte")
        }
        item {
            EnhancedStatCard(title = "Gecikmis", value = "${statistics.overdue}", color = AnkaraDanger, subtitle = "Kitap")
        }
        item {
            EnhancedStatCard(title = "Iade", value = "${statistics.returned}", color = AnkaraSuccess, subtitle = "Edildi")
        }
        if (statistics.total > 0) {
            item {
                EnhancedStatCard(
                    title = "Iade Orani",
                    value = "${statistics.returnRate.toInt()}%",
                    color = Color(0xFF9C27B0),
                    subtitle = "Basari"
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - EnhancedStatCard
// ══════════════════════════════════════════════════════════════

@Composable
private fun EnhancedStatCard(
    title: String,
    value: String,
    color: Color,
    subtitle: String? = null
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 10.sp
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - BorrowedBookRowView
// ══════════════════════════════════════════════════════════════

@Composable
private fun BorrowedBookRowView(
    borrowRecord: BorrowedBook,
    studentInfo: Student?,
    bookInfo: Pair<BookCopy?, BookTemplate?>,
    onReturn: () -> Unit,
    onViewDetails: () -> Unit,
    canReturn: Boolean
) {
    val (bookCopy, bookTemplate) = bookInfo

    val statusGradientColors = when {
        borrowRecord.isReturned -> listOf(AnkaraSuccess, AnkaraSuccess.copy(alpha = 0.7f))
        borrowRecord.isOverdue -> listOf(AnkaraDanger, Color(0xFFFF9800))
        else -> listOf(AnkaraBlue, AnkaraLightBlue)
    }

    val borderColor = when {
        borrowRecord.isReturned -> AnkaraSuccess.copy(alpha = 0.3f)
        borrowRecord.isOverdue -> AnkaraDanger.copy(alpha = 0.3f)
        else -> Color.Gray.copy(alpha = 0.2f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewDetails),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = Brush.linearGradient(listOf(borderColor, borderColor))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sol taraf: Kitap ikonu
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(statusGradientColors)
                    )
                    .shadow(4.dp, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Orta: Bilgiler
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = bookTemplate?.title ?: "Bilinmeyen Kitap",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = studentInfo?.fullName ?: "Bilinmeyen Ogrenci",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (bookCopy != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = bookCopy.barcode,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Durum rozeti
                StatusBadge(borrowRecord = borrowRecord)
            }

            // Sag taraf: Butonlar
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = onViewDetails,
                    modifier = Modifier
                        .size(32.dp)
                        .background(AnkaraLightBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Detay",
                        tint = AnkaraLightBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (canReturn) {
                    IconButton(
                        onClick = onReturn,
                        modifier = Modifier
                            .size(32.dp)
                            .background(AnkaraSuccess, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Iade",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - StatusBadge
// ══════════════════════════════════════════════════════════════

@Composable
private fun StatusBadge(borrowRecord: BorrowedBook) {
    val backgroundColor = when {
        borrowRecord.isReturned -> AnkaraSuccess.copy(alpha = 0.15f)
        borrowRecord.isOverdue -> AnkaraDanger.copy(alpha = 0.15f)
        else -> AnkaraLightBlue.copy(alpha = 0.15f)
    }

    val textColor = when {
        borrowRecord.isReturned -> AnkaraSuccess
        borrowRecord.isOverdue -> AnkaraDanger
        else -> AnkaraLightBlue
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = borrowRecord.statusEmoji,
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = borrowRecord.statusText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = textColor
        )

        if (!borrowRecord.isReturned) {
            Text(
                text = "•",
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.5f)
            )
            Text(
                text = if (borrowRecord.isOverdue) "${borrowRecord.overdueDays}g"
                else "${borrowRecord.remainingDays}g",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (borrowRecord.isOverdue) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - BorrowDetailSheet
// ══════════════════════════════════════════════════════════════

@Composable
private fun BorrowDetailSheet(
    borrowRecord: BorrowedBook,
    studentInfo: Student?,
    bookInfo: Pair<BookCopy?, BookTemplate?>,
    onDismiss: () -> Unit
) {
    val (bookCopy, bookTemplate) = bookInfo

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Baslik
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Odunc Detayi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Kapat")
            }
        }

        // Durum ozeti
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Durum",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            StatusBadge(borrowRecord = borrowRecord)
        }

        // Ogrenci detaylari
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Ogrenci Bilgileri",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (studentInfo != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DetailRow(Icons.Default.Person, "Ad Soyad", studentInfo.fullName, AnkaraSuccess)
                        DetailRow(Icons.Default.Info, "Ogrenci No", studentInfo.studentNumber, AnkaraLightBlue)
                    }
                }
            } else {
                Text(
                    text = "Ogrenci bilgisi bulunamadi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Kitap detaylari
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Kitap Bilgileri",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (bookTemplate != null && bookCopy != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DetailRow(Icons.Default.Book, "Kitap Adi", bookTemplate.title, AnkaraLightBlue)
                        DetailRow(Icons.Default.Person, "Yazar", bookTemplate.author, Color(0xFF9C27B0))
                        DetailRow(Icons.Default.QrCodeScanner, "Barkod", bookCopy.barcode, AnkaraSuccess)
                        DetailRow(Icons.Default.Info, "Kopya No", "#${bookCopy.copyNumber}", Color(0xFF3F51B5))
                    }
                }
            } else {
                Text(
                    text = "Kitap bilgisi bulunamadi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Tarih bilgileri
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Tarih Bilgileri",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailRow(Icons.Default.CalendarMonth, "Odunc Tarihi", borrowRecord.borrowDateText, AnkaraLightBlue)
                    DetailRow(
                        icon = if (borrowRecord.isOverdue) Icons.Default.Warning else Icons.Default.CalendarMonth,
                        title = "Teslim Tarihi",
                        value = borrowRecord.dueDateText,
                        iconColor = if (borrowRecord.isOverdue) AnkaraDanger else Color(0xFFFF9800)
                    )

                    if (!borrowRecord.isReturned) {
                        if (borrowRecord.isOverdue) {
                            DetailRow(Icons.Default.Warning, "Gecikme", "${borrowRecord.overdueDays} gun gecikmis", AnkaraDanger)
                        } else {
                            DetailRow(Icons.Default.Info, "Kalan Sure", "${borrowRecord.remainingDays} gun", AnkaraSuccess)
                        }
                    }

                    val returnDateText = borrowRecord.returnDateText
                    if (borrowRecord.isReturned && returnDateText != null) {
                        DetailRow(Icons.Default.CheckCircle, "Iade Tarihi", returnDateText, AnkaraSuccess)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - DetailRow
// ══════════════════════════════════════════════════════════════

@Composable
private fun DetailRow(
    icon: ImageVector,
    title: String,
    value: String,
    iconColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - MostBorrowedBooksSheet
// ══════════════════════════════════════════════════════════════

@Composable
private fun MostBorrowedBooksSheet(
    viewModel: BorrowingViewModel,
    onDismiss: () -> Unit
) {
    val mostBorrowed = viewModel.getMostBorrowedBooks(limit = 10)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Baslik
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "En Cok Odunc Alinan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Kapat")
            }
        }

        if (mostBorrowed.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Henuz veri yok",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(mostBorrowed.size) { index ->
                    val (template, count) = mostBorrowed[index]

                    val rankColor = when (index) {
                        0 -> Color(0xFFFFD700) // Altin
                        1 -> Color.Gray // Gumus
                        2 -> Color(0xFFCD7F32) // Bronz
                        else -> AnkaraLightBlue
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Siralama
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(rankColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Kitap bilgileri
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = template.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = template.author,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Odunc sayisi
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AnkaraLightBlue
                            )
                            Text(
                                text = "kez",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
