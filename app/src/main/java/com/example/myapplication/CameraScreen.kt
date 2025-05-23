package com.example.myapplication

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val tabs = listOf("Photo", "Clips", "Live")
    var selectedTab by remember { mutableStateOf("Clips") }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }
    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // Segment-wise recording state
    var isRecording by remember { mutableStateOf(false) }
    var isProgressbarShow by remember { mutableStateOf(false) }
    val maxRecordingDuration = 15_000L // max total duration in ms (e.g. 15 seconds)

    var segments by remember { mutableStateOf(listOf<Long>()) } // List of segment durations in ms
    var currentSegmentDuration by remember { mutableLongStateOf(0L) }

    // Request camera permission
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        cameraPermissionGranted = it
    }

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.CAMERA)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
        }, ContextCompat.getMainExecutor(context))
    }

    // Bind camera use cases on relevant state changes
    LaunchedEffect(lensFacing, previewView, cameraProvider, isFlashOn) {
        if (cameraProvider != null && previewView != null) {
            bindCameraUseCases(
                cameraProvider!!,
                previewView!!,
                lensFacing,
                lifecycleOwner,
                isFlashOn
            ) { control -> cameraControl = control }
        }
    }

    // Manage segment timing when recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            currentSegmentDuration = 0L
            // Simulate segment recording with a timer, update duration every 100ms
            while (isRecording  && segments.sum() + currentSegmentDuration < maxRecordingDuration) {
                delay(100)
                currentSegmentDuration += 100
            }
            if (isRecording) {
                // Auto stop segment after max duration
                isRecording = false
                segments = segments + currentSegmentDuration
                currentSegmentDuration = 0L
                Log.d("CameraScreen", "Segment auto-stopped at max duration")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraPermissionGranted) {
            AndroidView(factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }.also { previewView = it }
            }, modifier = Modifier.fillMaxSize())
        }

        // Top controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 10.dp)
                .align(Alignment.TopCenter)
        ) {

            // Top segmented progress bar for recorded segments
            if (selectedTab =="Clips" && isProgressbarShow) {
                SegmentedProgressBar(
                    segments = segments,
                    currentSegmentDuration = currentSegmentDuration,
                    maxRecordingDuration = maxRecordingDuration
                )
            }

            TopBar(
                onFlip = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                        CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                },
                onFlashToggle = {
                    isFlashOn = !isFlashOn
                    cameraControl?.enableTorch(isFlashOn)
                },
                isFlashOn = isFlashOn
            )
        }

        // Right side controls
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SideControl(icon = R.drawable.filter, label = "Filter")
            SideControl(icon = R.drawable.beautify, label = "Beaut.")
            SideControl(icon = R.drawable.timer, label = "Timer")
            SideControl(icon = R.drawable.speed, label = "Speed")
            SideControl(icon = R.drawable.template, label = "Template")
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (selectedTab != "Live") {
                RecordBar(
                    selectedTab = selectedTab,
                    isRecording = isRecording,
                    onRecordClick = {
                        if (isRecording) {
                            // Stop current segment recording
                            isRecording = false
                            segments = segments + currentSegmentDuration
                            currentSegmentDuration = 0L
                            Log.d("CameraScreen", "Recording stopped. Segments: $segments")
                        } else {
                            // Check total duration to prevent exceeding max
                            val totalDuration = segments.sum()
                            if (totalDuration < maxRecordingDuration) {
                                isRecording = true
                                isProgressbarShow=true
                                Log.d("CameraScreen", "Recording started")
                            } else {
                                Log.d("CameraScreen", "Max recording duration reached")
                            }
                        }
                    },
                    onPhotoClick = {
                        Log.d("CameraScreen", "Photo captured")
                        // Add actual photo capture logic here
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                tabs.forEach { tab ->
                    BottomControl(
                        label = tab,
                        highlight = (selectedTab == tab),
                        onClick = { selectedTab = tab }
                    )
                }
            }
        }
    }
}
@Composable
fun SegmentedProgressBar(
    modifier: Modifier = Modifier,
    segments: List<Long>,
    currentSegmentDuration: Long,
    maxRecordingDuration: Long
) {
    if (maxRecordingDuration <= 0L) return // Prevent divide-by-zero

    val totalRecorded = segments.sum() + currentSegmentDuration

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.LightGray.copy(alpha = 0.5f))
    ) {
        // Render each completed segment
        segments.forEach { segmentDuration ->
            val fraction = (segmentDuration.toFloat() / maxRecordingDuration).coerceIn(0.01f, 1f)
            Box(
                modifier = Modifier
                    .weight(fraction)
                    .fillMaxHeight()
                    .padding(end = 1.dp)
                    .background(Color.Red, RoundedCornerShape(4.dp))
            )
        }

        // Render the current in-progress segment

            val currentFraction = (currentSegmentDuration.toFloat() / maxRecordingDuration).coerceIn(0.01f, 1f)
            Box(
                modifier = Modifier
                    .weight(currentFraction)
                    .fillMaxHeight()
                    .padding(end = 1.dp)
                    .background(Color.Red, RoundedCornerShape(4.dp))
            )

        // Fill remaining space with light gray
        val remainingFraction = (1f - (totalRecorded.toFloat() / maxRecordingDuration)).coerceIn(0f, 1f)
        if (remainingFraction > 0f) {
            Box(
                modifier = Modifier
                    .weight(remainingFraction)
                    .fillMaxHeight()
                    .background(Color.LightGray.copy(alpha = 0.5f))
            )
        }
    }
}


@Composable
fun TopBar(onFlip: () -> Unit, onFlashToggle: () -> Unit, isFlashOn: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, start = 10.dp, end = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.3f), shape = RoundedCornerShape(50.dp))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Row(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.3f), shape = RoundedCornerShape(20.dp))
                .padding(horizontal = 15.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.music),
                contentDescription = "music",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Sound", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Row {
            Icon(
                painter = painterResource(id = R.drawable.flip),
                contentDescription = "Flip Camera",
                tint = Color.White,
                modifier = Modifier.size(24.dp).clickable { onFlip() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(id = R.drawable.flash),
                contentDescription = "Flash",
                tint = if (isFlashOn) Color.Yellow else Color.White,
                modifier = Modifier.size(24.dp).clickable { onFlashToggle() }
            )
        }
    }
}

@Composable
fun RecordBar(
    selectedTab: String,
    isRecording: Boolean,
    onRecordClick: () -> Unit,
    onPhotoClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.effect),
                contentDescription = "Effects",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Text("Effects", color = Color.White, fontSize = 12.sp)
        }

        Box(
            modifier = Modifier
                .size(70.dp)
                .clickable {
                    if (selectedTab == "Photo") {
                        onPhotoClick()
                    } else if (selectedTab == "Clips") {
                        onRecordClick()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (selectedTab == "Photo") {
                Icon(
                    painter = painterResource(id = R.drawable.camera_shutter),
                    contentDescription = "Capture Photo",
                    modifier = Modifier.size(70.dp),
                    tint = Color.Unspecified
                )
            } else if (selectedTab == "Clips") {
                if (!isRecording) {
                    Icon(
                        painter = painterResource(id = R.drawable.camera_shutter),
                        contentDescription = "Stop Recording",
                        modifier = Modifier.size(70.dp),
                        tint = Color.Unspecified
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_record),
                        contentDescription = "Start Recording",
                        modifier = Modifier.size(70.dp),
                        tint = Color.Unspecified
                    )
                }
            }
        }


        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.gallery),
                contentDescription = "Gallery",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Text("Gallery", color = Color.White, fontSize = 12.sp)
        }
    }
}


fun bindCameraUseCases(
    cameraProvider: ProcessCameraProvider,
    previewView: PreviewView,
    lensFacing: Int,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    flashOn: Boolean,
    onCameraControlAvailable: (androidx.camera.core.CameraControl) -> Unit
) {
    val preview = androidx.camera.core.Preview.Builder().build().apply {
        setSurfaceProvider(previewView.surfaceProvider)
    }

    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(lensFacing)
        .build()

    try {
        cameraProvider.unbindAll()
        val camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        onCameraControlAvailable(camera.cameraControl)
        camera.cameraControl.enableTorch(flashOn)
    } catch (exc: Exception) {
        Log.e("CameraX", "Use case binding failed", exc)
    }
}

@Composable
fun SideControl(icon: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(24.dp).clickable { }
        )
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun BottomControl(label: String, highlight: Boolean = false, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(horizontal = 18.dp, vertical = 24.dp)
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (highlight) Color.White else Color.Gray,
            fontSize = if (highlight) 18.sp else 16.sp,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal
        )
        if (highlight) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 4.dp)
                    .background(color = Color.White, shape = RoundedCornerShape(4.dp))
            )
        }
    }
}
