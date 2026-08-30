package com.raysix.fitns.feature.scanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn as AndroidXOptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import com.raysix.fitns.R

@AndroidXOptIn(ExperimentalGetImage::class)
@Composable
fun BarcodeScannerScreen(
    onBarcodeDetected: (String) -> Unit,
    onCancel: () -> Unit
) {
    BarcodeScannerSystemBars()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    var boundProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var detecting by remember { mutableStateOf(true) }
    var scanAttempt by remember { mutableIntStateOf(0) }
    var statusMessage by remember { mutableStateOf("Align a barcode within the frame") }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        statusMessage = if (granted) {
            "Align a barcode within the frame"
        } else {
            "Camera permission is required to scan barcodes."
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val previewView = remember { PreviewView(context) }
    val options = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_QR_CODE
            )
            .build()
    }
    val scanner = remember { BarcodeScanning.getClient(options) }
    DisposableEffect(Unit) {
        onDispose {
            boundProvider?.unbindAll()
            scanner.close()
            executor.shutdown()
        }
    }

    LaunchedEffect(hasCameraPermission, detecting, scanAttempt) {
        if (hasCameraPermission && detecting) {
            delay(8_000)
            if (detecting) {
                statusMessage = "No barcode detected yet. Move closer or improve lighting."
            }
        }
    }

    LaunchedEffect(hasCameraPermission, scanAttempt) {
        if (!hasCameraPermission) return@LaunchedEffect
        val provider = runCatching { cameraProvider(context) }.getOrElse {
            statusMessage = "Camera could not be started. Try again."
            detecting = false
            return@LaunchedEffect
        }
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analysis.setAnalyzer(executor) { imageProxy ->
            if (!detecting) {
                imageProxy.close()
                return@setAnalyzer
            }
            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                imageProxy.close()
                return@setAnalyzer
            }
            val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(input)
                .addOnSuccessListener { barcodes ->
                    val code = barcodes.firstOrNull()?.rawValue
                    if (code != null && detecting) {
                        detecting = false
                        statusMessage = "Barcode detected. Looking up product..."
                        onBarcodeDetected(code)
                    }
                }
                .addOnFailureListener {
                    statusMessage = "Scan failed. Try again."
                }
                .addOnCompleteListener { imageProxy.close() }
        }
        runCatching {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            boundProvider = provider
        }.onFailure {
            statusMessage = "Camera could not be started. Try again."
            detecting = false
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("barcode_scanner_screen")
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxSize()
                    .clearAndSetSemantics {
                        contentDescription = "Live camera preview for barcode scanning"
                    }
            )
        }

        val useSidePanel = maxHeight < 480.dp || maxWidth > maxHeight
        val portraitPanelMaxHeight = maxHeight * 0.34f
        val retryScan = {
            detecting = true
            scanAttempt += 1
            statusMessage = "Align a barcode within the frame"
        }

        if (useSidePanel) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BarcodeTargetArea(
                    hasCameraPermission = hasCameraPermission,
                    detecting = detecting,
                    modifier = Modifier
                        .weight(0.64f)
                        .fillMaxHeight()
                )
                BarcodeControlPanel(
                    statusMessage = statusMessage,
                    hasCameraPermission = hasCameraPermission,
                    detecting = detecting,
                    showCancel = true,
                    onCancel = onCancel,
                    onGrantPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onRetry = retryScan,
                    modifier = Modifier
                        .weight(0.36f)
                        .fillMaxHeight()
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BarcodeCancelButton(onClick = onCancel)
                BarcodeTargetArea(
                    hasCameraPermission = hasCameraPermission,
                    detecting = detecting,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
                BarcodeControlPanel(
                    statusMessage = statusMessage,
                    hasCameraPermission = hasCameraPermission,
                    detecting = detecting,
                    showCancel = false,
                    onCancel = onCancel,
                    onGrantPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onRetry = retryScan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = portraitPanelMaxHeight)
                )
            }
        }
    }
}

@Composable
private fun BarcodeScannerSystemBars() {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = (view.context as? Activity)?.window ?: return
    val controller = WindowCompat.getInsetsController(window, view)
    val previousStatus = controller.isAppearanceLightStatusBars
    val previousNavigation = controller.isAppearanceLightNavigationBars
    SideEffect {
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }
    DisposableEffect(window, view) {
        onDispose {
            controller.isAppearanceLightStatusBars = previousStatus
            controller.isAppearanceLightNavigationBars = previousNavigation
        }
    }
}

@Composable
private fun BarcodeTargetArea(
    hasCameraPermission: Boolean,
    detecting: Boolean,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (hasCameraPermission) {
            val frameWidth = minOf(maxWidth * 0.86f, 440.dp)
            val frameHeight = minOf(frameWidth * 0.42f, maxHeight * 0.42f, 180.dp)
            if (frameWidth > 0.dp && frameHeight > 0.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "CENTER THE BARCODE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Box(
                        modifier = Modifier
                            .width(frameWidth)
                            .height(frameHeight)
                            .border(
                                3.dp,
                                if (detecting) MaterialTheme.colorScheme.primary else Color.White,
                                RoundedCornerShape(22.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .height(2.dp)
                                .background(
                                    if (detecting) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f)
                                )
                        )
                    }
                }
            }
        } else {
            Text(
                text = "Camera preview unavailable",
                color = Color.White.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(20.dp)
            )
        }
    }
}

@Composable
private fun BarcodeControlPanel(
    statusMessage: String,
    hasCameraPermission: Boolean,
    detecting: Boolean,
    showCancel: Boolean,
    onCancel: () -> Unit,
    onGrantPermission: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color(0xE60A2442),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showCancel) {
                TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End)) {
                    Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White)
                    Text("Close", color = Color.White, modifier = Modifier.padding(start = 5.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(46.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.whale_coach),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.padding(3.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = "Scan barcode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Find your fuel in one scan", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.72f))
                }
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.12f),
                contentColor = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite }
            ) {
                Text(text = statusMessage, modifier = Modifier.padding(13.dp), color = Color.White.copy(alpha = 0.9f))
            }
            when {
                !hasCameraPermission -> BarcodePanelAction(
                    text = "Grant camera access",
                    onClick = onGrantPermission,
                    primary = true
                )
                !detecting -> BarcodePanelAction(
                    text = "Retry scan",
                    onClick = onRetry,
                    primary = false
                )
            }
        }
    }
}

@Composable
private fun BarcodePanelAction(
    text: String,
    onClick: () -> Unit,
    primary: Boolean
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (primary) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.16f),
        contentColor = if (primary) MaterialTheme.colorScheme.onPrimary else Color.White
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp)
        )
    }
}

@Composable
private fun BarcodeCancelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = Color.Black.copy(alpha = 0.74f),
        contentColor = Color.White,
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text = "Close", style = MaterialTheme.typography.labelLarge)
        }
    }
}

private suspend fun cameraProvider(context: Context): ProcessCameraProvider {
    return suspendCancellableCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            {
                try {
                    val provider = future.get()
                    if (continuation.isActive) continuation.resume(provider)
                } catch (error: Exception) {
                    if (continuation.isActive) continuation.resumeWith(Result.failure(error))
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }
}
