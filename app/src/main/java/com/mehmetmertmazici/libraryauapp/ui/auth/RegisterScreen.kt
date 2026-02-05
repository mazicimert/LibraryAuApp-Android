package com.mehmetmertmazici.libraryauapp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mehmetmertmazici.libraryauapp.ui.theme.*

/**
 * RegisterScreen
 * Kullanıcı kayıt ekranı
 *
 * iOS Karşılığı: RegisterView.swift
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    onRegistrationSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    // Alert Dialog
    if (uiState.showAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAlert() },
            title = { Text(uiState.alertTitle) },
            text = { Text(uiState.alertMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissAlert()
                        if (uiState.alertTitle == "Kayıt Başarılı") {
                            onRegistrationSuccess()
                        }
                    }
                ) {
                    Text("Tamam")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yeni Hesap") },
                navigationIcon = {
                    TextButton(onClick = onDismiss) {
                        Text("İptal", color = AnkaraBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // ── Header Section ──
                    RegisterHeaderSection()

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Registration Form ──
                    RegistrationFormSection(
                        displayName = uiState.registerDisplayName,
                        email = uiState.registerEmail,
                        password = uiState.registerPassword,
                        confirmPassword = uiState.registerConfirmPassword,
                        showPassword = showPassword,
                        showConfirmPassword = showConfirmPassword,
                        onDisplayNameChange = { viewModel.updateRegisterDisplayName(it) },
                        onEmailChange = { viewModel.updateRegisterEmail(it) },
                        onPasswordChange = { viewModel.updateRegisterPassword(it) },
                        onConfirmPasswordChange = { viewModel.updateRegisterConfirmPassword(it) },
                        onTogglePassword = { showPassword = !showPassword },
                        onToggleConfirmPassword = { showConfirmPassword = !showConfirmPassword }
                    )

                    // ── Validation Errors ──
                    if (uiState.registerValidationErrors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        ValidationErrorsSection(errors = uiState.registerValidationErrors)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Register Button ──
                    RegisterButtonSection(
                        isLoading = uiState.isRegisterLoading,
                        isEnabled = uiState.isRegisterFormValid && !uiState.isRegisterLoading && isOnline,
                        isOnline = isOnline,
                        onRegister = { viewModel.performRegistration() }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Info Section ──
                    InfoSection()

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun RegisterHeaderSection() {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.PersonAdd,
            contentDescription = "Register",
            modifier = Modifier.size(50.dp),
            tint = AnkaraBlue
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Admin Hesabı Oluştur",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Bilgilerinizi girin ve süper admin onayını bekleyin",
            fontSize = 14.sp,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RegistrationFormSection(
    displayName: String,
    email: String,
    password: String,
    confirmPassword: String,
    showPassword: Boolean,
    showConfirmPassword: Boolean,
    onDisplayNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onToggleConfirmPassword: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Display Name Field
        AuthTextField(
            value = displayName,
            onValueChange = onDisplayNameChange,
            label = "Ad Soyad",
            placeholder = "Adınız Soyadınız",
            leadingIcon = Icons.Outlined.Person
        )

        // Email Field
        AuthTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "E-posta",
            placeholder = "admin@example.com",
            leadingIcon = Icons.Outlined.Email,
            keyboardType = KeyboardType.Email
        )

        // Password Field
        AuthTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Şifre",
            placeholder = "En az 6 karakter",
            leadingIcon = Icons.Outlined.Lock,
            isPassword = true,
            showPassword = showPassword,
            onTogglePassword = onTogglePassword
        )

        // Confirm Password Field
        AuthTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = "Şifre Tekrar",
            placeholder = "Şifrenizi tekrar girin",
            leadingIcon = Icons.Outlined.Lock,
            isPassword = true,
            showPassword = showConfirmPassword,
            onTogglePassword = onToggleConfirmPassword
        )
    }
}

@Composable
private fun ValidationErrorsSection(errors: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AnkaraDanger.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = AnkaraDanger,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Lütfen aşağıdaki hataları düzeltin:",
                fontWeight = FontWeight.SemiBold,
                color = AnkaraDanger,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        errors.forEach { error ->
            Row(
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            ) {
                Text(
                    text = "•",
                    color = AnkaraDanger,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = error,
                    color = AnkaraDanger,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun RegisterButtonSection(
    isLoading: Boolean,
    isEnabled: Boolean,
    isOnline: Boolean,
    onRegister: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onRegister,
            enabled = isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AnkaraBlue,
                disabledContainerColor = Color.Gray.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Hesap Oluştur",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Offline Warning
        if (!isOnline) {
            OfflineWarning(message = "Kayıt için internet bağlantısı gereklidir")
        }
    }
}

@Composable
private fun InfoSection() {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 40.dp),
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = AnkaraBlue.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = AnkaraBlue,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Önemli Bilgi",
                    fontWeight = FontWeight.SemiBold,
                    color = AnkaraBlue,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Hesabınız oluşturulduktan sonra süper admin tarafından onaylanması gerekmektedir. Onay işlemi tamamlandıktan sonra giriş yapabilirsiniz.",
                    fontSize = 13.sp,
                    color = colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}