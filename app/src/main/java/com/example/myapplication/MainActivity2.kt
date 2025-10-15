package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import ai.deepar.ar.DeepAR
import ai.deepar.ar.AREventListener
import ai.deepar.ar.DeepARImageFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.concurrent.Executors

class MainActivity2 : ComponentActivity(), AREventListener {

    private var deepAR: DeepAR? = null
    private var surfaceView: DeepARSurfaceView? = null
    private var isSurfaceReady = false
    private var isInitialized = false
    private var currentEffectSlot = "effects"

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!allGranted) {
            requestPermissionsLauncher.launch(permissions)
        } else {
            setupDeepAR()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.all { it.value }) {
                setupDeepAR()
            } else {
                finish()
            }
        }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun setupDeepAR() {
        val deepar = DeepAR(this)
        deepar.setLicenseKey("83f789a8e6168a14db7bf506010e82c331d90f3f35296e1b63abf6c62a1e6e975700e5ad360c6cf5")
        deepar.initialize(this, this)

        surfaceView = DeepARSurfaceView(this)
        surfaceView?.bindDeepAR(deepar) {
            isSurfaceReady = true
            maybeStartCamera()
        }

        deepAR = deepar

        setContent {
            surfaceView?.let { sv ->
                DeepARComposeScreen(
                    surfaceView = sv,
                    onApplyEffect = { effectPath -> applyEffect(effectPath) },
                    onClearEffect = { clearEffect() }
                )
            }
        }
    }

    // Apply effect/filter
    private fun applyEffect(effectPath: String) {
        deepAR?.let { ar ->
            try {
                Log.d("DeepAR", "Attempting to apply effect: $effectPath")
                ar.switchEffect(currentEffectSlot, effectPath)
            } catch (e: Exception) {
                Log.e("DeepAR", "Error applying effect: ${e.message}", e)
            }
        } ?: Log.e("DeepAR", "DeepAR instance is null!")
    }

    // Clear current effect
    private fun clearEffect() {
        deepAR?.let { ar ->
            try {
                Log.d("DeepAR", "Clearing effect")
                ar.switchEffect(currentEffectSlot, null as String?)
            } catch (e: Exception) {
                Log.e("DeepAR", "Error clearing effect: ${e.message}", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        deepAR?.release()
        deepAR = null
    }

    // === DeepAR Listener ===

    @RequiresApi(Build.VERSION_CODES.P)
    override fun initialized() {
        Log.d("DeepAR", "✅ DeepAR initialized successfully")
        isInitialized = true
        maybeStartCamera()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun maybeStartCamera() {
        Log.d("DeepAR", "maybeStartCamera - isInitialized=$isInitialized, isSurfaceReady=$isSurfaceReady")
        if (isInitialized && isSurfaceReady) {
            runOnUiThread {
                Log.d("DeepAR", "🎥 Starting camera")
                startCamera()
            }
        }
    }

    override fun error(errorType: ai.deepar.ar.ARErrorType?, error: String?) {
        Log.e("DeepAR", "❌ DeepAR Error: $errorType - $error")
    }

    override fun effectSwitched(slot: String?) {
        Log.d("DeepAR", "✨ Effect switched in slot: $slot")
    }

    override fun faceVisibilityChanged(faceVisible: Boolean) {
        Log.d("DeepAR", "👤 Face visible: $faceVisible")
    }

    override fun frameAvailable(frame: android.media.Image) {}
    override fun imageVisibilityChanged(gameObjectName: String?, imageVisible: Boolean) {}
    override fun screenshotTaken(bitmap: android.graphics.Bitmap) {}
    override fun shutdownFinished() {}
    override fun videoRecordingFailed() {}
    override fun videoRecordingFinished() {}
    override fun videoRecordingPrepared() {}
    override fun videoRecordingStarted() {}

    // === CameraX Integration ===

    @RequiresApi(Build.VERSION_CODES.P)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // ONLY ImageAnalysis - DeepAR handles rendering
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()

            imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                processImageProxy(imageProxy)
            }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                // IMPORTANT: Only bind ImageAnalysis, NOT Preview
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, imageAnalysis
                )
                Log.d("DeepAR", "✅ Camera bound successfully")
            } catch (e: Exception) {
                Log.e("DeepAR", "❌ Camera bind error: ${e.message}", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val buffer = mediaImage.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            val width = mediaImage.width
            val height = mediaImage.height
            val rotation = imageProxy.imageInfo.rotationDegrees
            val timestamp = imageProxy.imageInfo.timestamp

            Handler(Looper.getMainLooper()).post {
                try {
                    deepAR?.receiveFrame(
                        buffer,
                        width,
                        height,
                        rotation,  // rotation
                        true,  // mirror for front camera
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
}

// Filter data class
data class FilterItem(
    val name: String,
    val path: String? // null means no filter
)

@Composable
fun DeepARComposeScreen(
    surfaceView: DeepARSurfaceView?,
    onApplyEffect: (String) -> Unit,
    onClearEffect: () -> Unit
) {
    // DeepAR effect filters with CORRECT path format
    val filters = remember {
        listOf(
            FilterItem("None", null),
            FilterItem("Viking", "file:///android_asset/viking_helmet.deepar"),
            FilterItem("Makeup", "file:///android_asset/MakeupLook.deepar"),
            FilterItem("Split View", "file:///android_asset/Split_View_Look.deepar"),
            FilterItem("Emotions", "file:///android_asset/Emotions_Exaggerator.deepar"),
            FilterItem("Meter", "file:///android_asset/Emotion_Meter.deepar"),
            FilterItem("Stallone", "file:///android_asset/Stallone.deepar"),
            FilterItem("Flowers", "file:///android_asset/flower_face.deepar"),
            FilterItem("Galaxy", "file:///android_asset/galaxy_background.deepar"),
            FilterItem("Humanoid", "file:///android_asset/Humanoid.deepar"),
            FilterItem("Neon Devil", "file:///android_asset/Neon_Devil_Horns.deepar"),
            FilterItem("Ping Pong", "file:///android_asset/Ping_Pong.deepar"),
            FilterItem("Hearts", "file:///android_asset/Pixel_Hearts.deepar"),
            FilterItem("Snail", "file:///android_asset/Snail.deepar"),
            FilterItem("Hope", "file:///android_asset/Hope.deepar"),
            FilterItem("Vendetta", "file:///android_asset/Vendetta_Mask.deepar"),
            FilterItem("Fire", "file:///android_asset/Fire_Effect.deepar"),
            FilterItem("Burning", "file:///android_asset/burning_effect.deepar"),
            FilterItem("Elephant", "file:///android_asset/Elephant_Trunk.deepar")
        )
    }

    var selectedFilter by remember { mutableStateOf(filters[0]) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview with DeepAR
        surfaceView?.let { sv ->
            AndroidView(
                factory = { sv },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Filter selector at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp)
        ) {
            Text(
                text = "Select Filter",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filter ->
                    FilterButton(
                        filter = filter,
                        isSelected = selectedFilter == filter,
                        onClick = {
                            selectedFilter = filter
                            Log.d("DeepAR", "Filter button clicked: ${filter.name}")
                            if (filter.path != null) {
                                onApplyEffect(filter.path)
                            } else {
                                onClearEffect()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FilterButton(
    filter: FilterItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color(0xFF2196F3) else Color.Gray.copy(alpha = 0.3f),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Text(
            text = filter.name,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}