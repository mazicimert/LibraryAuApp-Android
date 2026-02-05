package com.mehmetmertmazici.libraryauapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mehmetmertmazici.libraryauapp.ui.theme.AnkaraBlue
import com.mehmetmertmazici.libraryauapp.ui.theme.AnkaraLightBlue

/**
 * EmptyStateView
 * Boş durum görünümü
 *
 * iOS Karşılığı: EmptyStateView (SharedComponents.swift içinde)
 */
@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    message: String,
    actionTitle: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(60.dp)
            )

            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onBackground
            )

            Text(
                text = message,
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (actionTitle != null && onAction != null) {
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AnkaraBlue
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = actionTitle,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}