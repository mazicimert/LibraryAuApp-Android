package com.mehmetmertmazici.libraryauapp.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mehmetmertmazici.libraryauapp.data.model.AdminUser
import com.mehmetmertmazici.libraryauapp.ui.components.EmptyStateView
import com.mehmetmertmazici.libraryauapp.ui.components.LoadingOverlay
import com.mehmetmertmazici.libraryauapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * AdminManagementScreen
 * Admin kullanıcı yönetimi ekranı (Sadece Süper Admin için)
 *
 * iOS Karşılığı: AdminManagementView.swift
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManagementScreen(
    viewModel: AdminViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    // Sheet states
    var showApprovalSheet by remember { mutableStateOf(false) }
    var showBulkApprovalConfirmation by remember { mutableStateOf(false) }
    var selectedAdmin by remember { mutableStateOf<AdminUser?>(null) }
    var showRejectConfirmation by remember { mutableStateOf(false) }

    LoadingOverlay(isLoading = viewModel.showLoadingIndicator) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            TopAppBar(
                title = {
                    Text(
                        text = "Admin Yönetimi",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Toplu onaylama butonu
                    if (viewModel.hasPendingApprovals) {
                        IconButton(onClick = { showBulkApprovalConfirmation = true }) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Toplu Onayla",
                                tint = AnkaraLightBlue
                            )
                        }
                    }

                    // Yeni admin ekleme butonu
                    if (viewModel.canManageAdmins) {
                        IconButton(onClick = { viewModel.openAddAdminForm() }) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Yeni Admin Ekle",
                                tint = AnkaraLightBlue
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            // Pending Approval Banner
            if (viewModel.hasPendingApprovals) {
                PendingApprovalBanner(
                    notification = viewModel.pendingApprovalNotification ?: "",
                    onViewClick = { showApprovalSheet = true }
                )
            }

            // Statistics Section
            StatisticsSection(
                statistics = viewModel.adminStatistics,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(vertical = 8.dp)
            )

            // Admin List Section
            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = { viewModel.refreshAdminUsers() },
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                if (viewModel.showEmptyState) {
                    EmptyStateView(
                        icon = Icons.Filled.AdminPanelSettings,
                        title = "Admin Bulunamadı",
                        message = viewModel.emptyStateMessage,
                        actionTitle = if (viewModel.canManageAdmins) "İlk Admini Ekle" else null,
                        onAction = if (viewModel.canManageAdmins) {
                            { viewModel.openAddAdminForm() }
                        } else null,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    )
                } else {
                    AdminListSection(
                        pendingAdmins = uiState.pendingAdmins,
                        approvedAdmins = uiState.approvedAdmins,
                        currentUserId = viewModel.currentUserId,
                        onApprove = { admin -> viewModel.approveAdmin(admin) },
                        onReject = { admin ->
                            selectedAdmin = admin
                            showRejectConfirmation = true
                        },
                        onRoleChange = { admin, isSuperAdmin ->
                            viewModel.updateAdminRole(admin, isSuperAdmin)
                        },
                        onDelete = { admin ->
                            selectedAdmin = admin
                            showRejectConfirmation = true
                        },
                        onRefresh = { viewModel.refreshAdminUsers() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // ── Sheets & Dialogs ──

    // Pending Approvals Sheet
    if (showApprovalSheet) {
        PendingApprovalsSheet(
            pendingAdmins = uiState.pendingAdmins,
            onApprove = { admin -> viewModel.approveAdmin(admin) },
            onReject = { admin ->
                selectedAdmin = admin
                showRejectConfirmation = true
                showApprovalSheet = false
            },
            onBulkApprove = { viewModel.performBulkApproval() },
            onDismiss = { showApprovalSheet = false }
        )
    }

    // Add Admin Sheet
    if (uiState.showAddAdminSheet) {
        AddAdminSheet(
            uiState = uiState,
            isFormValid = viewModel.isAddAdminFormValid,
            formErrors = viewModel.addAdminFormErrors,
            onDisplayNameChange = { viewModel.updateNewAdminDisplayName(it) },
            onEmailChange = { viewModel.updateNewAdminEmail(it) },
            onPasswordChange = { viewModel.updateNewAdminPassword(it) },
            onIsSuperAdminChange = { viewModel.updateNewAdminIsSuperAdmin(it) },
            onSubmit = { viewModel.initiateAddNewAdmin() },
            onDismiss = { viewModel.closeAddAdminSheet() }
        )
    }

    // Bulk Approval Confirmation Dialog
    if (showBulkApprovalConfirmation) {
        AlertDialog(
            onDismissRequest = { showBulkApprovalConfirmation = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = AnkaraSuccess,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Toplu Onaylama",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "${viewModel.pendingAdminCount} admin kalıcı olarak onaylanacak. Devam etmek istediğinizden emin misiniz?",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.performBulkApproval()
                        showBulkApprovalConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AnkaraSuccess),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tümünü Onayla", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBulkApprovalConfirmation = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("İptal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Reject Confirmation Dialog
    if (showRejectConfirmation && selectedAdmin != null) {
        AlertDialog(
            onDismissRequest = {
                showRejectConfirmation = false
                selectedAdmin = null
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AnkaraDanger.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = AnkaraDanger,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Admin Reddet",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "'${selectedAdmin?.displayName}' adminini reddetmek istediğinizden emin misiniz? Bu işlem geri alınamaz ve başvuru kalıcı olarak silinir.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedAdmin?.let { viewModel.performRejectAdmin(it) }
                        showRejectConfirmation = false
                        selectedAdmin = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AnkaraDanger),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Evet, Reddet", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showRejectConfirmation = false
                        selectedAdmin = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text("İptal Et", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Password Verification Dialog
    if (uiState.showPasswordVerification) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPasswordVerification() },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Şifrenizi Girin",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Devam etmek için lütfen şifrenizi girin.",
                        textAlign = TextAlign.Center
                    )
                    OutlinedTextField(
                        value = uiState.superAdminPassword,
                        onValueChange = { viewModel.updateSuperAdminPassword(it) },
                        label = { Text("Süper Admin Şifresi") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.addNewAdmin() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Onayla", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissPasswordVerification() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("İptal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Generic Alert Dialog
    if (uiState.showAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAlert() },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = uiState.alertTitle,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = uiState.alertMessage,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissAlert() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tamam", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - Header Section
// ══════════════════════════════════════════════════════════════

@Composable
private fun HeaderSection(modifier: Modifier = Modifier) {
    Text(
        text = "Admin Yönetimi",
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.fillMaxWidth()
    )
}

// ══════════════════════════════════════════════════════════════
// MARK: - Pending Approval Banner
// ══════════════════════════════════════════════════════════════

@Composable
private fun PendingApprovalBanner(
    notification: String,
    onViewClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFA500).copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = Color(0xFFFFA500),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Onay bekleyen adminler var",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(
            onClick = onViewClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFA500),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(text = "Görüntüle", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - Statistics Section
// ══════════════════════════════════════════════════════════════

@Composable
private fun StatisticsSection(
    statistics: AdminStatistics,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AdminStatCard(
            icon = Icons.Filled.Groups,
            title = "Toplam",
            value = "${statistics.total}",
            color = Color(0xFF2196F3),
            modifier = Modifier.weight(1f)
        )
        AdminStatCard(
            icon = Icons.Filled.VerifiedUser,
            title = "Onaylı",
            value = "${statistics.approved}",
            color = AnkaraSuccess,
            modifier = Modifier.weight(1f)
        )
        AdminStatCard(
            icon = Icons.Filled.HourglassTop,
            title = "Bekleyen",
            value = "${statistics.pending}",
            color = Color(0xFFFFA500),
            modifier = Modifier.weight(1f)
        )
        AdminStatCard(
            icon = Icons.Filled.Star,
            title = "Süper Admin",
            value = "${statistics.superAdmins}",
            color = Color(0xFF9C27B0),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AdminStatCard(
    icon: ImageVector,
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = if (isDark) 0.1f else 0.2f))
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - Admin List Section
// ══════════════════════════════════════════════════════════════

@Composable
private fun AdminListSection(
    pendingAdmins: List<AdminUser>,
    approvedAdmins: List<AdminUser>,
    currentUserId: String?,
    onApprove: (AdminUser) -> Unit,
    onReject: (AdminUser) -> Unit,
    onRoleChange: (AdminUser, Boolean) -> Unit,
    onDelete: (AdminUser) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Onay Bekleyen Adminler
        if (pendingAdmins.isNotEmpty()) {
            item {
                SectionHeader(
                    icon = Icons.Filled.HourglassTop,
                    title = "Onay Bekleyenler",
                    color = Color(0xFFFFA500),
                    count = pendingAdmins.size
                )
            }
            items(pendingAdmins, key = { it.id ?: it.email }) { admin ->
                PendingAdminRowView(
                    admin = admin,
                    onApprove = { onApprove(admin) },
                    onReject = { onReject(admin) }
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // Aktif Adminler (Onaylı)
        if (approvedAdmins.isNotEmpty()) {
            item {
                SectionHeader(
                    icon = Icons.Filled.VerifiedUser,
                    title = "Aktif Adminler",
                    color = AnkaraSuccess,
                    count = approvedAdmins.size
                )
            }
            items(approvedAdmins, key = { it.id ?: it.email }) { admin ->
                ApprovedAdminRowView(
                    admin = admin,
                    currentUserId = currentUserId,
                    onRoleChange = { isSuperAdmin -> onRoleChange(admin, isSuperAdmin) },
                    onDelete = { onDelete(admin) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    color: Color,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "$count",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(color)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - PendingAdminRowView
// ══════════════════════════════════════════════════════════════

@Composable
private fun PendingAdminRowView(
    admin: AdminUser,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale("tr")) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFA500),
                                Color(0xFFFF8C00)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = admin.displayName.take(1).uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Admin bilgileri
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = admin.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = admin.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = dateFormat.format(admin.createdAt.toDate()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Aksiyon butonları
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // Onayla butonu
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AnkaraSuccess,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Onayla", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }

                    // Reddet butonu
                    OutlinedButton(
                        onClick = onReject,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AnkaraDanger
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AnkaraDanger),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Reddet", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - ApprovedAdminRowView
// ══════════════════════════════════════════════════════════════

@Composable
private fun ApprovedAdminRowView(
    admin: AdminUser,
    currentUserId: String?,
    onRoleChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val isCurrentUser = admin.id == currentUserId
    val roleColor = if (admin.isSuperAdmin) Color(0xFF9C27B0) else Color(0xFF2196F3)

    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                roleColor,
                                roleColor.copy(alpha = 0.8f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = admin.displayName.take(1).uppercase(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Admin bilgileri
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = admin.displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isCurrentUser) {
                        Text(
                            text = "(Sen)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = roleColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(roleColor.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = admin.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Rol Rozeti
                Text(
                    text = admin.roleText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(roleColor)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            // Menü Butonu
            if (!isCurrentUser) {
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AnkaraLightBlue.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Menü",
                            tint = AnkaraLightBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (admin.isSuperAdmin) "Normal Admin Yap"
                                    else "Süper Admin Yap"
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (admin.isSuperAdmin) Icons.Filled.Person
                                    else Icons.Filled.Star,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onRoleChange(!admin.isSuperAdmin)
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Text("Admini Sil", color = AnkaraDanger)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = AnkaraDanger
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - PendingApprovalsSheet
// ══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PendingApprovalsSheet(
    pendingAdmins: List<AdminUser>,
    onApprove: (AdminUser) -> Unit,
    onReject: (AdminUser) -> Unit,
    onBulkApprove: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Kapat")
                }
                Text(
                    text = "Onay Bekleyenler",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (pendingAdmins.isNotEmpty()) {
                    TextButton(onClick = onBulkApprove) {
                        Text("Tümünü Onayla")
                    }
                } else {
                    Spacer(modifier = Modifier.width(80.dp))
                }
            }

            HorizontalDivider()

            if (pendingAdmins.isEmpty()) {
                // Boş durum
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = AnkaraSuccess,
                        modifier = Modifier.size(60.dp)
                    )
                    Text(
                        text = "Bekleyen Onay Yok",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tüm admin başvuruları işlenmiş",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pendingAdmins, key = { it.id ?: it.email }) { admin ->
                        PendingAdminDetailCard(
                            admin = admin,
                            onApprove = { onApprove(admin) },
                            onReject = { onReject(admin) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - PendingAdminDetailCard
// ══════════════════════════════════════════════════════════════

@Composable
private fun PendingAdminDetailCard(
    admin: AdminUser,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale("tr")) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar + Name/Email
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFA500).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = admin.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFA500)
                    )
                }

                Column {
                    Text(
                        text = admin.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = admin.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            // Info Rows
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow(
                    icon = Icons.Filled.CalendarToday,
                    text = "Kayıt: ${dateFormat.format(admin.createdAt.toDate())}"
                )
                InfoRow(
                    icon = Icons.Filled.Email,
                    text = admin.email
                )
                InfoRow(
                    icon = Icons.Filled.AdminPanelSettings,
                    text = "Rol: Admin"
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AnkaraSuccess,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Onayla", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AnkaraDanger,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reddet", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF2196F3),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - AddAdminSheet
// ══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAdminSheet(
    uiState: AdminUiState,
    isFormValid: Boolean,
    formErrors: List<String>,
    onDisplayNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onIsSuperAdminChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    var showPasswordRequirements by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("İptal")
                }
                Text(
                    text = "Yeni Admin Ekle",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Header Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PersonAdd,
                    contentDescription = null,
                    tint = AnkaraBlue,
                    modifier = Modifier.size(50.dp)
                )
                Text(
                    text = "Yeni Admin Ekle",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Yeni admin hesabı oluşturun",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Admin Bilgileri
            Text(
                text = "Admin Bilgileri",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Ad Soyad
                    OutlinedTextField(
                        value = uiState.newAdminDisplayName,
                        onValueChange = onDisplayNameChange,
                        label = { Text("Ad Soyad *") },
                        placeholder = { Text("Adınız Soyadınız") },
                        leadingIcon = {
                            Icon(Icons.Filled.Person, contentDescription = null)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // E-posta
                    OutlinedTextField(
                        value = uiState.newAdminEmail,
                        onValueChange = onEmailChange,
                        label = { Text("E-posta *") },
                        placeholder = { Text("admin@example.com") },
                        leadingIcon = {
                            Icon(Icons.Filled.Email, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Şifre
                    OutlinedTextField(
                        value = uiState.newAdminPassword,
                        onValueChange = onPasswordChange,
                        label = { Text("Şifre *") },
                        placeholder = { Text("En az 6 karakter") },
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Filled.VisibilityOff
                                    else Icons.Filled.Visibility,
                                    contentDescription = if (showPassword) "Şifreyi Gizle" else "Şifreyi Göster"
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Şifre gereksinimleri butonu
                    TextButton(
                        onClick = { showPasswordRequirements = true },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = AnkaraBlue
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Şifre gereksinimleri",
                            fontSize = 12.sp,
                            color = AnkaraBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Yetki Seviyesi
            Text(
                text = "Yetki Seviyesi",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.newAdminIsSuperAdmin) Icons.Filled.Star
                                else Icons.Filled.AdminPanelSettings,
                                contentDescription = null,
                                tint = if (uiState.newAdminIsSuperAdmin) Color(0xFF9C27B0) else Color(
                                    0xFF2196F3
                                )
                            )
                            Text(
                                text = "Süper Admin",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Switch(
                            checked = uiState.newAdminIsSuperAdmin,
                            onCheckedChange = onIsSuperAdminChange,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = Color(0xFF9C27B0)
                            )
                        )
                    }

                    HorizontalDivider()

                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Süper adminler tüm yetkilere sahiptir ve diğer adminleri yönetebilir.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Hata Listesi
            if (formErrors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AnkaraDanger.copy(alpha = 0.1f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                AnkaraDanger.copy(alpha = 0.3f),
                                AnkaraDanger.copy(alpha = 0.3f)
                            )
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = AnkaraDanger
                            )
                            Text(
                                text = "Lütfen aşağıdaki hataları düzeltin",
                                style = MaterialTheme.typography.titleSmall,
                                color = AnkaraDanger,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        formErrors.forEach { error ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("•", color = AnkaraDanger)
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AnkaraDanger
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Admin Ekle Butonu
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = isFormValid && !uiState.isAddingAdmin,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AnkaraBlue,
                    disabledContainerColor = Color.Gray.copy(alpha = 0.6f)
                )
            ) {
                if (uiState.isAddingAdmin) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Admin Ekle",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Şifre gereksinimleri dialog
    if (showPasswordRequirements) {
        AlertDialog(
            onDismissRequest = { showPasswordRequirements = false },
            title = { Text("Şifre Gereksinimleri") },
            text = {
                Text("• En az 6 karakter\n• En az bir harf\n• En az bir rakam")
            },
            confirmButton = {
                TextButton(onClick = { showPasswordRequirements = false }) {
                    Text("Tamam")
                }
            }
        )
    }
}
