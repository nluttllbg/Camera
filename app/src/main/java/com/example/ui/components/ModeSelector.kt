package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.CameraMode
import com.example.ui.theme.IosCameraYellow

@Composable
fun ModeSelector(
    currentMode: CameraMode,
    onModeSelected: (CameraMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(
        CameraMode.CINEMATIC,
        CameraMode.PHOTO,
        CameraMode.PORTRAIT,
        CameraMode.NIGHT,
        CameraMode.VIDEO
    )

    val listState = rememberLazyListState()

    LaunchedEffect(currentMode) {
        val index = modes.indexOf(currentMode)
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .testTag("mode_selector_carousel"),
        contentPadding = PaddingValues(horizontal = 64.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(modes, key = { it.name }) { mode ->
            val isSelected = mode == currentMode

            val textColor by animateColorAsState(
                targetValue = if (isSelected) IosCameraYellow else Color(0xCCFFFFFF),
                animationSpec = tween(150),
                label = "modeTextColor"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onModeSelected(mode)
                    }
                    .padding(vertical = 4.dp, horizontal = 4.dp)
                    .testTag("mode_item_${mode.name}")
            ) {
                Text(
                    text = stringResource(id = mode.titleRes),
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .padding(0.dp)
                    )
                }
            }
        }
    }
}
