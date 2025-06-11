package com.example.myapplication

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CameraViewModel : ViewModel() {
    private val _recordedVideos = MutableStateFlow<List<Uri>>(emptyList())
    val recordedVideos: StateFlow<List<Uri>> = _recordedVideos

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