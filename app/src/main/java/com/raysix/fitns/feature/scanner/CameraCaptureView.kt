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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
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
import com.raysix.fitns.R

enum class CameraCaptureGuide { Meal, NutritionLabel }

@Composable
fun CameraCaptureView(
    onImageBytes: (ByteArray) -> Unit,
    modifier: Modifier = Modifier,
    captureButtonLabel: String = "Capture",
    guide: CameraCaptureGuide = CameraCaptureGuide.Meal,
    onCancel: (() -> Unit)? = null,
    fullScreen: Boolean = false,
    externalErrorMessage: String? = null
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

    val requestCameraPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) }
    if (fullScreen) {
        FullScreenCameraCapture(
            previewView = previewView,
            hasCameraPermission = hasCameraPermission,
            cameraReady = cameraReady,
            cameraStartFailed = cameraStartFailed,
            isProcessing = isProcessing,
            errorMessage = errorMessage ?: externalErrorMessage,
            captureButtonLabel = captureButtonLabel,
            guide = guide,
            onOpenGallery = openGallery,
            onRequestCameraPermission = requestCameraPermission,
            onRetryCamera = retryCamera,
            onCapture = takePhoto,
            onCancel = onCancel,
            modifier = modifier.fillMaxSize()
        )
    } else {
        val configuration = LocalConfiguration.current
        BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
            val compactHeight = maxHeight < 520.dp || configuration.screenHeightDp < 520
            // The camera is hosted in a 68% pane on wide capture screens. At 800 x 360
            // that pane is just under 500 dp wide, so a 520 dp threshold placed both
            // actions below the preview and outside the initial viewport.
            val useSideBySideLayout = compactHeight && maxWidth >= 420.dp
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
                        guide = guide,
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
                        onRequestCameraPermission = requestCameraPermission,
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
                        guide = guide,
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
                        onRequestCameraPermission = requestCameraPermission,
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
}

@Composable
private fun CameraPreview(
    previewView: PreviewView,
    hasCameraPermission: Boolean,
    cameraReady: Boolean,
    cameraStartFailed: Boolean,
    guide: CameraCaptureGuide,
    shape: Shape = RoundedCornerShape(28.dp),
    showGuide: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF111315)),
        contentAlignment = Alignment.Center
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxSize()
                    .clearAndSetSemantics {
                        contentDescription = if (guide == CameraCaptureGuide.NutritionLabel) {
                            "Live camera preview. Keep the nutrition table inside the guide."
                        } else {
                            "Live camera preview. Frame the whole plate inside the guide."
                        }
                    }
            )
            if (cameraReady && showGuide) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (guide == CameraCaptureGuide.NutritionLabel) 0.58f else 0.84f)
                        .fillMaxHeight(if (guide == CameraCaptureGuide.NutritionLabel) 0.84f else 0.72f)
                        .border(
                            2.dp,
                            Color.White.copy(alpha = 0.82f),
                            RoundedCornerShape(if (guide == CameraCaptureGuide.NutritionLabel) 16.dp else 28.dp)
                        )
                )
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = Color.Black.copy(alpha = 0.58f),
                    contentColor = Color.White
                ) {
                    Text(
                        text = if (guide == CameraCaptureGuide.NutritionLabel) "KEEP THE TABLE INSIDE" else "FRAME THE WHOLE PLATE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                    )
                }
            }
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
private fun FullScreenCameraCapture(
    previewView: PreviewView,
    hasCameraPermission: Boolean,
    cameraReady: Boolean,
    cameraStartFailed: Boolean,
    isProcessing: Boolean,
    errorMessage: String?,
    captureButtonLabel: String,
    guide: CameraCaptureGuide,
    onOpenGallery: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onRetryCamera: () -> Unit,
    onCapture: () -> Unit,
    onCancel: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val landscape = maxWidth > maxHeight
        val narrowLandscape = landscape && maxWidth < 700.dp
        CameraPreview(
            previewView = previewView,
            hasCameraPermission = hasCameraPermission,
            cameraReady = cameraReady,
            cameraStartFailed = cameraStartFailed,
            guide = guide,
            shape = RectangleShape,
            showGuide = false,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.64f),
                            0.2f to Color.Transparent,
                            0.58f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.82f)
                        )
                    )
                )
        )

        if (cameraReady) {
            val labelGuide = guide == CameraCaptureGuide.NutritionLabel
            Box(
                modifier = if (landscape) {
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset(y = if (narrowLandscape) 20.dp else 10.dp)
                        .padding(start = if (narrowLandscape) 16.dp else 24.dp)
                        .fillMaxWidth(if (labelGuide && narrowLandscape) 0.48f else if (labelGuide) 0.54f else 0.6f)
                        .fillMaxHeight(if (labelGuide) 0.62f else 0.7f)
                } else {
                    Modifier
                        .align(Alignment.Center)
                        .offset(y = (-34).dp)
                        .fillMaxWidth(if (labelGuide) 0.86f else 0.9f)
                        .fillMaxHeight(if (labelGuide) 0.56f else 0.5f)
                }
                    .border(
                        width = 2.dp,
                        color = Color.White.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(if (labelGuide) 22.dp else 34.dp)
                    )
            ) {
                if (!landscape) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopCenter).offset(y = (-16).dp),
                        shape = RoundedCornerShape(999.dp),
                        color = Color.Black.copy(alpha = 0.68f),
                        contentColor = Color.White
                    ) {
                        Text(
                            text = if (labelGuide) "KEEP THE NUTRITION TABLE INSIDE" else "FRAME THE WHOLE PLATE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        FullScreenCameraHeader(
            guide = guide,
            onCancel = onCancel,
            compact = landscape,
            modifier = Modifier.align(Alignment.TopStart)
        )

        FullScreenCapturePanel(
            hasCameraPermission = hasCameraPermission,
            cameraReady = cameraReady,
            cameraStartFailed = cameraStartFailed,
            isProcessing = isProcessing,
            errorMessage = errorMessage,
            captureButtonLabel = captureButtonLabel,
            guide = guide,
            onOpenGallery = onOpenGallery,
            onRequestCameraPermission = onRequestCameraPermission,
            onRetryCamera = onRetryCamera,
            onCapture = onCapture,
            compact = landscape,
            modifier = if (landscape) {
                Modifier
                    .align(Alignment.CenterEnd)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.End + WindowInsetsSides.Vertical)
                    )
                    .padding(end = 20.dp)
                    .widthIn(
                        min = if (narrowLandscape) 220.dp else 260.dp,
                        max = if (narrowLandscape) 260.dp else 320.dp
                    )
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                    )
                    .padding(16.dp)
                    .fillMaxWidth()
            }
        )
    }
}

@Composable
private fun FullScreenCameraHeader(
    guide: CameraCaptureGuide,
    onCancel: (() -> Unit)?,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        onCancel?.let { close ->
            Surface(
                onClick = close,
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.62f),
                contentColor = Color.White,
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Close, contentDescription = "Close scanner", modifier = Modifier.size(25.dp))
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.62f),
            contentColor = Color.White
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                if (guide == CameraCaptureGuide.NutritionLabel) {
                    Image(
                        painter = painterResource(R.drawable.whale_coach),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Column {
                    Text(
                        text = if (guide == CameraCaptureGuide.NutritionLabel) "Scan food label" else "Scan meal",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (!compact) {
                        Text(
                            text = if (guide == CameraCaptureGuide.NutritionLabel) "Nothing is saved before review" else "Review every detected item",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.76f)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun FullScreenCapturePanel(
    hasCameraPermission: Boolean,
    cameraReady: Boolean,
    cameraStartFailed: Boolean,
    isProcessing: Boolean,
    errorMessage: String?,
    captureButtonLabel: String,
    guide: CameraCaptureGuide,
    onOpenGallery: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onRetryCamera: () -> Unit,
    onCapture: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val actionLabel = cameraActionLabel(
        hasCameraPermission = hasCameraPermission,
        cameraReady = cameraReady,
        cameraStartFailed = cameraStartFailed,
        isProcessing = isProcessing,
        captureButtonLabel = captureButtonLabel
    )
    val action = when {
        !hasCameraPermission -> onRequestCameraPermission
        cameraStartFailed -> onRetryCamera
        else -> onCapture
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = Color.Black.copy(alpha = 0.7f),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 14.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 9.dp else 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.14f),
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = if (guide == CameraCaptureGuide.NutritionLabel) {
                            Icons.Filled.DocumentScanner
                        } else {
                            Icons.Filled.CameraAlt
                        },
                        contentDescription = null,
                        modifier = Modifier.padding(9.dp).size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (guide == CameraCaptureGuide.NutritionLabel) "Fill the frame with the table" else "Frame the whole plate",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (!compact) {
                        Text(
                            text = if (guide == CameraCaptureGuide.NutritionLabel) {
                                "Keep it flat, sharp, and free of glare."
                            } else {
                                "Keep every food clearly visible."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.76f)
                        )
                    }
                }
            }
            errorMessage?.let { message ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF7A2525).copy(alpha = 0.94f),
                    contentColor = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                            .semantics { liveRegion = LiveRegionMode.Assertive },
                        maxLines = 2
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onOpenGallery,
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.16f),
                    contentColor = Color.White,
                    modifier = Modifier.size(62.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.PhotoLibrary,
                            contentDescription = "Choose label photo from gallery",
                            modifier = Modifier.size(27.dp)
                        )
                    }
                }
                Surface(
                    onClick = action,
                    enabled = !isProcessing && (!hasCameraPermission || cameraReady || cameraStartFailed),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f).heightIn(min = 62.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(23.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(24.dp))
                        }
                        Text(
                            text = actionLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(start = 9.dp)
                        )
                    }
                }
            }
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
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }
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
