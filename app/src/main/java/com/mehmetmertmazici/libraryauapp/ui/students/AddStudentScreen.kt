package com.mehmetmertmazici.libraryauapp.ui.students

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mehmetmertmazici.libraryauapp.ui.books.FormSection
import com.mehmetmertmazici.libraryauapp.ui.components.LoadingOverlay
import com.mehmetmertmazici.libraryauapp.ui.theme.*

/**
 * AddStudentScreen
 * Yeni öğrenci ekleme ekranı
 *
 * iOS Karşılığı: AddStudentView.swift
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudentScreen(
    viewModel: AddStudentViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current

    // Alert dialog
    if (uiState.showAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAlert() },
            title = { Text(uiState.alertTitle) },
            text = { Text(uiState.alertMessage) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissAlert()
                    if (uiState.alertTitle == "Başarılı") {
                        onDismiss()
                    }
                }) {
                    Text("Tamam")
                }
            }
        )
    }

    // Help sheet
    if (uiState.showHelpSheet) {
        ValidationHelpSheet(
            onDismiss = { viewModel.dismissHelpSheet() }
        )
    }

    AnkaraBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Yeni Öğrenci",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        TextButton(onClick = onDismiss) {
                            Text("İptal", color = AnkaraLightBlue)
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.addStudent { onDismiss() }
                            },
                            enabled = uiState.isFormValid && !uiState.isAddingStudent
                        ) {
                            Text(
                                "Kaydet",
                                color = if (uiState.isFormValid) AnkaraLightBlue else colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
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
                    // ── Kişisel Bilgiler Section ──
                    item {
                        PersonalInfoSection(
                            name = uiState.name,
                            onNameChange = { viewModel.updateName(it) },
                            surname = uiState.surname,
                            onSurnameChange = { viewModel.updateSurname(it) },
                            focusManager = focusManager
                        )
                    }

                    // ── Kayıt Bilgileri Section ──
                    item {
                        ContactInfoSection(
                            studentNumber = uiState.studentNumber,
                            onStudentNumberChange = { viewModel.updateStudentNumber(it) },
                            email = uiState.email,
                            onEmailChange = { viewModel.updateEmail(it) },
                            isStudentNumberValid = uiState.isStudentNumberValid,
                            isEmailValid = uiState.isEmailValid,
                            focusManager = focusManager
                        )
                    }

                    // ── Validasyon Hataları Section ──
                    if (uiState.validationErrors.isNotEmpty()) {
                        item {
                            ValidationErrorsSection(errors = uiState.validationErrors)
                        }
                    }

                    // ── Önizleme Section ──
                    item {
                        PreviewSection(
                            name = uiState.name,
                            surname = uiState.surname,
                            studentNumber = uiState.studentNumber,
                            email = uiState.email,
                            isFormValid = uiState.isFormValid
                        )
                    }

                    // ── Yardım Section ──
                    item {
                        HelpSection(onClick = { viewModel.showHelpSheet() })
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }

                // Loading overlay
                if (uiState.isAddingStudent) {
                    LoadingOverlay(message = "Öğrenci kaydediliyor...")
                }
            }
        }
    }
}

@Composable
private fun PersonalInfoSection(
    name: String,
    onNameChange: (String) -> Unit,
    surname: String,
    onSurnameChange: (String) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    GroupedFormSection(title = "KİŞİSEL BİLGİLER", footer = "* ile işaretli alanlar zorunludur") {
        // Ad
        iOSFormFieldRow(
            label = "Ad *",
            value = name,
            onValueChange = onNameChange,
            placeholder = "Öğrenci adı",
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            modifier = Modifier.padding(start = 16.dp)
        )

        // Soyad
        iOSFormFieldRow(
            label = "Soyad *",
            value = surname,
            onValueChange = onSurnameChange,
            placeholder = "Öğrenci soyadı",
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )
    }
}

@Composable
private fun ContactInfoSection(
    studentNumber: String,
    onStudentNumberChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    isStudentNumberValid: Boolean,
    isEmailValid: Boolean,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    GroupedFormSection(title = "KAYIT BİLGİLERİ", footer = "Öğrenci numarası benzersiz olmalıdır") {
        // Öğrenci Numarası
        iOSFormFieldRow(
            label = "Numara *",
            value = studentNumber,
            onValueChange = onStudentNumberChange,
            placeholder = "22291001",
            textColor = if (studentNumber.isNotEmpty() && !isStudentNumberValid)
                AnkaraDanger else null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            modifier = Modifier.padding(start = 16.dp)
        )

        // E-posta
        iOSFormFieldRow(
            label = "E-posta *",
            value = email,
            onValueChange = onEmailChange,
            placeholder = "ornek@gmail.com",
            textColor = if (email.isNotEmpty() && isEmailValid)
                AnkaraLightBlue else if (email.isNotEmpty() && !isEmailValid)
                AnkaraDanger else null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            )
        )
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - iOS-Style Grouped Form Components
// ══════════════════════════════════════════════════════════════

/**
 * iOS Settings-like grouped section with a solid background card
 */
@Composable
private fun GroupedFormSection(
    title: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    titleIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Column {
        // Section header
        Row(
            modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                color = titleColor,
                letterSpacing = 0.5.sp
            )
        }

        // Grouped card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(content = content)
        }

        // Footer
        if (footer != null) {
            Text(
                text = footer,
                fontSize = 11.sp,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, start = 16.dp)
            )
        }
    }
}

/**
 * iOS Settings-style form row: label on the left, borderless text input on the right
 */
@Composable
private fun iOSFormFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textColor: Color? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val colorScheme = MaterialTheme.colorScheme
    val resolvedTextColor = textColor ?: colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = colorScheme.onSurface,
            modifier = Modifier.widthIn(min = 90.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Borderless text input
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            textStyle = TextStyle(
                fontSize = 16.sp,
                color = resolvedTextColor
            ),
            cursorBrush = SolidColor(AnkaraLightBlue),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 16.sp,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun ValidationErrorsSection(errors: List<String>) {
    GroupedFormSection(
        title = "DÜZELTİLMESİ GEREKENLER",
        titleColor = AnkaraDanger,
        titleIcon = Icons.Filled.Warning
    ) {
        errors.forEachIndexed { index, error ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Cancel,
                    contentDescription = null,
                    tint = AnkaraDanger,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    error,
                    fontSize = 14.sp,
                    color = AnkaraDanger
                )
            }
            if (index < errors.size - 1) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun PreviewSection(
    name: String,
    surname: String,
    studentNumber: String,
    email: String,
    isFormValid: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val fullName = "$name $surname".trim()
    val initial = name.take(1).uppercase().ifEmpty { "?" }

    FormSection(title = "ÖNİZLEME") {
        // Profile preview
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(AnkaraSuccess.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = AnkaraSuccess
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                // Name
                if (fullName.isNotBlank()) {
                    Text(
                        text = fullName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onBackground
                    )
                } else {
                    Text(
                        text = "Ad Soyad",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                // Student number
                if (studentNumber.isNotEmpty()) {
                    Text(
                        text = "No: $studentNumber",
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                // Email
                if (email.isNotEmpty()) {
                    Text(
                        text = "E-posta: $email",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Status indicator
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isFormValid) {
                Icon(
                    imageVector = Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    tint = AnkaraSuccess,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Kaydetmeye hazır",
                    fontSize = 12.sp,
                    color = AnkaraSuccess
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = AnkaraWarning,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Eksik bilgiler var",
                    fontSize = 12.sp,
                    color = AnkaraWarning
                )
            }
        }
    }
}

@Composable
private fun HelpSection(onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.HelpOutline,
                contentDescription = null,
                tint = AnkaraLightBlue
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Öğrenci ekleme kuralları",
                fontSize = 14.sp,
                color = AnkaraLightBlue,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - Validation Help Sheet
// ══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ValidationHelpSheet(onDismiss: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Yardım",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Genel Kurallar
            Text(
                text = "GENEL KURALLAR",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            HelpRowItem(
                icon = Icons.Filled.Person,
                title = "Ad ve Soyad",
                description = "En az 2 karakter olmalı, Türkçe karakterler desteklenir"
            )

            HelpRowItem(
                icon = Icons.Filled.Tag,
                title = "Öğrenci Numarası",
                description = "Tam 8 haneli, sadece rakam içermeli, benzersiz olmalı"
            )

            HelpRowItem(
                icon = Icons.Filled.Email,
                title = "E-posta Adresi",
                description = "Geçerli bir e-posta formatı olmalı (@ ve uzantı içermeli)"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // İpuçları
            Text(
                text = "İPUÇLARI",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            HelpRowItem(
                icon = Icons.Filled.Refresh,
                iconTint = AnkaraLightBlue,
                title = "Benzersizlik Kontrolü",
                description = "Öğrenci numarası girerken anlık kontrol yapılır"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Close button
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AnkaraBlue)
            ) {
                Text("Kapat")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HelpRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color = AnkaraLightBlue,
    title: String,
    description: String
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onBackground
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}