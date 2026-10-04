package com.example.kernelsustyleuikit.yolo

/** YOLO 检测结果（归一化坐标，相对屏幕）。 */
data class Detection(
    val label: String,
    val score: Float,
    val cx: Float,
    val cy: Float,
    val w: Float,
    val h: Float
)