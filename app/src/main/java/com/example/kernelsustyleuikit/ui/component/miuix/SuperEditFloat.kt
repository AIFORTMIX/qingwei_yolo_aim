package com.example.kernelsustyleuikit.ui.component.miuix

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.kernelsustyleuikit.R
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference

/**
 * 带点击弹出数字输入框的浮点参数项，复用 SuperEditArrow 的交互，专用于瞄准参数（置信度、灵敏度等）。
 */
@Composable
fun SuperEditFloat(
    modifier: Modifier = Modifier,
    title: String,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    value: Float,
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    startAction: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    suffix: String = "",
    onValueChange: ((Float) -> Unit)? = null,
) {
    val showDialog = remember { mutableStateOf(false) }

    ArrowPreference(
        title = title,
        titleColor = titleColor,
        summary = summary ?: (value.toString() + suffix),
        summaryColor = summaryColor,
        startAction = startAction,
        modifier = modifier,
        onClick = { showDialog.value = true },
        holdDownState = showDialog.value,
        enabled = enabled,
    )

    FloatEditDialog(
        title = title,
        show = showDialog.value,
        onDismissRequest = { showDialog.value = false },
        value = value,
        suffix = suffix,
        onValueChange = { onValueChange?.invoke(it) },
    )
}

@Composable
private fun FloatEditDialog(
    title: String,
    show: Boolean,
    onDismissRequest: () -> Unit,
    value: Float,
    suffix: String,
    onValueChange: (Float) -> Unit,
) {
    var text by remember(show) { mutableStateOf(value.toString()) }

    OverlayDialog(
        show = show,
        title = title,
        onDismissRequest = onDismissRequest,
        content = {
            TextField(
                modifier = Modifier.padding(bottom = 16.dp),
                value = text,
                maxLines = 1,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                onValueChange = { newValue ->
                    // 仅允许数字、负号与一个小数点
                    if (newValue.isEmpty() || newValue.matches(Regex("^-?\\d*\\.?\\d*$"))) {
                        text = newValue
                    }
                },
            )
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = stringResource(R.string.confirm),
                    onClick = {
                        val parsed = text.toFloatOrNull()
                        if (parsed != null) onValueChange(parsed)
                        onDismissRequest()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    )
}