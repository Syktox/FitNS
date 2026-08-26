package com.raysix.fitns.feature.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CameraCaptureView(
    onImageBytes: (ByteArray) -> Unit,
    modifier: Modifier = Modifier,
    captureButtonLabel: String = "Capture",
    onCancel: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    var boundProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var cameraReady by remember { mutableStateOf(false) }
    var cameraStartFailed by remember { mutableStateOf(false) }
    var cameraAttempt by remember { mutableIntStateOf(0) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            boundProvider?.unbindAll()
            executor.shutdown()
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        cameraReady = false
        cameraStartFailed = false
        errorMessage = if (granted) null else "Camera permission is required to take a photo."
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val previewView = remember { PreviewView(context) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }

    LaunchedEffect(hasCameraPermission, cameraAttempt) {
        cameraReady = false
        cameraStartFailed = false
        if (!hasCameraPermission) return@LaunchedEffect
        val provider = runCatching { cameraProvider(context) }.getOrElse {
            cameraStartFailed = true
            errorMessage = "Camera could not be started. You can retry or choose a photo."
            return@LaunchedEffect
        }
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        runCatching {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            boundProvider = provider
        }.onSuccess {
            cameraReady = true
            errorMessage = null
        }.onFailure {
            cameraStartFailed = true
            errorMessage = "Camera could not be started. You can retry or choose a photo."
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) {
            isProcessing = false
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { context.readBytesFromUri(uri) }
            }
            isProcessing = false
            result.fold(
                onSuccess = onImageBytes,
                onFailure = { error ->
                    errorMessage = error.message ?: "The selected photo could not be opened."
                }
            )
        }
    }

    val openGallery = {
        if (!isProcessing) {
            isProcessing = true
            errorMessage = null
            runCatching { galleryLauncher.launch("image/*") }
                .onFailure { error ->
                    isProcessing = false
                    errorMessage = error.message ?: "The photo picker could not be opened."
                }
        }
    }
    val takePhoto = {
        if (!isProcessing && cameraReady) {
            isProcessing = true
            errorMessage = null
            captureImage(context, imageCapture, executor) { result ->
                scope.launch {
                    isProcessing = false
                    result.fold(
                        onSuccess = onImageBytes,
                        onFailure = { error ->
                            errorMessage = error.message ?: "The photo could not be captured."
                        }
                    )
                }
            }
        }
    }
    val retryCamera = {
        if (!isProcessing) {
            errorMessage = null
            cameraAttempt += 1
        }
    }

    val configuration = LocalConfiguration.current
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val compactHeight = maxHeight < 520.dp || configuration.screenHeightDp < 520
        val useSideBySideLayout = compactHeight && maxWidth >= 520.dp
        val previewAspectRatio = if (compactHeight || maxWidth >= 600.dp) 16f / 9f else 4f / 3f

        if (useSideBySideLayout) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CameraPreview(
                    previewView = previewView,
                    hasCameraPermission = hasCameraPermission,
                    cameraReady = cameraReady,
                    cameraStartFailed = cameraStartFailed,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(previewAspectRatio)
                )
                CameraCaptureActions(
                    hasCameraPermission = hasCameraPermission,
                    cameraReady = cameraReady,
                    cameraStartFailed = cameraStartFailed,
                    isProcessing = isProcessing,
                    errorMessage = errorMessage,
                    captureButtonLabel = captureButtonLabel,
                    onOpenGallery = openGallery,
                    onRequestCameraPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onRetryCamera = retryCamera,
                    onCapture = takePhoto,
                    onCancel = onCancel,
                    stacked = true,
                    modifier = Modifier.widthIn(min = 176.dp, max = 220.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CameraPreview(
                    previewView = previewView,
                    hasCameraPermission = hasCameraPermission,
                    cameraReady = cameraReady,
                    cameraStartFailed = cameraStartFailed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(previewAspectRatio)
                )
                CameraCaptureActions(
                    hasCameraPermission = hasCameraPermission,
                    cameraReady = cameraReady,
                    cameraStartFailed = cameraStartFailed,
                    isProcessing = isProcessing,
                    errorMessage = errorMessage,
                    captureButtonLabel = captureButtonLabel,
                    onOpenGallery = openGallery,
                    onRequestCameraPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onRetryCamera = retryCamera,
                    onCapture = takePhoto,
                    onCancel = onCancel,
                    stacked = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CameraPreview(
    previewView: PreviewView,
    hasCameraPermission: Boolean,
    cameraReady: Boolean,
    cameraStartFailed: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF111315)),
        contentAlignment = Alignment.Center
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
            if (!cameraReady) {
                Text(
                    text = if (cameraStartFailed) "Camera unavailable" else "Starting camera…",
                    color = Color.White.copy(alpha = 0.82f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(20.dp)
                )
            }
        } else {
            Text(
                text = "Camera permission required",
                color = Color.White.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(20.dp)
            )
        }
    }
}

@Composable
private fun CameraCaptureActions(
    hasCameraPermission: Boolean,
    cameraReady: Boolean,
    cameraStartFailed: Boolean,
    isProcessing: Boolean,
    errorMessage: String?,
    captureButtonLabel: String,
    onOpenGallery: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onRetryCamera: () -> Unit,
    onCapture: () -> Unit,
    onCancel: (() -> Unit)?,
    stacked: Boolean,
    modifier: Modifier = Modifier
) {
    if (stacked) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GalleryButton(
                onClick = onOpenGallery,
                enabled = !isProcessing,
                showLabel = true,
                modifier = Modifier.fillMaxWidth()
            )
            CaptureButton(
                text = cameraActionLabel(
                    hasCameraPermission = hasCameraPermission,
                    cameraReady = cameraReady,
                    cameraStartFailed = cameraStartFailed,
                    isProcessing = isProcessing,
                    captureButtonLabel = captureButtonLabel
                ),
                onClick = when {
                    !hasCameraPermission -> onRequestCameraPermission
                    cameraStartFailed -> onRetryCamera
                    else -> onCapture
                },
                enabled = !isProcessing && (!hasCameraPermission || cameraReady || cameraStartFailed),
                modifier = Modifier.fillMaxWidth()
            )
            onCancel?.let { cancel ->
                TextButton(onClick = cancel, enabled = !isProcessing, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            }
            CameraErrorMessage(errorMessage)
        }
    } else {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GalleryButton(onClick = onOpenGallery, enabled = !isProcessing)
                CaptureButton(
                    text = cameraActionLabel(
                        hasCameraPermission = hasCameraPermission,
                        cameraReady = cameraReady,
                        cameraStartFailed = cameraStartFailed,
                        isProcessing = isProcessing,
                        captureButtonLabel = captureButtonLabel
                    ),
                    onClick = when {
                        !hasCameraPermission -> onRequestCameraPermission
                        cameraStartFailed -> onRetryCamera
                        else -> onCapture
                    },
                    enabled = !isProcessing && (!hasCameraPermission || cameraReady || cameraStartFailed),
                    modifier = Modifier.weight(1f)
                )
            }
            onCancel?.let { cancel ->
                TextButton(onClick = cancel, enabled = !isProcessing, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            }
            CameraErrorMessage(errorMessage)
        }
    }
}

private fun cameraActionLabel(
    hasCameraPermission: Boolean,
    cameraReady: Boolean,
    cameraStartFailed: Boolean,
    isProcessing: Boolean,
    captureButtonLabel: String
): String = when {
    isProcessing -> "Processing…"
    !hasCameraPermission -> "Enable camera"
    cameraStartFailed -> "Retry camera"
    !cameraReady -> "Starting camera…"
    else -> captureButtonLabel
}

@Composable
private fun CameraErrorMessage(message: String?) {
    message?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun GalleryButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier.heightIn(min = 56.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoLibrary,
                contentDescription = if (showLabel) null else "Choose photo from gallery",
                modifier = Modifier.size(26.dp)
            )
            if (showLabel) {
                Text(
                    text = "Gallery",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun CaptureButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier.heightIn(min = 56.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

private fun captureImage(
    context: Context,
    imageCapture: ImageCapture,
    executor: ExecutorService,
    onResult: (Result<ByteArray>) -> Unit
) {
    val file = runCatching {
        val outputDirectory = File(context.cacheDir, "captures")
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw IOException("Temporary photo storage could not be created.")
        }
        File(outputDirectory, "capture-${System.currentTimeMillis()}.jpg")
    }.getOrElse { error ->
        onResult(Result.failure(error))
        return
    }
    val options = ImageCapture.OutputFileOptions.Builder(file).build()
    runCatching {
        imageCapture.takePicture(
            options,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val result = runCatching { file.readImageBytes() }
                    file.delete()
                    onResult(result)
                }

                override fun onError(exception: ImageCaptureException) {
                    file.delete()
                    onResult(Result.failure(IOException("The photo could not be captured. Please try again.", exception)))
                }
            }
        )
    }.onFailure { error ->
        file.delete()
        onResult(Result.failure(error))
    }
}

private fun Context.readBytesFromUri(uri: Uri): ByteArray {
    val stream = contentResolver.openInputStream(uri)
        ?: throw IOException("The selected photo could not be opened.")
    return stream.use { input ->
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > MaxInputBytes) {
                throw IOException("The selected photo is larger than 20 MB.")
            }
            output.write(buffer, 0, count)
        }
        output.toByteArray().also { bytes ->
            if (bytes.isEmpty()) throw IOException("The selected photo is empty.")
        }
    }
}

private fun File.readImageBytes(): ByteArray {
    if (length() > MaxInputBytes) throw IOException("The captured photo is larger than 20 MB.")
    return readBytes().also { bytes ->
        if (bytes.isEmpty()) throw IOException("The captured photo is empty.")
    }
}

private const val MaxInputBytes = 20 * 1024 * 1024

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
