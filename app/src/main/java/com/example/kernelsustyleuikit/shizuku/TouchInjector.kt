package com.example.kernelsustyleuikit.shizuku

import android.os.SystemClock
import android.view.InputEvent
import android.view.MotionEvent
import com.example.kernelsustyleuikit.util.Reflection

/**
 * 触摸事件注入。
 * 反射注入隐藏的 InputManager.injectInputEvent（需 shell/root 身份才有 INJECT_EVENTS 权限）；
 * 默认走 Shizuku shell 的 `input swipe`（稳定，已具备 shell 权限）。
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

    /** 反射构造一串 DOWN→MOVE→UP 滑动事件并注入。 */
    fun swipe(
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        durationMs: Long,
    ) {
        val downTime = SystemClock.uptimeMillis()
        inject(MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x1, y1, 0f))
        val steps = 16
        for (i in 1..steps) {
            val t = downTime + durationMs / steps * i
            val p = i.toFloat() / steps
            val move = MotionEvent.obtain(
                downTime, t, MotionEvent.ACTION_MOVE,
                x1 + (x2 - x1) * p, y1 + (y2 - y1) * p, 0f
            )
            inject(move)
            SystemClock.sleep(durationMs / steps)
        }
        inject(
            MotionEvent.obtain(
                downTime, downTime + durationMs,
                MotionEvent.ACTION_UP, x2, y2, 0f
            )
        )
    }

    fun inject(event: InputEvent): Boolean {
        val im = getInputManager()
        val ok = Reflection.callMethod(
            im,
            "android.hardware.input.InputManager",
            "injectInputEvent",
            arrayOf(InputEvent::class.java, Int::class.javaPrimitiveType),
            arrayOf(event, MODE_ASYNC)
        ) as Boolean
        event.recycle()
        return ok
    }

    /** 走 Shizuku shell `input swipe`，稳定。 */
    suspend fun applySwipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long) {
        val cmd = "input swipe ${x1.toInt()} ${y1.toInt()} ${x2.toInt()} ${y2.toInt()} $durationMs"
        ShizukuManager.execShell(cmd)
    }
}