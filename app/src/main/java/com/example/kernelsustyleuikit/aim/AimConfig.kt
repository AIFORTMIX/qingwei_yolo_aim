package com.example.kernelsustyleuikit.aim

/** 瞄准参数配置。 */
data class AimConfig(
    var conf: Float = 0.30f,
    var nms: Float = 0.45f,
    var targetClass: Int = 0,
    var crossHairX: Float = 0.5f,
    var crossHairY: Float = 0.5f,
    var mappingFactor: Float = 1.0f,
    var verticalOffsetRatio: Float = 0.08f,
    var deadZone: Float = 0.02f,
    var maxStepPx: Float = 120f,
    var cooldownMs: Long = 100L,
    var swipeDurationMs: Long = 60L
)