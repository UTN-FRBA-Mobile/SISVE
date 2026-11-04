package com.utn.sisve.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Observer
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.utn.sisve.R
import androidx.compose.ui.unit.dp

private enum class CameraPreviewError {
    NO_FRONT_CAMERA,
    UNAVAILABLE
}

@Composable
fun CameraScreen(
    onPhotoCaptured: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var previewError by remember { mutableStateOf<CameraPreviewError?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        previewError = null
    }

    LaunchedEffect(uiState.uploadSucceeded) {
        if (uiState.uploadSucceeded) {
            onPhotoCaptured()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        // Detectar permisos concedidos desde Ajustes sin volver a pedirlos automáticamente.
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED

                if (granted != hasPermission) {
                    hasPermission = granted
                    previewError = null
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.camera_title),
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = stringResource(R.string.camera_access_notice),
                style = MaterialTheme.typography.bodyMedium
            )

            if (!hasPermission) {
                // Falta permiso
                Text(
                    text = stringResource(R.string.camera_permission_required),
                    color = MaterialTheme.colorScheme.error
                )

                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text(stringResource(R.string.camera_grant_permission))
                }

                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                        )
                    }
                ) {
                    Text(stringResource(R.string.camera_open_settings))
                }
            } else if (previewError != null) {
                // Problemas con el preview/camara
                Text(
                    text = stringResource(
                        when (previewError) {
                            CameraPreviewError.NO_FRONT_CAMERA -> R.string.camera_no_front_camera
                            else -> R.string.camera_unavailable
                        }
                    ),
                    color = MaterialTheme.colorScheme.error
                )

                Button(
                    onClick = { previewError = null },
                    enabled = !uiState.isBusy
                ) {
                    Text(stringResource(R.string.camera_retry))
                }
            } else {
                // Camino normal
                FrontCameraPreview(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f),
                    onError = {
                        imageCapture = null
                        previewError = it
                    },
                    onReady = { imageCapture = it },
                    onReleased = { released ->
                        if (imageCapture === released) {
                            imageCapture = null
                        }
                    }
                )

                Text(stringResource(R.string.camera_position_face))

                uiState.error?.let { error ->
                    Text(
                        text = stringResource(
                            when (error) {
                                CameraCaptureError.CAPTURE -> R.string.camera_capture_failed
                                CameraCaptureError.STORAGE -> R.string.camera_save_failed
                                CameraCaptureError.CONNECTION -> R.string.camera_upload_connection
                                CameraCaptureError.UPLOAD -> R.string.camera_upload_failed
                            }
                        ),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = { imageCapture?.let(viewModel::capture) },
                    enabled = imageCapture != null &&
                        !uiState.isBusy && !uiState.uploadSucceeded,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (uiState.isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )

                        Spacer(Modifier.width(8.dp))
                    }

                    Text(
                        stringResource(
                            when {
                                uiState.isUploading -> R.string.camera_uploading
                                uiState.isCapturing -> R.string.camera_saving
                                else -> R.string.camera_take_photo
                            }
                        )
                    )
                }
            }
        }
    }
}

@SuppressLint("MissingPermission") // Solo se compone después de verificar el permiso.
@Composable
private fun FrontCameraPreview(
    modifier: Modifier = Modifier,
    onError: (CameraPreviewError) -> Unit,
    onReady: (ImageCapture) -> Unit,
    onReleased: (ImageCapture) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val currentOnError by rememberUpdatedState(onError)
    val currentOnReady by rememberUpdatedState(onReady)
    val currentOnReleased by rememberUpdatedState(onReleased)

    AndroidView(factory = { previewView }, modifier = modifier)

    DisposableEffect(lifecycleOwner, previewView) {
        val future = ProcessCameraProvider.getInstance(context)
        val preview = Preview.Builder().build().apply {
            surfaceProvider = previewView.surfaceProvider
        }
        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setJpegQuality(85)
            .build()

        var disposed = false
        var provider: ProcessCameraProvider? = null
        var camera: Camera? = null

        fun notifyIfReady() {
            if (
                !disposed && camera != null &&
                previewView.previewStreamState.value == PreviewView.StreamState.STREAMING
            ) {
                imageCapture.targetRotation = previewView.display?.rotation ?: Surface.ROTATION_0
                currentOnReady(imageCapture)
            }
        }

        val streamObserver = Observer<PreviewView.StreamState> { state ->
            if (!disposed) {
                if (state == PreviewView.StreamState.STREAMING) {
                    notifyIfReady()
                } else {
                    currentOnReleased(imageCapture)
                }
            }
        }

        previewView.previewStreamState.observe(lifecycleOwner, streamObserver)

        val observer = Observer<CameraState> { state ->
            if (!disposed && state.error != null) {
                currentOnError(CameraPreviewError.UNAVAILABLE)
            }
        }

        future.addListener(
            Runnable {
                if (disposed) {
                    return@Runnable
                }

                try {
                    // El listener corre con el Future completado: get() no espera la inicialización.
                    val initializedProvider = future.get()
                    provider = initializedProvider

                    if (!initializedProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                        currentOnError(CameraPreviewError.NO_FRONT_CAMERA)
                        return@Runnable
                    }

                    camera = initializedProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        imageCapture
                    )
                    camera.cameraInfo.cameraState.observe(lifecycleOwner, observer)
                    notifyIfReady()
                } catch (_: Exception) {
                    currentOnError(CameraPreviewError.UNAVAILABLE)
                }
            },
            ContextCompat.getMainExecutor(context)
        )

        onDispose {
            disposed = true
            camera?.cameraInfo?.cameraState?.removeObserver(observer)
            previewView.previewStreamState.removeObserver(streamObserver)
            currentOnReleased(imageCapture)
            provider?.unbind(preview, imageCapture)
        }
    }
}
