package com.example.kernelsustyleuikit.ui.screen.camera

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kernelsustyleuikit.ui.theme.LocalEnableBlur
import com.example.kernelsustyleuikit.ui.util.BlurredBar
import com.example.kernelsustyleuikit.ui.util.rememberBlurBackdrop
import com.example.kernelsustyleuikit.ui.viewmodel.CameraUiState
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import kotlin.math.min

@Immutable
data class CameraActions(
    val onBack: () -> Unit,
    val onRequestPermission: () -> Unit,
)

@Composable
fun CameraScreenMiuix(
    state: CameraUiState,
    actions: CameraActions,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val barColor = if (backdrop != null) Color.Transparent else colorScheme.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                SmallTopAppBar(
                    title = "相机模式",
                    scrollBehavior = scrollBehavior,
                    color = barColor,
                    defaultWindowInsetsPadding = false,
                    navigationIcon = {
                        IconButton(onClick = actions.onBack) {
                            val layoutDirection = LocalLayoutDirection.current
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = null,
                                tint = colorScheme.onBackground,
                                modifier = Modifier.graphicsLayer {
                                    if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
                                },
                            )
                        }
                    },
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                !state.permissionGranted -> PermissionPlaceholder(actions.onRequestPermission)
                state.cameraError != null -> MessagePlaceholder(state.cameraError)
                else -> CameraPreview(state)
            }
        }
    }
}

@Composable
private fun PermissionPlaceholder(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(120.dp))
        Text("需要相机权限", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Text(
            "开启后置摄像头进行实时人物识别与打框",
            fontSize = 13.sp,
            color = colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequest) {
            Text("授权相机权限")
        }
    }
}

@Composable
private fun MessagePlaceholder(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(120.dp))
        Text(message, fontSize = 14.sp, color = colorScheme.error)
    }
}

@Composable
private fun CameraPreview(state: CameraUiState) {
    val bitmap = state.frame
    Box(modifier = Modifier.fillMaxSize()) {
        if (bitmap != null) {
            Canvas(Modifier.fillMaxSize()) {
                val bmpW = bitmap.width.toFloat()
                val bmpH = bitmap.height.toFloat()
                val scale = min(size.width / bmpW, size.height / bmpH)
                val drawW = bmpW * scale
                val drawH = bmpH * scale
                val left = (size.width - drawW) / 2f
                val top = (size.height - drawH) / 2f

                drawImage(
                    image = bitmap.asImageBitmap(),
                    dstOffset = Offset(left, top),
                    dstSize = Size(drawW, drawH),
                )

                // 检测框（坐标相对输入帧，等比换算到显示区域）
                state.detections.forEach { d ->
                    val x = left + d.cx * drawW
                    val y = top + d.cy * drawH
                    val bw = d.w * drawW
                    val bh = d.h * drawH
                    drawRect(
                        color = Color(0xFF00FF66),
                        topLeft = Offset(x - bw / 2f, y - bh / 2f),
                        size = Size(bw, bh),
                        style = Stroke(width = 3f * (scale / (bitmap.density / 160f)).coerceAtLeast(1f)),
                    )
                    drawLabel(
                        label = "person ${(d.score * 100).toInt()}%",
                        x = x - bw / 2f,
                        y = y - bh / 2f,
                        scale = scale,
                    )
                }
            }
        } else {
            MessagePlaceholder(if (state.modelError != null) "模型错误：${state.modelError}" else "相机启动中...")
        }

        if (bitmap != null) {
            Text(
                text = "FPS ${"%.0f".format(state.fps)} · 目标 ${state.detections.size}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
            )
        }
    }
}

/** 在检测框上方绘制标签（深色底 + 白色文字）。 */
private fun DrawScope.drawLabel(
    label: String,
    x: Float,
    y: Float,
    scale: Float,
) {
    if (y < 0) return
    val labelTextSize = 24f * scale
    val width = label.length * labelTextSize * 0.72f
    val height = labelTextSize * 1.3f
    val bgLeft = x
    val bgTop = y - height
    drawRect(
        color = Color(0xFF006B34),
        topLeft = Offset(bgLeft, bgTop),
        size = Size(width, height),
    )
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.White.toArgb()
        textSize = labelTextSize
    }
    drawContext.canvas.nativeCanvas.drawText(
        label,
        bgLeft + 4f * scale,
        bgTop + labelTextSize,
        paint,
    )
}