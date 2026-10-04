package com.example.kernelsustyleuikit.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kernelsustyleuikit.aim.AimController
import com.example.kernelsustyleuikit.aim.AimRuntime
import com.example.kernelsustyleuikit.shizuku.ShizukuManager
import com.example.kernelsustyleuikit.ui.screen.home.HomeUiState
import com.example.kernelsustyleuikit.ui.screen.home.getAppVersion
import com.example.kernelsustyleuikit.yolo.NcnnModel
import com.example.kernelsustyleuikit.yolo.YoloNative
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var modelReady = false

    fun refresh(context: Context) {
        viewModelScope.launch {
            val version = withContext(Dispatchers.IO) {
                getAppVersion(context).versionName
            }
            _uiState.update {
                it.copy(
                    shizukuAvailable = ShizukuManager.isAvailable,
                    shizukuGranted = ShizukuManager.isGranted,
                    modelReady = modelReady,
                    modelError = if (modelReady) null else it.modelError,
                    appVersion = version,
                    aiming = AimRuntime.controller?.isRunning == true,
                )
            }
        }
    }

    /** 初始化 ncnn 模型（幂等）。失败时在状态中记录错误。 */
    fun ensureModel(context: Context) {
        if (modelReady) return
        viewModelScope.launch {
            try {
                val (param, bin) = withContext(Dispatchers.IO) { NcnnModel.prepare(context) }
                val ret = withContext(Dispatchers.IO) { YoloNative.init(param, bin, false) }
                if (ret != 0) {
                    _uiState.update { it.copy(modelError = "模型初始化失败 code=$ret") }
                    return@launch
                }
                modelReady = true
                _uiState.update { it.copy(modelReady = true, modelError = null) }
            } catch (e: Throwable) {
                _uiState.update { it.copy(modelError = e.message ?: "模型加载失败") }
            }
        }
    }

    fun startAim() {
        if (!ShizukuManager.isGranted) {
            _uiState.update { it.copy(modelError = "请先完成 Shizuku 授权") }
            return
        }
        if (!modelReady) {
            _uiState.update { it.copy(modelError = "模型未就绪") }
            return
        }
        val controller = AimController(
            scope = viewModelScope,
            config = AimRuntime.config,
            detector = { rgba, w, h -> parse(YoloNative.detect(rgba, w, h, AimRuntime.config.conf, AimRuntime.config.nms), w, h) },
            onFrame = { fps, running, locked ->
                _uiState.update { it.copy(fps = fps, aiming = running, targetLocked = locked) }
            }
        )
        AimRuntime.controller = controller
        controller.start()
        _uiState.update { it.copy(aiming = true) }
    }

    fun stopAim() {
        AimRuntime.controller?.stop()
        AimRuntime.controller = null
        _uiState.update { it.copy(aiming = false, targetLocked = false) }
    }

    private fun parse(raw: FloatArray, w: Int, h: Int): List<com.example.kernelsustyleuikit.yolo.Detection> {
        val list = ArrayList<com.example.kernelsustyleuikit.yolo.Detection>(raw.size / 6)
        var i = 0
        while (i + 5 < raw.size) {
            list.add(
                com.example.kernelsustyleuikit.yolo.Detection(
                    label = raw[i].toInt().toString(),
                    score = raw[i + 1],
                    cx = raw[i + 2] / w,
                    cy = raw[i + 3] / h,
                    w = raw[i + 4] / w,
                    h = raw[i + 5] / h
                )
            )
            i += 6
        }
        return list
    }

    override fun onCleared() {
        AimRuntime.controller?.stop()
        super.onCleared()
    }
}