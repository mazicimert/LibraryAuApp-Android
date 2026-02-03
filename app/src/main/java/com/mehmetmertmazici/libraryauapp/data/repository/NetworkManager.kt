package com.mehmetmertmazici.libraryauapp.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.mehmetmertmazici.libraryauapp.data.model.NetworkStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NetworkManager
 * Ağ bağlantı durumunu izler ve yönetir
 *
 * iOS Karşılığı: NetworkManager.swift
 * iOS: NWPathMonitor → Android: ConnectivityManager
 */
@Singleton
class NetworkManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // ── State Flows ──
    private val _networkStatus = MutableStateFlow(NetworkStatus.CONNECTED)
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _connectionType = MutableStateFlow(ConnectionType.UNKNOWN)
    val connectionType: StateFlow<ConnectionType> = _connectionType.asStateFlow()

    private var lastSyncDate: Date? = null

    // ── Network Callback ──
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val wasOnline = _isOnline.value
            _isOnline.value = true
            _networkStatus.value = NetworkStatus.CONNECTED
            updateConnectionType()

            if (!wasOnline) {
                println("📡 İnternet bağlantısı yeniden kuruldu - Sync başlatılıyor...")
            }
        }

        override fun onLost(network: Network) {
            _isOnline.value = false
            _networkStatus.value = NetworkStatus.DISCONNECTED
            _connectionType.value = ConnectionType.UNKNOWN
            println("🌐 Ağ durumu: Çevrimdışı")
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            updateConnectionType(networkCapabilities)
        }
    }

    init {
        startNetworkMonitoring()
        checkInitialNetworkState()
    }

    private fun startNetworkMonitoring() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun checkInitialNetworkState() {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        _isOnline.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        _networkStatus.value = if (_isOnline.value) NetworkStatus.CONNECTED else NetworkStatus.DISCONNECTED

        if (capabilities != null) {
            updateConnectionType(capabilities)
        }
    }

    private fun updateConnectionType(capabilities: NetworkCapabilities? = null) {
        val caps = capabilities ?: connectivityManager.getNetworkCapabilities(
            connectivityManager.activeNetwork
        )

        _connectionType.value = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> ConnectionType.WIFI
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> ConnectionType.CELLULAR
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> ConnectionType.ETHERNET
            else -> ConnectionType.UNKNOWN
        }

        println("🌐 Ağ durumu: ${_networkStatus.value.description} - ${_connectionType.value.description}")
    }

    // ── Public Methods ──

    fun checkNetworkStatus(): Boolean = _isOnline.value

    fun updateLastSyncDate() {
        lastSyncDate = Date()
    }

    fun getLastSyncDate(): Date? = lastSyncDate

    fun getTimeSinceLastSync(): String {
        val lastSync = lastSyncDate ?: return "Hiç senkronize edilmedi"

        val timeInterval = (Date().time - lastSync.time) / 1000 // saniye

        return when {
            timeInterval < 60 -> "Az önce senkronize edildi"
            timeInterval < 3600 -> "${timeInterval / 60} dakika önce senkronize edildi"
            timeInterval < 86400 -> "${timeInterval / 3600} saat önce senkronize edildi"
            else -> "${timeInterval / 86400} gün önce senkronize edildi"
        }
    }

    fun getOfflineMessage(operation: String): String {
        return "$operation işlemi internet bağlantısı gerektirir. Lütfen çevrimiçi olduğunuzda tekrar deneyin."
    }

    val syncStatusInfo: String
        get() = if (_isOnline.value) {
            "🟢 Çevrimiçi - ${getTimeSinceLastSync()}"
        } else {
            "🔴 Çevrimdışı - Sınırlı özellikler kullanılabilir"
        }

    /**
     * Network durumunu Flow olarak dinle
     */
    fun observeNetworkStatus(): Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // İlk değeri gönder
        trySend(_isOnline.value)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}

/**
 * Bağlantı Türü
 */
enum class ConnectionType {
    WIFI,
    CELLULAR,
    ETHERNET,
    UNKNOWN;

    val description: String
        get() = when (this) {
            WIFI -> "WiFi"
            CELLULAR -> "Mobil Veri"
            ETHERNET -> "Kablolu Ağ"
            UNKNOWN -> "Bilinmiyor"
        }

    val emoji: String
        get() = when (this) {
            WIFI -> "📶"
            CELLULAR -> "📱"
            ETHERNET -> "🔌"
            UNKNOWN -> "❓"
        }
}

/**
 * Çevrimdışı İşlemler Yardımcısı
 */
object OfflineOperationsManager {

    // Çevrimdışı desteklenen işlemler
    private val supportedOfflineOperations = setOf(
        "viewBooks",
        "viewStudents",
        "viewBorrowedBooks",
        "searchBooks",
        "viewBookDetails",
        "viewOverdueBooks"
    )

    // Çevrimdışı desteklenmeyen işlemler
    private val onlineOnlyOperations = setOf(
        "addBook",
        "addStudent",
        "borrowBook",
        "returnBook",
        "approveAdmin",
        "deleteBook",
        "deleteStudent",
        "firstTimeDataLoad"
    )

    fun isOfflineSupported(operation: String): Boolean =
        supportedOfflineOperations.contains(operation)

    fun requiresOnline(operation: String): Boolean =
        onlineOnlyOperations.contains(operation)

    fun getOfflineMessage(operation: String): String {
        return when (operation) {
            "addBook" -> "Yeni kitap eklemek için internet bağlantısı gereklidir."
            "addStudent" -> "Yeni öğrenci eklemek için internet bağlantısı gereklidir."
            "borrowBook" -> "Kitap ödünç verme işlemi için internet bağlantısı gereklidir."
            "returnBook" -> "Kitap iade işlemi için internet bağlantısı gereklidir."
            "approveAdmin" -> "Admin onaylama işlemi için internet bağlantısı gereklidir."
            "deleteBook", "deleteStudent" -> "Silme işlemleri için internet bağlantısı gereklidir."
            "firstTimeDataLoad" -> "İlk veri yükleme işlemi için internet bağlantısı gereklidir."
            else -> "Bu işlem için internet bağlantısı gereklidir."
        }
    }
}