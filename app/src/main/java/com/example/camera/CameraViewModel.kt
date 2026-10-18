package com.example.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.ai.CinematicProcessor
import com.example.ai.GeminiCinematicClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.nio.ByteBuffer

data class CameraUiState(
    val currentMode: CameraMode = CameraMode.CINEMATIC,
    val flashMode: FlashMode = FlashMode.OFF,
    val hdrEnabled: Boolean = true,
    val aspectRatio: AspectRatioSetting = AspectRatioSetting.RATIO_CINEMA,
    val zoomRatio: Float = 1.0f,
    val exposureEv: Float = 0.0f,
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val focusState: FocusExposureState = FocusExposureState(),
    val fStop: Float = 1.8f,
    val selectedFilter: CinemaFilter = CinemaFilter.TEAL_ORANGE,
    val isProcessingAi: Boolean = false,
    val processingMessage: String = "",
    val latestShot: ProcessedShot? = null,
    val currentViewerShot: ProcessedShot? = null,
    val showViewerDialog: Boolean = false,
    val hasCameraPermission: Boolean = false,
    val showPermissionNotice: Boolean = false
)

class CameraViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var activeImageCapture: ImageCapture? = null

    fun setImageCapture(capture: ImageCapture) {
        activeImageCapture = capture
    }

    fun setCameraPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(hasCameraPermission = granted) }
    }

    fun setMode(mode: CameraMode) {
        _uiState.update {
            it.copy(
                currentMode = mode,
                aspectRatio = if (mode == CameraMode.CINEMATIC) AspectRatioSetting.RATIO_CINEMA else it.aspectRatio
            )
        }
    }

    fun setZoom(zoom: Float) {
        _uiState.update { it.copy(zoomRatio = zoom) }
    }

    fun setExposure(ev: Float) {
        _uiState.update {
            it.copy(
                exposureEv = ev,
                focusState = it.focusState.copy(exposureBias = ev)
            )
        }
    }

    fun setFocusPoint(position: Offset) {
        _uiState.update {
            it.copy(
                focusState = FocusExposureState(
                    position = position,
                    isVisible = true,
                    exposureBias = it.exposureEv
                )
            )
        }
    }

    fun toggleLensFacing() {
        _uiState.update {
            it.copy(
                lensFacing = if (it.lensFacing == CameraSelector.LENS_FACING_BACK) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
            )
        }
    }

    fun toggleFlash() {
        _uiState.update {
            val next = when (it.flashMode) {
                FlashMode.OFF -> FlashMode.AUTO
                FlashMode.AUTO -> FlashMode.ON
                FlashMode.ON -> FlashMode.OFF
            }
            it.copy(flashMode = next)
        }
    }

    fun toggleHdr() {
        _uiState.update { it.copy(hdrEnabled = !it.hdrEnabled) }
    }

    fun toggleAspectRatio() {
        _uiState.update {
            val next = when (it.aspectRatio) {
                AspectRatioSetting.RATIO_4_3 -> AspectRatioSetting.RATIO_16_9
                AspectRatioSetting.RATIO_16_9 -> AspectRatioSetting.RATIO_CINEMA
                AspectRatioSetting.RATIO_CINEMA -> AspectRatioSetting.RATIO_4_3
            }
            it.copy(aspectRatio = next)
        }
    }

    fun setFStop(f: Float) {
        _uiState.update { it.copy(fStop = f) }
        val currentShot = _uiState.value.currentViewerShot
        if (currentShot != null) {
            recalculateLocalCinematic(currentShot, _uiState.value.selectedFilter, f)
        }
    }

    fun setFilter(filter: CinemaFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
        val currentShot = _uiState.value.currentViewerShot
        if (currentShot != null) {
            recalculateLocalCinematic(currentShot, filter, _uiState.value.fStop)
        }
    }

    fun dismissViewer() {
        _uiState.update { it.copy(showViewerDialog = false) }
    }

    fun openViewerForLatest() {
        val shot = _uiState.value.latestShot
        if (shot != null) {
            _uiState.update { it.copy(currentViewerShot = shot, showViewerDialog = true) }
        }
    }

    /**
     * Captures a photo via CameraX or loads sample frame if camera hardware is not available.
     */
    fun capturePhoto(context: Context) {
        val capture = activeImageCapture
        val state = _uiState.value

        if (capture != null && state.hasCameraPermission) {
            capture.takePicture(
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val bitmap = imageProxyToBitmap(image)
                        image.close()
                        processCapturedBitmap(bitmap)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        // Fallback to sample photo if emulator capture failed
                        loadSampleAndProcess(context)
                    }
                }
            )
        } else {
            // Emulated live frame capture
            loadSampleAndProcess(context)
        }
    }

    /**
     * Imports a photo selected from the Android gallery
     */
    fun importPhotoUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }

                withContext(Dispatchers.Main) {
                    processCapturedBitmap(bitmap)
                }
            } catch (e: Exception) {
                // Ignore or handle
            }
        }
    }

    private fun loadSampleAndProcess(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val sample = BitmapFactory.decodeResource(context.resources, R.drawable.sample_cinema_shot)
            withContext(Dispatchers.Main) {
                processCapturedBitmap(sample)
            }
        }
    }

    private fun processCapturedBitmap(rawBitmap: Bitmap) {
        val state = _uiState.value

        viewModelScope.launch(Dispatchers.Default) {
            val processed = CinematicProcessor.processCinematic(
                source = rawBitmap,
                filter = state.selectedFilter,
                fStop = state.fStop,
                exposureEv = state.exposureEv,
                aspectRatioSetting = if (state.currentMode == CameraMode.CINEMATIC) AspectRatioSetting.RATIO_CINEMA else state.aspectRatio,
                addCinemaStamp = true
            )

            val newShot = ProcessedShot(
                originalBitmap = rawBitmap,
                processedBitmap = processed,
                mode = state.currentMode,
                filter = state.selectedFilter,
                fStop = "f/${state.fStop}",
                isAiProcessed = false,
                aiDescription = "Modo Cinemático 2.39:1 • f/${state.fStop} • ${state.selectedFilter.displayName}",
                aspectRatio = if (state.currentMode == CameraMode.CINEMATIC) AspectRatioSetting.RATIO_CINEMA else state.aspectRatio
            )

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        latestShot = newShot,
                        currentViewerShot = newShot,
                        showViewerDialog = true
                    )
                }
            }
        }
    }

    fun reprocessWithGemini(shot: ProcessedShot) {
        _uiState.update {
            it.copy(
                isProcessingAi = true,
                processingMessage = "Processando com Gemini IA Cinemática..."
            )
        }

        viewModelScope.launch {
            val state = _uiState.value
            val result = GeminiCinematicClient.enhancePhotoCinematic(
                source = shot.originalBitmap,
                filter = state.selectedFilter,
                fStop = state.fStop,
                exposureEv = state.exposureEv
            )

            val updatedShot = shot.copy(
                processedBitmap = result.resultBitmap,
                isAiProcessed = result.isFromAi,
                aiDescription = result.aiDescription
            )

            _uiState.update {
                it.copy(
                    isProcessingAi = false,
                    processingMessage = "",
                    latestShot = updatedShot,
                    currentViewerShot = updatedShot
                )
            }
        }
    }

    private fun recalculateLocalCinematic(shot: ProcessedShot, filter: CinemaFilter, fStop: Float) {
        viewModelScope.launch(Dispatchers.Default) {
            val state = _uiState.value
            val reprocessed = CinematicProcessor.processCinematic(
                source = shot.originalBitmap,
                filter = filter,
                fStop = fStop,
                exposureEv = state.exposureEv,
                aspectRatioSetting = shot.aspectRatio,
                addCinemaStamp = true
            )

            val updated = shot.copy(
                processedBitmap = reprocessed,
                filter = filter,
                fStop = "f/$fStop",
                aiDescription = "Abertura f/$fStop • ${filter.displayName} • Widescreen 2.39:1"
            )

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        currentViewerShot = updated,
                        latestShot = updated
                    )
                }
            }
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}
