package com.mehmetmertmazici.libraryauapp.ui.books

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mehmetmertmazici.libraryauapp.data.model.BookTemplate
import com.mehmetmertmazici.libraryauapp.ui.components.LoadingOverlay
import com.mehmetmertmazici.libraryauapp.ui.theme.*

/**
 * BookEditScreen Kitap düzenleme ekranı
 *
 * iOS Karşılığı: BookEditView.swift
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookEditScreen(
    bookId: String,
    viewModel: BookEditViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onBookUpdated: () -> Unit
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
                TextButton(onClick = { viewModel.dismissAlert() }) { Text("Tamam") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kitabı Düzenle") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("İptal", color = AnkaraLightBlue)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.updateBook { _ ->
                                onBookUpdated()
                            }
                        },
                        enabled = uiState.isFormValid && !uiState.isLoading
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── Temel Bilgiler Section ──
                    item {
                        FormSection(title = "TEMEL BİLGİLER") {
                            OutlinedTextField(
                                value = uiState.title,
                                onValueChange = { viewModel.updateTitle(it) },
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

                            OutlinedTextField(
                                value = uiState.author,
                                onValueChange = { viewModel.updateAuthor(it) },
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

                            OutlinedTextField(
                                value = uiState.isbn,
                                onValueChange = { viewModel.updateIsbn(it) },
                                label = { Text("ISBN *") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions =
                                    KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                colors = formTextFieldColors()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = uiState.publisher,
                                onValueChange = { viewModel.updatePublisher(it) },
                                label = { Text("Yayınevi *") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions =
                                    KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Words,
                                        imeAction = ImeAction.Next
                                    ),
                                colors = formTextFieldColors()
                            )
                        }
                    }

                    // ── Detaylar Section ──
                    item {
                        FormSection(title = "DETAYLAR") {
                            OutlinedTextField(
                                value = uiState.editor,
                                onValueChange = { viewModel.updateEditor(it) },
                                label = { Text("Editör") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions =
                                    KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Words,
                                        imeAction = ImeAction.Next
                                    ),
                                colors = formTextFieldColors()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = uiState.category,
                                onValueChange = { viewModel.updateCategory(it) },
                                label = { Text("Kategori") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions =
                                    KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Words,
                                        imeAction = ImeAction.Next
                                    ),
                                colors = formTextFieldColors()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Açıklama",
                                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            OutlinedTextField(
                                value = uiState.description,
                                onValueChange = { viewModel.updateDescription(it) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 4,
                                colors = formTextFieldColors()
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }

                // Loading overlay
                if (uiState.isLoading) {
                    LoadingOverlay(message = "Güncelleniyor...")
                }
            }
        }
    }
}
