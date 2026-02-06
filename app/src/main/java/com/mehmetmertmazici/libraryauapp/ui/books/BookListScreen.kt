package com.mehmetmertmazici.libraryauapp.ui.books

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mehmetmertmazici.libraryauapp.data.model.BookTemplate
import com.mehmetmertmazici.libraryauapp.ui.components.EmptyStateView
import com.mehmetmertmazici.libraryauapp.ui.components.LoadingOverlay
import com.mehmetmertmazici.libraryauapp.ui.theme.*
import kotlin.math.roundToInt

/**
 * BookListScreen Ana kitap listesi ekranı
 *
 * iOS Karşılığı: BookListView.swift
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookListScreen(
    viewModel: BookListViewModel = hiltViewModel(),
    onBookClick: (BookTemplate) -> Unit,
    onBarcodeScanner: () -> Unit,
    onAddBook: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val filteredBooks by viewModel.filteredBooks.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchText by viewModel.searchText.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    val focusManager = LocalFocusManager.current

    // Delete confirmation dialog
    if (uiState.showAlert && uiState.pendingDeleteBookId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteBook() },
            title = { Text(uiState.alertTitle) },
            text = { Text(uiState.alertMessage) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDeleteBookWithAllCopies() },
                    colors = ButtonDefaults.textButtonColors(contentColor = AnkaraDanger)
                ) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDeleteBook() }) { Text("İptal") }
            }
        )
    } else if (uiState.showAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAlert() },
            title = { Text(uiState.alertTitle) },
            text = { Text(uiState.alertMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissAlert() }) { Text("Tamam") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header Section ──
            HeaderSection(
                onBarcodeScanner = onBarcodeScanner,
                onAddBook = onAddBook,
                canAddBooks = viewModel.canAddBooks,
                isOnline = isOnline
            )

            // ── Search and Filter Section ──
            SearchAndFilterSection(
                searchText = searchText,
                onSearchTextChange = { viewModel.updateSearchText(it) },
                onClearSearch = { viewModel.clearSearch() },
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelect = { viewModel.updateSelectedCategory(it) },
                onDone = { focusManager.clearFocus() }
            )

            // ── Statistics Section ──
            if (filteredBooks.isNotEmpty()) {
                StatisticsSection(count = filteredBooks.size, isOnline = isOnline)
            }

            // ── Book List Section ──
            BookListSection(
                books = filteredBooks,
                isLoading = uiState.isLoading,
                showEmptyState = viewModel.showEmptyState,
                emptyStateMessage = viewModel.emptyStateMessage,
                canAddBooks = viewModel.canAddBooks,
                onBookClick = onBookClick,
                onDeleteBook = { viewModel.requestDeleteBook(it) },
                onRefresh = { viewModel.refreshBooks() },
                onAddBook = onAddBook
            )
        }

        // Loading overlay
        if (uiState.isLoading) {
            LoadingOverlay(message = "Yükleniyor...")
        }
    }
}

/**
 * Header Section
 * Sol: Başlık, Sağ: Aksiyon butonları
 */
@Composable
private fun HeaderSection(
    onBarcodeScanner: () -> Unit,
    onAddBook: () -> Unit,
    canAddBooks: Boolean,
    isOnline: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()

    // Buton arka plan renkleri
    val buttonBg =
        if (isDark) AnkaraLightBlue.copy(alpha = 0.15f) else AnkaraBlue.copy(alpha = 0.08f)
    val buttonTint = if (isDark) AnkaraLightBlue else AnkaraBlue

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sol: Başlık
        Text(
            text = "Kitaplar",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onBackground
        )

        // Sağ: Aksiyon butonları
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Barkod tarayıcı butonu
            IconButton(
                onClick = onBarcodeScanner,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(buttonBg)
            ) {
                Icon(
                    imageVector = Icons.Filled.QrCodeScanner,
                    contentDescription = "Barkod Tara",
                    tint = buttonTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Kitap ekle butonu
            if (canAddBooks) {
                IconButton(
                    onClick = onAddBook,
                    enabled = isOnline,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isOnline) AnkaraBlue
                            else colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Kitap Ekle",
                        tint = if (isOnline) Color.White else colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchAndFilterSection(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    onDone: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            placeholder = {
                Text("Kitap, yazar veya ISBN ara...", color = colorScheme.onSurfaceVariant)
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

        // Category filter chips
        if (categories.size > 1) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(categories) { category ->
                    CategoryFilterChip(
                        category = category,
                        isSelected = selectedCategory == category,
                        onSelect = { onCategorySelect(category) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterChip(category: String, isSelected: Boolean, onSelect: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    val backgroundColor =
        if (isSelected) {
            Brush.linearGradient(colors = listOf(AnkaraBlue, AnkaraLightBlue))
        } else {
            Brush.linearGradient(
                colors =
                    listOf(
                        colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
            )
        }

    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(backgroundColor)
                .clickable { onSelect() }
                .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = category,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) Color.White else colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun StatisticsSection(count: Int, isOnline: Boolean) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$count kitap gösteriliyor",
            fontSize = 12.sp,
            color = colorScheme.onSurfaceVariant
        )

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
private fun BookListSection(
    books: List<BookTemplate>,
    isLoading: Boolean,
    showEmptyState: Boolean,
    emptyStateMessage: String,
    canAddBooks: Boolean,
    onBookClick: (BookTemplate) -> Unit,
    onDeleteBook: (BookTemplate) -> Unit,
    onRefresh: () -> Unit,
    onAddBook: () -> Unit
) {
    if (showEmptyState) {
        EmptyStateView(
            icon = Icons.Filled.MenuBook,
            title = "Kitap Bulunamadı",
            message = emptyStateMessage,
            actionTitle = if (canAddBooks) "Kitap Ekle" else null,
            onAction = onAddBook
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
                items(books, key = { it.id ?: it.hashCode() }) { book ->
                    SwipeableBookRow(
                        book = book,
                        onClick = { onBookClick(book) },
                        onDelete = { onDeleteBook(book) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeableBookRow(book: BookTemplate, onClick: () -> Unit, onDelete: () -> Unit) {
    var offsetX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        // Delete background
        if (offsetX < 0) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AnkaraDanger)
                        .clickable {
                            onDelete()
                            offsetX = 0f
                        },
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    modifier = Modifier.padding(end = 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Sil",
                        tint = Color.White
                    )
                    Text(text = "Sil", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Book row content
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                offsetX = if (offsetX < -100f) -100f else 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                val newOffset = offsetX + dragAmount
                                offsetX = newOffset.coerceIn(-200f, 0f)
                            }
                        )
                    }
        ) { BookRowView(book = book, onClick = onClick) }
    }
}

@Composable
fun BookRowView(book: BookTemplate, onClick: () -> Unit) {
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
                    imageVector = Icons.Filled.MenuBook,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Book info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = book.title,
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
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = book.author,
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (book.category.isNotEmpty()) {
                    Text(
                        text = book.category,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = AnkaraLightBlue,
                        modifier =
                            Modifier
                                .background(
                                    color = AnkaraLightBlue.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Chevron
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
        }
    }
}