package com.mehmetmertmazici.libraryauapp.ui.scanner

import android.media.ToneGenerator
import android.media.AudioManager
import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.mehmetmertmazici.libraryauapp.domain.util.BarcodeGenerator
import com.mehmetmertmazici.libraryauapp.ui.theme.AnkaraBlue
import com.mehmetmertmazici.libraryauapp.ui.theme.AnkaraDanger
import com.mehmetmertmazici.libraryauapp.ui.theme.AnkaraSuccess
import java.util.concurrent.Executors

/**
 * BarcodeScannerScreen
 * CameraX + ML Kit ile barkod tarama ekrani
 *
 * iOS Karsiligi: BarcodeScannerView.swift
 */
@kotlin.OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current

    var statusMessage by remember { mutableStateOf("Kamera baslatiliyor...") }
    var isFlashOn by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var hasScannedSuccessfully by remember { mutableStateOf(false) }
    var showManualEntry by remember { mutableStateOf(false) }
    var lastScannedBarcode by remember { mutableStateOf<String?>(null) }
    var lastScanTime by remember { mutableStateOf(0L) }
    var cameraRef by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    val scanCooldown = 2000L // 2 saniye

    // Kamera izni
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // Manuel giris bottom sheet
    if (showManualEntry) {
        ManualBarcodeEntrySheet(
            onSubmit = { barcode ->
                showManualEntry = false
                onBarcodeScanned(barcode)
                onDismiss()
            },
            onDismiss = { showManualEntry = false }
        )
    }

    if (!cameraPermissionState.status.isGranted) {
        // Izin verilmedi ekrani
        CameraPermissionDeniedView(
            shouldShowRationale = cameraPermissionState.status.shouldShowRationale,
            onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
            onManualEntry = { showManualEntry = true },
            onDismiss = onDismiss
        )
    } else {
        // Kamera ile tarama ekrani
        Box(modifier = Modifier.fillMaxSize()) {
            // Kamera onizleme
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder()
                            .build()
                            .also { it.surfaceProvider = previewView.surfaceProvider }

                        val barcodeScanner = BarcodeScanning.getClient()
                        val analysisExecutor = Executors.newSingleThreadExecutor()

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                    processImage(
                                        imageProxy = imageProxy,
                                        barcodeScanner = barcodeScanner,
                                        hasScannedSuccessfully = hasScannedSuccessfully,
                                        lastScannedBarcode = lastScannedBarcode,
                                        lastScanTime = lastScanTime,
                                        scanCooldown = scanCooldown,
                                        onBarcodeDetected = { barcode ->
                                            hasScannedSuccessfully = true
                                            lastScannedBarcode = barcode
                                            lastScanTime = System.currentTimeMillis()
                                            statusMessage = "Barkod okundu: $barcode"

                                            // Haptic feedback
                                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

                                            // Ses efekti
                                            try {
                                                val toneGen = ToneGenerator(
                                                    AudioManager.STREAM_NOTIFICATION,
                                                    100
                                                )
                                                toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 150)
                                                toneGen.release()
                                            } catch (_: Exception) { }

                                            Log.d("BarcodeScanner", "Barkod basariyla tarandi: $barcode")
                                            onBarcodeScanned(barcode)
                                            onDismiss()
                                        },
                                        onInvalidBarcode = { barcode ->
                                            lastScannedBarcode = barcode
                                            lastScanTime = System.currentTimeMillis()
                                            statusMessage = "Gecersiz barkod formati"
                                            Log.w("BarcodeScanner", "Gecersiz barkod: $barcode")
                                        }
                                    )
                                }
                            }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                            cameraRef = camera
                            isScanning = true
                            statusMessage = "Barkodu taramaya hazir"
                            Log.d("BarcodeScanner", "Kamera baslatildi")
                        } catch (e: Exception) {
                            statusMessage = "Kamera baslatilamadi"
                            Log.e("BarcodeScanner", "Kamera hatasi: ${e.message}")
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Overlay UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                // Ust toolbar
                TopToolbar(
                    isFlashOn = isFlashOn,
                    onFlashToggle = {
                        cameraRef?.let { camera ->
                            if (camera.cameraInfo.hasFlashUnit()) {
                                isFlashOn = !isFlashOn
                                camera.cameraControl.enableTorch(isFlashOn)
                            }
                        }
                    },
                    onDismiss = onDismiss
                )

                Spacer(modifier = Modifier.weight(1f))

                // Tarama alani
                ScanningAreaOverlay(
                    statusMessage = statusMessage,
                    isScanning = isScanning
                )

                Spacer(modifier = Modifier.weight(1f))

                // Alt kontroller
                BottomControls(
                    onManualEntry = { showManualEntry = true }
                )
            }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            isScanning = false
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - Image Processing
// ══════════════════════════════════════════════════════════════

@OptIn(ExperimentalGetImage::class)
private fun processImage(
    imageProxy: ImageProxy,
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    hasScannedSuccessfully: Boolean,
    lastScannedBarcode: String?,
    lastScanTime: Long,
    scanCooldown: Long,
    onBarcodeDetected: (String) -> Unit,
    onInvalidBarcode: (String) -> Unit
) {
    if (hasScannedSuccessfully) {
        imageProxy.close()
        return
    }

    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }

    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

    barcodeScanner.process(inputImage)
        .addOnSuccessListener { barcodes ->
            for (barcode in barcodes) {
                val rawValue = barcode.rawValue ?: continue

                // Throttle: ayni barkod + cooldown suresi icinde ise atla
                if (lastScannedBarcode == rawValue &&
                    System.currentTimeMillis() - lastScanTime < scanCooldown
                ) {
                    continue
                }

                // Barkod validasyonu
                if (BarcodeGenerator.isValidBarcode(rawValue)) {
                    onBarcodeDetected(rawValue)
                    break
                } else {
                    // 1 saniye throttle ile gecersiz barkod bildirimi
                    if (lastScannedBarcode != rawValue ||
                        System.currentTimeMillis() - lastScanTime >= 1000
                    ) {
                        onInvalidBarcode(rawValue)
                    }
                }
            }
        }
        .addOnFailureListener { e ->
            Log.e("BarcodeScanner", "Barkod okuma hatasi: ${e.message}")
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}

// ══════════════════════════════════════════════════════════════
// MARK: - Top Toolbar
// ══════════════════════════════════════════════════════════════

@Composable
private fun TopToolbar(
    isFlashOn: Boolean,
    onFlashToggle: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Kapatma butonu
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Kapat",
                tint = Color.White
            )
        }

        // Flas butonu
        IconButton(
            onClick = onFlashToggle,
            modifier = Modifier
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
        ) {
            Icon(
                imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = "Flas",
                tint = if (isFlashOn) Color.Yellow else Color.White
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - Scanning Area Overlay
// ══════════════════════════════════════════════════════════════

@Composable
private fun ScanningAreaOverlay(
    statusMessage: String,
    isScanning: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Baslik
        Text(
            text = "Barkodu cerceve icine hizalayin",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )

        // Tarama cercevesi
        Box(
            modifier = Modifier.size(width = 280.dp, height = 180.dp),
            contentAlignment = Alignment.Center
        ) {
            // Cerceve kenarligi
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRoundRect(
                            color = Color.White,
                            style = Stroke(width = 2.dp.toPx()),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )
                    }
            )

            // Kose isaretleri
            CornerMarkers()

            // Tarama cizgisi animasyonu
            if (isScanning) {
                ScanningLineAnimation()
            }
        }

        // Durum mesaji
        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - Corner Markers
// ══════════════════════════════════════════════════════════════

@Composable
private fun CornerMarkers() {
    val markerLength = 24.dp
    val markerThickness = 3.dp

    Box(modifier = Modifier.fillMaxSize()) {
        // Sol ust
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(markerLength)
                    .height(markerThickness)
                    .background(Color.White)
            )
            Box(
                modifier = Modifier
                    .width(markerThickness)
                    .height(markerLength)
                    .background(Color.White)
            )
        }

        // Sag ust
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(markerLength)
                    .height(markerThickness)
                    .background(Color.White)
                    .align(Alignment.TopEnd)
            )
            Box(
                modifier = Modifier
                    .width(markerThickness)
                    .height(markerLength)
                    .background(Color.White)
                    .align(Alignment.TopEnd)
            )
        }

        // Sol alt
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(markerThickness)
                    .height(markerLength)
                    .background(Color.White)
                    .align(Alignment.BottomStart)
            )
            Box(
                modifier = Modifier
                    .width(markerLength)
                    .height(markerThickness)
                    .background(Color.White)
                    .align(Alignment.BottomStart)
            )
        }

        // Sag alt
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(markerThickness)
                    .height(markerLength)
                    .background(Color.White)
                    .align(Alignment.BottomEnd)
            )
            Box(
                modifier = Modifier
                    .width(markerLength)
                    .height(markerThickness)
                    .background(Color.White)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - Scanning Line Animation
// ══════════════════════════════════════════════════════════════

@Composable
private fun ScanningLineAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -80f,
        targetValue = 80f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLineOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .offset(y = offsetY.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Red,
                        Color.Transparent
                    )
                )
            )
    )
}

// ══════════════════════════════════════════════════════════════
// MARK: - Bottom Controls
// ══════════════════════════════════════════════════════════════

@Composable
private fun BottomControls(onManualEntry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Manuel giris butonu
        Button(
            onClick = onManualEntry,
            colors = ButtonDefaults.buttonColors(
                containerColor = AnkaraBlue.copy(alpha = 0.8f)
            ),
            shape = RoundedCornerShape(25.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Keyboard,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Manuel Giris",
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        // Desteklenen formatlar
        Text(
            text = "Desteklenen: ISBN, LIB, QR Code",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - Camera Permission Denied View
// ══════════════════════════════════════════════════════════════

@Composable
private fun CameraPermissionDeniedView(
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit,
    onManualEntry: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = AnkaraDanger,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Kamera Izni Gerekli",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (shouldShowRationale)
                "Barkod okumak icin kamera iznine ihtiyac var. Lutfen izin verin."
            else
                "Barkod okumak icin kamera iznine ihtiyac var. Lutfen ayarlardan kamera erisimini acin.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (shouldShowRationale) {
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = AnkaraBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Izin Ver", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onManualEntry,
            colors = ButtonDefaults.buttonColors(containerColor = AnkaraBlue.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Keyboard, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Manuel Giris", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onDismiss) {
            Text("Iptal", color = Color.White.copy(alpha = 0.7f))
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - Manual Barcode Entry Sheet
// ══════════════════════════════════════════════════════════════

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualBarcodeEntrySheet(
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var barcode by remember { mutableStateOf("") }
    val isValidBarcode = barcode.isNotEmpty() && BarcodeGenerator.isValidBarcode(barcode)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Baslik
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = null,
                    tint = AnkaraBlue,
                    modifier = Modifier.size(50.dp)
                )
                Text(
                    text = "Manuel Barkod Girisi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Barkod numarasini elle girebilirsiniz",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Barkod girisi
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Barkod",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    placeholder = { Text("ISBN veya LIB barkodu") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Validasyon mesaji
                if (barcode.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isValidBarcode) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AnkaraSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Gecerli barkod formati",
                                style = MaterialTheme.typography.labelSmall,
                                color = AnkaraSuccess
                            )
                        } else {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = AnkaraDanger,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Gecersiz barkod formati",
                                style = MaterialTheme.typography.labelSmall,
                                color = AnkaraDanger
                            )
                        }
                    }
                }
            }

            // Ornek barkodlar
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Ornek Formatlar:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                ExampleBarcodeItem(
                    format = "ISBN-13",
                    example = "978-605-9129-68-8",
                    description = "Standart kitap barkodu",
                    onTap = { barcode = "978-605-9129-68-8" }
                )

                ExampleBarcodeItem(
                    format = "LIB Format",
                    example = "LIB001001",
                    description = "Kutuphane kopya barkodu",
                    onTap = { barcode = "LIB001001" }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Gonder butonu
            Button(
                onClick = {
                    if (isValidBarcode) {
                        onSubmit(barcode.trim())
                    }
                },
                enabled = isValidBarcode,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AnkaraBlue,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(
                    text = "Onayla",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════
// MARK: - Example Barcode Item
// ══════════════════════════════════════════════════════════════

@Composable
private fun ExampleBarcodeItem(
    format: String,
    example: String,
    description: String,
    onTap: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onTap)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = format,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = example,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = AnkaraBlue
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "Kullan",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = AnkaraBlue
        )
    }
}
