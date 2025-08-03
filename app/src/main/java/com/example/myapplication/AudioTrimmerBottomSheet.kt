// File: AudioTrimmerBottomSheet.kt
package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTrimmerBottomSheet(
    item: MusicItem,
    onDismiss: () -> Unit,
    onTrimmed: (startMs: Long, endMs: Long) -> Unit
) {
    val totalDuration = remember { item.duration.durationSeconds().coerceAtLeast(1) }
    val minDuration = 1 // seconds
    val maxDuration = min(15, totalDuration) // max 15s or shorter if audio is short

    var selectedRange by remember {
        mutableStateOf(
            0f..min(maxDuration.toFloat(), 10f) // default 10s or shorter
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1F1F1F),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = item.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
            Text(
                text = item.creator,
                color = Color.Gray,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            WaveformTrimmer(
                selectedRange = selectedRange,
                totalDuration = totalDuration,
                onRangeChange = { selectedRange = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            val durationSec = (selectedRange.endInclusive - selectedRange.start).toInt()
            Text(
                text = "Duration: $durationSec seconds",
                color = Color.Yellow,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Button(
                onClick = {
                    val startMs = (selectedRange.start * 1000).toLong()
                    val endMs = (selectedRange.endInclusive * 1000).toLong()
                    onTrimmed(startMs, endMs)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC))
            ) {
                Text("Use this trimmed audio", color = Color.White)
            }
        }
    }
}

@Composable
fun WaveformTrimmer(
    selectedRange: ClosedFloatingPointRange<Float>,
    totalDuration: Int,
    onRangeChange: (ClosedFloatingPointRange<Float>) -> Unit
) {
    val barCount = 40
    val heights = remember(barCount) {
        List(barCount) { i ->
            val progress = i.toFloat() / barCount * totalDuration
            if (progress in selectedRange) (40..70).random()
            else (10..30).random()
        }
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            for (i in 0 until barCount) {
                val progress = i.toFloat() / barCount * totalDuration
                val isActive = progress in selectedRange
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(heights[i].dp)
                        .background(if (isActive) Color.White else Color.Gray)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        RangeSlider(
            value = selectedRange,
            onValueChange = onRangeChange,
            valueRange = 0f..totalDuration.toFloat(),
            steps = barCount - 2,
            colors = SliderDefaults.colors(
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.Gray,
                thumbColor = Color.White
            )
        )

        Text(
            "Trim: ${selectedRange.start.toInt()}s - ${selectedRange.endInclusive.toInt()}s",
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

// Helper: Convert "2:01" → 121 seconds
fun String.durationSeconds(): Int {
    return try {
        if (this.contains(":")) {
            val parts = this.split(":")
            parts[0].toInt() * 60 + parts[1].toInt()
        } else {
            this.toInt()
        }
    } catch (e: NumberFormatException) {
        30
    }
}