package com.example.kernelsustyleuikit.ui.screen.camera

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kernelsustyleuikit.ui.LocalUiMode
import com.example.kernelsustyleuikit.ui.UiMode
import com.example.kernelsustyleuikit.ui.navigation3.LocalNavigator
import com.example.kernelsustyleuikit.ui.viewmodel.CameraViewModel
import com.example.kernelsustyleuikit.ui.viewmodel.cameraViewModelFactory

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val navigator = LocalNavigator.current
    val vmFactory = remember(app) { cameraViewModelFactory(app) }
    val viewModel: CameraViewModel = viewModel(factory = vmFactory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var granted by remember { mutableStateOf(hasCameraPermission(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok -> granted = ok }

    // 初始化模型
    LaunchedEffect(Unit) { viewModel.start() }

    // key 上依赖 granted：授权完成后自动绑定相机
    LifecycleResumeEffect(granted) {
        viewModel.onPermissionResult(granted)
        if (granted) viewModel.bindCamera(LocalLifecycleOwner.current)
        onPauseOrDispose { }
    }

    val actions = CameraActions(
        onBack = { navigator.pop() },
        onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> CameraScreenMiuix(uiState, actions)
        UiMode.Material -> CameraScreenMiuix(uiState, actions)
    }
}

private fun hasCameraPermission(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED