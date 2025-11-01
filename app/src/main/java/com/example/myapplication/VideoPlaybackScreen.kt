@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.example.myapplication

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Composable
fun VideoPlaybackScreen(videoUris: List<Uri>) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }

    // Initialize player
    LaunchedEffect(Unit) {
        val exoPlayer = ExoPlayer.Builder(context).build().apply {
            val mediaItems = videoUris.map { MediaItem.fromUri(it) }
            addMediaItems(mediaItems)
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ALL
        }
        player = exoPlayer
    }

    // Observe playback progress
    LaunchedEffect(player) {
        while (true) {
            player?.let {
                currentPosition = it.currentPosition
                duration = it.duration.takeIf { d -> d > 0 } ?: duration
            }
            delay(500) // update every 0.5 sec
        }
    }

    // Handle lifecycle (pause when background)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                player?.playWhenReady = false
                isPlaying = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Release player
    DisposableEffect(Unit) {
        onDispose {
            player?.release()
            player = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL)
                }
            },
            update = { playerView ->
                playerView.player = player
            },
            modifier = Modifier.fillMaxSize()
        )

        // Back Button (Top Left)
        IconButton(
            onClick = {
                player?.release()
                coroutineScope.launch {
                    (context as? MainActivity)?.onBackPressedDispatcher?.onBackPressed()
                }
            },
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        // Play / Pause Button (Center)
        Surface(
            onClick = {
                isPlaying = !isPlaying
                player?.playWhenReady = isPlaying
            },
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.5f),
            modifier = Modifier
                .size(50.dp)
                .align(Alignment.Center)
        ) {
            Icon(
                painter = painterResource(id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.padding(10.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 80.dp) // move it slightly below the play/pause button
                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

// Helper function to format time in mm:ss
private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return String.format("%02d:%02d", minutes, seconds)
}
