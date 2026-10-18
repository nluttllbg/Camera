package com.example.camera

import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import com.example.R

enum class CameraMode(@get:StringRes val titleRes: Int, val idName: String) {
    CINEMATIC(R.string.mode_cinematic, "CINEMÁTICO"),
    PHOTO(R.string.mode_photo, "FOTO"),
    PORTRAIT(R.string.mode_portrait, "RETRATO"),
    NIGHT(R.string.mode_night, "NOITE"),
    VIDEO(R.string.mode_video, "VÍDEO")
}

enum class FlashMode {
    OFF,
    AUTO,
    ON
}

enum class AspectRatioSetting(val label: String, val ratio: Float) {
    RATIO_4_3("4:3", 4f / 3f),
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_CINEMA("2.39:1", 2.39f)
}

enum class CinemaFilter(
    val displayName: String,
    val description: String,
    val warmTint: Float,
    val coolTint: Float,
    val contrast: Float,
    val saturation: Float
) {
    TEAL_ORANGE("Teal & Orange", "Clássico blockbuster com sombras ciano e realces quentes", 0.3f, 0.4f, 1.25f, 1.2f),
    GOLDEN_HOUR("Golden Hour", "Iluminação suave, dourada e quente de fim de tarde", 0.6f, 0.0f, 1.15f, 1.3f),
    FILM_NOIR("Film Noir", "Preto e branco dramático de alto contraste com sombras profundas", 0.0f, 0.0f, 1.45f, 0.0f),
    PANAVISION("Panavision 35mm", "Textura orgânica de película com tons naturais equilibrados", 0.15f, 0.1f, 1.10f, 1.05f),
    CYBERPUNK("Cyber Neon", "Grading noturno com realces magenta e tons escuros intensos", 0.35f, 0.5f, 1.35f, 1.4f)
}

data class FocusExposureState(
    val position: Offset = Offset.Zero,
    val isVisible: Boolean = false,
    val exposureBias: Float = 0.0f, // -2.0 to +2.0 EV
    val isLocked: Boolean = false
)

data class ProcessedShot(
    val id: String = java.util.UUID.randomUUID().toString(),
    val originalBitmap: Bitmap,
    val processedBitmap: Bitmap,
    val mode: CameraMode,
    val filter: CinemaFilter,
    val fStop: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isAiProcessed: Boolean = false,
    val aiDescription: String = "",
    val aspectRatio: AspectRatioSetting = AspectRatioSetting.RATIO_CINEMA
)
