import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SpeedControlBottomSheet(
    selectedSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.3f, 0.5f, 1f, 2f, 3f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E))
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = "Speed",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Speed options grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            speeds.forEach { speed ->
                SpeedOption(
                    speed = speed,
                    isSelected = speed == selectedSpeed,
                    onClick = {
                        onSpeedSelected(speed)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Description text
        Text(
            text = when (selectedSpeed) {
                0.3f -> "0.3x - Very Slow Motion"
                0.5f -> "0.5x - Slow Motion"
                1f -> "1x - Normal Speed"
                2f -> "2x - Fast Motion"
                3f -> "3x - Very Fast Motion"
                else -> "1x - Normal Speed"
            },
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Done button
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF3B5C)
            ),
            shape = RoundedCornerShape(25.dp)
        ) {
            Text(
                text = "Done",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SpeedOption(
    speed: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        // Speed circle
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) Color(0xFFFF3B5C)
                    else Color(0xFF2C2C2E)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${speed}x",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Label
        Text(
            text = when (speed) {
                0.3f -> "Very Slow"
                0.5f -> "Slow"
                1f -> "Normal"
                2f -> "Fast"
                3f -> "Very Fast"
                else -> ""
            },
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}