package com.example.kernelsustyleuikit.permission

data class PermissionState(
    val shizukuAvailable: Boolean = false,
    val shizukuGranted: Boolean = false,
    val rootAvailable: Boolean = false,
    val notification: Boolean = false,
) {
    val requiredGranted: Boolean
        get() = shizukuGranted || rootAvailable
}