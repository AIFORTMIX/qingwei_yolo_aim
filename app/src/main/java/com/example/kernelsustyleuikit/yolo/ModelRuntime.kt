package com.example.kernelsustyleuikit.yolo

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 共享的 ncnn 模型初始化入口。相机模式等需要在任意页面初始化模型，
 * 这里保证只加载一次并暴露就绪状态（App 进程内全局单例）。
 */
object ModelRuntime {

    @Volatile
    private var ready = false

    val isReady: Boolean get() = ready

    /** 初始化模型（幂等）。成功返回 null，失败返回错误信息。 */
    suspend fun ensure(context: Context): String? = withContext(Dispatchers.IO) {
        if (ready) return@withContext null
        synchronized(ModelRuntime) {
            if (ready) return@withContext null
            runCatching {
                val (param, bin) = NcnnModel.prepare(context)
                val ret = YoloNative.init(param, bin, false)
                check(ret == 0) { "模型初始化失败 code=$ret" }
            }.fold(
                onSuccess = { ready = true; null },
                onFailure = { it.message ?: "模型初始化失败" }
            )
        }
    }

    /** 执行推理（全局共用一个原生实例）。 */
    fun detect(rgba: ByteArray, w: Int, h: Int, conf: Float, nms: Float): FloatArray =
        YoloNative.detect(rgba, w, h, conf, nms)
}