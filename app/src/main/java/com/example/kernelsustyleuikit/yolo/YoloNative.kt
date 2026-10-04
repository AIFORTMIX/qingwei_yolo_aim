package com.example.kernelsustyleuikit.yolo

/**
 * JNI 桥接（对应 cpp 中的 YoloNative）。入参 RGBA，出参为检测框。
 */
object YoloNative {
    init {
        System.loadLibrary("shizukuYolo")
    }

    external fun init(paramPath: String, binPath: String, useGpu: Boolean): Int
    external fun detect(rgba: ByteArray, w: Int, h: Int, conf: Float, nms: Float): FloatArray
    external fun destroy()
}