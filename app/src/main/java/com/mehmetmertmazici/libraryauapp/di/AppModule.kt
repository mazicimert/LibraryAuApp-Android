package com.mehmetmertmazici.libraryauapp.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mehmetmertmazici.libraryauapp.data.repository.AuthRepository
import com.mehmetmertmazici.libraryauapp.data.repository.FirebaseRepository
import com.mehmetmertmazici.libraryauapp.data.repository.ISBNLookupRepository
import com.mehmetmertmazici.libraryauapp.data.repository.NetworkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AppModule
 * Hilt dependency injection module
 *
 * iOS Karşılığı: Singleton pattern'lar (AuthenticationService.shared, FirebaseService.shared, vb.)
 * Android: Hilt ile constructor injection
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ══════════════════════════════════════════════════════════════
    // MARK: - Firebase Instances
    // ══════════════════════════════════════════════════════════════

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

    // ══════════════════════════════════════════════════════════════
    // MARK: - Repositories
    // ══════════════════════════════════════════════════════════════

    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): AuthRepository {
        return AuthRepository(auth, firestore)
    }

    @Provides
    @Singleton
    fun provideFirebaseRepository(
        firestore: FirebaseFirestore,
        @ApplicationContext context: Context
    ): FirebaseRepository {
        return FirebaseRepository(firestore, context)
    }

    @Provides
    @Singleton
    fun provideNetworkManager(
        @ApplicationContext context: Context
    ): NetworkManager {
        return NetworkManager(context)
    }

    @Provides
    @Singleton
    fun provideISBNLookupRepository(): ISBNLookupRepository {
        return ISBNLookupRepository()
    }
}