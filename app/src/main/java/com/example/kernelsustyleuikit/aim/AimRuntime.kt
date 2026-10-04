package com.example.kernelsustyleuikit.aim

/** 全局瞄准运行时：共享参数与控制器实例，供 Home/设置页读写。 */
object AimRuntime {
    val config = AimConfig()
    var controller: AimController? = null
        set(value) {
            field?.stop()
            field = value
        }
}