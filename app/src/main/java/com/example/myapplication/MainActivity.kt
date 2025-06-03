package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.accompanist.permissions.
*import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                AppNavHost(navController)
            }
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController, startDestination = "camera") {
        composable("camera") {
            var canRecord by remember { mutableStateOf(false) }
            if (canRecord) {
                CameraScreen(navController)
            } else {
                RequestAudioAndCameraPermissions {
                    canRecord = true
                }
            }
            CameraScreen(navController)
        }
        composable(
            "videoPlayer?paths={paths}",
            arguments = listOf(navArgument("paths") { defaultValue = "" })
        ) { backStackEntry ->
            val pathsParam = backStackEntry.arguments?.getString("paths") ?: ""
            val paths = pathsParam.split(",").filter { it.isNotEmpty() }
            VideoPlayerScreen(paths)
        }
    }

}
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestAudioAndCameraPermissions(onPermissionsGranted: () -> Unit) {
    val cameraPermissionState = rememberPermissionState(permission = android.Manifest.permission.CAMERA)
    val audioPermissionState = rememberPermissionState(permission = android.Manifest.permission.RECORD_AUDIO)

    LaunchedEffect(key1 = Unit) {
        cameraPermissionState.launchPermissionRequest()
        audioPermissionState.launchPermissionRequest()
    }

    when {
        cameraPermissionState.status.isGranted && audioPermissionState.status.isGranted -> {
            // Permissions granted, call your recording start here
            onPermissionsGranted()
        }

        cameraPermissionState.status.shouldShowRationale ||
                audioPermissionState.status.shouldShowRationale -> {
            // Show UI explaining why permissions are needed
            Text("Camera and Audio permissions are needed to record video.")
        }

        !cameraPermissionState.status.isGranted || !audioPermissionState.status.isGranted -> {
            // Permissions not granted, maybe show button to request
            Button(onClick = {
                cameraPermissionState.launchPermissionRequest()
                audioPermissionState.launchPermissionRequest()
            }) {
                Text("Request Permissions")
            }
        }
    }
}
