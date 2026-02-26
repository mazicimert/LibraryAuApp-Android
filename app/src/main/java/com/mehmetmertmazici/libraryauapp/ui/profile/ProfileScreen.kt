package com.mehmetmertmazici.libraryauapp.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mehmetmertmazici.libraryauapp.data.model.AdminUser
import com.mehmetmertmazici.libraryauapp.ui.components.LoadingOverlay
import com.mehmetmertmazici.libraryauapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

// ══════════════════════════════════════════════════════════════
// MARK: - ProfileScreen
// iOS Karşılığı: ProfileView
// ══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToTrash: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val adminUser by viewModel.currentAdminUser.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // TopAppBar ile toolbar aksiyonları
            TopAppBar(
                title = { },
                actions = {
                    // Süper Admin için Çöp Kutusu butonu
                    if (viewModel.isSuperAdmin) {
                        IconButton(onClick = onNavigateToTrash) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Çöp Kutusu",
                                tint = AnkaraLightBlue
                            )
                        }
                    }
                    // Ayarlar butonu
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Ayarlar",
                            tint = AnkaraLightBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            // Header Section
            ProfileHeaderSection()

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                val currentUser = adminUser
                if (currentUser != null) {
                    // Kullanıcı profil bölümü
                    UserProfileSection(adminUser = currentUser)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Ağ durumu bölümü
                    NetworkStatusSection(
                        isOnline = isOnline,
                        syncStatusInfo = viewModel.syncStatusInfo
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Hesap bilgileri bölümü
                    UserStatsSection(adminUser = currentUser)

                    Spacer(modifier = Modifier.height(40.dp))

                    // Çıkış butonu
                    LogoutButton(onClick = { viewModel.showLogoutConfirmation() })

                    Spacer(modifier = Modifier.height(30.dp))
                } else {
                    // Kullanıcı bilgisi yüklenemedi
                    UserLoadErrorView(onRetryLogin = { viewModel.performLogout() })
                }
            }
        }

        // Loading overlay
        if (uiState.isLoggingOut) {
            LoadingOverlay(message = "Çıkış yapılıyor...")
        }
    }

    // ── Dialogs ──

    // Logout confirmation
    if (uiState.showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissLogoutConfirmation() },
            title = { Text("Çıkış Yap") },
            text = { Text("Çıkış yapmak istediğinizden emin misiniz?") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.performLogout() },
                    colors = ButtonDefaults.textButtonColors(contentColor = AnkaraDanger)
                ) {
                    Text("Çıkış Yap")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissLogoutConfirmation() }) {
                    Text("İptal")
                }
            }
        )
    }

    // Generic alert
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
}

// ══════════════════════════════════════════════════════════════
// MARK: - Profile Sub-Components
// ══════════════════════════════════════════════════════════════

@Composable
private fun ProfileHeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Profil",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Fallback icon (logo yerine)
        Icon(
            imageVector = Icons.Filled.AccountBalance,
            contentDescription = "Logo",
            tint = AnkaraLightBlue,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun UserProfileSection(adminUser: AdminUser) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Profil ikonu — gradient daire
        Box(
            modifier = Modifier
                .size(100.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = AnkaraBlue.copy(alpha = 0.3f)
                )
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(AnkaraBlue, AnkaraLightBlue)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "Profil",
                tint = Color.White,
                modifier = Modifier.size(50.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Kullanıcı adı
        Text(
            text = adminUser.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        // E-posta
        Text(
            text = adminUser.email,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Rol rozeti
        val roleColor = if (adminUser.isSuperAdmin) Color(0xFF9C27B0) else AnkaraBlue
        Text(
            text = adminUser.roleText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = roleColor,
            modifier = Modifier
                .background(
                    color = roleColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun NetworkStatusSection(
    isOnline: Boolean,
    syncStatusInfo: String
) {
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) {
                MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
            } else {
                MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Bağlantı Durumu",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status icon
                val statusColor = if (isOnline) AnkaraSuccess else AnkaraDanger
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = statusColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isOnline) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                        contentDescription = "Ağ durumu",
                        tint = statusColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isOnline) "Çevrimiçi" else "Çevrimdışı",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = syncStatusInfo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun UserStatsSection(adminUser: AdminUser) {
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) {
                MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
            } else {
                MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Hesap Bilgileri",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Hesap Türü
            val roleColor = if (adminUser.isSuperAdmin) Color(0xFF9C27B0) else Color(0xFF2196F3)
            InfoRow(
                icon = if (adminUser.isSuperAdmin) Icons.Filled.Star else Icons.Filled.Badge,
                iconColor = roleColor,
                label = "Hesap Türü",
                value = if (adminUser.isSuperAdmin) "Süper Admin" else "Admin",
                valueColor = roleColor,
                showBadge = true
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Durum
            val statusColor = if (adminUser.isActive) AnkaraSuccess else AnkaraWarning
            InfoRow(
                icon = if (adminUser.isActive) Icons.Filled.CheckCircle else Icons.Filled.Schedule,
                iconColor = statusColor,
                label = "Durum",
                value = if (adminUser.isActive) "Aktif" else "Beklemede",
                valueColor = statusColor,
                showBadge = false
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Kayıt Tarihi
            val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("tr"))
            val dateText = try {
                dateFormat.format(adminUser.createdAt.toDate())
            } catch (e: Exception) {
                "Bilinmiyor"
            }

            InfoRow(
                icon = Icons.Filled.CalendarToday,
                iconColor = Color(0xFF2196F3),
                label = "Kayıt Tarihi",
                value = dateText,
                valueColor = MaterialTheme.colorScheme.onBackground,
                showBadge = false
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    valueColor: Color,
    showBadge: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (showBadge) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = valueColor,
                modifier = Modifier
                    .background(
                        color = valueColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = valueColor
            )
        }
    }
}

@Composable
private fun LogoutButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Red, Color.Red.copy(alpha = 0.8f))
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Çıkış",
                    tint = Color.White
                )
                Text(
                    text = "Çıkış Yap",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun UserLoadErrorView(onRetryLogin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = "Hata",
            tint = AnkaraWarning,
            modifier = Modifier.size(60.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Kullanıcı Bilgisi Yüklenemedi",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Lütfen internet bağlantınızı kontrol edin ve tekrar giriş yapın",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onRetryLogin,
            colors = ButtonDefaults.buttonColors(containerColor = AnkaraBlue),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = "Tekrar Giriş Yap",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - SettingsScreen
// iOS Karşılığı: SettingsView
// ══════════════════════════════════════════════════════════════

private const val APP_VERSION = "1.0.0"
private const val APP_BUILD_NUMBER = "1"
private const val DEVELOPER_NAME = "Mehmet Mert Mazıcı"
private const val DEVELOPER_EMAIL = "libraryau.app@gmail.com"
private const val PRIVACY_POLICY_URL = "https://tree-bottle-a85.notion.site/A-K-t-phane-Gizlilik-Politikas-2bfc4029a7aa80148b30c8bc1e3fff05"
private const val TERMS_OF_SERVICE_URL = "https://tree-bottle-a85.notion.site/A-K-t-phane-Kullan-m-Ko-ullar-2bfc4029a7aa80338cccea31715f1b66"
private const val SUPPORT_URL = "https://tree-bottle-a85.notion.site/A-K-t-phane-Yard-m-Merkezi-2bfc4029a7aa8078a7e2e151ba7e1071"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    var showAbout by remember { mutableStateOf(false) }

    if (showAbout) {
        AboutScreen(onBack = { showAbout = false })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayarlar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Uygulama Section ──
            SettingsSectionHeader(title = "UYGULAMA")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    // Hakkında
                    SettingsRow(
                        icon = Icons.Filled.Info,
                        iconColor = Color(0xFF2196F3),
                        title = "Hakkında",
                        onClick = { showAbout = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
                    // Gizlilik Politikası
                    SettingsRow(
                        icon = Icons.Filled.PrivacyTip,
                        iconColor = Color(0xFF9C27B0),
                        title = "Gizlilik Politikası",
                        isExternal = true,
                        onClick = { uriHandler.openUri(PRIVACY_POLICY_URL) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
                    // Kullanım Koşulları
                    SettingsRow(
                        icon = Icons.Filled.Description,
                        iconColor = AnkaraWarning,
                        title = "Kullanım Koşulları",
                        isExternal = true,
                        onClick = { uriHandler.openUri(TERMS_OF_SERVICE_URL) }
                    )
                }
            }

            // ── Destek Section ──
            SettingsSectionHeader(title = "DESTEK")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    // Yardım Merkezi
                    SettingsRow(
                        icon = Icons.Filled.Help,
                        iconColor = AnkaraSuccess,
                        title = "Yardım Merkezi",
                        isExternal = true,
                        onClick = { uriHandler.openUri(SUPPORT_URL) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
                    // Bize Ulaşın
                    SettingsRow(
                        icon = Icons.Filled.Email,
                        iconColor = Color.Red,
                        title = "Bize Ulaşın",
                        isExternal = true,
                        onClick = {
                            val subject = "AÜ Kütüphane - Destek Talebi"
                            val body = "\n\n---\nUygulama: AÜ Kütüphane\nVersiyon: $APP_VERSION\nCihaz: ${Build.MODEL}\nAndroid: ${Build.VERSION.RELEASE}"
                            val encodedSubject = Uri.encode(subject)
                            val encodedBody = Uri.encode(body)
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:$DEVELOPER_EMAIL?subject=$encodedSubject&body=$encodedBody")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                // No email client
                            }
                        }
                    )
                }
            }

            // ── Hesap Yönetimi Section ──
            SettingsSectionHeader(title = "HESAP YÖNETİMİ")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                SettingsRow(
                    icon = Icons.Filled.Delete,
                    iconColor = Color.Red,
                    title = "Hesabı Sil",
                    titleColor = Color.Red,
                    enabled = isOnline && !uiState.isCheckingDeletion,
                    onClick = { viewModel.checkAndShowDeleteConfirmation() }
                )
            }

            // ── Versiyon Section ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Versiyon",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$APP_VERSION ($APP_BUILD_NUMBER)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Geliştirici",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = DEVELOPER_NAME,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // ── Checking deletion overlay ──
    if (uiState.isCheckingDeletion) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text("Kontrol ediliyor...")
                }
            }
        }
    }

    // ── Delete Confirmation Dialog ──
    if (uiState.showDeleteConfirmation) {
        val isSuperAdmin = viewModel.isSuperAdmin
        val canDelete = uiState.canDeleteAccount

        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirmation() },
            title = {
                Text(
                    if (isSuperAdmin && !canDelete) "İşlem Yapılamıyor" else "Hesabı Sil"
                )
            },
            text = {
                Text(
                    if (isSuperAdmin && !canDelete) {
                        "Sistemde tek süper admin olduğunuz için hesabınızı silemezsiniz. Önce başka bir süper admin eklemeniz gerekmektedir."
                    } else {
                        "Hesabınızı silmek üzeresiniz. Bu işlem geri alınamaz. Devam etmek istiyor musunuz?"
                    }
                )
            },
            confirmButton = {
                if (isSuperAdmin && !canDelete) {
                    TextButton(onClick = { viewModel.dismissDeleteConfirmation() }) {
                        Text("Tamam")
                    }
                } else {
                    TextButton(
                        onClick = { viewModel.showPasswordPrompt() },
                        colors = ButtonDefaults.textButtonColors(contentColor = AnkaraDanger)
                    ) {
                        Text("Devam Et")
                    }
                }
            },
            dismissButton = {
                if (canDelete || !isSuperAdmin) {
                    TextButton(onClick = { viewModel.dismissDeleteConfirmation() }) {
                        Text("İptal")
                    }
                }
            }
        )
    }

    // ── Password Prompt Dialog ──
    if (uiState.showPasswordPrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPasswordPrompt() },
            title = { Text("Şifrenizi Girin") },
            text = {
                Column {
                    Text("Hesabınızı silmek için şifrenizi girmeniz gerekiyor")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = uiState.deleteAccountPassword,
                        onValueChange = { viewModel.updateDeletePassword(it) },
                        label = { Text("Şifre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.performDeleteAccount() },
                    colors = ButtonDefaults.textButtonColors(contentColor = AnkaraDanger),
                    enabled = uiState.deleteAccountPassword.isNotEmpty()
                ) {
                    Text("Hesabı Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPasswordPrompt() }) {
                    Text("İptal")
                }
            }
        )
    }

    // ── Generic Alert ──
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
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    titleColor: Color = MaterialTheme.colorScheme.onBackground,
    isExternal: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (enabled) iconColor else iconColor.copy(alpha = 0.5f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) titleColor else titleColor.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f)
        )
        if (isExternal) {
            Icon(
                imageVector = Icons.Filled.OpenInNew,
                contentDescription = "Dış bağlantı",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - AboutScreen
// iOS Karşılığı: AboutView
// ══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hakkında") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App Icon fallback
                    Icon(
                        imageVector = Icons.Filled.MenuBook,
                        contentDescription = "App Icon",
                        tint = AnkaraLightBlue,
                        modifier = Modifier.size(60.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "AÜ Kütüphane",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Kütüphane Yönetim Sistemi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Versiyon $APP_VERSION ($APP_BUILD_NUMBER)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Özellikler ──
            SettingsSectionHeader(title = "ÖZELLİKLER")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    FeatureRow(
                        icon = Icons.Filled.MenuBook,
                        title = "Kitap Yönetimi",
                        description = "Kitap ekleme, düzenleme ve silme"
                    )
                    FeatureRow(
                        icon = Icons.Filled.Groups,
                        title = "Öğrenci Yönetimi",
                        description = "Öğrenci kayıtları ve takibi"
                    )
                    FeatureRow(
                        icon = Icons.Filled.SwapHoriz,
                        title = "Ödünç Takibi",
                        description = "Kitap ödünç alma ve iade işlemleri"
                    )
                    FeatureRow(
                        icon = Icons.Filled.BarChart,
                        title = "Raporlama",
                        description = "Detaylı istatistikler ve raporlar"
                    )
                    FeatureRow(
                        icon = Icons.Filled.WifiOff,
                        title = "Çevrimdışı Mod",
                        description = "İnternet olmadan da çalışır"
                    )
                }
            }

            // ── Hakkında ──
            SettingsSectionHeader(title = "HAKKINDA")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "LibraryAu, Ankara Üniversitesi Yazılım Mühendisliği Bölümü için geliştirilmiş modern bir kütüphane yönetim sistemidir. Sadece yönetici kullanımına özel olarak tasarlanmış olup, Firebase altyapısı ile güvenli, hızlı ve profesyonel bir yönetim deneyimi sunar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // ── Credits ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Geliştirici",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = DEVELOPER_NAME,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "© 2025",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tüm hakları saklıdır",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = AnkaraLightBlue,
            modifier = Modifier.size(22.dp)
        )

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
