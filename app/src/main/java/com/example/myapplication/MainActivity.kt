package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(color = MaterialTheme.colorScheme.background) {

                    // Move ViewModel declaration here
                    val viewModel: CameraViewModel = viewModel()
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "camera") {
                        composable("camera") {
                            CameraScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("video_playback_screen") {
                            VideoPlaybackScreen(viewModel.recordedVideos.value)
                        }
                    }
                }
            }
        }
    }
}