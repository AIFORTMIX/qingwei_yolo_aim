package com.example.kernelsustyleuikit.shizuku

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/**
 * Shizuku 管理：绑定服务、申请权限、以 shell 身份执行系统命令。
 */
object ShizukuManager {

    const val REQUEST_PERMISSION_CODE = 10001

    val isAvailable: Boolean get() = Shizuku.pingBinder()
    val isGranted: Boolean
        get() = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }

    fun connect() {
        if (isAvailable) {
            Shizuku.addBinderReceivedListenerSticky(emptyListener)
            try { Shizuku.bindRequestService(conn) } catch (_: Throwable) {}
        }
    }

    fun requestPermission() {
        if (isAvailable && !isGranted) {
            try { Shizuku.requestPermission(REQUEST_PERMISSION_CODE) } catch (_: Throwable) {}
        }
    }

    suspend fun execShell(cmd: String): String = withContext(Dispatchers.IO) {
        String(execShellBytes(cmd), Charsets.UTF_8)
    }

    suspend fun execShellBytes(cmd: String): ByteArray = withContext(Dispatchers.IO) {
        val process = try {
            Shizuku.newProcess(arrayOf("/system/bin/sh", "-c", cmd), null, null)
        } catch (e: Throwable) {
            return@withContext ("ERR:" + e.message).toByteArray()
        }
        val stdout = process.inputStream.readBytes()
        process.waitFor()
        stdout
    }

    fun addPermissionResultListener(l: Shizuku.OnRequestPermissionResultListener) {
        Shizuku.addRequestPermissionResultListener(l)
    }

    fun removePermissionResultListener(l: Shizuku.OnRequestPermissionResultListener) {
        Shizuku.removeRequestPermissionResultListener(l)
    }

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {}
        override fun onServiceDisconnected(name: ComponentName?) {}
    }

    private val emptyListener = Shizuku.OnBinderReceivedListener {
        if (isGranted) {
            try { Shizuku.bindRequestService(conn) } catch (_: Throwable) {}
        }
    }
}