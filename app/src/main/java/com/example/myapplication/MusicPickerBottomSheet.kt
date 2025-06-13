import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter

@Composable
fun MusicPickerScreen(
    onDismiss: () -> Unit,
    onSongSelected: (SongItem) -> Unit) {
    var selectedSong by remember { mutableStateOf<SongItem?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    MusicPickerBottomSheet(
        onDismiss = onDismiss,
        onSongSelected = { song ->
            selectedSong = song
            mediaPlayer?.release() // Release previous player
            mediaPlayer = MediaPlayer().apply {
                setDataSource(song.audioUrl)
                prepare()
                start()
            }
            isPlaying = true
            onSongSelected(song) // notify your parent screen
        },
        selectedSong = selectedSong,
        isPlaying = isPlaying,
        onPausePlayClick = {
            if (isPlaying) {
                mediaPlayer?.pause()
            } else {
                mediaPlayer?.start()
            }
            isPlaying = !isPlaying
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPickerBottomSheet(
    onDismiss: () -> Unit,
    onSongSelected: (SongItem) -> Unit,
    selectedSong: SongItem?,
    isPlaying: Boolean,
    onPausePlayClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Search bar
            TextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Search music") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tabs
            val tabs = listOf("For you", "Trending", "Saved", "Original audio")
            var selectedTab by remember { mutableStateOf(0) }

            Row(
                Modifier.horizontalScroll(rememberScrollState())
            ) {
                tabs.forEachIndexed { index, tab ->
                    Button(
                        onClick = { selectedTab = index },
                        colors = if (selectedTab == index)
                            ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
                        else
                            ButtonDefaults.buttonColors(Color.Gray),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .height(36.dp)
                    ) {
                        Text(tab, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Horizontal carousel
            CarouselSection()

            Spacer(modifier = Modifier.height(16.dp))

            // Song list
            SongListSection(onSongSelected = onSongSelected)

            Spacer(modifier = Modifier.height(60.dp))
        }

        // Mini player at bottom
        if (selectedSong != null) {
            MiniPlayer(selectedSong, isPlaying, onPausePlayClick)
        }
    }
}

@Composable
fun CarouselSection() {
    val images = listOf(
        "https://via.placeholder.com/300x100.png?text=Heart+%26+Pain",
        "https://via.placeholder.com/300x100.png?text=Love+Beats",
        "https://via.placeholder.com/300x100.png?text=Trending+Hits"
    )
    val pagerState = rememberScrollState()

    Row(
        Modifier
            .horizontalScroll(pagerState)
            .fillMaxWidth()
    ) {
        images.forEach { imageUrl ->
            Image(
                painter = rememberAsyncImagePainter(imageUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(width = 300.dp, height = 120.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }
    }
}

data class SongItem(
    val title: String,
    val artist: String,
    val reelsCount: String,
    val duration: String,
    val imageUrl: String,
    val audioUrl: String
)

@Composable
fun SongListSection(onSongSelected: (SongItem) -> Unit) {
    val songs = listOf(
        SongItem("Meri Duniya", "Dhanu Singh KDM", "239K reels", "3:09", "https://via.placeholder.com/60", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
        SongItem("Teri Muskan Pe", "Apka Asis", "465K reels", "3:38", "https://via.placeholder.com/60", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
        SongItem("Cute Song", "Nitin Srivastava", "105K reels", "2:01", "https://via.placeholder.com/60", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3")
    )

    LazyColumn {
        items(songs) { song ->
            SongListItem(song = song, onSongSelected = onSongSelected)
        }
    }
}

@Composable
fun SongListItem(song: SongItem, onSongSelected: (SongItem) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onSongSelected(song) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberAsyncImagePainter(song.imageUrl),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("${song.artist} • ${song.reelsCount} • ${song.duration}", fontSize = 13.sp, color = Color.Gray)
        }
        IconButton(onClick = { }) {
            Icon(
                painter = painterResource(android.R.drawable.btn_star_big_off),
                contentDescription = "Save"
            )
        }
    }
}

@Composable
fun MiniPlayer(song: SongItem, isPlaying: Boolean, onPausePlayClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color.Black),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(song.imageUrl),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(song.artist, color = Color.Gray, fontSize = 12.sp)
            }
            IconButton(onClick = onPausePlayClick) {
                val icon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
                Icon(painter = painterResource(icon), contentDescription = "PlayPause", tint = Color.White)
            }
        }
    }
}
