package com.raysix.fitns.feature.scanner

import android.Manifest
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine

@AndroidXOptIn(ExperimentalGetImage::class)
@Composable
fun BarcodeScannerScreen(
    onBarcodeDetected: (String) -> Unit,
    onCancel: () -> Unit
) {
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
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
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
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BarcodeTargetArea(
                    hasCameraPermission = hasCameraPermission,
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BarcodeCancelButton(onClick = onCancel)
                BarcodeTargetArea(
                    hasCameraPermission = hasCameraPermission,
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
private fun BarcodeTargetArea(
    hasCameraPermission: Boolean,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (hasCameraPermission) {
            val frameSize = minOf(maxWidth * 0.78f, maxHeight * 0.78f, 320.dp)
            if (frameSize > 0.dp) {
                Box(
                    modifier = Modifier
                        .size(frameSize)
                        .border(3.dp, Color.White, RoundedCornerShape(18.dp))
                )
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
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.74f),
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
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = Color.White)
                }
            }
            Text(
                text = "Scan barcode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = statusMessage,
                color = Color.White.copy(alpha = 0.88f)
            )
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
        modifier = Modifier.fillMaxWidth(),
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
        modifier = modifier
    ) {
        Text(
            text = "Cancel",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp)
        )
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
