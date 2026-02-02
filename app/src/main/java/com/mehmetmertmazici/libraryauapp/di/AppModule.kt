package com.mehmetmertmazici.libraryauapp.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AppModule
 * Hilt bağımlılık enjeksiyon modülü
 *
 * iOS Karşılığı:
 *   iOS'ta Singleton pattern kullanılıyordu:
 *     - AuthenticationService.shared
 *     - FirebaseService.shared
 *     - NetworkManager.shared
 *
 *   Android'de Hilt ile merkezi DI kullanıyoruz:
 *     - @Singleton scope ile tek instance garanti
 *     - Constructor injection ile bağımlılıklar otomatik sağlanır
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ── Firebase Instances ──

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    // ── Context ──

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }

    // ─────────────────────────────────────────────
    // Adım 4'te eklenecek repository provide'lar:
    // ─────────────────────────────────────────────
    //
    // @Provides @Singleton
    // fun provideFirebaseRepository(db: FirebaseFirestore): FirebaseRepository
    //
    // @Provides @Singleton
    // fun provideAuthRepository(auth: FirebaseAuth, db: FirebaseFirestore): AuthRepository
    //
    // @Provides @Singleton
    // fun provideNetworkManager(@ApplicationContext context: Context): NetworkManager
    //
    // @Provides @Singleton
    // fun provideISBNLookupRepository(): ISBNLookupRepository
}
