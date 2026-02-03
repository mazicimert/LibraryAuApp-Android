package com.mehmetmertmazici.libraryauapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mehmetmertmazici.libraryauapp.ui.navigation.AppNavigation
import com.mehmetmertmazici.libraryauapp.ui.theme.LibraryAuTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity
 * Ana aktivite - Uygulama giriş noktası
 *
 * iOS Karşılığı: LibraryAuAppApp.swift (@main struct)
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge display
        enableEdgeToEdge()

        println("📱 Kütüphane Yönetim Uygulaması başlatıldı")

        setContent {
            LibraryAuTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    AppNavigation()
                }
            }
        }
    }
}