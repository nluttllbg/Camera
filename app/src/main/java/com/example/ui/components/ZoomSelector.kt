package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IosCameraYellow

@Composable
fun ZoomSelector(
    currentZoom: Float,
    onZoomSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val zoomOptions = listOf(
        0.5f to ".5",
        1.0f to "1x",
        2.0f to "2",
        3.0f to "3"
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0x9918181A))
            .padding(horizontal = 4.dp, vertical = 3.dp)
            .testTag("zoom_selector_pill")
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            zoomOptions.forEach { (zoomValue, label) ->
                val isSelected = (currentZoom - zoomValue).let { it >= -0.1f && it <= 0.1f }

                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) Color(0x33FFFFFF) else Color.Transparent,
                    animationSpec = tween(150),
                    label = "zoomBg"
                )

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) IosCameraYellow else Color.White,
                    animationSpec = tween(150),
                    label = "zoomText"
                )

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(bgColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onZoomSelected(zoomValue)
                        }
                        .testTag("zoom_button_${label}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = textColor,
                        fontSize = if (isSelected) 13.sp else 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
