package com.example.myapplication

import android.os.SystemClock
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun SegmentRecordButton(
    modifier: Modifier = Modifier,
    isRecording: Boolean,
    isPaused: Boolean,
    externalTriggerStart: Boolean = false,
    maxDurationMs: Long = 15000L,
    onStartRecording: () -> Unit = {},
    onPauseRecording: () -> Unit = {},
    onResumeRecording: () -> Unit = {},
    onStopRecording: () -> Unit = {},
    segmentDurations: List<Float>,
    onSegmentRecorded: (Float) -> Unit,
) {
    val segments = segmentDurations
    val whiteSeparators = remember { mutableStateListOf<Float>() }
    var currentSegmentProgress by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    var recordingJob by remember { mutableStateOf<Job?>(null) }

    fun stopRecording(reason: String = "Manual") {
        recordingJob?.cancel()
        recordingJob = null
        if (currentSegmentProgress > 0f) {
            onSegmentRecorded(currentSegmentProgress)
            whiteSeparators.add(segments.sum() * 360f)
            currentSegmentProgress = 0f
            onStopRecording()
        }
    }

    fun startSegment() {
        onStartRecording()
        recordingJob = coroutineScope.launch {
            val startTime = SystemClock.elapsedRealtime()
            while (isActive) {
                val now = SystemClock.elapsedRealtime()
                val elapsed = now - startTime
                currentSegmentProgress = (elapsed / maxDurationMs.toFloat()).coerceAtMost(1f)
                val totalProgress = segments.sum() + currentSegmentProgress
                if (totalProgress >= 1f) {
                    stopRecording("Auto - Max Duration Reached")
                    break
                }
                delay(16)
            }
        }
    }

    fun pauseSegment() {
        onPauseRecording()
        recordingJob?.cancel()
        recordingJob = null
        if (currentSegmentProgress > 0f) {
            onSegmentRecorded(currentSegmentProgress)
            whiteSeparators.add(segments.sum() * 360f)
            currentSegmentProgress = 0f
        }
        onStopRecording()
    }

    fun resumeSegment() {
        onResumeRecording()
        startSegment()
    }

    LaunchedEffect(externalTriggerStart) {
        if (externalTriggerStart && !isRecording && !isPaused) {
            startSegment()
        }
    }

    val outerSize by animateDpAsState(targetValue = if (!isRecording && !isPaused) 70.dp else 100.dp)
    val innerSize by animateDpAsState(targetValue = if (!isRecording && !isPaused) 50.dp else 70.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(outerSize).clickable {
            when {
                !isRecording -> startSegment()
                isRecording && !isPaused -> pauseSegment()
                isRecording && isPaused -> resumeSegment()
            }
        }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = if (!isRecording && !isPaused) 8.dp.toPx() else 12.dp.toPx()
            var startAngle = -90f

            segments.forEach { segment ->
                val sweep = 360f * segment
                drawArc(color = Color.Red, startAngle = startAngle, sweepAngle = sweep, useCenter = false, style = Stroke(width = stroke))
                startAngle += sweep
            }

            if (isRecording && !isPaused && currentSegmentProgress > 0f) {
                val sweep = 360f * currentSegmentProgress
                drawArc(color = Color.Red, startAngle = startAngle, sweepAngle = sweep, useCenter = false, style = Stroke(width = stroke))
                startAngle += sweep
            }

            if (!isRecording && !isPaused) {
                whiteSeparators.forEach { angle ->
                    drawArc(color = Color.LightGray, startAngle = -90f + angle, sweepAngle = 3f, useCenter = false, style = Stroke(width = stroke))
                }
            }

            val totalProgress = segments.sum() + currentSegmentProgress
            val remaining = 360f - (360f * totalProgress)
            if (remaining > 0f) {
                drawArc(color = Color.White, startAngle = startAngle, sweepAngle = remaining, useCenter = false, style = Stroke(width = stroke))
            }
        }

        Box(modifier = Modifier.size(innerSize).clip(CircleShape).background(if (isRecording) Color.Red else Color.White))
    }
}