package com.example.kernelsustyleuikit.ui.screen.home

import androidx.compose.runtime.Immutable

/** 首页 UI 状态：Shizuku 授权 + 模型 + 瞄准运行状态。 */
@Immutable
data class HomeUiState(
    val shizukuAvailable: Boolean = false,
    val shizukuGranted: Boolean = false,
    val rootAvailable: Boolean = false,
    val modelReady: Boolean = false,
    val modelError: String? = null,
    val aiming: Boolean = false,
    val fps: Float = 0f,
    val targetLocked: Boolean = false,
    val appVersion: String = "",
)

@Immutable
data class HomeActions(
    val onShizukuClick: () -> Unit,
    val onStartAim: () -> Unit,
    val onStopAim: () -> Unit,
    val onCameraClick: () -> Unit,
)