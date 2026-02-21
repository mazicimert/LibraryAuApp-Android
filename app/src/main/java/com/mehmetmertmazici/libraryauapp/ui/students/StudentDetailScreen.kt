package com.mehmetmertmazici.libraryauapp.ui.students

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mehmetmertmazici.libraryauapp.data.model.Student
import com.mehmetmertmazici.libraryauapp.domain.util.turkishDateString
import com.mehmetmertmazici.libraryauapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * StudentDetailScreen Öğrenci detay ekranı - profil ve ödünç geçmişi
 *
 * iOS Karşılığı: StudentDetailView.swift
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailScreen(
        studentId: String,
        viewModel: StudentDetailViewModel = hiltViewModel(),
        onBack: () -> Unit
) {
        val uiState by viewModel.uiState.collectAsState()
        val activeBorrows by viewModel.activeBorrows.collectAsState()
        val historyBorrows by viewModel.historyBorrows.collectAsState()
        val colorScheme = MaterialTheme.colorScheme
        val context = LocalContext.current

        // Load data on appear
        LaunchedEffect(studentId) { viewModel.loadData(studentId) }

        val student = uiState.student

        Scaffold(
                topBar = {
                        TopAppBar(
                                title = { Text("Öğrenci Detayı") },
                                navigationIcon = {
                                        IconButton(onClick = onBack) {
                                                Icon(
                                                        imageVector =
                                                                Icons.AutoMirrored.Filled.ArrowBack,
                                                        contentDescription = "Geri",
                                                        tint = AnkaraLightBlue
                                                )
                                        }
                                },
                                colors =
                                        TopAppBarDefaults.topAppBarColors(
                                                containerColor = Color.Transparent
                                        )
                        )
                }
        ) { paddingValues ->
                AnkaraBackground {
                        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                                if (uiState.isLoading || student == null) {
                                        Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                        ) {
                                                Column(
                                                        horizontalAlignment =
                                                                Alignment.CenterHorizontally,
                                                        verticalArrangement =
                                                                Arrangement.spacedBy(16.dp)
                                                ) {
                                                        CircularProgressIndicator(
                                                                color = AnkaraBlue
                                                        )
                                                        Text(
                                                                "Bilgiler Yükleniyor...",
                                                                color = colorScheme.onSurfaceVariant
                                                        )
                                                }
                                        }
                                } else {
                                        LazyColumn(
                                                modifier = Modifier.fillMaxSize(),
                                                contentPadding = PaddingValues(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                                // ── Student Profile Section ──
                                                item {
                                                        StudentProfileSection(
                                                                student = student,
                                                                onSendEmail = {
                                                                        val intent =
                                                                                Intent(
                                                                                                Intent.ACTION_SENDTO
                                                                                        )
                                                                                        .apply {
                                                                                                data =
                                                                                                        Uri.parse(
                                                                                                                "mailto:${student.email}"
                                                                                                        )
                                                                                        }
                                                                        context.startActivity(
                                                                                intent
                                                                        )
                                                                }
                                                        )
                                                }

                                                item {
                                                        HorizontalDivider(
                                                                color =
                                                                        colorScheme.onSurfaceVariant
                                                                                .copy(alpha = 0.3f)
                                                        )
                                                }

                                                // ── Active Borrows Section ──
                                                item {
                                                        SectionHeader(
                                                                icon = Icons.Filled.MenuBook,
                                                                iconTint = AnkaraLightBlue,
                                                                title = "Aktif Ödünçler",
                                                                count = activeBorrows.size
                                                        )
                                                }

                                                if (activeBorrows.isEmpty()) {
                                                        item {
                                                                EmptyBorrowStateRow(
                                                                        icon =
                                                                                Icons.Filled
                                                                                        .LibraryBooks,
                                                                        message =
                                                                                "Şu anda ödünç kitabı yok"
                                                                )
                                                        }
                                                } else {
                                                        items(activeBorrows, key = { it.id }) { item
                                                                ->
                                                                BorrowInfoRow(
                                                                        item = item,
                                                                        isActive = true
                                                                )
                                                        }
                                                }

                                                item {
                                                        HorizontalDivider(
                                                                color =
                                                                        colorScheme.onSurfaceVariant
                                                                                .copy(alpha = 0.3f)
                                                        )
                                                }

                                                // ── Borrow History Section ──
                                                item {
                                                        SectionHeader(
                                                                icon = Icons.Filled.History,
                                                                iconTint =
                                                                        colorScheme
                                                                                .onSurfaceVariant,
                                                                title = "Ödünç Geçmişi",
                                                                count = null
                                                        )
                                                }

                                                if (historyBorrows.isEmpty()) {
                                                        item {
                                                                EmptyBorrowStateRow(
                                                                        icon =
                                                                                Icons.Filled
                                                                                        .Schedule,
                                                                        message =
                                                                                "Geçmiş kayıt bulunamadı"
                                                                )
                                                        }
                                                } else {
                                                        items(historyBorrows, key = { it.id }) {
                                                                item ->
                                                                BorrowInfoRow(
                                                                        item = item,
                                                                        isActive = false
                                                                )
                                                        }
                                                }

                                                item { Spacer(modifier = Modifier.height(32.dp)) }
                                        }
                                }
                        }
                }
        }
}

@Composable
private fun StudentProfileSection(student: Student, onSendEmail: () -> Unit) {
        val colorScheme = MaterialTheme.colorScheme

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Profile header
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                        // Avatar
                        Box(
                                modifier =
                                        Modifier.size(80.dp)
                                                .clip(CircleShape)
                                                .background(
                                                        brush =
                                                                Brush.linearGradient(
                                                                        colors =
                                                                                listOf(
                                                                                        AnkaraSuccess,
                                                                                        AnkaraSuccess
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.6f
                                                                                                )
                                                                                )
                                                                )
                                                ),
                                contentAlignment = Alignment.Center
                        ) {
                                Text(
                                        text = student.name.take(1).uppercase(),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                )
                        }

                        // Name and info
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                        text = student.fullName,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onBackground
                                )

                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                        Icon(
                                                imageVector = Icons.Filled.Tag,
                                                contentDescription = null,
                                                tint = colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                                text = student.studentNumber,
                                                fontSize = 14.sp,
                                                color = colorScheme.onSurfaceVariant
                                        )
                                }

                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                        Icon(
                                                imageVector = Icons.Filled.CalendarMonth,
                                                contentDescription = null,
                                                tint = colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                                text =
                                                        "Kayıt: ${student.createdAt?.toDate()?.turkishDateString ?: "-"}",
                                                fontSize = 12.sp,
                                                color = colorScheme.onSurfaceVariant
                                        )
                                }
                        }
                }

                // Email card
                Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = colorScheme.surface.copy(alpha = 0.6f)
                                )
                ) {
                        Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Icon(
                                        imageVector = Icons.Filled.Email,
                                        contentDescription = null,
                                        tint = AnkaraSuccess,
                                        modifier = Modifier.size(28.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                        text = student.email,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Send email button
                                Text(
                                        text = "Mail Gönder",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AnkaraSuccess,
                                        modifier =
                                                Modifier.background(
                                                                color =
                                                                        AnkaraSuccess.copy(
                                                                                alpha = 0.1f
                                                                        ),
                                                                shape = RoundedCornerShape(12.dp)
                                                        )
                                                        .clickable { onSendEmail() }
                                                        .padding(
                                                                horizontal = 12.dp,
                                                                vertical = 6.dp
                                                        )
                                )
                        }
                }
        }
}

@Composable
private fun SectionHeader(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        iconTint: Color,
        title: String,
        count: Int?
) {
        val colorScheme = MaterialTheme.colorScheme

        Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                )

                Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onBackground
                )

                Spacer(modifier = Modifier.weight(1f))

                if (count != null && count > 0) {
                        Text(
                                text = "$count",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier =
                                        Modifier.background(
                                                        color = AnkaraLightBlue,
                                                        shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                }
        }
}

@Composable
private fun EmptyBorrowStateRow(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        message: String
) {
        val colorScheme = MaterialTheme.colorScheme

        Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors =
                        CardDefaults.cardColors(
                                containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
        ) {
                Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                        Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(40.dp)
                        )

                        Text(text = message, fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                }
        }
}

@Composable
private fun BorrowInfoRow(item: BorrowInfoModel, isActive: Boolean) {
        val colorScheme = MaterialTheme.colorScheme
        val statusColor =
                when {
                        isActive && item.borrowRecord.isOverdue -> AnkaraDanger
                        isActive -> AnkaraWarning
                        else -> AnkaraSuccess
                }

        Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors =
                        CardDefaults.cardColors(
                                containerColor = colorScheme.surface.copy(alpha = 0.7f)
                        ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        // Book icon
                        Box(
                                modifier =
                                        Modifier.size(width = 50.dp, height = 70.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(statusColor.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        imageVector = Icons.Filled.Book,
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(28.dp)
                                )
                        }

                        // Book info
                        Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                                // Title
                                Text(
                                        text = item.bookTitle,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorScheme.onBackground,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                )

                                // Author
                                Text(
                                        text = item.bookAuthor,
                                        fontSize = 12.sp,
                                        color = colorScheme.onSurfaceVariant
                                )

                                // Dates
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        // Borrow date
                                        Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                                Icon(
                                                        imageVector = Icons.Filled.ArrowDownward,
                                                        contentDescription = null,
                                                        tint = AnkaraSuccess,
                                                        modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                        text =
                                                                formatDate(
                                                                        item.borrowRecord.borrowDate
                                                                                ?.toDate()
                                                                ),
                                                        fontSize = 10.sp,
                                                        color = colorScheme.onSurfaceVariant
                                                )
                                        }

                                        if (isActive) {
                                                // Due date / Remaining days
                                                Row(
                                                        verticalAlignment =
                                                                Alignment.CenterVertically,
                                                        horizontalArrangement =
                                                                Arrangement.spacedBy(4.dp)
                                                ) {
                                                        Icon(
                                                                imageVector =
                                                                        Icons.Filled
                                                                                .HourglassBottom,
                                                                contentDescription = null,
                                                                tint =
                                                                        if (item.borrowRecord
                                                                                        .isOverdue
                                                                        )
                                                                                AnkaraDanger
                                                                        else AnkaraWarning,
                                                                modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                                text =
                                                                        if (item.borrowRecord
                                                                                        .isOverdue
                                                                        ) {
                                                                                "${item.borrowRecord.overdueDays} gün gecikmiş"
                                                                        } else {
                                                                                "${item.borrowRecord.remainingDays} gün kaldı"
                                                                        },
                                                                fontSize = 10.sp,
                                                                fontWeight =
                                                                        if (item.borrowRecord
                                                                                        .isOverdue
                                                                        )
                                                                                FontWeight.Bold
                                                                        else FontWeight.Normal,
                                                                color =
                                                                        if (item.borrowRecord
                                                                                        .isOverdue
                                                                        )
                                                                                AnkaraDanger
                                                                        else
                                                                                colorScheme
                                                                                        .onSurfaceVariant
                                                        )
                                                }
                                        } else {
                                                // Return date
                                                Row(
                                                        verticalAlignment =
                                                                Alignment.CenterVertically,
                                                        horizontalArrangement =
                                                                Arrangement.spacedBy(4.dp)
                                                ) {
                                                        Icon(
                                                                imageVector =
                                                                        Icons.Filled.ArrowUpward,
                                                                contentDescription = null,
                                                                tint = AnkaraLightBlue,
                                                                modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                                text =
                                                                        formatDate(
                                                                                item.borrowRecord
                                                                                        .returnDate
                                                                                        ?.toDate()
                                                                        ),
                                                                fontSize = 10.sp,
                                                                color = colorScheme.onSurfaceVariant
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }
}

private fun formatDate(date: Date?): String {
        if (date == null) return "-"
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return formatter.format(date)
}
