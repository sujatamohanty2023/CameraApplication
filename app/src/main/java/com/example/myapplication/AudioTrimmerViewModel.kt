package com.example.myapplication

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class AudioTrimmerViewModel : ViewModel() {
    var selectedAudioUrl by mutableStateOf<String?>(null)
    var startMs by mutableStateOf(0L)
    var endMs by mutableStateOf(0L)
}
