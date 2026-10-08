package com.example.kernelsustyleuikit.permission

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.UserManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.kernelsustyleuikit.root.RootManager
import com.example.kernelsustyleuikit.shizuku.ShizukuManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PermissionManager(context: Context) {

    private val appContext = context.applicationContext
    private val _state = MutableStateFlow(readState())

    val state: StateFlow<PermissionState> = _state.asStateFlow()

    fun refresh() {
        _state.value = readState()
    }

    /** 重新检测 root（仅当尚未确认有 root 时真正执行 su，避免重复弹窗）。 */
    suspend fun refreshRoot() {
        RootManager.detect()
        refresh()
    }

    /** 发起 Shizuku 授权申请。若已授权则无操作。 */
    fun requestShizuku() {
        ShizukuManager.requestPermission()
    }

    fun notificationRuntimePermission(): String? =
        android.Manifest.permission.POST_NOTIFICATIONS.takeIf {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        }

    fun notificationSettingsIntent(): Intent =
        Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, appContext.packageName)
        }

    fun shizukuLauncherIntent(): Intent? =
        appContext.packageManager.getLaunchIntentForPackage("moe.rikka.shizuku")

    private fun readState() = PermissionState(
        shizukuAvailable = ShizukuManager.isAvailable,
        shizukuGranted = ShizukuManager.isGranted,
        rootAvailable = RootManager.rootAvailable,
        notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            NotificationManagerCompat.from(appContext).areNotificationsEnabled() &&
                    isUserUnlocked()
        } else {
            hasPermission(android.Manifest.permission.POST_NOTIFICATIONS)
        },
    )

    /** 用户解锁判定，避免在 Direct Boot 阶段误判。 */
    private fun isUserUnlocked(): Boolean {
        return try {
            val um = appContext.getSystemService(UserManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                um.isUserUnlocked
            } else {
                true
            }
        } catch (_: Throwable) {
            true
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(appContext, permission) == PermissionChecker.PERMISSION_GRANTED
}