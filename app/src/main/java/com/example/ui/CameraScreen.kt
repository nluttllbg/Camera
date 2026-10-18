package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.camera.CameraMode
import com.example.camera.CameraViewModel
import com.example.ui.components.CameraPreviewView
import com.example.ui.components.CinematicViewerDialog
import com.example.ui.components.ModeSelector
import com.example.ui.components.ShutterControls
import com.example.ui.components.TopControlsBar
import com.example.ui.components.ZoomSelector
import com.example.ui.theme.IosCameraBlack
import com.example.ui.theme.IosCameraYellow
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Shutter flash animation
    val flashOpacity = remember { Animatable(0f) }

    // Modern Android Photo Picker (zero broad storage permission required)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.importPhotoUri(context, uri)
        }
    }

    // Camera runtime permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setCameraPermissionGranted(isGranted)
    }

    LaunchedEffect(Unit) {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            viewModel.setCameraPermissionGranted(true)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun triggerShutter() {
        // Haptic feedback
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

        // Shutter blink flash
        scope.launch {
            flashOpacity.snapTo(0.85f)
            flashOpacity.animateTo(0f, tween(180))
        }

        viewModel.capturePhoto(context)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(IosCameraBlack)
            .testTag("camera_main_screen")
    ) {
        // 1. Fullscreen Viewfinder (Live Camera or Realistic Lens Simulator)
        CameraPreviewView(
            currentMode = state.currentMode,
            aspectRatio = state.aspectRatio,
            zoomRatio = state.zoomRatio,
            exposureEv = state.exposureEv,
            lensFacing = state.lensFacing,
            selectedFilter = state.selectedFilter,
            fStop = state.fStop,
            focusState = state.focusState,
            onTapFocus = { offset ->
                viewModel.setFocusPoint(offset)
            },
            onExposureChanged = { ev ->
                viewModel.setExposure(ev)
            },
            hasCameraPermission = state.hasCameraPermission,
            onImageCaptureReady = { capture ->
                viewModel.setImageCapture(capture)
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Shutter Flash Effect
        if (flashOpacity.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flashOpacity.value))
            )
        }

        // 3. UI Overlay Controls
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Controls Bar (Flash, HDR, Aspect Ratio, f-stop badge, Quick Drawer)
            TopControlsBar(
                currentMode = state.currentMode,
                flashMode = state.flashMode,
                hdrEnabled = state.hdrEnabled,
                aspectRatio = state.aspectRatio,
                fStop = state.fStop,
                exposureEv = state.exposureEv,
                selectedFilter = state.selectedFilter,
                onFlashToggle = { viewModel.toggleFlash() },
                onHdrToggle = { viewModel.toggleHdr() },
                onAspectRatioToggle = { viewModel.toggleAspectRatio() },
                onFStopClick = {
                    // Open viewer or adjust fStop
                    val nextF = when (state.fStop) {
                        1.4f -> 1.8f
                        1.8f -> 2.8f
                        2.8f -> 4.0f
                        4.0f -> 8.0f
                        else -> 1.4f
                    }
                    viewModel.setFStop(nextF)
                },
                onExposureChanged = { viewModel.setExposure(it) },
                onFilterSelected = { viewModel.setFilter(it) }
            )

            // Cinematic Mode Active Banner
            if (state.currentMode == CameraMode.CINEMATIC) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "MODO CINEMÁTICO • BOKEH PROFUNDO • 2.39:1",
                        color = IosCameraYellow,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Floating Zoom Selector (.5, 1x, 2, 3) placed smoothly over the image preview
            ZoomSelector(
                currentZoom = state.zoomRatio,
                onZoomSelected = { zoom ->
                    viewModel.setZoom(zoom)
                },
                modifier = Modifier.padding(bottom = 14.dp)
            )

            // Bottom Mode Slider (CINEMÁTICO, FOTO, RETRATO, NOITE, VÍDEO)
            ModeSelector(
                currentMode = state.currentMode,
                onModeSelected = { mode ->
                    viewModel.setMode(mode)
                },
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Bottom Shutter Bar (Gallery Thumbnail, Big Round White Shutter Button, Flip Camera)
            ShutterControls(
                currentMode = state.currentMode,
                latestPhoto = state.latestShot?.processedBitmap,
                onShutterClick = { triggerShutter() },
                onGalleryClick = {
                    // Launch photo picker to import, or if latest shot exists, open viewer
                    if (state.latestShot != null) {
                        viewModel.openViewerForLatest()
                    } else {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                },
                onFlipCameraClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    viewModel.toggleLensFacing()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
        }

        // 4. Cinematic Result Viewer / Inspector Dialog
        if (state.showViewerDialog && state.currentViewerShot != null) {
            CinematicViewerDialog(
                shot = state.currentViewerShot!!,
                isProcessingAi = state.isProcessingAi,
                processingMessage = state.processingMessage,
                onDismiss = { viewModel.dismissViewer() },
                onReprocessWithGemini = { shot ->
                    viewModel.reprocessWithGemini(shot)
                },
                onFilterChanged = { filter, fStop ->
                    viewModel.setFilter(filter)
                    viewModel.setFStop(fStop)
                }
            )
        }
    }
}
