package com.example.kernelsustyleuikit.util

import java.lang.reflect.Modifier

/**
 * 绕开 Hidden API 限制的反射工具，用于调用非 SDK 接口（如 InputManager.injectInputEvent）。
 */
object Reflection {

    fun getSystemService(name: String): Any? {
        val clazz = Class.forName("android.os.ServiceManager")
        val method = clazz.getMethod("getService", String::class.java)
        return if (Modifier.isStatic(method.modifiers)) method.invoke(null, name)
        else method.invoke(clazz.getDeclaredConstructor().newInstance(), name)
    }

    fun callMethod(
        target: Any?,
        className: String,
        methodName: String,
        argTypes: Array<Class<*>>,
        args: Array<Any?>
    ): Any? {
        val clazz = Class.forName(className)
        val method: java.lang.reflect.Method = clazz.getDeclaredMethod(methodName, *argTypes).apply {
            isAccessible = true
        }
        return method.invoke(target, *args)
    }
}