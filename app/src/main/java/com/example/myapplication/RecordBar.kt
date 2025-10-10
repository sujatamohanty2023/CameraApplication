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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun RecordBar(
    selectedTab: String, onPhotoClick: () -> Unit, isRecording: Boolean, isPaused: Boolean,externalTriggerStart: Boolean,
    onStartRecording: () -> Unit, onPauseRecording: () -> Unit, onResumeRecording: () -> Unit, onStopRecording: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (!isRecording && !isPaused) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { }) {
                Icon(painter = painterResource(id = R.drawable.effect), contentDescription = "Effects", tint = Color.White, modifier = Modifier.size(32.dp))
                Text("Effects", color = Color.White, fontSize = 12.sp)
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
                    onPauseRecording = onPauseRecording, onResumeRecording = onResumeRecording, onStopRecording = onStopRecording
                )
            }
        }

        if (!isRecording && !isPaused) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { }) {
                Icon(painter = painterResource(id = R.drawable.gallery), contentDescription = "Gallery", tint = Color.White, modifier = Modifier.size(32.dp))
                Text("Gallery", color = Color.White, fontSize = 12.sp)
            }
        } else {
            Spacer(modifier = Modifier.width(0.dp))
        }
    }
}