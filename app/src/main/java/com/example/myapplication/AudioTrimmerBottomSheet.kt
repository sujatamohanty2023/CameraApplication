package com.example.myapplication
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTrimmerBottomSheet(
    item: MusicItem,
    onDismiss: () -> Unit
) {
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

            WaveformTrimmer()

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC))
            ) {
                Text("Use this trimmed audio", color = Color.White)
            }
        }
    }
}
@Composable
fun WaveformTrimmer() {
    val barCount = 40
    val totalDuration = 40 // seconds
    val selectedRange = remember { mutableStateOf(10f..30f) }

    val heights = remember {
        List(barCount) { i ->
            if (i in selectedRange.value.start.toInt()..selectedRange.value.endInclusive.toInt())
                (40..70).random()
            else (10..30).random()
        }
    }

    Column {
        // Waveform Bars
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
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(heights[i].dp)
                        .background(
                            if (i in selectedRange.value.start.toInt()..selectedRange.value.endInclusive.toInt())
                                Color.White else Color.Gray
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Interactive Range Slider
        RangeSlider(
            value = selectedRange.value,
            onValueChange = {
                selectedRange.value = it
            },
            valueRange = 0f..totalDuration.toFloat(),
            steps = barCount - 2,
            colors = SliderDefaults.colors(
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.Gray,
                thumbColor = Color.White
            )
        )

        Text(
            "Trim Range: ${selectedRange.value.start.toInt()}s - ${selectedRange.value.endInclusive.toInt()}s",
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

