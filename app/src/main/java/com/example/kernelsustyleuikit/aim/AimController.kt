package com.example.kernelsustyleuikit.aim

import com.example.kernelsustyleuikit.shizuku.ScreenCapturer
import com.example.kernelsustyleuikit.shizuku.TouchInjector
import com.example.kernelsustyleuikit.yolo.Detection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 瞄准主循环：截屏 → YOLO 识别人物 → 选距准星最近目标 → 计算位移 → 注入滑动。
 * detector 为 null 时只做截屏+滑动（配合独立识别线程）。
 */
class AimController(
    private val scope: CoroutineScope,
    private val config: AimConfig,
    private val detector: ((rgba: ByteArray, w: Int, h: Int) -> List<Detection>)? = null,
    private val onFrame: ((fps: Float, running: Boolean, targetLocked: Boolean) -> Unit)? = null
) {
    @Volatile private var running = false
    private var job: Job? = null
    @Volatile var lastFps: Float = 0f
        private set

    val isRunning get() = running

    fun start() {
        if (running) return
        running = true
        job = scope.launch(Dispatchers.IO) { runLoop() }
    }

    fun stop() {
        running = false
        job?.cancel()
        job = null
    }

    private suspend fun runLoop() {
        var frames = 0
        var lastTime = System.nanoTime()
        while (isActive && running) {
            try {
                val frame = ScreenCapturer.capture()
                val dets = detector?.invoke(frame.rgba, frame.width, frame.height) ?: emptyList()

                frames++
                val now = System.nanoTime()
                if (now - lastTime > 1_000_000_000L) {
                    lastFps = frames * 1_000_000_000f / (now - lastTime)
                    lastTime = now
                    frames = 0
                    onFrame?.invoke(lastFps, running, dets.isNotEmpty())
                }

                val dex = pickTarget(dets, frame.width, frame.height)
                aim(dex, frame.width, frame.height)
                if (config.cooldownMs > 0) delay(config.cooldownMs)
            } catch (e: Throwable) {
                delay(30)
            }
        }
    }

    fun pickTarget(dets: List<Detection>, w: Int, h: Int): Detection? {
        if (dets.isEmpty()) return null
        val cx = config.crossHairX * w
        val cy = config.crossHairY * h
        var best: Detection? = null
        var bestDist = Float.MAX_VALUE
        for (d in dets) {
            if (d.label.toIntOrNull() != config.targetClass) continue
            if (d.score < config.conf) continue
            val dx = d.cx * w - cx
            val dy = d.cy * h - cy
            val dist = dx * dx + dy * dy
            if (dist < bestDist) {
                bestDist = dist
                best = d
            }
        }
        return best
    }

    suspend fun aim(target: Detection?, w: Int, h: Int) {
        if (target == null) return
        val crossX = config.crossHairX * w
        val crossY = config.crossHairY * h
        val targetX = target.cx * w
        val targetY = (target.cy - target.h * config.verticalOffsetRatio) * h

        val dx = targetX - crossX
        val dy = targetY - crossY
        val deadPx = config.deadZone * w
        if (abs(dx) < deadPx && abs(dy) < deadPx) return

        var swipeDx = dx * config.mappingFactor
        var swipeDy = dy * config.mappingFactor

        val len = sqrt(swipeDx * swipeDx + swipeDy * swipeDy)
        if (len > config.maxStepPx) {
            val k = config.maxStepPx / len
            swipeDx *= k
            swipeDy *= k
        }

        val sx = crossX.coerceIn(0f, w.toFloat())
        val sy = crossY.coerceIn(0f, h.toFloat())
        val ex = (sx + swipeDx).coerceIn(0f, w.toFloat())
        val ey = (sy + swipeDy).coerceIn(0f, h.toFloat())

        TouchInjector.applySwipe(sx, sy, ex, ey, config.swipeDurationMs)
    }
}