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
import androidx.compose.ui.draw.shadow
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

    AnkaraBackground {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(100.dp))

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
    val colorScheme = MaterialTheme.colorScheme
    
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
            color = colorScheme.onBackground
        )

        Text(
            text = "Yönetim Sistemi",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Admin ve Süper Admin Girişi",
            fontSize = 14.sp,
            color = colorScheme.onSurfaceVariant
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

        // Gradient Tanımı (Soldan sağa, Koyu Mavi -> Açık Mavi)
        val buttonGradient = Brush.horizontalGradient(
            colors = listOf(AnkaraBlue, AnkaraLightBlue)
        )

        // Disabled rengi (Düz gri) için Brush
        val disabledBrush = androidx.compose.ui.graphics.SolidColor(Color.Gray.copy(alpha = 0.6f))

        Button(
            onClick = onLogin,
            enabled = isEnabled,
            contentPadding = PaddingValues(), // İçeriği biz yöneteceğiz, varsayılan padding'i sıfırla
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent, // Rengi background modifier ile vereceğiz
                disabledContainerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                // Gölge Efekti (Sadece aktifken mavi gölge)
                .then(
                    if (isEnabled) {
                        Modifier.shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(12.dp),
                            ambientColor = AnkaraBlue,
                            spotColor = AnkaraBlue // iOS'teki renkli gölge efekti
                        )
                    } else Modifier
                )
                // Arka Plan (Gradient veya Gri)
                .background(
                    brush = if (isEnabled) buttonGradient else disabledBrush,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // iOS'teki "arrow.right.circle.fill" ikonuna en yakın material ikon
                        Icon(
                            imageVector = Icons.Filled.ArrowCircleRight, // Hata verirse ArrowForward yapabilirsin
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Giriş Yap",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
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
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 40.dp),
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Hesabınız yok mu?",
            fontSize = 14.sp,
            color = colorScheme.onSurfaceVariant
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
    val colorScheme = MaterialTheme.colorScheme
    
    Column {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { onTogglePassword?.invoke() }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (showPassword) "Şifreyi gizle" else "Şifreyi göster",
                            tint = colorScheme.onSurfaceVariant
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
                unfocusedBorderColor = colorScheme.outline,
                focusedContainerColor = colorScheme.surface.copy(alpha = 0.8f),
                unfocusedContainerColor = colorScheme.surface.copy(alpha = 0.6f),
                focusedTextColor = colorScheme.onSurface,
                unfocusedTextColor = colorScheme.onSurface
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