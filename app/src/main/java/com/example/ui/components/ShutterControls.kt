package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.camera.CameraMode
import com.example.ui.theme.IosCameraCinemaRed
import com.example.ui.theme.IosCameraYellow

@Composable
fun ShutterControls(
    currentMode: CameraMode,
    latestPhoto: Bitmap?,
    onShutterClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onFlipCameraClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 12.dp)
            .testTag("shutter_controls_row"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Gallery Thumbnail Button
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x662C2C2E))
                .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(10.dp))
                .clickable { onGalleryClick() }
                .testTag("gallery_thumbnail_button"),
            contentAlignment = Alignment.Center
        ) {
            if (latestPhoto != null) {
                Image(
                    bitmap = latestPhoto.asImageBitmap(),
                    contentDescription = "Galeria",
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.sample_cinema_shot),
                    contentDescription = "Galeria",
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Center: Large Round White iOS Shutter Button
        val shutterInteraction = remember { MutableInteractionSource() }
        val isPressed by shutterInteraction.collectIsPressedAsState()
        val discScale = remember { Animatable(1f) }

        LaunchedEffect(isPressed) {
            if (isPressed) {
                discScale.animateTo(0.86f, spring(stiffness = Spring.StiffnessHigh))
            } else {
                discScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
        }

        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .border(4.dp, Color.White, CircleShape)
                .clickable(
                    interactionSource = shutterInteraction,
                    indication = null
                ) {
                    onShutterClick()
                }
                .testTag("main_shutter_button"),
            contentAlignment = Alignment.Center
        ) {
            val innerSize = 60.dp
            Box(
                modifier = Modifier
                    .size(innerSize)
                    .scale(discScale.value)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                // In CINEMATIC mode, add red cinema recording / snap dot in the center
                if (currentMode == CameraMode.CINEMATIC) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(IosCameraCinemaRed)
                    )
                } else if (currentMode == CameraMode.NIGHT) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(IosCameraYellow)
                    )
                }
            }
        }

        // Right: Flip Camera Button with rotation animation
        val rotationAnim = remember { Animatable(0f) }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0x662C2C2E))
                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                .clickable {
                    onFlipCameraClick()
                }
                .testTag("flip_camera_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Cameraswitch,
                contentDescription = "Alternar Câmera",
                tint = Color.White,
                modifier = Modifier
                    .size(26.dp)
                    .rotate(rotationAnim.value)
            )
        }
    }
}
