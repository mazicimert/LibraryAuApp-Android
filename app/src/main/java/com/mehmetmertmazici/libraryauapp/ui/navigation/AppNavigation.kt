package com.mehmetmertmazici.libraryauapp.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mehmetmertmazici.libraryauapp.data.repository.AuthState
import com.mehmetmertmazici.libraryauapp.ui.components.ErrorView
import com.mehmetmertmazici.libraryauapp.ui.components.LoadingView
import com.mehmetmertmazici.libraryauapp.ui.splash.SplashScreen
import com.mehmetmertmazici.libraryauapp.ui.theme.AnkaraBlue
import kotlinx.coroutines.delay

/**
 * AppNavigation
 * Ana navigasyon - Auth durumuna göre ekranları yönetir
 *
 * iOS Karşılığı: ContentView.swift
 */
@Composable
fun AppNavigation(
    viewModel: AppNavigationViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isSuperAdmin by viewModel.isSuperAdmin.collectAsState()
    val pendingAdminCount by viewModel.pendingAdminCount.collectAsState()

    var showSplash by remember { mutableStateOf(true) }
    var splashVisible by remember { mutableStateOf(true) }

    // Splash dismiss handler
    LaunchedEffect(showSplash) {
        if (!showSplash) {
            // Fade out animation delay
            delay(500)
            splashVisible = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main content (hidden during splash)
        AnimatedVisibility(
            visible = !splashVisible,
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(500))
        ) {
            AuthStateContent(
                authState = authState,
                isOnline = isOnline,
                isSuperAdmin = isSuperAdmin,
                pendingAdminCount = pendingAdminCount,
                onRetry = { viewModel.retry() },
                onSignOut = { viewModel.signOut() }
            )
        }

        // Splash screen overlay
        AnimatedVisibility(
            visible = splashVisible,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(500))
        ) {
            SplashScreen(
                onSplashFinished = {
                    showSplash = false
                }
            )
        }
    }
}

@Composable
private fun AuthStateContent(
    authState: AuthState,
    isOnline: Boolean,
    isSuperAdmin: Boolean,
    pendingAdminCount: Int,
    onRetry: () -> Unit,
    onSignOut: () -> Unit
) {
    when (authState) {
        is AuthState.Idle, is AuthState.Loading -> {
            LoadingView()
        }

        is AuthState.SignedOut -> {
            // TODO: LoginScreen()
            PlaceholderAuthScreen(
                title = "Giriş Yap",
                subtitle = "LoginScreen buraya gelecek"
            )
        }

        is AuthState.PendingApproval -> {
            // TODO: PendingApprovalScreen()
            PlaceholderAuthScreen(
                title = "Onay Bekleniyor",
                subtitle = "Hesabınız süper admin onayı bekliyor"
            )
        }

        is AuthState.SignedIn -> {
            MainScreen(
                isSuperAdmin = isSuperAdmin,
                isOnline = isOnline,
                pendingAdminCount = pendingAdminCount
            )
        }

        is AuthState.Error -> {
            ErrorView(
                message = authState.message,
                onRetry = onRetry
            )
        }
    }
}

@Composable
private fun PlaceholderAuthScreen(
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = AnkaraBlue,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}