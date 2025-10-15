package com.example.myapplication

import TimerGridPopup
import ai.deepar.ar.AREventListener
import ai.deepar.ar.DeepAR
import ai.deepar.ar.DeepARImageFormat
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

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

    var showFilterSheet by remember { mutableStateOf(false) }

    // DeepAR state
    var deepAR by remember { mutableStateOf<DeepAR?>(null) }
    var surfaceView by remember { mutableStateOf<DeepARSurfaceView?>(null) }
    var isDeepARInitialized by remember { mutableStateOf(false) }
    var isSurfaceReady by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf<FilterItem?>(null) }
    var useDeepAR by remember { mutableStateOf(true) } // Toggle for DeepAR

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

    // Initialize DeepAR
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && useDeepAR) {
            try {
                val deepar = DeepAR(context)
                deepar.setLicenseKey("83f789a8e6168a14db7bf506010e82c331d90f3f35296e1b63abf6c62a1e6e975700e5ad360c6cf5")

                val arEventListener = object : AREventListener {
                    override fun initialized() {
                        Log.d("DeepAR", "✅ DeepAR initialized successfully")
                        isDeepARInitialized = true
                    }

                    override fun error(errorType: ai.deepar.ar.ARErrorType?, error: String?) {
                        Log.e("DeepAR", "❌ DeepAR Error: $errorType - $error")
                    }

                    override fun effectSwitched(slot: String?) {
                        Log.d("DeepAR", "✨ Effect switched in slot: $slot")
                    }

                    override fun faceVisibilityChanged(faceVisible: Boolean) {}
                    override fun frameAvailable(frame: android.media.Image) {}
                    override fun imageVisibilityChanged(gameObjectName: String?, imageVisible: Boolean) {}
                    override fun screenshotTaken(bitmap: android.graphics.Bitmap) {}
                    override fun shutdownFinished() {}
                    override fun videoRecordingFailed() {}
                    override fun videoRecordingFinished() {}
                    override fun videoRecordingPrepared() {}
                    override fun videoRecordingStarted() {}
                }

                deepar.initialize(context, arEventListener)
                deepAR = deepar

                val surface = DeepARSurfaceView(context)
                surface.bindDeepAR(deepar) {
                    isSurfaceReady = true
                }
                surfaceView = surface

            } catch (e: Exception) {
                Log.e("DeepAR", "Failed to initialize DeepAR: ${e.message}", e)
                useDeepAR = false
            }
        }
    }

    // Apply DeepAR filter
    fun applyDeepARFilter(filter: FilterItem?) {
        deepAR?.let { ar ->
            try {
                if (filter?.path != null) {
                    Log.d("DeepAR", "Applying filter: ${filter.name}")
                    ar.switchEffect("effects", filter.path)
                } else {
                    Log.d("DeepAR", "Clearing filter")
                    ar.switchEffect("effects", null as String?)
                }
                selectedFilter = filter
            } catch (e: Exception) {
                Log.e("DeepAR", "Error applying filter: ${e.message}", e)
            }
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
    fun stopDeepARRecording(file: File) {
        try {
            deepAR?.stopVideoRecording()
            isRecording = false
            isPaused = false
            isTimerActive = false

            outputSegments.add(Uri.fromFile(file))
            viewModel.addVideo(Uri.fromFile(file))

            if (isMusicPlaying) {
                exoPlayer.pause()
                isMusicPlaying = false
            }

            Log.d("DeepAR", "Recording stopped and saved: ${file.absolutePath}")

        } catch (e: Exception) {
            Log.e("DeepAR", "Error stopping DeepAR recording: ${e.message}", e)
            Toast.makeText(context, "Stop recording failed", Toast.LENGTH_SHORT).show()
        }
    }

    fun startVideoRecordingDeepAR() {
        if (!hasAllPermissions(context)) {
            Toast.makeText(context, "Permissions missing", Toast.LENGTH_SHORT).show()
            return
        }

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        dir?.mkdirs()
        val file = File(dir, "deepar_segment_${System.currentTimeMillis()}.mp4")

        try {
            // Start DeepAR recording
            deepAR?.startVideoRecording(file.absolutePath)
            isRecording = true
            isPaused = false

            Log.d("DeepAR", "Recording started at: ${file.absolutePath}")

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

                    val durationMs = if (isTimerActive) {
                        ((recordingEndTime - recordingStartTime) * 1000).toLong()
                    } else {
                        (audioTrimViewModel.endMs - audioTrimViewModel.startMs).coerceAtLeast(100)
                    }

                    autoStopJob?.cancel()
                    autoStopJob = coroutineScope.launch {
                        delay(durationMs)
                        stopDeepARRecording(file)
                        outputSegments.add(Uri.fromFile(file))
                        viewModel.addVideo(Uri.fromFile(file))
                    }

                } catch (e: Exception) {
                    Log.e("AudioTrimmer", "Error playing audio", e)
                }
            } ?: run {
                // No music
                if (isTimerActive) {
                    val durationMs = ((recordingEndTime - recordingStartTime) * 1000).toLong()
                    autoStopJob?.cancel()
                    autoStopJob = coroutineScope.launch {
                        delay(durationMs)
                        stopDeepARRecording(file)
                        outputSegments.add(Uri.fromFile(file))
                        viewModel.addVideo(Uri.fromFile(file))
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("DeepAR", "Error starting DeepAR recording: ${e.message}", e)
            Toast.makeText(context, "Recording failed", Toast.LENGTH_SHORT).show()
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
        onDispose {
            exoPlayer.release()
            deepAR?.release()
        }
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

   /* LaunchedEffect(cameraPermissionGranted, lensFacing, previewView, cameraProvider, isFlashOn) {
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
    }*/

    // Start camera with DeepAR integration
    LaunchedEffect(cameraPermissionGranted, lensFacing, cameraProvider, isFlashOn, isDeepARInitialized, isSurfaceReady) {
        if (cameraPermissionGranted && cameraProvider != null && useDeepAR && isDeepARInitialized && isSurfaceReady) {
            bindCameraWithDeepAR(
                cameraProvider!!,
                lensFacing,
                lifecycleOwner,
                deepAR,
                onVideoCaptureReady = { videoCapture = it }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraPermissionGranted) {
           /* AndroidView(factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    setBackgroundColor(android.graphics.Color.BLACK)
                }.also { previewView = it }
            }, modifier = Modifier.fillMaxSize())*/
            if (useDeepAR && surfaceView != null) {
                // DeepAR Surface View
                AndroidView(
                    factory = { surfaceView!! },
                    modifier = Modifier.fillMaxSize()
                )
            }
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
                SideControl(icon = R.drawable.filter, label = "Filter", onClick = { showFilterSheet = true})
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
                        //startVideoRecording()
                        startVideoRecordingDeepAR()
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
        if (showFilterSheet) {
            val sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true, // prevent half state
                confirmValueChange = { value ->
                    // ✅ Prevent swipe-down dismiss
                    value != SheetValue.Hidden
                }
            )
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                containerColor = Color(0xFF101010),
                sheetState = sheetState,
                dragHandle = null
            ) {
                FilterBottomSheetUI(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { filter ->
                        applyDeepARFilter(filter)
                    },
                    onDismiss = { showFilterSheet = false }
                )
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

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalGetImage::class)
fun bindCameraWithDeepAR(
    cameraProvider: ProcessCameraProvider,
    lensFacing: Int,
    lifecycleOwner: LifecycleOwner,
    deepAR: DeepAR?,
    onVideoCaptureReady: (VideoCapture<Recorder>) -> Unit
) {
    val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HIGHEST)).build()
    val videoCapture = VideoCapture.withOutput(recorder)

    val imageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
        .build()

    imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
        val mediaImage = imageProxy.image
        if (mediaImage != null && deepAR != null) {
            val buffer = mediaImage.planes[0].buffer
            val width = mediaImage.width
            val height = mediaImage.height
            val rotation = imageProxy.imageInfo.rotationDegrees

            Handler(Looper.getMainLooper()).post {
                try {
                    deepAR.receiveFrame(
                        buffer,
                        width,
                        height,
                        rotation,
                        lensFacing == CameraSelector.LENS_FACING_FRONT,
                        DeepARImageFormat.YUV_NV21,
                        mediaImage.planes[0].rowStride / width
                    )
                } catch (e: Exception) {
                    Log.e("DeepAR", "receiveFrame failed: ${e.message}", e)
                } finally {
                    imageProxy.close()
                }
            }
        } else {
            imageProxy.close()
        }
    }

    val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

    try {
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalysis, videoCapture)
        onVideoCaptureReady(videoCapture)
        Log.d("CameraX", "✅ Camera bound with DeepAR")
    } catch (exc: Exception) {
        Log.e("CameraX", "Use case binding failed", exc)
    }
}




