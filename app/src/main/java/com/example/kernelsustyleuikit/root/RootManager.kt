package com.example.kernelsustyleuikit.root

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Root 权限管理：检测是否具备 root，并以 `su` 身份执行系统命令。
 * 优先于 Shizuku：若设备已 root，则直接使用 root 提权执行，否则回退 Shizuku。
 */
object RootManager {

    @Volatile
    var rootAvailable: Boolean = false
        private set

    private val detectLock = Mutex()

    /** 检测 root 是否可用（以 `su -c id` 能否得到 uid=0 为准），不阻塞主线程。
     *  幂等且并发安全：已确认有 root 时直接返回，避免重复触发 su 授权弹窗。 */
    suspend fun detect(): Boolean = withContext(Dispatchers.IO) {
        if (rootAvailable) return@withContext true
        detectLock.withLock {
            if (rootAvailable) return@withContext true
            val ok = try {
                val process = ProcessBuilder("/system/bin/sh", "-c", "su -c id")
                    .redirectErrorStream(true)
                    .start()
                val out = BufferedReader(InputStreamReader(process.inputStream)).readText()
                process.waitFor()
                out.trim().startsWith("uid=0")
            } catch (_: Throwable) {
                false
            }
            rootAvailable = ok
            ok
        }
    }

    /** 主动重置 root 状态（如应用重新获取授权后），下次 detect 重新检测。 */
    fun reset() {
        rootAvailable = false
    }

    suspend fun execShell(cmd: String): String = withContext(Dispatchers.IO) {
        String(execShellBytes(cmd), Charsets.UTF_8)
    }

    suspend fun execShellBytes(cmd: String): ByteArray = withContext(Dispatchers.IO) {
        val process = try {
            ProcessBuilder("/system/bin/sh", "-c", "su -c \"$cmd\"")
                .redirectErrorStream(true)
                .start()
        } catch (e: Throwable) {
            return@withContext ("ERR:" + e.message).toByteArray()
        }
        val out = process.inputStream.readBytes()
        process.waitFor()
        out
    }
}