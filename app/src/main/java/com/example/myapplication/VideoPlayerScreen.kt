package com.example.myapplication

import android.widget.VideoView
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun VideoPlayerScreen(videoPaths: List<String>) {
    var currentIndex by remember { mutableStateOf(0) }

    if (videoPaths.isNotEmpty()) {
        AndroidView(
            factory = { context ->
                val videoView = VideoView(context).apply {
                    setVideoPath(videoPaths[currentIndex])
                    setOnCompletionListener {
                        if (currentIndex < videoPaths.lastIndex) {
                            currentIndex++
                            setVideoPath(videoPaths[currentIndex])
                            start()
                        }
                    }
                    start()
                }
                videoView
            },
            update = { view ->
                if (!view.isPlaying) {
                    view.start()
                }
            }
        )
    } else {
        Text("No video segments available.")
    }
}
