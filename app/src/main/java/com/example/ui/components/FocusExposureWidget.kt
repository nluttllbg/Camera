package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.FocusExposureState
import com.example.ui.theme.IosCameraYellow
import kotlin.math.roundToInt

@Composable
fun FocusExposureWidget(
    state: FocusExposureState,
    onExposureChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible) return

    val scale = remember { Animatable(1.3f) }
    LaunchedEffect(state.position) {
        scale.snapTo(1.35f)
        scale.animateTo(1.0f, tween(250, easing = FastOutSlowInEasing))
    }

    val density = LocalDensity.current
    val boxSizeDp = 76.dp
    val boxSizePx = with(density) { boxSizeDp.toPx() }

    val leftPx = state.position.x - boxSizePx / 2f
    val topPx = state.position.y - boxSizePx / 2f

    Box(
        modifier = modifier
            .offset { IntOffset(leftPx.roundToInt(), topPx.roundToInt()) }
            .size(boxSizeDp + 48.dp, boxSizeDp)
            .testTag("focus_exposure_widget")
    ) {
        // iOS Focus Square with corner brackets
        Canvas(
            modifier = Modifier.size(boxSizeDp)
        ) {
            val strokeWidth = 1.6.dp.toPx()
            val bracketLength = 14.dp.toPx()
            val yellow = IosCameraYellow
            val w = size.width * scale.value
            val h = size.height * scale.value
            val offsetX = (size.width - w) / 2f
            val offsetY = (size.height - h) / 2f

            // Top-Left corner
            drawLine(yellow, Offset(offsetX, offsetY), Offset(offsetX + bracketLength, offsetY), strokeWidth)
            drawLine(yellow, Offset(offsetX, offsetY), Offset(offsetX, offsetY + bracketLength), strokeWidth)

            // Top-Right corner
            drawLine(yellow, Offset(offsetX + w, offsetY), Offset(offsetX + w - bracketLength, offsetY), strokeWidth)
            drawLine(yellow, Offset(offsetX + w, offsetY), Offset(offsetX + w, offsetY + bracketLength), strokeWidth)

            // Bottom-Left corner
            drawLine(yellow, Offset(offsetX, offsetY + h), Offset(offsetX + bracketLength, offsetY + h), strokeWidth)
            drawLine(yellow, Offset(offsetX, offsetY + h), Offset(offsetX, offsetY + h - bracketLength), strokeWidth)

            // Bottom-Right corner
            drawLine(yellow, Offset(offsetX + w, offsetY + h), Offset(offsetX + w - bracketLength, offsetY + h), strokeWidth)
            drawLine(yellow, Offset(offsetX + w, offsetY + h), Offset(offsetX + w, offsetY + h - bracketLength), strokeWidth)
        }

        // Draggable iOS Sun Icon next to the focus box
        val sliderHeightPx = with(density) { 76.dp.toPx() }
        // exposureBias goes from -2f to +2f -> map to -sliderHeightPx/2 .. +sliderHeightPx/2
        val sunOffsetYDp = (-state.exposureBias / 2.0f * 32f).dp

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(36.dp, 76.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        // dragging up (negative dragAmount) increases EV
                        val deltaEv = -dragAmount / 50f
                        val newEv = (state.exposureBias + deltaEv).coerceIn(-2.0f, 2.0f)
                        onExposureChanged(newEv)
                    }
                }
                .testTag("exposure_sun_slider"),
            contentAlignment = Alignment.Center
        ) {
            // Subtle vertical tracking guide line
            Canvas(modifier = Modifier.size(2.dp, 60.dp)) {
                drawLine(
                    color = Color(0x66FFCC00),
                    start = Offset(size.width / 2, 0f),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Glowing iOS Sun symbol
            Canvas(
                modifier = Modifier
                    .offset(y = sunOffsetYDp)
                    .size(22.dp)
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val sunRadius = 4.5.dp.toPx()
                val rayLength = 3.5.dp.toPx()
                val rayStart = sunRadius + 2.dp.toPx()

                // Sun Core
                drawCircle(color = IosCameraYellow, radius = sunRadius, center = center)

                // 8 Sun Rays
                for (i in 0 until 8) {
                    val angle = (i * 45) * (Math.PI / 180.0)
                    val startX = center.x + (rayStart * Math.cos(angle)).toFloat()
                    val startY = center.y + (rayStart * Math.sin(angle)).toFloat()
                    val endX = center.x + ((rayStart + rayLength) * Math.cos(angle)).toFloat()
                    val endY = center.y + ((rayStart + rayLength) * Math.sin(angle)).toFloat()
                    drawLine(IosCameraYellow, Offset(startX, startY), Offset(endX, endY), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                }
            }
        }
    }
}
