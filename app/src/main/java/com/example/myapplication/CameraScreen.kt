package com.example.myapplication

import TimerGridPopup
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavHostController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(navController: NavHostController, viewModel: CameraViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

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

    val outputSegments = remember { mutableStateListOf<Uri>() }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var triggerStartRecording  by remember { mutableStateOf(false) }

    var showTimerPicker by remember { mutableStateOf(false) }
    var selectedTimerSeconds by remember { mutableIntStateOf(0) }
    var timerAnchor by remember { mutableStateOf(Offset.Zero) }

    var showVideoTimerScreen by remember { mutableStateOf(false) }
    var recordingStartTime by remember { mutableFloatStateOf(0f) }
    var recordingEndTime by remember { mutableFloatStateOf(15f) }
    var timerCountdown by remember { mutableIntStateOf(3) }
    var isTimerActive by remember { mutableStateOf(false) }

    val audioTrimViewModel: AudioTrimmerViewModel = viewModel()
    var showMusicPicker by remember { mutableStateOf(false) }
    var selectedMusic by remember { mutableStateOf<MusicItem?>(null) }
    var isMusicPlaying by remember { mutableStateOf(false) }
    var autoStopJob by remember { mutableStateOf<Job?>(null) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .build().apply {
                volume = 1.0f
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    // SEPARATED RECORDING FUNCTION
    fun startVideoRecording() {
        if (!hasAllPermissions(context)) {
            Toast.makeText(context, "Permissions missing", Toast.LENGTH_SHORT).show()
            return
        }

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        dir?.mkdirs()
        val name = "segment_${System.currentTimeMillis()}.mp4"
        val file = File(dir, name)
        val outputOptions = FileOutputOptions.Builder(file).build()

        val pendingRecording = videoCapture?.output?.prepareRecording(context, outputOptions)
        val finalRecording = try {
            pendingRecording?.withAudioEnabled()
        } catch (e: SecurityException) {
            Log.e("VideoCapture", "SecurityException: Missing RECORD_AUDIO permission", e)
            pendingRecording
        }

        val recordingSession = finalRecording?.start(ContextCompat.getMainExecutor(context)) { event ->
            if (event is VideoRecordEvent.Finalize) {
                if (event.hasError()) {
                    Log.e("VideoCapture", "Recording error: ${event.error}")
                } else {
                    outputSegments.add(event.outputResults.outputUri)
                    Log.d("VideoCapture", "Saved at: ${file.absolutePath}")
                    viewModel.addVideo(event.outputResults.outputUri)
                }
            }
        }

        recording = recordingSession
        isRecording = true
        isPaused = false

        selectedMusic?.let {
            try {
                Log.d("AudioTrimmer", "Starting playback from ${audioTrimViewModel.startMs}ms")

                if (exoPlayer.playbackState == Player.STATE_READY) {
                    exoPlayer.seekTo(audioTrimViewModel.startMs)
                    exoPlayer.play()
                    isMusicPlaying = true
                } else {
                    coroutineScope.launch {
                        var attempts = 0
                        while (exoPlayer.playbackState != Player.STATE_READY && attempts < 50) {
                            delay(50)
                            attempts++
                        }
                        if (exoPlayer.playbackState == Player.STATE_READY && isRecording && !isPaused) {
                            exoPlayer.seekTo(audioTrimViewModel.startMs)
                            exoPlayer.play()
                            isMusicPlaying = true
                        }
                    }
                }

                autoStopJob?.cancel()
                val durationMs = if (isTimerActive) {
                    ((recordingEndTime - recordingStartTime) * 1000).toLong()
                } else {
                    (audioTrimViewModel.endMs - audioTrimViewModel.startMs).coerceAtLeast(100)
                }

                autoStopJob = coroutineScope.launch {
                    delay(durationMs)
                    if (isRecording) {
                        recording?.stop()
                        recording = null
                        isRecording = false
                        isPaused = false
                        isTimerActive = false
                        if (isMusicPlaying) {
                            exoPlayer.pause()
                            isMusicPlaying = false
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AudioTrimmer", "Error playing audio", e)
            }
        } ?: run {
            if (isTimerActive) {
                val durationMs = ((recordingEndTime - recordingStartTime) * 1000).toLong()
                autoStopJob?.cancel()
                autoStopJob = coroutineScope.launch {
                    delay(durationMs)
                    if (isRecording) {
                        recording?.stop()
                        recording = null
                        isRecording = false
                        isPaused = false
                        isTimerActive = false
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isMusicPlaying = isPlaying
            }
            override fun onPlayerError(error: PlaybackException) {
                Toast.makeText(context, "Audio error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        exoPlayer.addListener(listener)
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    LaunchedEffect(selectedMusic) {
        if (selectedMusic != null) {
            try {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.setMediaItem(MediaItem.fromUri(selectedMusic!!.audioUrl))
                exoPlayer.prepare()
                var attempts = 0
                while (exoPlayer.playbackState != Player.STATE_READY && attempts < 100) {
                    delay(50)
                    attempts++
                }
                if (exoPlayer.playbackState == Player.STATE_READY) {
                    exoPlayer.seekTo(audioTrimViewModel.startMs)
                }
            } catch (e: Exception) {
                Log.e("AudioTrimmer", "Error preparing music", e)
            }
        } else {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            isMusicPlaying = false
        }
    }

    val requiredPermissions = remember {
        mutableStateListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_VIDEO)
            } else {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        cameraPermissionGranted = requiredPermissions.all { permissions[it] == true }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(requiredPermissions.toTypedArray())
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
        }, ContextCompat.getMainExecutor(context))
    }

    LaunchedEffect(cameraPermissionGranted, lensFacing, previewView, cameraProvider, isFlashOn) {
        if (cameraPermissionGranted && cameraProvider != null && previewView != null) {
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
                    setBackgroundColor(android.graphics.Color.BLACK)
                }.also { previewView = it }
            }, modifier = Modifier.fillMaxSize())
        }

        Box(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
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
                visible = !isRecording && !isPaused,
                onShowMusicPicker = { showMusicPicker = true }
            )
        }

        if (!isRecording && !isPaused) {
            Column(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SideControl(icon = R.drawable.filter, label = "Filter", onClick = {})
                SideControl(icon = R.drawable.beautify, label = "Beaut.", onClick = {})
                SideControl(
                    icon = R.drawable.timer,
                    label = if (selectedTimerSeconds > 0) "${selectedTimerSeconds}s" else "Length",
                    onClick = { showTimerPicker = true },
                    onPositionReady = { offset -> timerAnchor = offset }
                )
                SideControl(
                    icon = R.drawable.timer,
                    label = if (isTimerActive) "${timerCountdown}s" else "Timer",
                    onClick = { showVideoTimerScreen = true }
                )
                SideControl(icon = R.drawable.speed, label = "Speed", onClick = {})
                SideControl(icon = R.drawable.template, label = "Template", onClick = {})
                if (outputSegments.isNotEmpty()) {
                    SideControl(icon = R.drawable.ic_check, label = "Done",
                        onClick = { navController.navigate("video_playback_screen") })
                }
            }
        }

        if (showTimerPicker) {
            TimerGridPopup(
                expanded = showTimerPicker,
                onDismissRequest = { showTimerPicker = false },
                selectedTimer = selectedTimerSeconds,
                onTimerSelected = { seconds ->
                    selectedTimerSeconds = seconds
                    showTimerPicker = false
                },
                anchorPosition = timerAnchor,
            )
        }

        if (showVideoTimerScreen) {
            ModalBottomSheet(
                onDismissRequest = {
                    showVideoTimerScreen = false
                    isTimerActive = false
                },
                containerColor = Color(0xFF101010),
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                VideoTimerScreen(
                    totalDuration = 60f,
                    onRecordStart = {
                        showVideoTimerScreen = false
                        isTimerActive = true
                    },
                    onCancel = {
                        showVideoTimerScreen = false
                        isTimerActive = false
                    },
                    onTimerSet = { start, end, countdown ->
                        recordingStartTime = start
                        recordingEndTime = end
                        timerCountdown = countdown
                    }
                )
            }
        }

// Add countdown overlay on main camera screen
        if (isTimerActive && timerCountdown > 0) {
            CountdownOverlay(
                countdown = timerCountdown,
                context = context,
                onCountdownFinish = {
                    timerCountdown = 0
                    isTimerActive = false
                    triggerStartRecording  = true
                }
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (selectedTab != "Live") {
                RecordBar(
                    selectedTab = selectedTab,
                    onPhotoClick = { Log.d("CameraScreen", "Photo captured") },
                    isRecording = isRecording,
                    isPaused = isPaused,
                    externalTriggerStart = triggerStartRecording ,
                    onStartRecording = {
                        //isTimerActive = false
                        startVideoRecording()
                        triggerStartRecording  = false
                    },
                    onPauseRecording = {
                        recording?.pause()
                        autoStopJob?.cancel()
                        if (isMusicPlaying) {
                            exoPlayer.pause()
                            isMusicPlaying = false
                        }
                        isPaused = true
                    },
                    onResumeRecording = {
                        recording?.resume()
                        selectedMusic?.let {
                            try {
                                exoPlayer.play()
                                isMusicPlaying = true
                                val remainingDuration = audioTrimViewModel.endMs - exoPlayer.currentPosition
                                if (remainingDuration > 0) {
                                    autoStopJob?.cancel()
                                    autoStopJob = coroutineScope.launch {
                                        delay(remainingDuration)
                                        if (isRecording) {
                                            recording?.stop()
                                            recording = null
                                            isRecording = false
                                            isPaused = false
                                            if (isMusicPlaying) {
                                                exoPlayer.pause()
                                                isMusicPlaying = false
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("AudioTrimmer", "Error resuming audio", e)
                            }
                        }
                        isPaused = false
                    },
                    onStopRecording = {
                        recording?.stop()
                        recording = null
                        isRecording = false
                        isPaused = false
                        autoStopJob?.cancel()
                        autoStopJob = null
                        if (isMusicPlaying) {
                            exoPlayer.pause()
                            isMusicPlaying = false
                        }
                        isTimerActive = false
                        selectedMusic?.let { exoPlayer.seekTo(audioTrimViewModel.startMs) }
                    }
                )
            }

            if (!isRecording && !isPaused) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    tabs.forEach { tab ->
                        BottomControl(label = tab, highlight = (selectedTab == tab), onClick = { selectedTab = tab })
                    }
                }
            }
        }

        if (showMusicPicker) {
            MusicAppTheme {
                var showBottomSheet by remember { mutableStateOf(true) }
                if (showBottomSheet) {
                    ModalBottomSheet(
                        onDismissRequest = {
                            showBottomSheet = false
                            showMusicPicker = false
                        },
                        containerColor = Color(0xFF121212)
                    ) {
                        MusicBrowseScreen(
                            viewModel = audioTrimViewModel,
                            onMusicSelected = { music ->
                                selectedMusic = music
                                showBottomSheet = false
                                showMusicPicker = false
                            },
                            onBackPressed = {
                                showBottomSheet = false
                                showMusicPicker = false
                            }
                        )
                    }
                }
            }
        }
    }
}

fun hasAllPermissions(context: Context): Boolean {
    val permissions = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
    return permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
}
fun bindCameraUseCases(
    cameraProvider: ProcessCameraProvider, previewView: PreviewView, lensFacing: Int, lifecycleOwner: LifecycleOwner,
    flashOn: Boolean, onCameraControlAvailable: (CameraControl) -> Unit, onVideoCaptureReady: (VideoCapture<Recorder>) -> Unit
) {
    val preview = Preview.Builder().build().apply { setSurfaceProvider(previewView.surfaceProvider) }
    val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HIGHEST)).build()
    val videoCapture = VideoCapture.withOutput(recorder)
    val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

    try {
        cameraProvider.unbindAll()
        val camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
        onCameraControlAvailable(camera.cameraControl)
        camera.cameraControl.enableTorch(flashOn)
        onVideoCaptureReady(videoCapture)
    } catch (exc: Exception) {
        Log.e("CameraX", "Use case binding failed", exc)
    }
}




