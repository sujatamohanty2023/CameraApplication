// VideoTimerScreen.kt - Fixed Implementation
package com.example.myapplication

import android.content.Context
import android.media.ToneGenerator
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoTimerScreen(
    totalDuration: Float,
    lastSegmentDuration: Float = 0f, // ✅ Pass last segment duration
    onRecordStart: () -> Unit = {},
    onCancel: () -> Unit = {},
    onTimerSet: (Float, Float, Int) -> Unit = { _, _, _ -> }
) {
    var clipStart by remember { mutableFloatStateOf(0f) }
    // ✅ If lastSegmentDuration > 0, use it; otherwise use totalDuration
    var clipEnd by remember {
        mutableFloatStateOf(
            if (lastSegmentDuration > 0f) lastSegmentDuration
            else totalDuration.coerceAtLeast(5f)
        )
    }
    var countdown by remember { mutableIntStateOf(3) }

    val duration = (clipEnd - clipStart).roundToInt()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A1A),
                        Color(0xFF0D0D0D)
                    )
                )
            )
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp, top = 8.dp)
    ) {
        // Drag Handle
        Box(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .width(48.dp)
                .height(5.dp)
                .background(Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
        )

        Spacer(Modifier.height(8.dp))

        // Title with Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.timer),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Recording Timer",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(20.dp))

        // ✅ Pass state and callbacks to OneThumbRangeSlider
        OneThumbRangeSlider(
            totalDuration = totalDuration,
            clipEnd = clipEnd,
            onClipEndChange = { newEnd -> clipEnd = newEnd }
        )

        Spacer(Modifier.height(20.dp))

        // Countdown Selection Card
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
                    text = "Countdown Timer",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(3, 5, 10).forEach { sec ->
                        CountdownChip(
                            seconds = sec,
                            isSelected = countdown == sec,
                            onClick = { countdown = sec },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Start Button
        Button(
            onClick = {
                onTimerSet(clipStart, clipEnd, countdown)
                onRecordStart()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.timer),
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Start Timer Recording",
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Cancel Button
        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Cancel",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun CountdownChip(
    seconds: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Box(
        modifier = modifier
            .scale(scale)
            .height(56.dp)
            .background(
                color = if (isSelected) Color.White else Color(0xFF1A1A1A),
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color.White.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(14.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = onClick,
            color = Color.Transparent,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$seconds",
                    color = if (isSelected) Color.Black else Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "sec",
                    color = if (isSelected) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun CountdownOverlay(
    countdown: Int,
    context: Context,
    onCountdownFinish: () -> Unit
) {
    var currentCount by remember { mutableIntStateOf(countdown) }
    var showGo by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (currentCount > 0) 1f else 1.2f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val alpha by animateFloatAsState(
        targetValue = if (showGo) 0f else 1f,
        animationSpec = tween(durationMillis = 300)
    )

    LaunchedEffect(Unit) {
        val toneGen = ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)

        for (i in countdown downTo 1) {
            currentCount = i
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            delay(1000)
        }

        showGo = true
        toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
        delay(500)

        toneGen.release()
        onCountdownFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!showGo) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(scale)
                        .background(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                        .border(
                            width = 4.dp,
                            color = Color.White,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$currentCount",
                        color = Color.White,
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "GO!",
                    color = Color.White,
                    fontSize = 100.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.scale(scale)
                )
            }
        }
    }
}