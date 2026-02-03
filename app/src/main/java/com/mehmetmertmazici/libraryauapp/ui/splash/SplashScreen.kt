package com.mehmetmertmazici.libraryauapp.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mehmetmertmazici.libraryauapp.R
import com.mehmetmertmazici.libraryauapp.ui.theme.AnkaraBlue
import com.mehmetmertmazici.libraryauapp.ui.theme.AnkaraGold
import com.mehmetmertmazici.libraryauapp.ui.theme.AnkaraLightBlue
import kotlinx.coroutines.delay

/**
 * SplashScreen
 * Animasyonlu açılış ekranı
 *
 * iOS Karşılığı: SplashView.swift
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // Animation states
    var logoVisible by remember { mutableStateOf(false) }
    var textVisible by remember { mutableStateOf(false) }
    var progressVisible by remember { mutableStateOf(false) }

    // Logo scale animation
    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = 0.68f,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )

    // Logo alpha animation
    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 750),
        label = "logoAlpha"
    )

    // Text alpha animation
    val textAlpha by animateFloatAsState(
        targetValue = if (textVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 650),
        label = "textAlpha"
    )

    // Progress alpha animation
    val progressAlpha by animateFloatAsState(
        targetValue = if (progressVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "progressAlpha"
    )

    // Pulse animation for logo
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Animation sequence
    LaunchedEffect(Unit) {
        // 1. Logo appears
        logoVisible = true

        // 2. Text appears after 350ms
        delay(350)
        textVisible = true

        // 3. Progress appears after 750ms
        delay(400)
        progressVisible = true

        // 4. Wait for splash duration then finish
        delay(1500)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(AnkaraBlue, AnkaraLightBlue)
                )
            )
    ) {
        // Decorative elements
        DecorativeElements()

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.yazilim_kutuphanem_logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(180.dp)
                    .scale(logoScale * pulseScale)
                    .alpha(logoAlpha)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Title Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(textAlpha)
            ) {
                Text(
                    text = "Yazılım Kütüphanem",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Kütüphane",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.95f),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Yönetim Sistemi",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.95f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Loading Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(progressAlpha)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Outer circle
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.25f),
                                shape = CircleShape
                            )
                    )

                    // Progress indicator
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Sistem yükleniyor...",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun DecorativeElements() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top left circle
        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(x = (-80).dp, y = (-220).dp)
                .blur(25.dp)
                .background(
                    color = Color.White.copy(alpha = 0.06f),
                    shape = CircleShape
                )
        )

        // Bottom right circle
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 140.dp, y = 320.dp)
                .blur(35.dp)
                .background(
                    color = AnkaraGold.copy(alpha = 0.08f),
                    shape = CircleShape
                )
        )
    }
}