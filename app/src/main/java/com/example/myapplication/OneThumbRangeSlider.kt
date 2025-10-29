package com.example.myapplication

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneThumbRangeSlider(
    totalDuration: Float,
    clipEnd: Float,  // ✅ Receive state from parent
    onClipEndChange: (Float) -> Unit  // ✅ Callback to update parent
) {
    val clipStart = 0f // Fixed start

    // Disable interaction for the start thumb
    val disabledInteractionSource = remember { MutableInteractionSource() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF252525)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Recording Duration",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(20.dp))

            RangeSlider(
                value = clipStart..clipEnd,
                onValueChange = { range ->
                    // Only update clipEnd — clipStart is fixed
                    val newEnd = range.endInclusive.coerceIn(clipStart + 1f, totalDuration)
                    onClipEndChange(newEnd)  // ✅ Notify parent of change
                },
                valueRange = 0f..totalDuration,
                startInteractionSource = disabledInteractionSource, // Disable start thumb
                steps = 0,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Start: ${clipStart.toInt()}s", color = Color.Gray, fontSize = 14.sp)
                Text("End: ${clipEnd.toInt()}s", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(4.dp))

            Text(
                "Duration: ${(clipEnd - clipStart).toInt()}s",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}