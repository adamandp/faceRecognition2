package com.ml.shubham0204.facenet_android.presentation.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.LocationServices
import com.ml.shubham0204.facenet_android.R
import com.ml.shubham0204.facenet_android.presentation.screens.detect_screen.DetectScreenViewModel
import com.ml.shubham0204.facenet_android.presentation.theme.FaceNetAndroidTheme
import java.io.File

private val cameraPermissionStatus = mutableStateOf(false)
private val cameraFacing = mutableIntStateOf(CameraSelector.LENS_FACING_BACK)
private lateinit var cameraPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectScreen(onOpenFaceListClick: (() -> Unit)) {
    val context = LocalContext.current
    val latitude = remember { mutableStateOf("N/A") }
    val longitude = remember { mutableStateOf("N/A") }

    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            getCurrentLocation(context) { lat, long ->
                latitude.value = lat.toString()
                longitude.value = long.toString()
            }
        }
    }

    FaceNetAndroidTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(),
                    title = {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    actions = {
                        IconButton(onClick = onOpenFaceListClick) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "Open Face List"
                            )
                        }
                        IconButton(onClick = {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            getCurrentLocation(
                                context
                            ) { lat, long ->
                                latitude.value = lat.toString()
                                longitude.value = long.toString()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Get Current Location"
                            )
                        }
                        IconButton(
                            onClick = {
                                if (cameraFacing.intValue == CameraSelector.LENS_FACING_BACK) {
                                    cameraFacing.intValue = CameraSelector.LENS_FACING_FRONT
                                } else {
                                    cameraFacing.intValue = CameraSelector.LENS_FACING_BACK
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                BottomAppBar(
                    contentColor = Color.White,

                    ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {

                        val scale = remember { mutableStateOf(1f) }
                        val animatedScale = animateFloatAsState(
                            targetValue = scale.value,
                            animationSpec = tween(durationMillis = 300)
                        )

                        IconButton(
                            onClick = {
                                scale.value = 0.8f
                                scale.value = 1f

                            },
                            modifier = Modifier.scale(animatedScale.value)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = "Open Camera",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) { ScreenUI() }
        }
    }
}

@Composable
private fun ScreenUI() {
    val viewModel: DetectScreenViewModel = hiltViewModel()
    Box {
        Camera(viewModel)
        DelayedVisibility(viewModel.getNumPeople() > 0) {
            val metrics by remember{ viewModel.faceDetectionMetricsState }
            Column {
                Text(
                    text = "Recognition on ${viewModel.getNumPeople()} face(s)",
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.weight(1f))
                metrics?.let {
                    Text(
                        text = "face detection: ${it.timeFaceDetection} ms" +
                                "\nface embedding: ${it.timeFaceEmbedding} ms\nvector search: ${it.timeVectorSearch} ms",
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        DelayedVisibility(viewModel.getNumPeople() == 0L) {
            Text(
                text = "No images in database",
                color = Color.White,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(Color.Blue, RoundedCornerShape(16.dp))
                    .padding(8.dp),
                textAlign = TextAlign.Center
            )
        }
        AppAlertDialog()
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun Camera(viewModel: DetectScreenViewModel) {
    val context = LocalContext.current
    val cameraFacing by remember { cameraFacing }
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { mutableStateOf<ImageCapture?>(null) }

    cameraPermissionStatus.value =
        ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                cameraPermissionStatus.value = true
            } else {
                camaraPermissionDialog()
            }
        }

    DelayedVisibility(cameraPermissionStatus.value) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    // Here ctx is Context, so we use it to create PreviewView
                    val previewView = PreviewView(ctx)
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    imageCapture.value = ImageCapture.Builder().build()

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.Builder().requireLensFacing(cameraFacing).build(),
                            preview,
                            imageCapture.value
                        )
                    } catch (exc: Exception) {
                        Log.e("CameraResult", "Use case binding failed", exc)
                    }

                    previewView
                },
                update = { previewView ->
                    // Rebinding the preview when switching cameras
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.Builder().requireLensFacing(cameraFacing).build(),
                            preview,
                            imageCapture.value
                        )
                    } catch (exc: Exception) {
                        Log.e("CameraResult", "Use case re-binding failed", exc)
                    }
                }
            )

            IconButton(
                onClick = {
                    val photoFile = File(
                        context.externalMediaDirs.first(),
                        "${System.currentTimeMillis()}.jpg"
                    )
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    imageCapture.value?.takePicture(
                        outputOptions, ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                Log.d("CameraResult", "Photo saved successfully at: ${photoFile.absolutePath}")
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Log.e("CameraResult", "Photo capture failed: ${exception.message}")
                            }
                        }
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Camera, contentDescription = "Capture Photo")
            }
        }
    }

//    DelayedVisibility(cameraPermissionStatus.value) {
//        AndroidView(
//            modifier = Modifier.fillMaxSize(),
//            factory = { FaceDetectionOverlay(lifecycleOwner, context, viewModel) },
//            update = { it.initializeCamera(cameraFacing) }
//        )
//    }

    DelayedVisibility(!cameraPermissionStatus.value) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Allow Camera Permissions\nThe app cannot work without the camera permission.",
                textAlign = TextAlign.Center
            )
            Button(
                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = "Allow")
            }
        }
    }

}

private fun camaraPermissionDialog() {
    createAlertDialog(
        "Camera Permission",
        "The app couldn't function without the camera permission.",
        "ALLOW",
        "CLOSE",
        onPositiveButtonClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
        onNegativeButtonClick = {
            // TODO: Handle deny camera permission action
            //       close the app
        }
    )
}

private fun getCurrentLocation(context: Context, onLocationReceived: (Double, Double) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        return
    }

    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
        if (location != null) {
            onLocationReceived(location.latitude, location.longitude,)
        }
    }
}