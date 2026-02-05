package com.mehmetmertmazici.libraryauapp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Email
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
 * ForgotPasswordScreen
 * Şifre sıfırlama ekranı
 *
 * iOS Karşılığı: ForgotPasswordView.swift (RegisterView.swift içinde)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

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
                        if (uiState.alertTitle == "E-posta Gönderildi") {
                            onDismiss()
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
                title = { Text("Şifre Sıfırlama") },
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
                val colorScheme = MaterialTheme.colorScheme

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(32.dp))

                    // ── Header Section ──
                    Icon(
                        imageVector = Icons.Filled.Key,
                        contentDescription = "Key",
                        modifier = Modifier.size(50.dp),
                        tint = AnkaraBlue
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Şifre Sıfırlama",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "E-posta adresinizi girin. Size şifre sıfırlama bağlantısı göndereceğiz.",
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // ── Email Field ──
                    AuthTextField(
                        value = uiState.resetEmail,
                        onValueChange = { viewModel.updateResetEmail(it) },
                        label = "E-posta",
                        placeholder = "admin@example.com",
                        leadingIcon = Icons.Outlined.Email,
                        keyboardType = KeyboardType.Email
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Send Button ──
                    val isButtonEnabled = uiState.resetEmail.trim().isNotEmpty() &&
                            !uiState.isResetLoading && isOnline

                    Button(
                        onClick = { viewModel.sendPasswordReset() },
                        enabled = isButtonEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AnkaraBlue,
                            disabledContainerColor = Color.Gray.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isResetLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sıfırlama Bağlantısı Gönder",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // ── Offline Warning ──
                    if (!isOnline) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OfflineWarning(message = "Şifre sıfırlama için internet bağlantısı gereklidir")
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}