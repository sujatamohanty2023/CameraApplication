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
import ai.deepar.ar.DeepARImageFormat
import ai.deepar.ar.AREventListener
import android.os.Build
import androidx.annotation.RequiresApi
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class MainActivity2 : ComponentActivity(), AREventListener {

    private var deepAR: DeepAR? = null
    private var surfaceView: DeepARSurfaceView? = null
    private var isSurfaceReady = false
    private var isInitialized = false

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

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.all { it.value }) {
                setupDeepAR()
            } else {
                finish()
            }
        }

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
                DeepARComposeScreen(sv)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        deepAR?.release()
        deepAR = null
    }

    // === DeepAR Listener ===

    override fun initialized() {
        Log.d("DeepAR", "DeepAR initialized callback")
        isInitialized = true
        maybeStartCamera()
    }

    private fun maybeStartCamera() {
        Log.d("DeepAR", "maybeStartCamera called: isInitialized=$isInitialized, isSurfaceReady=$isSurfaceReady")
        if (isInitialized && isSurfaceReady) {
            runOnUiThread {
                Log.d("DeepAR", "Starting camera")
                startCamera()
            }
        }
    }

    override fun error(errorType: ai.deepar.ar.ARErrorType?, error: String?) {
        Log.e("DeepAR", "Error: $errorType, $error")
    }

    override fun effectSwitched(slot: String?) {}
    override fun faceVisibilityChanged(faceVisible: Boolean) {}
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

            val preview = Preview.Builder().build()
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                processImageProxy(imageProxy)
            }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
                )

                /*preview.setSurfaceProvider { request ->
                    surfaceView?.let { sv ->
                        request.provideSurface(
                            sv.holder.surface,
                            ContextCompat.getMainExecutor(this)
                        ) {}
                    }
                }*/
                preview.setSurfaceProvider { request ->
                    surfaceView?.provideSurfaceToCamera(request)
                }
            } catch (e: Exception) {
                Log.e("DeepAR", "Camera bind error: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImageProxy(image: ImageProxy) {
        if (!isInitialized || deepAR == null) {
            image.close()
            return
        }

        val nv21 = yuv420ToNv21(image)
        val buffer = ByteBuffer.wrap(nv21)

        try {
            deepAR?.receiveFrame(
                buffer,
                image.width,
                image.height,
                0,
                false,
                DeepARImageFormat.YUV_NV21,
                1
            )
        } catch (e: Exception) {
            Log.e("DeepAR", "receiveFrame error: ${e.message}")
        }

        image.close()
    }

    private fun yuv420ToNv21(image: ImageProxy): ByteArray {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uvSize = uBuffer.remaining() + vBuffer.remaining()

        val nv21 = ByteArray(ySize + uvSize)
        yBuffer.get(nv21, 0, ySize)

        var index = ySize
        val rowStride = image.planes[1].rowStride
        val pixelStride = image.planes[1].pixelStride

        for (row in 0 until image.height / 2) {
            for (col in 0 until image.width / 2) {
                val pos = row * rowStride + col * pixelStride
                nv21[index++] = vBuffer.get(pos)
                nv21[index++] = uBuffer.get(pos)
            }
        }

        return nv21
    }
}

@Composable
fun DeepARComposeScreen(surfaceView: DeepARSurfaceView) {
    AndroidView(
        factory = { surfaceView },
        modifier = Modifier.fillMaxSize()
    )
}
