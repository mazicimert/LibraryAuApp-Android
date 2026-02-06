package com.mehmetmertmazici.libraryauapp.ui.books

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mehmetmertmazici.libraryauapp.ui.components.LoadingOverlay
import com.mehmetmertmazici.libraryauapp.ui.theme.*

/**
 * AddBookScreen Yeni kitap ekleme ekranı
 *
 * iOS Karşılığı: AddBookView.swift
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookScreen(
    viewModel: AddBookViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onBookAdded: () -> Unit = {},
    onBarcodeScanner: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme

    // Alert dialog
    if (uiState.showAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAlert() },
            title = { Text(uiState.alertTitle) },
            text = { Text(uiState.alertMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissAlert()
                        if (uiState.alertTitle == "Başarılı") {
                            onBookAdded()
                        }
                    }
                ) { Text("Tamam") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yeni Kitap Ekle") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("İptal", color = AnkaraLightBlue)
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.addBookWithCopies { onBookAdded() } },
                        enabled = uiState.isFormValid && !uiState.isAddingBook
                    ) {
                        Text(
                            "Kaydet",
                            color =
                                if (uiState.isFormValid) AnkaraLightBlue
                                else colorScheme.onSurfaceVariant
                        )
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
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── Temel Bilgiler Section ──
                    item {
                        BasicInfoSection(
                            title = uiState.title,
                            onTitleChange = { viewModel.updateTitle(it) },
                            author = uiState.author,
                            onAuthorChange = { viewModel.updateAuthor(it) },
                            isbn = uiState.isbn,
                            onIsbnChange = { viewModel.updateIsbn(it) },
                            publisher = uiState.publisher,
                            onPublisherChange = { viewModel.updatePublisher(it) },
                            isFetchingBookInfo = uiState.isFetchingBookInfo,
                            isValidIsbn = uiState.isValidIsbn,
                            onBarcodeScanner = onBarcodeScanner
                        )
                    }

                    // ── Gelişmiş Seçenekler Section ──
                    item {
                        AdvancedOptionsSection(
                            showAdvanced = uiState.showAdvancedOptions,
                            onToggleAdvanced = { viewModel.toggleAdvancedOptions() },
                            editor = uiState.editor,
                            onEditorChange = { viewModel.updateEditor(it) },
                            category = uiState.category,
                            onCategoryChange = { viewModel.updateCategory(it) },
                            description = uiState.description,
                            onDescriptionChange = { viewModel.updateDescription(it) }
                        )
                    }

                    // ── İlk Kopya Ayarları Section ──
                    item {
                        InitialCopySection(
                            copyCount = uiState.initialCopyCount,
                            onCopyCountChange = { viewModel.updateCopyCount(it) },
                            isbn = uiState.isbn,
                            nextBookId = uiState.nextBookId
                        )
                    }

                    // ── Hatalar Section ──
                    if (uiState.validationErrors.isNotEmpty()) {
                        item { ValidationErrorsSection(errors = uiState.validationErrors) }
                    }

                    // ── Önizleme Section ──
                    item {
                        PreviewSection(
                            title = uiState.title,
                            author = uiState.author,
                            publisher = uiState.publisher,
                            category = uiState.category,
                            copyCount = uiState.initialCopyCount,
                            isFormValid = uiState.isFormValid
                        )
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }

                // Loading overlay
                if (uiState.isAddingBook) {
                    LoadingOverlay(message = "Kitap ekleniyor...")
                }
            }
        }
    }
}

@Composable
private fun BasicInfoSection(
    title: String,
    onTitleChange: (String) -> Unit,
    author: String,
    onAuthorChange: (String) -> Unit,
    isbn: String,
    onIsbnChange: (String) -> Unit,
    publisher: String,
    onPublisherChange: (String) -> Unit,
    isFetchingBookInfo: Boolean,
    isValidIsbn: Boolean,
    onBarcodeScanner: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    FormSection(title = "TEMEL BİLGİLER") {
        // Kitap Adı
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Kitap Adı *") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
            colors = formTextFieldColors()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Yazar
        OutlinedTextField(
            value = author,
            onValueChange = onAuthorChange,
            label = { Text("Yazar *") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
            colors = formTextFieldColors()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ISBN with scanner button
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = isbn,
                onValueChange = onIsbnChange,
                label = { Text("ISBN *") },
                modifier = Modifier.weight(1f),
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                enabled = !isFetchingBookInfo,
                colors = formTextFieldColors()
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onBarcodeScanner,
                enabled = !isFetchingBookInfo,
                colors = ButtonDefaults.buttonColors(containerColor = AnkaraLightBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tara", fontSize = 12.sp)
            }
        }

        // ISBN status indicator
        if (isFetchingBookInfo) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Kitap bilgileri çekiliyor...",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        } else if (isbn.isNotEmpty() && isValidIsbn) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = AnkaraSuccess,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Geçerli ISBN formatı", fontSize = 12.sp, color = AnkaraSuccess)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Yayınevi
        OutlinedTextField(
            value = publisher,
            onValueChange = onPublisherChange,
            label = { Text("Yayınevi *") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
            colors = formTextFieldColors()
        )
    }
}

@Composable
private fun AdvancedOptionsSection(
    showAdvanced: Boolean,
    onToggleAdvanced: () -> Unit,
    editor: String,
    onEditorChange: (String) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    FormSection(
        title = "GELİŞMİŞ SEÇENEKLER",
        action = {
            TextButton(onClick = onToggleAdvanced) {
                Text(
                    if (showAdvanced) "Gizle" else "Göster",
                    color = AnkaraLightBlue,
                    fontSize = 12.sp
                )
            }
        }
    ) {
        AnimatedVisibility(visible = showAdvanced) {
            Column {
                OutlinedTextField(
                    value = editor,
                    onValueChange = onEditorChange,
                    label = { Text("Editör") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions =
                        KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = formTextFieldColors()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = onCategoryChange,
                    label = { Text("Kategori") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions =
                        KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = formTextFieldColors()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Açıklama") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    colors = formTextFieldColors()
                )
            }
        }
    }
}

@Composable
private fun InitialCopySection(
    copyCount: Int,
    onCopyCountChange: (Int) -> Unit,
    isbn: String,
    nextBookId: Int
) {
    val colorScheme = MaterialTheme.colorScheme

    FormSection(
        title = "İLK KOPYA AYARLARI",
        footer = "Kitap eklendikten sonra daha fazla kopya ekleyebilirsiniz"
    ) {
        // Stepper
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Kopya Sayısı: $copyCount",
                fontWeight = FontWeight.Medium,
                color = colorScheme.onBackground
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                modifier =
                    Modifier.background(
                        color = colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                IconButton(
                    onClick = { if (copyCount > 1) onCopyCountChange(copyCount - 1) },
                    enabled = copyCount > 1
                ) {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = "Azalt",
                        tint =
                            if (copyCount > 1) colorScheme.onBackground
                            else colorScheme.onSurfaceVariant
                    )
                }

                VerticalDivider(
                    modifier = Modifier.height(24.dp),
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )

                IconButton(
                    onClick = { if (copyCount < 10) onCopyCountChange(copyCount + 1) },
                    enabled = copyCount < 10
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Artır",
                        tint =
                            if (copyCount < 10) colorScheme.onBackground
                            else colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Barkod Önizlemesi
        Text(
            "Oluşturulacak Barkodlar:",
            fontWeight = FontWeight.Medium,
            color = colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isbn.isNotEmpty()) {
            (1..copyCount).forEach { copyNumber ->
                val barcode =
                    "LIB${String.format("%03d", nextBookId)}${String.format("%03d", copyNumber)}"
                BarcodePreviewRow(barcode = barcode, copyNumber = copyNumber)
            }
        } else {
            Text(
                "ISBN girilince barkod önizlemesi gösterilecek",
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BarcodePreviewRow(barcode: String, copyNumber: Int) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.QrCode,
            contentDescription = null,
            tint = AnkaraLightBlue,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = barcode,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = colorScheme.onBackground,
            modifier =
                Modifier
                    .background(
                        color = colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Text("Kopya $copyNumber", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ValidationErrorsSection(errors: List<String>) {
    FormSection(title = "HATALAR", titleColor = AnkaraDanger, titleIcon = Icons.Filled.Warning) {
        errors.forEach { error ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Cancel,
                    contentDescription = null,
                    tint = AnkaraDanger,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(error, fontSize = 12.sp, color = AnkaraDanger)
            }
        }
    }
}

@Composable
private fun PreviewSection(
    title: String,
    author: String,
    publisher: String,
    category: String,
    copyCount: Int,
    isFormValid: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme

    FormSection(title = "ÖNİZLEME") {
        // Title
        Text(
            text = title.ifEmpty { "Kitap Adı" },
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontStyle = if (title.isEmpty()) FontStyle.Italic else FontStyle.Normal,
            color =
                if (title.isEmpty()) colorScheme.onSurfaceVariant
                else colorScheme.onBackground
        )

        // Author
        Text(
            text = if (author.isNotEmpty()) "Yazar: $author" else "Yazar: -",
            fontSize = 14.sp,
            fontStyle = if (author.isEmpty()) FontStyle.Italic else FontStyle.Normal,
            color =
                if (author.isEmpty()) colorScheme.onSurfaceVariant
                else colorScheme.onBackground
        )

        // Publisher
        if (publisher.isNotEmpty()) {
            Text(
                text = "Yayınevi: $publisher",
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant
            )
        }

        // Category
        if (category.isNotEmpty()) {
            Text(
                text = category,
                fontSize = 12.sp,
                color = AnkaraLightBlue,
                modifier =
                    Modifier
                        .background(
                            color = AnkaraLightBlue.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )

        // Copy count + validation status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Toplam $copyCount kopya oluşturulacak",
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant
            )

            Icon(
                imageVector = if (isFormValid) Icons.Filled.CheckCircle else Icons.Filled.Error,
                contentDescription = null,
                tint = if (isFormValid) AnkaraSuccess else AnkaraDanger,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - Reusable Components
// ══════════════════════════════════════════════════════════════

@Composable
fun FormSection(
    title: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    titleIcon: ImageVector? = null,
    footer: String? = null,
    action: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Column {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (titleIcon != null) {
                    Icon(
                        imageVector = titleIcon,
                        contentDescription = null,
                        tint = titleColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
            }
            action?.invoke()
        }

        // Content Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
        ) { Column(modifier = Modifier.padding(16.dp), content = content) }

        // Footer
        if (footer != null) {
            Text(
                text = footer,
                fontSize = 11.sp,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

@Composable
fun formTextFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AnkaraBlue,
        unfocusedBorderColor =
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent
    )
