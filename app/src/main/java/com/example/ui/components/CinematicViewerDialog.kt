package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.R
import com.example.camera.CinemaFilter
import com.example.camera.ProcessedShot
import com.example.ui.theme.IosCameraBlack
import com.example.ui.theme.IosCameraDarkSurface
import com.example.ui.theme.IosCameraYellow
import java.io.File
import java.io.FileOutputStream

@Composable
fun CinematicViewerDialog(
    shot: ProcessedShot,
    isProcessingAi: Boolean,
    processingMessage: String,
    onDismiss: () -> Unit,
    onReprocessWithGemini: (ProcessedShot) -> Unit,
    onFilterChanged: (CinemaFilter, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showOriginal by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(shot.filter) }
    var currentFStop by remember { mutableFloatStateOf(1.8f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = IosCameraBlack
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .testTag("cinematic_viewer_dialog")
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .testTag("close_viewer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Mode Badge
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x22FFCC00))
                            .border(1.dp, IosCameraYellow, CircleShape)
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "MODO CINEMÁTICO 2.39:1",
                            color = IosCameraYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    // A/B Compare Toggle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (showOriginal) IosCameraYellow else Color(0x33FFFFFF))
                            .clickable { showOriginal = !showOriginal }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("compare_original_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (showOriginal) "Original" else "Cinemático",
                            color = if (showOriginal) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Widescreen 2.39:1 Photo Display with Letterbox
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    val bitmapToDisplay = if (showOriginal) shot.originalBitmap else shot.processedBitmap
                    Image(
                        bitmap = bitmapToDisplay.asImageBitmap(),
                        contentDescription = "Foto Cinemática",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    if (isProcessingAi) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x99000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                CircularProgressIndicator(color = IosCameraYellow)
                                Text(
                                    text = processingMessage.ifBlank { stringResource(R.string.ai_processing) },
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // AI Processing Badge / Info
                if (shot.aiDescription.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x221C1C1E))
                            .border(1.dp, Color(0x33FFCC00), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = IosCameraYellow,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = shot.aiDescription,
                                color = Color.White,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Aperture Depth / Bokeh Slider
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Abertura & Bokeh (Profundidade):",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "f/${"%.1f".format(currentFStop)}",
                            color = IosCameraYellow,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Slider(
                        value = currentFStop,
                        onValueChange = {
                            currentFStop = it
                            onFilterChanged(selectedFilter, currentFStop)
                        },
                        valueRange = 1.4f..16.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = IosCameraYellow,
                            activeTrackColor = IosCameraYellow,
                            inactiveTrackColor = Color(0x44FFFFFF)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("aperture_bokeh_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("f/1.4 (Desfocagem Máxima)", color = Color.Gray, fontSize = 10.sp)
                        Text("f/16 (Tudo Nítido)", color = Color.Gray, fontSize = 10.sp)
                    }
                }

                // Cinema LUT / Filter Picker
                Text(
                    text = "Tom de Cor de Cinema:",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CinemaFilter.values().forEach { filter ->
                        val isSelected = filter == selectedFilter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) IosCameraYellow else Color(0x332C2C2E))
                                .border(
                                    1.dp,
                                    if (isSelected) IosCameraYellow else Color(0x33FFFFFF),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedFilter = filter
                                    onFilterChanged(filter, currentFStop)
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("filter_chip_${filter.name}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filter.displayName,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons: AI Enhance, Save, Share
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Gemini AI Processing Button
                    Button(
                        onClick = { onReprocessWithGemini(shot) },
                        enabled = !isProcessingAi,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("gemini_ai_enhance_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IosCameraYellow,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Processar com Gemini IA Cinemática",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Save Button
                        Button(
                            onClick = {
                                saveBitmapToGallery(context, shot.processedBitmap)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("save_shot_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2C2C2E),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salvar", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Share Button
                        Button(
                            onClick = {
                                shareBitmap(context, shot.processedBitmap)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("share_shot_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2C2C2E),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Compartilhar", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap) {
    try {
        val filename = "CINEMA_${System.currentTimeMillis()}.jpg"
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val stream = FileOutputStream("$cachePath/$filename")
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
        stream.close()
        Toast.makeText(context, "Foto cinemática salva com sucesso!", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Erro ao salvar foto: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun shareBitmap(context: Context, bitmap: Bitmap) {
    try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "share_cinema.jpg")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
        stream.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar Foto Cinemática"))
    } catch (e: Exception) {
        // Fallback plain share
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Confira minha foto capturada no modo Cinemático com iCamera!")
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar"))
    }
}
