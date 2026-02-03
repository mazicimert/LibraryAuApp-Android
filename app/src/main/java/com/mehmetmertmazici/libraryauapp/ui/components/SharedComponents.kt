package com.mehmetmertmazici.libraryauapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mehmetmertmazici.libraryauapp.ui.theme.AnkaraColors
import com.mehmetmertmazici.libraryauapp.ui.theme.AnkaraDanger
import com.mehmetmertmazici.libraryauapp.ui.theme.AnkaraLightBlue
import com.mehmetmertmazici.libraryauapp.ui.theme.AnkaraWarning
import kotlinx.coroutines.delay

/**
 * SharedComponents
 * Uygulama genelinde kullanılan ortak UI bileşenleri
 *
 * iOS Karşılığı: SharedComponents.swift
 */

// ══════════════════════════════════════════════════════════════
// MARK: - LoadingOverlay
// iOS Karşılığı: LoadingOverlayView + loadingOverlay() modifier
// ══════════════════════════════════════════════════════════════

@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    message: String = "Yükleniyor...",
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()

        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = message,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - LoadingView
// iOS Karşılığı: LoadingView.swift
// ══════════════════════════════════════════════════════════════

@Composable
fun LoadingView(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            CircularProgressIndicator(
                color = AnkaraLightBlue,
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Text(
                text = "Yükleniyor...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - ErrorView
// iOS Karşılığı: ErrorView.swift
// ══════════════════════════════════════════════════════════════

@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Hata ikonu
            Icon(
                imageVector = Icons.Default.Close, // exclamationmark.triangle.fill yerine
                contentDescription = "Hata",
                modifier = Modifier.size(60.dp),
                tint = AnkaraDanger
            )

            // Hata mesajı
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Bir Hata Oluştu",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }

            // Tekrar dene butonu
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close, // arrow.clockwise yerine refresh icon
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tekrar Dene",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - EmptyStateView
// iOS Karşılığı: EmptyStateView
// ══════════════════════════════════════════════════════════════

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    message: String,
    actionTitle: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = Color.Gray
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }

            if (actionTitle != null && onAction != null) {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = actionTitle,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - StatCard
// iOS Karşılığı: StatCard
// ══════════════════════════════════════════════════════════════

@Composable
fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = color
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - CustomButton
// iOS Karşılığı: CustomButton
// ══════════════════════════════════════════════════════════════

enum class CustomButtonStyle {
    PRIMARY,
    SECONDARY,
    DESTRUCTIVE,
    OUTLINE
}

@Composable
fun CustomButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: CustomButtonStyle = CustomButtonStyle.PRIMARY,
    enabled: Boolean = true
) {
    val backgroundColor = when (style) {
        CustomButtonStyle.PRIMARY -> MaterialTheme.colorScheme.primary
        CustomButtonStyle.SECONDARY -> Color.Gray.copy(alpha = 0.2f)
        CustomButtonStyle.DESTRUCTIVE -> AnkaraDanger.copy(alpha = 0.1f)
        CustomButtonStyle.OUTLINE -> Color.Transparent
    }

    val contentColor = when (style) {
        CustomButtonStyle.PRIMARY -> Color.White
        CustomButtonStyle.SECONDARY -> MaterialTheme.colorScheme.onBackground
        CustomButtonStyle.DESTRUCTIVE -> AnkaraDanger
        CustomButtonStyle.OUTLINE -> MaterialTheme.colorScheme.primary
    }

    val borderColor = when (style) {
        CustomButtonStyle.DESTRUCTIVE -> AnkaraDanger
        CustomButtonStyle.OUTLINE -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }

    when (style) {
        CustomButtonStyle.OUTLINE -> {
            OutlinedButton(
                onClick = onClick,
                enabled = enabled,
                shape = RoundedCornerShape(10.dp),
                modifier = modifier
            ) {
                ButtonContent(icon, title, contentColor)
            }
        }
        else -> {
            Button(
                onClick = onClick,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = backgroundColor,
                    contentColor = contentColor
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = modifier.then(
                    if (borderColor != Color.Transparent) {
                        Modifier.border(1.dp, borderColor, RoundedCornerShape(10.dp))
                    } else Modifier
                )
            ) {
                ButtonContent(icon, title, contentColor)
            }
        }
    }
}

@Composable
private fun ButtonContent(icon: ImageVector?, title: String, contentColor: Color) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - NetworkStatusBanner
// iOS Karşılığı: NetworkStatusBanner.swift
// ══════════════════════════════════════════════════════════════

@Composable
fun NetworkStatusBanner(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    var showBanner by remember { mutableStateOf(!isOnline) }

    // Online durumu değiştiğinde banner'ı güncelle
    LaunchedEffect(isOnline) {
        if (isOnline) {
            delay(2000) // 2 saniye sonra banner'ı gizle
            showBanner = false
        } else {
            showBanner = true
        }
    }

    AnimatedVisibility(
        visible = showBanner && !isOnline,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AnkaraWarning)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "İnternet bağlantısı yok - Sınırlı özellikler kullanılabilir",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { showBanner = false },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Kapat",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}