//package com.example.myapplication
//
//import android.net.Uri
//import androidx.compose.foundation.layout.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import com.google.android.exoplayer2.ExoPlayer
//import com.google.android.exoplayer2.MediaItem
//import com.google.android.exoplayer2.ui.PlayerView
//import kotlinx.coroutines.MainScope
//import kotlinx.coroutines.launch
//
//@Composable
//fun VideoPlaybackScreen(videoUris: List<Uri>) {
//    val context = LocalContext.current
//    val coroutineScope = MainScope()
//    var player by remember { mutableStateOf<ExoPlayer?>(null) }
//
//    LaunchedEffect(Unit) {
//        val exoPlayer = ExoPlayer.Builder(context).build().apply {
//            // Convert URIs to MediaItems
//            val mediaItems = videoUris.map { MediaItem.fromUri(it) }
//            addMediaItems(mediaItems)
//            prepare()
//            playWhenReady = true
//        }
//
//        player = exoPlayer
//    }
//
//    DisposableEffect(Unit) {
//        onDispose {
//            player?.release()
//            player = null
//        }
//    }
//
//    Box(modifier = Modifier.fillMaxSize()) {
//        AndroidView(
//            factory = { ctx ->
//                PlayerView(ctx).apply {
//                    player = this@apply.player
//                    useController = false
//                }
//            },
//            modifier = Modifier.fillMaxWidth()
//                .aspectRatio(16f / 9f)
//                .align(Alignment.TopCenter)
//        )
//
//        IconButton(
//            onClick = {
//                player?.release()
//                coroutineScope.launch {
//                    (context as? MainActivity)?.onBackPressedDispatcher?.onBackPressed()
//                }
//            },
//            modifier = Modifier
//                .padding(16.dp)
//                .align(Alignment.TopStart)
//        ) {
//            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
//        }
//
//        if (player?.playbackState == ExoPlayer.STATE_ENDED) {
//            Text(
//                text = "Playback finished",
//                color = Color.White,
//                modifier = Modifier.align(Alignment.Center)
//            )
//        }
//    }
//}