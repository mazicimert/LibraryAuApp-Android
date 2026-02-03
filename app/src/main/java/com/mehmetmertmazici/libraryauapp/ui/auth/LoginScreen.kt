package com.mehmetmertmazici.libraryauapp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mehmetmertmazici.libraryauapp.ui.theme.*

/**
 * LoginScreen
 * Kullanıcı giriş ekranı
 *
 * iOS Karşılığı: LoginView.swift
 */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    var showPassword by remember { mutableStateOf(false) }

    // Alert Dialog
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color.White, AnkaraLightBlue.copy(alpha = 0.3f))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // ── Header Section ──
            HeaderSection()

            Spacer(modifier = Modifier.height(32.dp))

            // ── Login Form ──
            LoginFormSection(
                email = uiState.loginEmail,
                password = uiState.loginPassword,
                showPassword = showPassword,
                onEmailChange = { viewModel.updateLoginEmail(it) },
                onPasswordChange = { viewModel.updateLoginPassword(it) },
                onTogglePassword = { showPassword = !showPassword }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Action Buttons ──
            ActionButtonsSection(
                isLoading = uiState.isLoginLoading,
                isEnabled = uiState.isLoginFormValid && !uiState.isLoginLoading && isOnline,
                isOnline = isOnline,
                onLogin = { viewModel.performLogin() },
                onForgotPassword = onNavigateToForgotPassword
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Register Section ──
            RegisterSection(
                isOnline = isOnline,
                onNavigateToRegister = onNavigateToRegister
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun HeaderSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo icon
        Icon(
            imageVector = Icons.Filled.MenuBook,
            contentDescription = "Logo",
            modifier = Modifier.size(60.dp),
            tint = AnkaraBlue
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Kütüphane",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Text(
            text = "Yönetim Sistemi",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Admin ve Süper Admin Girişi",
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun LoginFormSection(
    email: String,
    password: String,
    showPassword: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
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
            placeholder = "Şifrenizi girin",
            leadingIcon = Icons.Outlined.Lock,
            isPassword = true,
            showPassword = showPassword,
            onTogglePassword = onTogglePassword
        )
    }
}

@Composable
private fun ActionButtonsSection(
    isLoading: Boolean,
    isEnabled: Boolean,
    isOnline: Boolean,
    onLogin: () -> Unit,
    onForgotPassword: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Login Button
        Button(
            onClick = onLogin,
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
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Giriş Yap",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Forgot Password
        TextButton(
            onClick = onForgotPassword,
            enabled = isOnline
        ) {
            Text(
                text = "Şifremi Unuttum",
                color = AnkaraBlue,
                fontSize = 14.sp
            )
        }

        // Offline Warning
        if (!isOnline) {
            OfflineWarning(message = "Giriş için internet bağlantısı gereklidir")
        }
    }
}

@Composable
private fun RegisterSection(
    isOnline: Boolean,
    onNavigateToRegister: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 40.dp),
            color = Color.Gray.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Hesabınız yok mu?",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onNavigateToRegister,
            enabled = isOnline,
            modifier = Modifier.height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = AnkaraBlue
            ),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, AnkaraBlue)
        ) {
            Icon(
                imageVector = Icons.Filled.PersonAdd,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Yeni Hesap Oluştur",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - Reusable Components
// ══════════════════════════════════════════════════════════════

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null
) {
    Column {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color.Gray.copy(alpha = 0.6f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = Color.Gray
                )
            },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { onTogglePassword?.invoke() }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (showPassword) "Şifreyi gizle" else "Şifreyi göster",
                            tint = Color.Gray
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !showPassword) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AnkaraBlue,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                focusedContainerColor = Color.White.copy(alpha = 0.8f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.6f)
            )
        )
    }
}

@Composable
fun OfflineWarning(message: String) {
    Row(
        modifier = Modifier
            .background(
                color = AnkaraWarning.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.WifiOff,
            contentDescription = null,
            tint = AnkaraWarning,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message,
            fontSize = 12.sp,
            color = AnkaraWarning
        )
    }
}