package com.example.myapplication

import SpeedControlBottomSheet
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
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(navController: NavHostController, viewModel: CameraViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // Timer state for showing recorded time
    var displayTime by remember { mutableFloatStateOf(0f) } // total time shown on screen
    var lastSessionDuration by remember { mutableFloatStateOf(0f) } // cumulative time before last pause
    var recordingStartTimeMs by remember { mutableLongStateOf(0L) } // system time when current session started


    // ✅ Collect states from ViewModel
    val recordedVideos by viewModel.recordedVideos.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val isTimerActive by viewModel.isTimerActive.collectAsState()
    val triggerStartRecording by viewModel.triggerStartRecording.collectAsState()
    var externalTriggerPause by remember { mutableStateOf(false) }

    val tabs = listOf("Photo", "Clips", "Live")
    var selectedTab by remember { mutableStateOf("Clips") }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    var currentRecordingFile by remember { mutableStateOf<File?>(null) }

    var showTimerPicker by remember { mutableStateOf(false) }
    var selectedTimerSeconds by remember { mutableIntStateOf(0) }
    var timerAnchor by remember { mutableStateOf(Offset.Zero) }

    var showVideoTimerScreen by remember { mutableStateOf(false) }
    var recordingStartTime by remember { mutableFloatStateOf(0f) }
    var recordingEndTime by remember { mutableFloatStateOf(15f) }
    var timerCountdown by remember { mutableIntStateOf(3) }
    var isCountingDown by remember { mutableStateOf(false) }

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
    var useDeepAR by remember { mutableStateOf(true) }
    var showBeautySheet by remember { mutableStateOf(false) }
    var smoothness by remember { mutableFloatStateOf(0.9f) }
    var whiteness by remember { mutableFloatStateOf(0.9f) }
    var redness by remember { mutableFloatStateOf(0.9f) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var selectedSpeed by remember { mutableFloatStateOf(1f) }
    var isRecordingLocked by remember { mutableStateOf(false) }

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

    // ✅ FIXED: Updated stopDeepARRecording with shouldPause parameter
    fun stopDeepARRecording(file: File, shouldPause: Boolean = false) {
        try {
            deepAR?.stopVideoRecording()

            // Stop music immediately
            if (isMusicPlaying) {
                exoPlayer.pause()
                isMusicPlaying = false
            }

            autoStopJob?.cancel()
            autoStopJob = null

            // ✅ Update states immediately
            viewModel.setRecordingState(false)

            // ✅ Set paused state based on parameter
            if (shouldPause) {
                // For auto-pause: First stop recording, then set pause
                viewModel.setPausedState(true)
                Log.d("DeepAR", "⏸️ Auto-paused after recording")
            } else {
                // For manual stop: Clear both states
                viewModel.setPausedState(false)
                // ✅ Release lock when fully stopped
                isRecordingLocked = false
            }

            viewModel.setTimerActive(false)

            // ✅ File verification happens AFTER state update
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    if (file.exists()) {
                        val fileSize = file.length()
                        if (fileSize > 0) {
                            val uri = Uri.fromFile(file)
                            viewModel.addVideo(uri)
                            Log.d("DeepAR", "✅ Video saved: ${file.name} (${fileSize / 1024}KB)")
                        } else {
                            file.delete()
                            Log.e("DeepAR", "❌ Empty file deleted")
                        }
                    } else {
                        Log.e("DeepAR", "❌ File not found: ${file.absolutePath}")
                       // Toast.makeText(context, "Recording failed - file not saved", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("DeepAR", "Error checking file", e)
                } finally {
                    currentRecordingFile = null
                }
            }, 500)

        } catch (e: Exception) {
            Log.e("DeepAR", "Error stopping DeepAR recording: ${e.message}", e)
            viewModel.setRecordingState(false)
            viewModel.setPausedState(false)
            viewModel.setTimerActive(false)
            currentRecordingFile = null
            isRecordingLocked = false
        }
    }

    fun startVideoRecordingDeepAR() {
        if (isRecordingLocked) {
            Log.d("DeepAR", "⚠️ Recording already in progress, ignoring trigger")
            return
        }
        if (!hasAllPermissions(context)) {
            Toast.makeText(context, "Permissions missing", Toast.LENGTH_SHORT).show()
            return
        }

        if (deepAR == null || !isDeepARInitialized) {
            Toast.makeText(context, "DeepAR not initialized", Toast.LENGTH_SHORT).show()
            Log.e("DeepAR", "Cannot start recording - DeepAR not ready")
            return
        }

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        if (dir == null) {
            Toast.makeText(context, "Storage not available", Toast.LENGTH_SHORT).show()
            return
        }
        dir.mkdirs()
        val file = File(dir, "deepar_${System.currentTimeMillis()}.mp4")
        currentRecordingFile = file

        // ✅ Lock recording
        isRecordingLocked = true

        try {
            // Start DeepAR recording
            val width = 720
            val height = 1280
            deepAR?.startVideoRecording(
                file.absolutePath,
                width,
                height,
            )
            viewModel.setRecordingState(true)
            viewModel.setPausedState(false)

            Log.d("DeepAR", "Recording started at: ${file.absolutePath}")

            selectedMusic?.let {
                try {
                    Log.d("DeepAR", "Starting playback from ${audioTrimViewModel.startMs}ms")
                    exoPlayer.playbackParameters = PlaybackParameters(selectedSpeed)

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

                    val durationMs = when {
                        isTimerActive -> {
                            val duration = (recordingEndTime * 1000).toLong()
                            Log.d("DeepAR", "⏱️ Using TIMER duration: ${duration}ms (end: ${recordingEndTime}s)")
                            duration
                        }
                        selectedTimerSeconds > 0 -> {
                            val duration = (selectedTimerSeconds * 1000).toLong()
                            Log.d("DeepAR", "⏱️ Using LENGTH duration: ${duration}ms")
                            duration
                        }
                        selectedMusic != null -> {
                            val duration = (audioTrimViewModel.endMs - audioTrimViewModel.startMs).coerceAtLeast(100)
                            Log.d("DeepAR", "🎵 Using MUSIC duration: ${duration}ms")
                            duration
                        }
                        else -> {
                            Log.d("DeepAR", "⏱️ Using DEFAULT duration: 15000ms")
                            15_000L
                        }
                    }

                    Log.d("DeepAR", "Auto-stop scheduled for ${durationMs}ms (isTimerActive=$isTimerActive)")

                    // ✅ FIXED: Auto-stop with shouldPause = true
                    autoStopJob?.cancel()
                    autoStopJob = coroutineScope.launch {
                        delay(durationMs)
                        if (isRecording && currentRecordingFile != null) {
                            currentRecordingFile?.let { file ->
                                stopDeepARRecording(file, shouldPause = true) // ✅ Enable auto-pause
                            }
                            if (isTimerActive) {
                                viewModel.setTimerActive(false)
                                isCountingDown = false
                                Log.d("DeepAR", "✅ Timer reset after auto-stop")
                            }
                        }
                    }

                } catch (e: Exception) {
                    Log.e("DeepAR", "Error starting DeepAR recording: ${e.message}", e)
                }
            } ?: run {
                // No music
                if (isTimerActive) {
                    val durationMs = (recordingEndTime * 1000).toLong()

                    // ✅ FIXED: Auto-stop with shouldPause = true
                    autoStopJob?.cancel()
                    autoStopJob = coroutineScope.launch {
                        delay(durationMs)
                        if (isRecording && currentRecordingFile != null) {
                            currentRecordingFile?.let { file ->
                                stopDeepARRecording(file, shouldPause = true) // ✅ Enable auto-pause
                            }
                            if (isTimerActive) {
                                viewModel.setTimerActive(false)
                                isCountingDown = false
                                Log.d("DeepAR", "✅ Timer reset after auto-stop")
                            }
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("DeepAR", "Error starting DeepAR recording: ${e.message}", e)
            Toast.makeText(context, "Recording failed", Toast.LENGTH_SHORT).show()
            viewModel.setRecordingState(false)
            viewModel.setPausedState(false)
            currentRecordingFile = null
            isRecordingLocked = false
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

    LaunchedEffect(triggerStartRecording) {
        if (triggerStartRecording && !isRecording && !isRecordingLocked) {
            startVideoRecordingDeepAR()
            viewModel.resetRecordingTrigger()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.resetRecordingTrigger()

        if (recordedVideos.isEmpty()) {
            viewModel.resetAll()
        } else {
            viewModel.resetStates()
        }

        Log.d("CameraScreen", "Screen initialized")
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            deepAR?.release()
            viewModel.resetStates()
            Log.d("CameraScreen", "Screen disposed, states reset")
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

    LaunchedEffect(cameraPermissionGranted, lensFacing, cameraProvider, isFlashOn, isDeepARInitialized, isSurfaceReady) {
        if (cameraPermissionGranted && cameraProvider != null && useDeepAR && isDeepARInitialized && isSurfaceReady) {
            bindCameraWithDeepAR(
                cameraProvider!!,
                lensFacing,
                lifecycleOwner,
                deepAR
            )
        }
    }
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingStartTimeMs = System.currentTimeMillis()
            while (isRecording) {
                val currentDuration = (System.currentTimeMillis() - recordingStartTimeMs) / 1000f
                displayTime = lastSessionDuration + currentDuration
                delay(100)
            }
            // when recording stops, preserve elapsed time
            lastSessionDuration = displayTime
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraPermissionGranted) {
            if (useDeepAR && surfaceView != null) {
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
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SideControl(icon = R.drawable.filter, label = "Filter", onClick = { showFilterSheet = true})
                SideControl(icon = R.drawable.beautify, label = "Beaut.", onClick = {showBeautySheet = true })
                SideControl(
                    icon = R.drawable.timer,
                    label = if (selectedTimerSeconds > 0) "${selectedTimerSeconds}s" else "Length",
                    onClick = { showTimerPicker = true },
                    onPositionReady = { offset -> timerAnchor = offset }
                )
                SideControl(
                    icon = R.drawable.timer,
                    label = if (isTimerActive) "${timerCountdown}s" else "Timer",
                    onClick = {
                        showVideoTimerScreen = true
                        isCountingDown = false
                    }
                )
                SideControl(
                    icon = R.drawable.speed,
                    label = if (selectedSpeed == 1f) "Speed" else "${selectedSpeed}x",
                    onClick = { showSpeedSheet = true }
                )
                SideControl(icon = R.drawable.template, label = "Template", onClick = {})
            }
        }

        if (showTimerPicker) {
            TimerGridPopup(
                expanded = showTimerPicker,
                onDismissRequest = { showTimerPicker = false },
                selectedTimer = selectedTimerSeconds,
                onTimerSelected = { seconds ->
                    selectedTimerSeconds = seconds
                    recordingStartTime = 0f
                    recordingEndTime = seconds.toFloat()
                    showTimerPicker = false
                },
                anchorPosition = timerAnchor,
            )
        }

        if (showVideoTimerScreen) {
            ModalBottomSheet(
                onDismissRequest = {
                    showVideoTimerScreen = false
                    viewModel.setTimerActive(false)
                },
                containerColor = Color(0xFF101010),
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                VideoTimerScreen(
                    totalDuration =  selectedTimerSeconds.toFloat().coerceAtLeast(5f),
                    onRecordStart = {
                        showVideoTimerScreen = false
                        viewModel.setTimerActive(true)
                        isCountingDown = true
                    },
                    onCancel = {
                        showVideoTimerScreen = false
                        viewModel.setTimerActive(false)
                    },
                    onTimerSet = { start, end, countdown ->
                        recordingStartTime = start
                        recordingEndTime = end
                        timerCountdown = countdown
                    }
                )
            }
        }

        if (isTimerActive && isCountingDown && timerCountdown > 0) {
            CountdownOverlay(
                countdown = timerCountdown,
                context = context,
                onCountdownFinish = {
                    isCountingDown = false
                    timerCountdown = 0
                    viewModel.setTimerActive(false)
                    viewModel.triggerRecordingStart()
                }
            )
        }

        // 🔴 Show remaining recording time countdown while recording

            // 🎨 UI for time display
        if(recordedVideos.isNotEmpty())
            Text(
                text = String.format("%.0f s", displayTime),
                color = if (isTimerActive) Color.Red else Color.White,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )


        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (selectedTab != "Live") {
                RecordBar(
                    navController=navController,
                    cameraViewmodel=viewModel,
                    selectedTab = selectedTab,
                    onPhotoClick = { Log.d("CameraScreen", "Photo captured") },
                    isRecording = isRecording,
                    isPaused = isPaused,
                    externalTriggerPause=externalTriggerPause,
                    externalTriggerStart = triggerStartRecording ,
                    onStartRecording = {
                        if (!isRecordingLocked) {
                            startVideoRecordingDeepAR()
                            viewModel.resetRecordingTrigger()
                        }
                    },
                    onPauseRecording = {
                        currentRecordingFile?.let { file ->
                            stopDeepARRecording(file, shouldPause = true) // Manual pause, don't enable shouldPause
                        }
                        autoStopJob?.cancel()
                        if (isMusicPlaying) {
                            exoPlayer.pause()
                            isMusicPlaying = false
                        }
                        lastSessionDuration = displayTime
                    },
                    onResumeRecording = {
                        if (!isRecordingLocked) {
                            startVideoRecordingDeepAR()
                            selectedMusic?.let {
                                try {
                                    exoPlayer.playbackParameters = PlaybackParameters(selectedSpeed)
                                    exoPlayer.play()
                                    isMusicPlaying = true
                                    val remainingDuration =
                                        audioTrimViewModel.endMs - exoPlayer.currentPosition
                                    if (remainingDuration > 0) {
                                        // ✅ FIXED: Auto-stop with shouldPause = true
                                        autoStopJob?.cancel()
                                        autoStopJob = coroutineScope.launch {
                                            delay(remainingDuration)
                                            if (isRecording && currentRecordingFile != null) {
                                                stopDeepARRecording(currentRecordingFile!!, shouldPause = true)
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("AudioTrimmer", "Error resuming audio", e)
                                }
                            }
                            viewModel.setPausedState(false)
                        }
                    },
                    onStopRecording = {
                        currentRecordingFile?.let { file ->
                            stopDeepARRecording(file, shouldPause = false) // Full stop, no pause
                        }
                        autoStopJob?.cancel()
                        autoStopJob = null
                        if (isMusicPlaying) {
                            exoPlayer.pause()
                            isMusicPlaying = false
                        }
                        viewModel.setTimerActive(false)
                        selectedMusic?.let { exoPlayer.seekTo(audioTrimViewModel.startMs) }
                        isRecordingLocked = false

                        // 🔄 reset timer
                        displayTime = 0f
                        lastSessionDuration = 0f
                        recordingStartTimeMs = 0L
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
                skipPartiallyExpanded = true,
                confirmValueChange = { value ->
                    value != SheetValue.Hidden
                }
            )
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                containerColor = Color.Black.copy(alpha = 0.9f),
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

        if (showBeautySheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showBeautySheet = false },
                sheetState = sheetState,
                containerColor = Color.Black.copy(alpha = 0.95f)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Face Beauty", color = Color.White, style = MaterialTheme.typography.titleMedium)

                    Spacer(Modifier.height(16.dp))

                    BeautySlider("Smoothness", smoothness) {
                        smoothness = it
                        deepAR?.changeParameterFloat("beauty", "smoothness","float", it)
                    }

                    BeautySlider("Whitening", whiteness) {
                        whiteness = it
                        deepAR?.changeParameterFloat("beauty", "whiteness", "float",it)
                    }

                    BeautySlider("Redness", redness) {
                        redness = it
                        deepAR?.changeParameterFloat("beauty", "redness", "float",it)
                    }

                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { showBeautySheet = false }) {
                        Text("Close")
                    }
                }
            }
        }

        if (showSpeedSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showSpeedSheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFF1C1C1E),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.Gray)
                    )
                }
            ) {
                SpeedControlBottomSheet(
                    selectedSpeed = selectedSpeed,
                    onSpeedSelected = { speed ->
                        selectedSpeed = speed
                        deepAR?.let { ar ->
                            try {
                                Log.d("Speed", "Speed changed to: $speed")
                            } catch (e: Exception) {
                                Log.e("Speed", "Error setting speed: ${e.message}")
                            }
                        }
                    },
                    onDismiss = { showSpeedSheet = false }
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

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalGetImage::class)
fun bindCameraWithDeepAR(
    cameraProvider: ProcessCameraProvider,
    lensFacing: Int,
    lifecycleOwner: LifecycleOwner,
    deepAR: DeepAR?
) {
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
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalysis)
        Log.d("CameraX", "✅ Camera bound with DeepAR")
    } catch (exc: Exception) {
        Log.e("CameraX", "Use case binding failed", exc)
    }
}

@Composable
fun BeautySlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}