package com.example.routes.ui.login

import android.Manifest
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class, ExperimentalPermissionsApi::class)
@Composable
fun PreviewViewComposable(
    onDismiss: () -> Unit,
    db: FirebaseFirestore
) {
    val friendsViewModel: FriendsViewModel = viewModel(
        factory = FriendsViewModelFactory(db)
    )
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }


    AndroidView(factory = { ctx ->
        val cameraExecutor = Executors.newSingleThreadExecutor()
        val previewView = androidx.camera.view.PreviewView(ctx).apply {
            scaleType = androidx.camera.view.PreviewView.ScaleType.FILL_CENTER
        }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = androidx.camera.core.Preview.Builder()
                .build()
                .apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyser { scannedResult ->
                        friendsViewModel.handleFriendshipCreation(scannedResult)
                        onDismiss() // Dismiss dialog after handling scan
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }, androidx.core.content.ContextCompat.getMainExecutor(ctx))

        previewView
    },
        modifier = Modifier.fillMaxSize()
    )
}



