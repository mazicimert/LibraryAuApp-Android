package com.mehmetmertmazici.libraryauapp.ui.books

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mehmetmertmazici.libraryauapp.data.model.BookCopy
import com.mehmetmertmazici.libraryauapp.data.model.BookTemplate
import com.mehmetmertmazici.libraryauapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * BookDetailScreen Kitap detay ve kopyalar ekranı
 *
 * iOS Karşılığı: BookDetailView.swift
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    bookId: String,
    viewModel: BookDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onEditBook: (BookTemplate) -> Unit,
    onAddCopy: (BookTemplate) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val bookCopies by viewModel.bookCopies.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    val currentBook = uiState.currentBook

    // Show loading state while book is being loaded
    if (currentBook == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = AnkaraBlue)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Kitap bulunamadı", color = MaterialTheme.colorScheme.onBackground)
                    TextButton(onClick = onBack) { Text("Geri Dön") }
                }
            }
        }
        return
    }

    // Delete copy confirmation dialog
    if (uiState.showDeleteConfirmation) {
        val copy = uiState.copyToDelete
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteCopy() },
            title = { Text("Kopyayı Sil") },
            text = {
                Text(
                    "'${currentBook.title}' kitabının #${copy?.copyNumber ?: ""} numaralı kopyasını silmek istediğinizden emin misiniz?\n\n" +
                            "Barkod: ${copy?.displayBarcode ?: ""}\n\n" +
                            "Bu işlem geri alınamaz."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDeleteCopy() },
                    colors = ButtonDefaults.textButtonColors(contentColor = AnkaraDanger)
                ) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDeleteCopy() }) { Text("İptal") }
            }
        )
    }

    // Alert dialog
    if (uiState.showAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAlert() },
            title = { Text(uiState.alertTitle) },
            text = { Text(uiState.alertMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissAlert() }) { Text("Tamam") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kitap Detayı") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = AnkaraLightBlue
                        )
                    }
                },
                actions = {
                    if (viewModel.canManageBooks) {
                        IconButton(
                            onClick = { onEditBook(currentBook) },
                            enabled = isOnline
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Düzenle",
                                tint = if (isOnline) AnkaraLightBlue else Color.Gray
                            )
                        }
                        IconButton(
                            onClick = { onAddCopy(currentBook) },
                            enabled = isOnline
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Kopya Ekle",
                                tint = if (isOnline) AnkaraLightBlue else Color.Gray
                            )
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
            )
        }
    ) { paddingValues ->
        AnkaraBackground {
            val colorScheme = MaterialTheme.colorScheme

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                PullToRefreshBox(
                    isRefreshing = uiState.isLoading,
                    onRefresh = { currentBook.id?.let { viewModel.loadBookCopies(it) } }
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Book Info Section
                        item {
                            BookInfoSection(
                                book = currentBook,
                                isDescriptionExpanded = uiState.isDescriptionExpanded,
                                isTitleExpanded = uiState.isTitleExpanded
                            )
                        }

                        item { HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f)) }

                        // Copies Section Header
                        item {
                            CopiesSectionHeader(
                                totalCopies = bookCopies.size,
                                availableCopies = viewModel.availableCopiesCount,
                                borrowedCopies = viewModel.borrowedCopiesCount
                            )
                        }

                        // Copies List
                        if (bookCopies.isEmpty() && !uiState.isLoading) {
                            item { EmptyCopyStateView(onAddCopy = { onAddCopy(currentBook) }) }
                        } else {
                            items(bookCopies, key = { it.id ?: it.hashCode() }) { copy ->
                                BookCopyRowView(
                                    copy = copy,
                                    onDelete = { viewModel.requestDeleteCopy(copy) },
                                    canDelete = viewModel.canManageBooks && copy.isAvailable
                                )
                            }
                        }

                        // Bottom spacer
                        item { Spacer(modifier = Modifier.height(20.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookInfoSection(
    book: BookTemplate,
    isDescriptionExpanded: Boolean,
    isTitleExpanded: Boolean,
    colorScheme: ColorScheme = MaterialTheme.colorScheme
) {
    var titleExpanded by remember { mutableStateOf(isTitleExpanded) }
    var descExpanded by remember { mutableStateOf(isDescriptionExpanded) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Title
        Text(
            text = book.title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onBackground,
            maxLines = if (titleExpanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis
        )

        if (book.title.length > 40) {
            Text(
                text = if (titleExpanded) "Daha Az Göster" else "Devamını Gör",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = AnkaraLightBlue,
                modifier = Modifier.clickable { titleExpanded = !titleExpanded }
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
        )

        // Author
        Text(text = "Yazar: ${book.author}", fontSize = 16.sp, color = colorScheme.onBackground)

        // Publisher
        Text(
            text = "Yayınevi: ${book.publisher}",
            fontSize = 14.sp,
            color = colorScheme.onSurfaceVariant
        )

        // Editor (if exists)
        if (book.editor.isNotEmpty()) {
            Text(
                text = "Editör: ${book.editor}",
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant
            )
        }

        // ISBN
        if (book.isbn.isNotEmpty()) {
            Text(
                text = "ISBN: ${book.isbn}",
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .background(
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Category
        if (book.category.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Kategori:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onBackground
                )
                Text(
                    text = book.category,
                    fontSize = 14.sp,
                    color = AnkaraLightBlue,
                    modifier =
                        Modifier
                            .background(
                                color = AnkaraLightBlue.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        // Description
        if (book.description.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Açıklama",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onBackground
            )

            Text(
                text = book.description,
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant,
                maxLines = if (descExpanded) Int.MAX_VALUE else 5,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { descExpanded = !descExpanded }
            )

            if (book.description.length > 150) {
                Text(
                    text = if (descExpanded) "Daha Az Göster" else "Devamını Gör",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = AnkaraLightBlue,
                    modifier = Modifier.clickable { descExpanded = !descExpanded }
                )
            }
        }
    }
}

@Composable
private fun CopiesSectionHeader(
    totalCopies: Int,
    availableCopies: Int,
    borrowedCopies: Int,
    colorScheme: ColorScheme = MaterialTheme.colorScheme
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Fiziksel Kopyalar",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onBackground
            )
            Text(
                text = "$totalCopies kopya • $availableCopies müsait",
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatusIndicator(
                count = availableCopies,
                label = "Müsait",
                color = AnkaraSuccess,
                labelColor = colorScheme.onSurfaceVariant
            )
            StatusIndicator(
                count = borrowedCopies,
                label = "Ödünçte",
                color = AnkaraDanger,
                labelColor = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusIndicator(
    count: Int,
    label: String,
    color: Color,
    labelColor: Color = Color.Gray
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "$count", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 11.sp, color = labelColor)
    }
}

@Composable
fun BookCopyRowView(copy: BookCopy, onDelete: () -> Unit, canDelete: Boolean) {
    val colorScheme = MaterialTheme.colorScheme
    val statusColor = if (copy.isAvailable) AnkaraSuccess else AnkaraDanger
    val statusBackgroundColor =
        if (copy.isAvailable) AnkaraLightBlue.copy(alpha = 0.15f)
        else AnkaraDanger.copy(alpha = 0.15f)
    val statusForegroundColor = if (copy.isAvailable) AnkaraLightBlue else AnkaraDanger
    val borderColor =
        if (copy.isAvailable) colorScheme.onSurfaceVariant.copy(alpha = 0.2f) else AnkaraDanger.copy(
            alpha = 0.3f
        )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book icon
            Box(
                modifier =
                    Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush =
                                Brush.linearGradient(
                                    colors =
                                        listOf(
                                            AnkaraBlue,
                                            AnkaraLightBlue
                                        )
                                )
                        )
                        .shadow(4.dp, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector =
                        if (copy.isAvailable) Icons.Filled.MenuBook else Icons.Filled.Book,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Copy info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Kopya #${copy.copyNumber}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onBackground
                )

                // Barcode
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCode,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = copy.displayBarcode,
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                // Date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = formatDate(copy.createdAt?.toDate()),
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                // Status badge
                Text(
                    text = copy.statusText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusForegroundColor,
                    modifier =
                        Modifier
                            .background(
                                color = statusBackgroundColor,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            // Right side - Status icon and delete button
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status icon
                Box(
                    modifier =
                        Modifier
                            .size(32.dp)
                            .background(
                                color = statusColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector =
                            if (copy.isAvailable) Icons.Filled.CheckCircle
                            else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Delete button (only for available copies)
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
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCopyStateView(onAddCopy: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.MenuBook,
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp)
        )

        Text(
            text = "Henüz fiziksel kopya yok",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onBackground
        )

        Text(
            text = "Bu kitabın fiziksel kopyalarını ekleyerek ödünç vermeye başlayabilirsiniz",
            fontSize = 14.sp,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        TextButton(
            onClick = onAddCopy,
            colors = ButtonDefaults.textButtonColors(contentColor = AnkaraLightBlue)
        ) { Text(text = "İlk Kopyayı Ekle", fontWeight = FontWeight.Medium) }
    }
}

private fun formatDate(date: Date?): String {
    if (date == null) return "-"
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(date)
}
