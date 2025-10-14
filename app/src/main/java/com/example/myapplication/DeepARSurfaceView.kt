package com.example.myapplication

import android.content.Context
import android.view.SurfaceHolder
import android.view.SurfaceView
import ai.deepar.ar.DeepAR
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.core.SurfaceRequest

class DeepARSurfaceView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private var deepAR: DeepAR? = null
    private var onSurfaceReady: (() -> Unit)? = null
    private var pendingSurfaceRequest: SurfaceRequest? = null

    init {
        holder.addCallback(this)
    }

    fun bindDeepAR(deepAR: DeepAR, onSurfaceReady: () -> Unit = {}) {
        this.deepAR = deepAR
        this.onSurfaceReady = onSurfaceReady
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun provideSurfaceToCamera(request: SurfaceRequest) {
        if (holder.surface.isValid) {
            request.provideSurface(holder.surface, context.mainExecutor) {}
        } else {
            // Save request for when surface becomes ready
            pendingSurfaceRequest = request
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun surfaceCreated(holder: SurfaceHolder) {
        deepAR?.let {
            try {
                it.setRenderSurface(holder.surface, width, height)
                onSurfaceReady?.invoke()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        pendingSurfaceRequest?.let { request ->
            request.provideSurface(holder.surface, context.mainExecutor) {}
            pendingSurfaceRequest = null
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        deepAR?.setRenderSurface(holder.surface, width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        deepAR?.setRenderSurface(null, 0, 0)
    }
}

