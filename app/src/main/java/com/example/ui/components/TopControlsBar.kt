package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.AspectRatioSetting
import com.example.camera.CameraMode
import com.example.camera.CinemaFilter
import com.example.camera.FlashMode
import com.example.ui.theme.IosCameraYellow

@Composable
fun TopControlsBar(
    currentMode: CameraMode,
    flashMode: FlashMode,
    hdrEnabled: Boolean,
    aspectRatio: AspectRatioSetting,
    fStop: Float,
    exposureEv: Float,
    selectedFilter: CinemaFilter,
    onFlashToggle: () -> Unit,
    onHdrToggle: () -> Unit,
    onAspectRatioToggle: () -> Unit,
    onFStopClick: () -> Unit,
    onExposureChanged: (Float) -> Unit,
    onFilterSelected: (CinemaFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDrawerExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x99000000))
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("top_controls_bar")
    ) {
        // Main Top Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onFlashToggle() }
                    .testTag("flash_toggle_button"),
                contentAlignment = Alignment.Center
            ) {
                when (flashMode) {
                    FlashMode.OFF -> Icon(
                        imageVector = Icons.Default.FlashOff,
                        contentDescription = "Flash Desativado",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    FlashMode.AUTO -> Icon(
                        imageVector = Icons.Default.FlashAuto,
                        contentDescription = "Flash Automático",
                        tint = IosCameraYellow,
                        modifier = Modifier.size(22.dp)
                    )
                    FlashMode.ON -> Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Flash Ativado",
                        tint = IosCameraYellow,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Center Chevron drawer expander / Night mode indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentMode == CameraMode.NIGHT) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(IosCameraYellow)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Nightlight,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = " 2s",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Drawer Chevron
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .clickable { isDrawerExpanded = !isDrawerExpanded }
                        .testTag("drawer_chevron_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDrawerExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Menu de Ajustes Rápidos",
                        tint = if (isDrawerExpanded) IosCameraYellow else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Right Controls: HDR, Aspect Ratio & f-stop
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // HDR Toggle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (hdrEnabled) Color(0x33FFCC00) else Color.Transparent)
                        .clickable { onHdrToggle() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("hdr_toggle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "HDR",
                        color = if (hdrEnabled) IosCameraYellow else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Aspect Ratio Selector
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x33FFFFFF))
                        .clickable { onAspectRatioToggle() }
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                        .testTag("aspect_ratio_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = aspectRatio.label,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // In CINEMATIC mode: Aperture f-stop Badge (iOS style yellow badge)
                if (currentMode == CameraMode.CINEMATIC) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(IosCameraYellow)
                            .clickable { onFStopClick() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("cinematic_fstop_badge"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "f/${"%.1f".format(fStop)}",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // Expandable Quick Settings Drawer (Exposure slider + Cinema Filters)
        AnimatedVisibility(
            visible = isDrawerExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .background(Color(0x991C1C1E), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                // Exposure EV slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Exposição: ${if (exposureEv > 0) "+" else ""}${"%.1f".format(exposureEv)} EV",
                        color = IosCameraYellow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Brilho Manual",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }

                Slider(
                    value = exposureEv,
                    onValueChange = onExposureChanged,
                    valueRange = -2.0f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = IosCameraYellow,
                        activeTrackColor = IosCameraYellow,
                        inactiveTrackColor = Color(0x44FFFFFF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Cinema Filters quick row
                Text(
                    text = "Lente & Estilo Cinematográfico:",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CinemaFilter.values().forEach { filter ->
                        val isFilterSelected = filter == selectedFilter
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isFilterSelected) IosCameraYellow else Color(0x33FFFFFF))
                                .clickable { onFilterSelected(filter) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filter.displayName.split(" ").first(),
                                color = if (isFilterSelected) Color.Black else Color.White,
                                fontSize = 10.sp,
                                fontWeight = if (isFilterSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
