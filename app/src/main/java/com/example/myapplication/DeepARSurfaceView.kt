package com.example.myapplication

import android.content.Context
import android.view.SurfaceHolder
import android.view.SurfaceView
import ai.deepar.ar.DeepAR
import android.util.Log
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.core.SurfaceRequest

class DeepARSurfaceView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private var deepAR: DeepAR? = null
    private var onSurfaceReady: (() -> Unit)? = null

    init {
        holder.addCallback(this)
        // Make sure surface is visible
        setZOrderMediaOverlay(false)
    }

    fun bindDeepAR(deepAR: DeepAR, onSurfaceReady: () -> Unit = {}) {
        Log.d("DeepAR", "🔗 Binding DeepAR to surface")
        this.deepAR = deepAR
        this.onSurfaceReady = onSurfaceReady
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d("DeepAR", "📱 Surface created")
        // Wait for surfaceChanged to get dimensions
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.d("DeepAR", "📐 Surface changed: ${width}x${height}, format: $format")
        if (width > 0 && height > 0) {
            deepAR?.let {
                try {
                    Log.d("DeepAR", "🎨 Setting render surface")
                    it.setRenderSurface(holder.surface, width, height)
                    onSurfaceReady?.invoke()
                    Log.d("DeepAR", "✅ Surface ready callback invoked")
                } catch (e: Exception) {
                    Log.e("DeepAR", "❌ Error setting render surface", e)
                }
            } ?: Log.e("DeepAR", "❌ DeepAR is null in surfaceChanged")
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d("DeepAR", "🗑️ Surface destroyed")
        deepAR?.setRenderSurface(null, 0, 0)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun provideSurfaceToCamera(request: SurfaceRequest) {
        // Not used anymore, kept for compatibility
        Log.d("DeepAR", "⚠️ provideSurfaceToCamera called but not used")
    }
}