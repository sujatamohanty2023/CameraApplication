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
import androidx.camera.lifecycle.ProcessCameraProvider
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

    // ✅ Collect states from ViewModel
    val recordedVideos by viewModel.recordedVideos.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val isTimerActive by viewModel.isTimerActive.collectAsState()
    val triggerStartRecording by viewModel.triggerStartRecording.collectAsState()

    val tabs = listOf("Photo", "Clips", "Live")
    var selectedTab by remember { mutableStateOf("Clips") }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    var currentRecordingFile by remember { mutableStateOf<File?>(null) }

    //var showDeleteDialog by remember { mutableStateOf(false) }

    var showTimerPicker by remember { mutableStateOf(false) }
    var selectedTimerSeconds by remember { mutableIntStateOf(0) }
    var timerAnchor by remember { mutableStateOf(Offset.Zero) }

    var showVideoTimerScreen by remember { mutableStateOf(false) }
    var recordingStartTime by remember { mutableFloatStateOf(0f) }
    var recordingEndTime by remember { mutableFloatStateOf(15f) }
    var timerCountdown by remember { mutableIntStateOf(3) }

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
    var showBeautySheet by remember { mutableStateOf(false) }
    var smoothness by remember { mutableFloatStateOf(0.9f) }
    var whiteness by remember { mutableFloatStateOf(0.9f) }
    var redness by remember { mutableFloatStateOf(0.9f) }

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
    fun stopDeepARRecording(file: File) {
        try {
            deepAR?.stopVideoRecording()
            // Stop music immediately
            if (isMusicPlaying) {
                exoPlayer.pause()
                isMusicPlaying = false
            }

            autoStopJob?.cancel()
            autoStopJob = null

            // ✅ Update via ViewModel
            viewModel.setRecordingState(false)
            viewModel.setPausedState(false)
            viewModel.setTimerActive(false)

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    if (file.exists()) {
                        val fileSize = file.length()
                        if (fileSize > 0) {
                            val uri = Uri.fromFile(file)
                            viewModel.addVideo(uri)
                        } else {
                            file.delete()
                        }
                    } else {
                        Log.e("DeepAR", "❌ File not found: ${file.absolutePath}")
                        Toast.makeText(context, "Recording failed - file not saved", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("DeepAR", "Error checking file", e)
                } finally {
                    currentRecordingFile = null
                }
            }, 1000)

        } catch (e: Exception) {
            Log.e("DeepAR", "Error stopping DeepAR recording: ${e.message}", e)
            viewModel.setRecordingState(false)
            viewModel.setPausedState(false)
            viewModel.setTimerActive(false)
            currentRecordingFile = null
        }
    }

    fun startVideoRecordingDeepAR() {
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
                        if (isRecording && currentRecordingFile != null) {
                            stopDeepARRecording(currentRecordingFile!!)
                        }
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
                        if (isRecording && currentRecordingFile != null) {
                            stopDeepARRecording(currentRecordingFile!!)
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
        if (triggerStartRecording && !isRecording) {
            startVideoRecordingDeepAR()
            viewModel.resetRecordingTrigger() // Reset immediately after starting
        }
    }

    // ✅ Reset trigger when screen appears
    LaunchedEffect(Unit) {
        // Reset recording trigger when screen appears
        viewModel.resetRecordingTrigger()

        // ✅ If coming back without videos, reset everything
        if (recordedVideos.isEmpty()) {
            viewModel.resetAll()
        } else {
            // Just reset recording states but keep segments
            viewModel.resetStates()
        }

        Log.d("CameraScreen", "Screen initialized")
    }

    DisposableEffect(Unit) {
        onDispose {
            // Clean up when leaving screen
            exoPlayer.release()
            deepAR?.release()

            // Reset recording states when leaving
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
                deepAR
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
                    onClick = { showVideoTimerScreen = true }
                )
                SideControl(icon = R.drawable.speed, label = "Speed", onClick = {})
                SideControl(icon = R.drawable.template, label = "Template", onClick = {})

                // ✅ Add Delete button (only show when segments exist)
               /* if (recordedVideos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SideControl(
                        icon = R.drawable.ic_delete,
                        label = "Delete",
                        onClick = { showDeleteDialog = true },
                        iconTint = Color.Red,
                        labelColor = Color.Red
                    )
                }*/
              /*  if (recordedVideos.isNotEmpty()) {
                    SideControl(icon = R.drawable.ic_check, label = "Done",
                        onClick = {
                            // Reset recording states before navigation
                            viewModel.resetStates()
                            navController.navigate("video_playback_screen")
                        })
                }*/
            }
        }
        /*if (showDeleteDialog) {
            DeleteSegmentDialog(
                onDismiss = { showDeleteDialog = false },
                onConfirm = {
                    viewModel.deleteLastSegment()
                    showDeleteDialog = false
                    Toast.makeText(context, "Segment deleted", Toast.LENGTH_SHORT).show()
                }
            )
        }*/

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
                    viewModel.setTimerActive(false)
                },
                containerColor = Color(0xFF101010),
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                VideoTimerScreen(
                    totalDuration = 60f,
                    onRecordStart = {
                        showVideoTimerScreen = false
                        viewModel.setTimerActive(true)
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

// Add countdown overlay on main camera screen
        if (isTimerActive && timerCountdown > 0) {
            CountdownOverlay(
                countdown = timerCountdown,
                context = context,
                onCountdownFinish = {
                    timerCountdown = 0
                    viewModel.setTimerActive(false)
                    viewModel.triggerRecordingStart()
                }
            )
        }

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
                    externalTriggerStart = triggerStartRecording ,
                    onStartRecording = {
                        startVideoRecordingDeepAR()
                        viewModel.resetRecordingTrigger()
                    },
                    onPauseRecording = {
                        currentRecordingFile?.let { file ->
                            stopDeepARRecording(file)
                        } ?: run {
                            Log.e("DeepAR", "No recording in progress!")
                            viewModel.setRecordingState(false)
                        }
                        autoStopJob?.cancel()
                        if (isMusicPlaying) {
                            exoPlayer.pause()
                            isMusicPlaying = false
                        }
                        viewModel.setPausedState(true)
                    },
                    onResumeRecording = {
                        startVideoRecordingDeepAR()
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
                                            currentRecordingFile?.let { file ->
                                                stopDeepARRecording(file)
                                            } ?: run {
                                                Log.e("DeepAR", "No recording in progress!")
                                                viewModel.setRecordingState(false)
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("AudioTrimmer", "Error resuming audio", e)
                            }
                        }
                        viewModel.setPausedState(false)
                    },
                    onStopRecording = {
                        /*recording?.stop()
                        recording = null*/
                        currentRecordingFile?.let { file ->
                            stopDeepARRecording(file)
                        } ?: run {
                            Log.e("DeepAR", "No recording in progress!")
                            viewModel.setRecordingState(false)
                        }
                        viewModel.setRecordingState(false)
                        viewModel.setPausedState(false)
                        autoStopJob?.cancel()
                        autoStopJob = null
                        if (isMusicPlaying) {
                            exoPlayer.pause()
                            isMusicPlaying = false
                        }
                        viewModel.setTimerActive(false)
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
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector,imageAnalysis)
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




