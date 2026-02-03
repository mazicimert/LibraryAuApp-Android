package com.mehmetmertmazici.libraryauapp.ui.auth

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * AuthNavigation
 * Auth ekranları arasında navigasyon
 *
 * iOS Karşılığı: LoginView.swift'teki sheet navigasyonları
 */
@Composable
fun AuthNavigation(
    viewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AuthScreen.Login.route
    ) {
        composable(AuthScreen.Login.route) {
            LoginScreen(
                viewModel = viewModel,
                onNavigateToRegister = {
                    navController.navigate(AuthScreen.Register.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(AuthScreen.ForgotPassword.route)
                }
            )
        }

        composable(AuthScreen.Register.route) {
            RegisterScreen(
                viewModel = viewModel,
                onDismiss = {
                    navController.popBackStack()
                },
                onRegistrationSuccess = {
                    navController.popBackStack()
                }
            )
        }

        composable(AuthScreen.ForgotPassword.route) {
            ForgotPasswordScreen(
                viewModel = viewModel,
                onDismiss = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * Auth Screen Routes
 */
sealed class AuthScreen(val route: String) {
    data object Login : AuthScreen("auth_login")
    data object Register : AuthScreen("auth_register")
    data object ForgotPassword : AuthScreen("auth_forgot_password")
}