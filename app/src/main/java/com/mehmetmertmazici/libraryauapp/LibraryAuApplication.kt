package com.mehmetmertmazici.libraryauapp

import android.app.Application
import com.google.firebase.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PersistentCacheSettings

/**
 * LibraryAuApplication
 * Ana uygulama sınıfı - Hilt DI ve Firebase konfigürasyonu
 *
 * iOS Karşılığı: LibraryAuAppApp.swift → init() + configureFirebase()
 */
@HiltAndroidApp
class LibraryAuApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Firebase konfigürasyonu
        configureFirebase()

        if (BuildConfig.DEBUG) {
            println("📱 Kütüphane Yönetim Uygulaması başlatıldı")
        }
    }

    /**
     * Firebase yapılandırması
     * iOS Karşılığı: LibraryAuAppApp.swift → configureFirebase()
     */
    private fun configureFirebase() {
        // Firebase otomatik olarak google-services.json'dan başlatılır
        // Ancak ek Firestore ayarları burada yapılır
        FirebaseApp.initializeApp(this)

        // Firestore offline persistence ve cache ayarları
        val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                PersistentCacheSettings.newBuilder()
                    .setSizeBytes(100L * 1024 * 1024) // 100MB cache (iOS ile aynı)
                    .build()
            )
            .build()

        FirebaseFirestore.getInstance().firestoreSettings = settings

        if (BuildConfig.DEBUG) {
            println("🔥 Firebase başarıyla yapılandırıldı")
        }
    }
}