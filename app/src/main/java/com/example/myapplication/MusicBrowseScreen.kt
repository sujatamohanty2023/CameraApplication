package com.example.myapplication

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class MusicItem(
    val title: String,
    val creator: String,
    val views: String,
    val duration: String,
    val thumbnailUrl: String,
    val audioUrl: String
) {
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicBrowseScreen(
    onMusicSelected: (MusicItem) -> Unit, // Callback to pass selected music to CameraScreen
    onBackPressed: () -> Unit, // Callback to close the bottom sheet
    viewModel: AudioTrimmerViewModel
) {
    val musicItems = listOf(
        MusicItem(
            "Kyun Hote Kitni Pyari He", "Nithin Srivastva", "100K reels", "2:01",
            "https://picsum.photos/200/200?random=1", "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp3-file.mp3"
        ),
        MusicItem(
            "Main Teri Maa from Bhootu", "Preetam Maer", "928K reels", "1:21",
            "https://picsum.photos/200/200?random=2", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
        ),
        MusicItem(
            "Beti Meri Jaan LOFI (Daughter Beti Song)", "Vicky D Parekh", "218K reels", "2:50",
            "https://picsum.photos/200/200?random=3", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
        ),
        MusicItem(
            "Original audio", "bikash.studio", "2.3M reels", "0:58",
            "https://picsum.photos/200/200?random=4", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"
        ),
        MusicItem(
            "Teri Muskan Pe", "Apka Asis", "475K reels", "3:38",
            "https://picsum.photos/200/200?random=5", "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp3-file.mp3"
        ),
        MusicItem(
            "Laadla Beta...My Son", "Swasti Mehul", "1.5M reels", "4:57",
            "https://picsum.photos/200/200?random=6", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
        ),
        MusicItem(
            "Meri Duniya", "Dhanu Singh KDM", "253K reels", "3:09",
            "https://picsum.photos/200/200?random=7", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
        ),
        MusicItem(
            "Dheere Dheere", "raghavsachar", "1.6M reels", "0:50",
            "https://picsum.photos/200/200?random=8", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"
        )
    )

    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    // Set the first item as the default currentPlayingItem
    var currentPlayingItem by remember { mutableStateOf<MusicItem?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var showTrimmerSheet by remember { mutableStateOf(false) }
    var selectedItemForTrimmer by remember { mutableStateOf<MusicItem?>(null) }

    // ✅ Call the sheet **above Scaffold**
    if (showTrimmerSheet && selectedItemForTrimmer != null) {
        println("Showing AudioTrimmerBottomSheet for: ${selectedItemForTrimmer!!.title}")
        AudioTrimmerBottomSheet(
            item = selectedItemForTrimmer!!,
            onDismiss = {
                showTrimmerSheet = false
                selectedItemForTrimmer = null
            },
            onTrimmed = { startMs, endMs ->
                showTrimmerSheet = false
                selectedItemForTrimmer = null
                viewModel.startMs = startMs
                viewModel.endMs = endMs
                viewModel.selectedAudioUrl = selectedItemForTrimmer!!.audioUrl
            }
        )
    }

    // Initialize the first item and load its audio in a paused state
    LaunchedEffect(Unit) {
        if (musicItems.isNotEmpty()) {
            val firstItem = musicItems[0]
            currentPlayingItem = firstItem
            exoPlayer.setMediaItem(MediaItem.fromUri(firstItem.audioUrl))
            exoPlayer.prepare()
            // Do not call exoPlayer.play() to keep it paused
            isPlaying = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF121212),
        bottomBar = {
            currentPlayingItem?.let { item ->
                AudioPlayerBar(
                    item = item,
                    isPlaying = isPlaying,
                    onPlayPause = {
                        if (isPlaying) {
                            exoPlayer.pause()
                        } else {
                            exoPlayer.play()
                        }
                        isPlaying = !isPlaying
                    },
                    onMusicSelected = {
                        // Wait a frame to ensure it's dismissed before showing the new one
                        coroutineScope.launch {
                            delay(300) // wait for animation to finish
                            selectedItemForTrimmer = item
                            showTrimmerSheet = true
                        }
                    }

                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar with Reduced Height
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Search music", color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(20.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Gray,
                    unfocusedBorderColor = Color.Gray,
                    focusedContainerColor = Color(0xFF1F1F1F),
                    unfocusedContainerColor = Color(0xFF1F1F1F),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
            )

            // Custom Scrollable Buttons (Replacing Tabs) with No Internal Padding
            var selectedTab by remember { mutableStateOf(0) }
            val tabs = listOf("For you", "Trending", "Saved", "Original audio")
            val scrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (selectedTab == index) Color.White else Color.Transparent
                            )
                            .border(
                                width = 1.dp,
                                color = if (selectedTab == index) Color(0xFFFFFFFF) else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { selectedTab = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) Color.Black else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Music List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Banner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.DarkGray)
                    ) {
                        Text(
                            text = "Ganga Kinare (From \"Bhool Chuk Maaf\")",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Music Items
                items(musicItems) { item ->
                    MusicItemRow(
                        item = item,
                        isPlaying = item == currentPlayingItem && isPlaying,
                        onClick = {
                            if (currentPlayingItem != item) {
                                exoPlayer.stop()
                                exoPlayer.setMediaItem(MediaItem.fromUri(item.audioUrl))
                                exoPlayer.prepare()
                                exoPlayer.play()
                                currentPlayingItem = item
                                isPlaying = true
                            } else {
                                if (isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    exoPlayer.play()
                                }
                                isPlaying = !isPlaying
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MusicItemRow(
    item: MusicItem,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp)
            .then(
                if (isPlaying) {
                    Modifier
                        .border(
                            width = 2.dp,
                            color = Color(0xFFBB86FC),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(4.dp)
                } else {
                    Modifier
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = item.thumbnailUrl),
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${item.creator} • ${item.views} • ${item.duration}",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }

        IconButton(onClick = { onClick() }) {
            Icon(
                painter = painterResource(R.drawable.ic_music_note),
                contentDescription = "save",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun AudioPlayerBar(
    item: MusicItem,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onMusicSelected: () -> Unit
) {
    val backgroundColor = remember(item.title) {
        val hash = item.title.hashCode()
        Color(
            red = (hash and 0xFF0000 shr 16) % 256,
            green = (hash and 0x00FF00 shr 8) % 256,
            blue = (hash and 0x0000FF) % 256,
            alpha = 255
        )
    }

   Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = item.thumbnailUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = item.creator,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                // Play/Pause Button (Smaller)
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    val icon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = "PlayPause",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Back Arrow Button (Smaller)
                IconButton(
                    onClick = onMusicSelected,
                    modifier = Modifier
                        .padding(start = 20.dp, end = 8.dp)
                        .size(32.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = "Trim",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }


            }
        }
    }
}

@Composable
fun MusicAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFBB86FC),
            background = Color(0xFF121212),
            surface = Color(0xFF1F1F1F),
            onPrimary = Color.Black,
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}