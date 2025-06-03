package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CameraScreen(navController: NavHostController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val tabs = listOf("Photo", "Clips", "Live")
    var selectedTab by remember { mutableStateOf("Clips") }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    val segmentFiles = remember { mutableStateListOf<File>() }

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
                isFlashOn,
                onCameraControlAvailable = { cameraControl = it },
                onVideoCaptureReady = { videoCapture = it }
            )
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
                .align(Alignment.TopCenter)
        ) {
            TopBar(
                onFlip = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                        CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                },
                onFlashToggle = {
                    isFlashOn = !isFlashOn
                    cameraControl?.enableTorch(isFlashOn)
                },
                isFlashOn = isFlashOn,
                visible = !isRecording && !isPaused
            )
        }

        // Right side controls
        if (!isRecording && !isPaused) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SideControl(
                    icon = R.drawable.filter,
                    label = "Filter",
                    segmentFiles = segmentFiles,
                    navController = navController,
                )
                SideControl(
                    icon = R.drawable.beautify,
                    label = "Beaut.",
                    segmentFiles = segmentFiles,
                    navController = navController,
                )
                SideControl(icon = R.drawable.timer, label = "Timer", segmentFiles = segmentFiles, navController = navController,)
                SideControl(icon = R.drawable.speed, label = "Speed", segmentFiles = segmentFiles, navController = navController,)
                SideControl(
                    icon = R.drawable.template,
                    label = "Template",
                    segmentFiles = segmentFiles,
                    navController = navController,
                )
                if (segmentFiles.isNotEmpty()) {
                    SideControl(icon = R.drawable.complete, label = "Complete",segmentFiles = segmentFiles,navController=navController)
                }
            }
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
                    onRecordClick = { },
                    onPhotoClick = {
                        Log.d("CameraScreen", "Photo captured")
                    },
                    isRecording = isRecording,
                    isPaused = isPaused,
                    videoCapture=videoCapture,
                   segmentFiles=segmentFiles,
                    onStartRecording = {
                        isRecording = true
                        isPaused = false
                    },
                    onPauseRecording = {
                        isPaused = true
                    },
                    onResumeRecording = {
                        isPaused = false
                    },
                    onStopRecording = {
                        isRecording = false
                        isPaused = false
                    }
                )
            }

            if (!isRecording && !isPaused) {
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
}
@Composable
fun SegmentRecordButton(
    modifier: Modifier = Modifier,
    isRecording: Boolean,
    isPaused: Boolean,
    videoCapture: VideoCapture<Recorder>?,
    segmentFiles: SnapshotStateList<File>,
    context: Context,
    maxDurationMs: Long = 15000L,
    onStartRecording: () -> Unit = {},
    onPauseRecording: () -> Unit = {},
    onResumeRecording: () -> Unit = {},
    onStopRecording: () -> Unit = {}
) {
    val segments = remember { mutableStateListOf<Float>() }
    val whiteSeparators = remember { mutableStateListOf<Float>() }
    var currentSegmentProgress by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    var recordingJob by remember { mutableStateOf<Job?>(null) }
    var currentRecording by remember { mutableStateOf<Recording?>(null) }
    var recordingStartTime by remember { mutableStateOf(0L) }

    fun stopRecording(reason: String = "Manual") {
        Log.d("SegmentRecordButton", "Stopping recording: $reason")
        recordingJob?.cancel()
        recordingJob = null
        if (currentSegmentProgress > 0f) {
            segments.add(currentSegmentProgress)
            whiteSeparators.add(segments.sum() * 360f)
            currentSegmentProgress = 0f
            onStopRecording()
        }
    }
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startSegment() {
        // 1. Start actual video file recording
        val segmentName = "segment_${System.currentTimeMillis()}.mp4"
        val file = File(context.cacheDir, segmentName)
        val outputOptions = FileOutputOptions.Builder(file).build()

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        recordingStartTime = SystemClock.elapsedRealtime()
        val prepared = videoCapture?.output
            ?.prepareRecording(context, outputOptions)
            ?.withAudioEnabled()

        if (prepared == null) {
            Log.e("Recording", "prepareRecording returned null")
            return
        }
        currentRecording = prepared.start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        Log.d("Recording", "Recording started")
                    }

                    is VideoRecordEvent.Finalize -> {
                        if (event.hasError()) {
                            Log.e("Recording", "Recording error: ${event.error}, cause: ${event.cause}, message: ${event.cause?.message}")
                        } else {
                            Log.d("Recording", "Saved segment: ${file.absolutePath}")
                            segmentFiles.add(file)  // Save for later merging
                        }
                        currentRecording = null
                        onStopRecording()
                    }
                }
            }

        // 2. Update UI state and start progress job
        onStartRecording()

        recordingJob = coroutineScope.launch {
            val startTime = SystemClock.elapsedRealtime()
            while (isActive) {
                val now = SystemClock.elapsedRealtime()
                val elapsed = now - startTime
                currentSegmentProgress = (elapsed / maxDurationMs.toFloat()).coerceAtMost(1f)
                val totalProgress = segments.sum() + currentSegmentProgress

                if (totalProgress >= 1f) {
                    stopRecording("Auto - Max Duration Reached")
                    break
                }

                delay(16)
            }
        }
    }
    fun finishPause() {
        if (currentSegmentProgress > 0f) {
            segments.add(currentSegmentProgress)
            whiteSeparators.add(segments.sum() * 360f)
            currentSegmentProgress = 0f
        }

        currentRecording?.stop()
    }
    fun pauseSegment() {
        onPauseRecording()
        recordingJob?.cancel()
        recordingJob = null

        val minDurationMs = 500L
        val elapsed = SystemClock.elapsedRealtime() - recordingStartTime

        if (elapsed < minDurationMs) {
            coroutineScope.launch {
                delay(minDurationMs - elapsed)
                finishPause()
            }
        } else {
            finishPause()
        }
    }
    fun resumeSegment() {
        onResumeRecording()
        startSegment()
    }
    // Animate outer box size smoothly
    val outerSize by animateDpAsState(targetValue = if (!isRecording && !isPaused) 70.dp else 100.dp)
    // Animate inner circle size smoothly
    val innerSize by animateDpAsState(targetValue = if (!isRecording && !isPaused) 50.dp else 70.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            // Zoom the button here by increasing size
            .size(outerSize)  // Changed from 100.dp to 120.dp for zoom effect
            .clickable {
                when {
                    !isRecording -> startSegment()
                    isRecording && !isPaused -> pauseSegment()
                    isRecording && isPaused -> resumeSegment()
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = if(!isRecording && !isPaused) 8.dp.toPx() else 12.dp.toPx()
            var startAngle = -90f

            // Draw previous segments
            segments.forEach { segment ->
                val sweep = 360f * segment
                drawArc(
                    color = Color.Red,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = stroke)
                )
                startAngle += sweep
            }

            // Draw current segment progress if recording and not paused
            if (isRecording && !isPaused && currentSegmentProgress > 0f) {
                val sweep = 360f * currentSegmentProgress
                drawArc(
                    color = Color.Red,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = stroke)
                )
                startAngle += sweep
            }

            // **Hide white separators when recording or paused**
            if (!isRecording && !isPaused) {
                whiteSeparators.forEach { angle ->
                    drawArc(
                        color = Color.LightGray,
                        startAngle = -90f + angle,
                        sweepAngle = 3f,
                        useCenter = false,
                        style = Stroke(width = stroke)
                    )
                }
            }

            val totalProgress = segments.sum() + currentSegmentProgress
            val remaining = 360f - (360f * totalProgress)
            if (remaining > 0f) {
                drawArc(
                    color = Color.White,
                    startAngle = startAngle,
                    sweepAngle = remaining,
                    useCenter = false,
                    style = Stroke(width = stroke)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(innerSize)  // Increased from 50.dp for better zoom effect
                .clip(CircleShape)
                .background(
                    when {
                        isRecording -> Color.Red
                        else -> Color.White
                    }
                )
        )
    }
}

@Composable
fun TopBar(onFlip: () -> Unit, onFlashToggle: () -> Unit, isFlashOn: Boolean,visible: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 10.dp, end = 10.dp),
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
        if (visible) {
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
                Text(
                    "Add Sound",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
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
}

@Composable
fun RecordBar(
    selectedTab: String,
    onRecordClick: () -> Unit,
    onPhotoClick: () -> Unit,
    isRecording: Boolean,
    isPaused: Boolean,
    videoCapture: VideoCapture<Recorder>?,
    onStartRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onStopRecording: () -> Unit,
    segmentFiles: SnapshotStateList<File>,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (!isRecording && !isPaused) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    // You can add your effects click handler here if needed
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.effect),
                    contentDescription = "Effects",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Text("Effects", color = Color.White, fontSize = 12.sp)
            }
        } else {
            Spacer(modifier = Modifier.width(0.dp))  // Optional: to keep code symmetry but no space reserved
        }

        Box(
            modifier = Modifier
                .clickable {
                    if (selectedTab == "Photo") {
                        onPhotoClick()
                    }
                }
            .padding(bottom =if(!isRecording && !isPaused) 0.dp else 100.dp),
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
                SegmentRecordButton(
                    isRecording = isRecording,
                    isPaused = isPaused,
                    videoCapture = videoCapture,
                    segmentFiles = segmentFiles,
                    context = LocalContext.current,
                    onStartRecording = onStartRecording,
                    onPauseRecording = onPauseRecording,
                    onResumeRecording = onResumeRecording,
                    onStopRecording = onStopRecording
                )
            }
        }

        if (!isRecording && !isPaused) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    // You can add your gallery click handler here if needed
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.gallery),
                    contentDescription = "Gallery",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Text("Gallery", color = Color.White, fontSize = 12.sp)
            }
        } else {
            Spacer(modifier = Modifier.width(0.dp))
        }
    }

}

fun bindCameraUseCases(
    cameraProvider: ProcessCameraProvider,
    previewView: PreviewView,
    lensFacing: Int,
    lifecycleOwner: LifecycleOwner,
    flashOn: Boolean,
    onCameraControlAvailable: (CameraControl) -> Unit,
    onVideoCaptureReady: (VideoCapture<Recorder>) -> Unit
) {
    val preview = Preview.Builder().build().apply {
        setSurfaceProvider(previewView.surfaceProvider)
    }
    val recorder = Recorder.Builder()
        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
        .build()
    val videoCapture = VideoCapture.withOutput(recorder)

    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(lensFacing)
        .build()

    try {
        cameraProvider.unbindAll()
        val camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        onCameraControlAvailable(camera.cameraControl)
        camera.cameraControl.enableTorch(flashOn)
        onVideoCaptureReady(videoCapture)
    } catch (exc: Exception) {
        Log.e("CameraX", "Use case binding failed", exc)
    }
}

@Composable
fun SideControl(
    icon: Int,
    label: String,
    segmentFiles: SnapshotStateList<File>,
    navController: NavHostController
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(24.dp).clickable {
                if(label=="Complete"){
                    val paths = segmentFiles.joinToString(",") { it.absolutePath }
                    navController.navigate("videoPlayer?paths=$paths")
                }
            }
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