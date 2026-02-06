package com.mehmetmertmazici.libraryauapp.ui.books

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mehmetmertmazici.libraryauapp.data.model.BookCopy
import com.mehmetmertmazici.libraryauapp.data.model.BookTemplate
import com.mehmetmertmazici.libraryauapp.ui.components.LoadingOverlay
import com.mehmetmertmazici.libraryauapp.ui.theme.*

/**
 * AddCopyScreen Mevcut kitaba yeni kopya ekleme ekranı
 *
 * iOS Karşılığı: AddCopyView.swift
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCopyScreen(
    bookId: String,
    viewModel: AddCopyViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onCopyAdded: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme

    // Load book data by ID
    LaunchedEffect(bookId) { viewModel.loadBookById(bookId) }

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
                            onCopyAdded()
                        }
                    }
                ) { Text("Tamam") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yeni Kopya Ekle") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("İptal", color = AnkaraLightBlue)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.addCopies {
                                onCopyAdded()
                            }
                        },
                        enabled = uiState.isFormValid && !uiState.isAddingCopies
                    ) {
                        Text(
                            "Ekle",
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
                    // ── Kitap Bilgisi Section ──
                    item {
                        uiState.bookTemplate?.let { book ->
                            BookInfoSection(bookTemplate = book)
                        }
                    }

                    // ── Mevcut Durumu Section ──
                    item {
                        ExistingCopiesSection(
                            totalCopies = uiState.totalCopies,
                            availableCopies = uiState.availableCopies,
                            borrowedCopies = uiState.borrowedCopies
                        )
                    }

                    // ── Yeni Kopya Ayarları Section ──
                    item {
                        NewCopySettingsSection(
                            copyCount = uiState.copyCount,
                            onCopyCountChange = { viewModel.updateCopyCount(it) }
                        )
                    }

                    // ── Oluşturulacak Barkodlar Section ──
                    item {
                        FormSection(title = "OLUŞTURULACAK BARKODLAR") {
                            if (uiState.previewBarcodes.isEmpty()) {
                                Text(
                                    "Barkod önizlemesi yüklenemedi",
                                    fontSize = 14.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = colorScheme.onSurfaceVariant
                                )
                            } else {
                                uiState.previewBarcodes.forEach { preview ->
                                    CopyBarcodePreviewRow(
                                        barcode = preview.barcode,
                                        copyNumber = preview.copyNumber,
                                        barcodeType = preview.barcodeType
                                    )
                                }
                            }
                        }
                    }

                    // ── Hatalar Section ──
                    if (uiState.validationErrors.isNotEmpty()) {
                        item {
                            FormSection(
                                title = "HATALAR",
                                titleColor = AnkaraDanger,
                                titleIcon = Icons.Filled.Warning
                            ) {
                                uiState.validationErrors.forEach { error ->
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
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }

                // Loading overlay
                if (uiState.isAddingCopies) {
                    LoadingOverlay(message = "Kopyalar ekleniyor...")
                }
            }
        }
    }
}

@Composable
private fun BookInfoSection(bookTemplate: BookTemplate) {
    val colorScheme = MaterialTheme.colorScheme

    FormSection(title = "KİTAP BİLGİSİ") {
        Text(
            text = bookTemplate.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onBackground
        )

        Text(
            text = "Yazar: ${bookTemplate.author}",
            fontSize = 14.sp,
            color = colorScheme.onSurfaceVariant
        )

        if (bookTemplate.isbn.isNotEmpty()) {
            Text(
                text = "ISBN: ${bookTemplate.isbn}",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = colorScheme.onBackground,
                modifier =
                    Modifier
                        .background(
                            color = colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun ExistingCopiesSection(totalCopies: Int, availableCopies: Int, borrowedCopies: Int) {
    val colorScheme = MaterialTheme.colorScheme

    FormSection(title = "MEVCUT DURUMU") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            // Toplam Kopya
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Toplam Kopya", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                Text(
                    text = "$totalCopies",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onBackground
                )
            }

            // Müsait
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Müsait", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                Text(
                    text = "$availableCopies",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AnkaraSuccess
                )
            }

            // Ödünçte
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Ödünçte", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                Text(
                    text = "$borrowedCopies",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AnkaraDanger
                )
            }
        }
    }
}

@Composable
private fun NewCopySettingsSection(copyCount: Int, onCopyCountChange: (Int) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    FormSection(
        title = "YENİ KOPYA AYARLARI",
        footer = "En fazla 10 kopya aynı anda ekleyebilirsiniz"
    ) {
        // Stepper
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Eklenecek Kopya: $copyCount",
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

        // Barkod formatı bilgisi
        Text(
            "Barkod Formatı",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onBackground
        )

        Text(
            "Sistem tarafından otomatik olarak sıralı LIB barkodları (Örn: LIB001...) oluşturulacaktır. ISBN sadece kitap bilgisi olarak saklanır.",
            fontSize = 12.sp,
            fontStyle = FontStyle.Italic,
            color = colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CopyBarcodePreviewRow(barcode: String, copyNumber: Int, barcodeType: BarcodeType) {
    val colorScheme = MaterialTheme.colorScheme
    val iconTint =
        if (barcodeType == BarcodeType.ISBN) AnkaraLightBlue else Color(0xFF9C27B0) // purple

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Barkod tipi ikonu
        Icon(
            imageVector =
                if (barcodeType == BarcodeType.ISBN) Icons.Filled.QrCode
                else Icons.Filled.QrCode2,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Barkod ve tipi
        Column {
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

            Text(
                text = barcodeType.description,
                fontSize = 10.sp,
                color = colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Kopya numarası
        Text(
            "Kopya #$copyNumber",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurfaceVariant
        )
    }
}
