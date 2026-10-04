package com.example.kernelsustyleuikit.shizuku

import android.os.SystemClock
import android.view.InputEvent
import android.view.MotionEvent
import com.example.kernelsustyleuikit.root.RootManager
import com.example.kernelsustyleuikit.util.Reflection

/**
 * 触摸事件注入。
 * 权限策略（自动降级）：
 * 1. root：优先在本进程内直接反射调用 InputManager.injectInputEvent，从输入节点注入触摸；
 *    若应用进程无 INJECT_EVENTS 权限导致失败，则回退 `su -c input swipe`。
 * 2. 无 root：走 Shizuku shell 的 `input swipe`（shell 身份已具备注入权限）。
 */
object TouchInjector {

    private const val MODE_ASYNC = 0
    var inputManagerInstance: Any? = null
        private set

    private fun getInputManager(): Any {
        inputManagerInstance?.let { return it }
        val im = Class.forName("android.hardware.input.InputManager")
            .getMethod("getInstance")
            .invoke(null)
        inputManagerInstance = im
        return im
    }

    /** 本进程内反射构造 DOWN→MOVE→UP 滑动事件并注入（需应用进程具备 INJECT_EVENTS 权限）。 */
    fun swipe(
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        durationMs: Long,
    ): Boolean = try {
        val downTime = SystemClock.uptimeMillis()
        inject(MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x1, y1, 0))
        val steps = 16
        for (i in 1..steps) {
            val t = downTime + durationMs / steps * i
            val p = i.toFloat() / steps
            val move = MotionEvent.obtain(
                downTime, t, MotionEvent.ACTION_MOVE,
                x1 + (x2 - x1) * p, y1 + (y2 - y1) * p, 0
            )
            inject(move)
            SystemClock.sleep(durationMs / steps)
        }
        inject(
            MotionEvent.obtain(
                downTime, downTime + durationMs,
                MotionEvent.ACTION_UP, x2, y2, 0
            )
        )
        true
    } catch (_: Throwable) {
        false
    }

    fun inject(event: InputEvent): Boolean {
        val im = getInputManager()
        val ok = Reflection.callMethod(
            im,
            "android.hardware.input.InputManager",
            "injectInputEvent",
            arrayOf<Class<*>>(InputEvent::class.java, Int::class.javaPrimitiveType!!),
            arrayOf(event, MODE_ASYNC)
        ) as Boolean
        return ok
    }

    /** 执行一次滑动，自动选择 root 优先 / Shizuku 回退的注入方式。 */
    suspend fun applySwipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long) {
        if (RootManager.rootAvailable) {
            // 优先从输入节点直接注入；失败则用 su 执行 shell 命令兜底。
            val ok = swipe(x1, y1, x2, y2, durationMs)
            if (ok) return
            val cmd = "input swipe ${x1.toInt()} ${y1.toInt()} ${x2.toInt()} ${y2.toInt()} $durationMs"
            RootManager.execShell(cmd)
        } else {
            val cmd = "input swipe ${x1.toInt()} ${y1.toInt()} ${x2.toInt()} ${y2.toInt()} $durationMs"
            ShizukuManager.execShell(cmd)
        }
    }
}