package com.example.kernelsustyleuikit.yolo

import android.content.Context
import java.io.File

/** 将 assets 中的 ncnn 模型文件拷贝到应用私有目录，返回 (param, bin) 绝对路径。 */
object NcnnModel {

    private const val ASSET_DIR = "models"
    private const val PARAM_NAME = "yolov8n.param"
    private const val BIN_NAME = "yolov8n.bin"

    fun prepare(context: Context): Pair<String, String> {
        val dest = File(context.filesDir, ASSET_DIR).apply { mkdirs() }
        val param = copyAsset(context, "$ASSET_DIR/$PARAM_NAME", File(dest, PARAM_NAME))
        val bin = copyAsset(context, "$ASSET_DIR/$BIN_NAME", File(dest, BIN_NAME))
        return param.path to bin.path
    }

    private fun copyAsset(ctx: Context, assetName: String, out: File): File {
        if (out.exists() && out.length() > 0) return out
        ctx.assets.open(assetName).use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        }
        return out
    }
}