package com.example.kernelsustyleuikit.shizuku

import android.graphics.BitmapFactory
import com.example.kernelsustyleuikit.root.RootManager
import java.nio.ByteBuffer

/** 截屏获取屏幕帧，解码为 RGBA 供 YOLO 推理。root 优先，其次 Shizuku。 */
object ScreenCapturer {

    class Frame(val rgba: ByteArray, val width: Int, val height: Int)

    private const val TMP_PATH = "/data/local/tmp/syzaim.png"

    suspend fun capture(): Frame {
        val out = if (RootManager.rootAvailable) {
            RootManager.execShellBytes("screencap -p $TMP_PATH && cat $TMP_PATH")
        } else {
            ShizukuManager.execShellBytes("screencap -p $TMP_PATH && cat $TMP_PATH")
        }
        if (out.size < 8) throw IllegalStateException("截屏失败: ${String(out)}")

        val bmp = BitmapFactory.decodeByteArray(out, 0, out.size)
            ?: throw IllegalStateException("截图解码失败")

        val w = bmp.width
        val h = bmp.height
        val rgba = ByteArray(w * h * 4)
        bmp.copyPixelsToBuffer(ByteBuffer.wrap(rgba))
        bmp.recycle()
        return Frame(rgba, w, h)
    }
}