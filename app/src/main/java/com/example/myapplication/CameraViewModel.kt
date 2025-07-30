package com.example.myapplication

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.State

class CameraViewModel : ViewModel() {
    private val _recordedVideos = MutableStateFlow<List<Uri>>(emptyList())
    val recordedVideos: StateFlow<List<Uri>> = _recordedVideos

    private val _selectedMusic = mutableStateOf<MusicItem?>(null)
    val selectedMusic: State<MusicItem?> = _selectedMusic

    fun setSelectedMusic(item: MusicItem?) {
        _selectedMusic.value = item
    }

    fun addVideo(uri: Uri) {
        viewModelScope.launch {
            _recordedVideos.value = _recordedVideos.value + uri
        }
    }

    fun clearVideos() {
        viewModelScope.launch {
            _recordedVideos.value = emptyList()
        }
    }
}