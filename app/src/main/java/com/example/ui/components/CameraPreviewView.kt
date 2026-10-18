package com.example.ui.components

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.R
import com.example.camera.AspectRatioSetting
import com.example.camera.CameraMode
import com.example.camera.CinemaFilter
import com.example.camera.FocusExposureState
import com.example.ui.theme.IosCameraYellow

@Composable
fun CameraPreviewView(
    currentMode: CameraMode,
    aspectRatio: AspectRatioSetting,
    zoomRatio: Float,
    exposureEv: Float,
    lensFacing: Int,
    selectedFilter: CinemaFilter,
    fStop: Float,
    focusState: FocusExposureState,
    onTapFocus: (Offset) -> Unit,
    onExposureChanged: (Float) -> Unit,
    hasCameraPermission: Boolean,
    onImageCaptureReady: (ImageCapture) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    var cameraInitFailed by remember { mutableStateOf(false) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }

    val animatedZoom by animateFloatAsState(
        targetValue = zoomRatio,
        animationSpec = tween(250),
        label = "zoomAnim"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onTapFocus(offset)
                }
            }
            .testTag("camera_preview_container")
    ) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight

        // 1. Live Camera or Fallback Scene with Zoom scale
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(animatedZoom),
            contentAlignment = Alignment.Center
        ) {
            if (hasCameraPermission && !cameraInitFailed) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            previewView = this
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                try {
                                    val provider = cameraProviderFuture.get()
                                    cameraProvider = provider

                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(surfaceProvider)
                                    }

                                    val imageCapture = ImageCapture.Builder()
                                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                        .build()

                                    onImageCaptureReady(imageCapture)

                                    val cameraSelector = CameraSelector.Builder()
                                        .requireLensFacing(lensFacing)
                                        .build()

                                    provider.unbindAll()
                                    provider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageCapture
                                    )
                                } catch (e: Exception) {
                                    cameraInitFailed = true
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Realistic Camera Lens Simulation with Live Scene
                Image(
                    painter = painterResource(id = R.drawable.sample_cinema_shot),
                    contentDescription = "Câmera Live Feed",
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (currentMode == CameraMode.CINEMATIC && fStop <= 2.8f) {
                                Modifier.blur(1.dp)
                            } else Modifier
                        ),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // 2. Real-time Cinematic Color Filter Tint & Lighting
        if (currentMode == CameraMode.CINEMATIC) {
            // Apply live tint overlay corresponding to cinema filter
            val filterColor = when (selectedFilter) {
                CinemaFilter.TEAL_ORANGE -> Color(0x22008899)
                CinemaFilter.GOLDEN_HOUR -> Color(0x25FFAA00)
                CinemaFilter.FILM_NOIR -> Color(0x35000000)
                CinemaFilter.CYBERPUNK -> Color(0x28CC0077)
                CinemaFilter.PANAVISION -> Color(0x18FFBB44)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(filterColor)
            )

            // Live Depth of Field Vignette / Bokeh perimeter blur
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.maxDimension * 0.7f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x33000000),
                            Color(0xBB000000)
                        ),
                        center = focusState.position.takeIf { it != Offset.Zero } ?: Offset(size.width / 2, size.height / 2),
                        radius = radius
                    )
                )
            }
        }

        // 3. Exposure compensation live brightness shade
        if (exposureEv != 0.0f) {
            val evColor = if (exposureEv > 0) {
                Color.White.copy(alpha = (exposureEv / 2f * 0.22f).coerceIn(0f, 0.35f))
            } else {
                Color.Black.copy(alpha = (-exposureEv / 2f * 0.45f).coerceIn(0f, 0.6f))
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(evColor)
            )
        }

        // 4. Rule-of-Thirds Grid (iOS subtle camera grid)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val gridColor = Color(0x22FFFFFF)

            // Vertical lines
            drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = 1f)
            drawLine(gridColor, Offset(2 * w / 3f, 0f), Offset(2 * w / 3f, h), strokeWidth = 1f)

            // Horizontal lines
            drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = 1f)
            drawLine(gridColor, Offset(0f, 2 * h / 3f), Offset(w, 2 * h / 3f), strokeWidth = 1f)
        }

        // 5. Cinematic Widescreen Letterbox Bars (2.39:1 Cinema Matte)
        if (aspectRatio == AspectRatioSetting.RATIO_CINEMA || currentMode == CameraMode.CINEMATIC) {
            val targetRatio = 2.39f
            val currentRatio = totalWidth / totalHeight
            val barHeight = if (currentRatio < targetRatio) {
                val visibleHeight = totalWidth / targetRatio
                ((totalHeight - visibleHeight) / 2).coerceAtLeast(0.dp)
            } else 0.dp

            if (barHeight > 0.dp) {
                // Top Letterbox Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .align(Alignment.TopCenter)
                        .background(Color.Black)
                ) {
                    Text(
                        text = "CINEMÁTICO 2.39:1",
                        color = Color(0x88FFFFFF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                    )
                }

                // Bottom Letterbox Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .align(Alignment.BottomCenter)
                        .background(Color.Black)
                )
            }
        }

        // 6. Focus & Exposure Interactive Sun Widget
        FocusExposureWidget(
            state = focusState,
            onExposureChanged = onExposureChanged
        )
    }
}
