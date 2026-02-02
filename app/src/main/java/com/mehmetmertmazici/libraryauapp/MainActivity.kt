package com.mehmetmertmazici.libraryauapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.libraryau.app.ui.theme.LibraryAuAppTheme
import com.mehmetmertmazici.libraryauapp.ui.theme.LibraryAuTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity
 * Tek Activity - Compose ile tüm ekranlar burada yönetilir
 *
 * iOS Karşılığı: ContentView.swift (Splash + Auth routing mantığı)
 *
 * Splash Akışı:
 *   iOS  → SplashView overlay (2.5s) + fade-out animasyonu
 *   Android → SplashScreen API (sistem splash) + custom Compose splash
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        // Android 12+ SplashScreen API
        val splashScreen = installSplashScreen()

        // Splash'ı Firebase auth durumu hazır olana kadar tut
        // (Adım 5'te AuthViewModel ile bağlanacak)
        splashScreen.setKeepOnScreenCondition { false }

        super.onCreate(savedInstanceState)

        // Edge-to-edge görünüm (modern Android tasarım)
        enableEdgeToEdge()

        setContent {
            LibraryAuTheme{
                // Adım 5'te LibraryAuApp composable buraya gelecek
                // Şimdilik boş bir ekran
                // LibraryAuApp()
            }
        }
    }
}