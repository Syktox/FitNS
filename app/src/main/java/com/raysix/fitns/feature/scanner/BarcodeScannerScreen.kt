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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    var scanAttempt by remember { mutableStateOf(0) }
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

    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) return@LaunchedEffect
        val provider = runCatching { cameraProvider(context) }.getOrElse {
            statusMessage = "Camera could not be started. Try again."
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
        provider.unbindAll()
        runCatching {
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            boundProvider = provider
        }.onFailure {
            statusMessage = "Camera could not be started. Try again."
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Camera permission is required to scan barcodes.",
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Surface(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(
                        text = "Grant permission",
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(280.dp)
                .border(3.dp, Color.White, RoundedCornerShape(16.dp))
        )

        Surface(
            onClick = onCancel,
            shape = RoundedCornerShape(50),
            color = Color(0xCC2A2A2A),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Text(
                text = "Cancel",
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = statusMessage,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            if (hasCameraPermission && !detecting) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Surface(
                        onClick = {
                            detecting = true
                            scanAttempt += 1
                            statusMessage = "Align a barcode within the frame"
                        },
                        shape = RoundedCornerShape(50),
                        color = Color(0xCC2A2A2A)
                    ) {
                        Text(
                            text = "Retry scan",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

private suspend fun cameraProvider(context: Context): ProcessCameraProvider {
    return suspendCancellableCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            {
                continuation.resume(future.get())
            },
            ContextCompat.getMainExecutor(context)
        )
    }
}
