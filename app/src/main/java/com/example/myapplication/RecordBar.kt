package com.example.myapplication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@Composable
fun RecordBar(
    navController: NavHostController,
    cameraViewmodel: CameraViewModel,
    selectedTab: String, onPhotoClick: () -> Unit, isRecording: Boolean, isPaused: Boolean,externalTriggerStart: Boolean,
    onStartRecording: () -> Unit, onPauseRecording: () -> Unit, onResumeRecording: () -> Unit, onStopRecording: () -> Unit
) {
    val segmentDurations by cameraViewmodel.segmentDurations.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (!isRecording && !isPaused) {
            if (segmentDurations.isEmpty()) {
                // Show Effects when no segments
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.effect),
                        contentDescription = "Effects",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Text("Effects", color = Color.White, fontSize = 12.sp)
                }
            } else {
                // Show Delete when segments exist
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        showDeleteDialog = true
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete),
                        contentDescription = "Delete",
                        tint = Color.Red,
                        modifier = Modifier.size(32.dp)
                    )
                    Text("Delete", color = Color.Red, fontSize = 12.sp)
                }
            }
        } else {
            Spacer(modifier = Modifier.width(0.dp))
        }

        Box(
            modifier = Modifier.clickable { if (selectedTab == "Photo") { onPhotoClick() } }
                .padding(bottom = if (!isRecording && !isPaused) 0.dp else 100.dp),
            contentAlignment = Alignment.Center
        ) {
            if (selectedTab == "Photo") {
                Icon(painter = painterResource(id = R.drawable.camera_shutter), contentDescription = "Capture Photo", modifier = Modifier.size(70.dp), tint = Color.Unspecified)
            } else if (selectedTab == "Clips") {
                SegmentRecordButton(
                    isRecording = isRecording, isPaused = isPaused, onStartRecording = onStartRecording,externalTriggerStart=externalTriggerStart,
                    onPauseRecording = onPauseRecording, onResumeRecording = onResumeRecording, onStopRecording = onStopRecording,segmentDurations = segmentDurations,                            // ✅ Pass segments
                    onSegmentRecorded = { cameraViewmodel.addSegment(it) },
                )
            }
        }

        if (!isRecording && !isPaused) {
            if (segmentDurations.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { }) {
                    Icon(
                        painter = painterResource(id = R.drawable.gallery),
                        contentDescription = "Gallery",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Text("Gallery", color = Color.White, fontSize = 12.sp)
                }
            }else{
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        cameraViewmodel.resetStates()
                        navController.navigate("video_playback_screen")
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check),
                        contentDescription = "Done",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Text("Done", color = Color.White, fontSize = 12.sp)
                }
            }
        } else {
            Spacer(modifier = Modifier.width(0.dp))
        }
    }
    if (showDeleteDialog) {
        DeleteSegmentDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                cameraViewmodel.deleteLastSegment()
                showDeleteDialog = false
            }
        )
    }
}