package com.example.myapplication

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.State
import kotlinx.coroutines.Dispatchers

class CameraViewModel : ViewModel() {
    private val _recordedVideos = MutableStateFlow<List<Uri>>(emptyList())
    val recordedVideos: StateFlow<List<Uri>> = _recordedVideos

    private val _selectedMusic = mutableStateOf<MusicItem?>(null)
    val selectedMusic: State<MusicItem?> = _selectedMusic

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

    // ✅ ADD: Track timer active state
    private val _isTimerActive = MutableStateFlow(false)
    val isTimerActive: StateFlow<Boolean> = _isTimerActive

    private val _triggerStartRecording = MutableStateFlow(false)
    val triggerStartRecording: StateFlow<Boolean> = _triggerStartRecording
    private val _segmentDurations = MutableStateFlow<List<Float>>(emptyList())
    val segmentDurations: StateFlow<List<Float>> = _segmentDurations

    fun setSelectedMusic(item: MusicItem?) {
        _selectedMusic.value = item
    }
    fun addVideo(uri: Uri) {
        viewModelScope.launch {
            _recordedVideos.value = _recordedVideos.value + uri
            Log.d("CameraViewModel", "✅ Video added. Total: ${_recordedVideos.value.size}")
        }
    }

    fun clearVideos() {
        viewModelScope.launch {
            _recordedVideos.value = emptyList()
            _segmentDurations.value = emptyList()
        }
    }

    fun setRecordingState(isRecording: Boolean) {
        viewModelScope.launch {
            _isRecording.value = isRecording
            Log.d("CameraViewModel", "Recording state: $isRecording")
        }
    }

    fun setPausedState(isPaused: Boolean) {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            _isPaused.value = isPaused
            Log.d("CameraViewModel", "Paused state: $isPaused")
        }
    }

    // ✅ ADD: Timer active state
    fun setTimerActive(isActive: Boolean) {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            _isTimerActive.value = isActive
            Log.d("CameraViewModel", "Timer active: $isActive")
        }
    }
    // ✅ ADD: Trigger recording start
    fun triggerRecordingStart() {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            _triggerStartRecording.value = true
            Log.d("CameraViewModel", "Recording trigger activated")
        }
    }

    // ✅ ADD: Reset trigger
    fun resetRecordingTrigger() {
        viewModelScope.launch {
            _triggerStartRecording.value = false
            Log.d("CameraViewModel", "Recording trigger reset")
        }
    }

    fun addSegment(duration: Float) {
        _segmentDurations.value = _segmentDurations.value + duration
    }

    fun clearSegments() {
        _segmentDurations.value = emptyList()
    }

    fun deleteLastSegment() {
        viewModelScope.launch {
            if (_segmentDurations.value.isNotEmpty()) {
                _segmentDurations.value = _segmentDurations.value.dropLast(1)
                // ✅ Also remove the corresponding video
                if (_recordedVideos.value.isNotEmpty()) {
                    _recordedVideos.value = _recordedVideos.value.dropLast(1)
                }
                Log.d("CameraViewModel", "✅ Last segment deleted. Remaining: ${_segmentDurations.value.size}")
            }
        }
    }

    // ✅ ADD: Reset all states
    fun resetStates() {
        viewModelScope.launch {
            _isRecording.value = false
            _isPaused.value = false
            _isTimerActive.value = false
            _triggerStartRecording.value = false
            Log.d("CameraViewModel", "All states reset")
        }
    }
    fun resetAll() {
        viewModelScope.launch {
            _isRecording.value = false
            _isPaused.value = false
            _isTimerActive.value = false
            _triggerStartRecording.value = false
            _segmentDurations.value = emptyList()
            _recordedVideos.value = emptyList()
            Log.d("CameraViewModel", "✅ Complete reset - all data cleared")
        }
    }
}