package com.example.kernelsustyleuikit.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.Immutable
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.kernelsustyleuikit.aim.AimRuntime
import com.example.kernelsustyleuikit.yolo.Detection
import com.example.kernelsustyleuikit.yolo.ModelRuntime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

fun cameraViewModelFactory(application: Application): ViewModelProvider.Factory = viewModelFactory {
    initializer { CameraViewModel(application) }
}

@Immutable
data class CameraUiState(
    val permissionGranted: Boolean = false,
    val modelError: String? = null,
    val cameraError: String? = null,
    val running: Boolean = false,
    val fps: Float = 0f,
    val detections: List<Detection> = emptyList(),
    val frame: Bitmap? = null,
)

class CameraViewModel(context: Context) : ViewModel() {

    private val appContext = context.applicationContext
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private val mainExecutor =
        java.util.concurrent.Executor { android.os.Handler(android.os.Looper.getMainLooper()).post(it) }
    private val bound = AtomicBoolean(false)
    @Volatile private var provider: ProcessCameraProvider? = null

    /** 初始化模型，成功后置 running 状态。 */
    fun start() {
        viewModelScope.launch {
            val err = ModelRuntime.ensure(appContext)
            if (err != null) {
                _uiState.value = _uiState.value.copy(modelError = err)
            } else {
                _uiState.value = _uiState.value.copy(modelError = null, running = true)
            }
        }
    }

    /** 绑定后置摄像头 + ImageAnalysis，实时检测并发布结果。 */
    fun bindCamera(lifecycleOwner: LifecycleOwner) {
        if (bound.get() || !_uiState.value.permissionGranted) return
        val future = ProcessCameraProvider.getInstance(appContext)
        future.addListener({
            try {
                val p = future.get()
                provider = p
                p.unbindAll()
                val analysis = buildAnalysis()
                p.bindToLifecycle(lifecycleOwner, backSelector, analysis)
                bound.set(true)
                _uiState.value = _uiState.value.copy(cameraError = null)
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(cameraError = e.message ?: "相机启动失败")
            }
        }, mainExecutor)
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(permissionGranted = granted)
    }

    @OptIn(ExperimentalGetImage::class)
    private fun buildAnalysis(): ImageAnalysis {
        val obj = ImageAnalysis.Builder()
            .setTargetResolution(Size(640, 480))
            .setTargetRotation(Surface.ROTATION_0)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        obj.setAnalyzer(analysisExecutor) { proxy ->
            analyze(proxy)
        }
        return obj
    }

    private fun analyze(proxy: ImageProxy) {
        try {
            val bmp = proxy.toBitmap() // 直接转为 ARGB_8888
            val w = bmp.width
            val h = bmp.height
            val pixels = IntArray(w * h)
            bmp.getPixels(pixels, 0, w, 0, 0, w, h)
            val rgba = ByteArray(w * h * 4)
            var i = 0
            while (i < pixels.size) {
                val c = pixels[i]
                val base = i * 4
                rgba[base] = (c shr 16 and 0xFF).toByte() // R
                rgba[base + 1] = (c shr 8 and 0xFF).toByte() // G
                rgba[base + 2] = (c and 0xFF).toByte() // B
                rgba[base + 3] = (c shr 24 and 0xFF).toByte() // A
                i++
            }
            val raw = ModelRuntime.detect(rgba, w, h, AimRuntime.config.conf, AimRuntime.config.nms)
            val det = parse(raw, w, h)
            val fps = rollingFps()
            _uiState.value = _uiState.value.copy(frame = bmp, detections = det, fps = fps)
        } catch (t: Throwable) {
            // 单帧失败直接跳过
        } finally {
            proxy.close()
        }
    }

    // ---- 简单滑动窗口 FPS ----
    @Volatile private var prevNanos = 0L
    @Volatile private var instantFps = 0f
    private fun rollingFps(): Float {
        val now = System.nanoTime()
        if (prevNanos != 0L) {
            val dt = (now - prevNanos) / 1e9f
            if (dt in 0.001f..1f) instantFps = 0.9f * instantFps + 0.1f / dt
        }
        prevNanos = now
        return instantFps
    }

    private fun parse(raw: FloatArray, w: Int, h: Int): List<Detection> {
        val list = ArrayList<Detection>(raw.size / 6)
        var i = 0
        while (i + 5 < raw.size) {
            list.add(
                Detection(
                    label = raw[i].toInt().toString(),
                    score = raw[i + 1],
                    cx = raw[i + 2] / w,
                    cy = raw[i + 3] / h,
                    w = raw[i + 4] / w,
                    h = raw[i + 5] / h,
                )
            )
            i += 6
        }
        return list
    }

    override fun onCleared() {
        bound.set(false)
        runCatching { provider?.unbindAll() }
        runCatching { analysisExecutor.shutdownNow() }
        super.onCleared()
    }
}

private val backSelector = CameraSelector.DEFAULT_BACK_CAMERA