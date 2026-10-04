package com.example.kernelsustyleuikit.ui.screen.settings

import androidx.compose.runtime.Immutable
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.example.kernelsustyleuikit.ui.UiMode

@Immutable
data class SettingsUiState(
    val uiMode: String = UiMode.DEFAULT_VALUE,
    val checkUpdate: Boolean = true,
    val themeMode: Int = 0,
    val miuixMonet: Boolean = false,
    val keyColor: Int = 0,
    val colorStyle: String = PaletteStyle.TonalSpot.name,
    val colorSpec: String = ColorSpec.SpecVersion.Default.name,
    val enablePredictiveBack: Boolean = false,
    val enableBlur: Boolean = true,
    val enableFloatingBottomBar: Boolean = true,
    val enableFloatingBottomBarBlur: Boolean = true,
    val pageScale: Float = 1.0f,
    val aim: AimSettingsUiState = AimSettingsUiState(),
)

@Immutable
data class AimSettingsUiState(
    val conf: Float = 0.30f,
    val nms: Float = 0.45f,
    val targetClass: Int = 0,
    val crossHairX: Float = 0.5f,
    val crossHairY: Float = 0.5f,
    val mappingFactor: Float = 1.0f,
    val verticalOffsetRatio: Float = 0.08f,
    val deadZone: Float = 0.02f,
    val maxStepPx: Float = 120f,
    val cooldownMs: Long = 100L,
    val swipeDurationMs: Long = 60L,
)

@Immutable
data class SettingsScreenActions(
    val onSetCheckUpdate: (Boolean) -> Unit,
    val onOpenTheme: () -> Unit,
    val onSetUiModeIndex: (Int) -> Unit,
    val onOpenAbout: () -> Unit,
    val onSetAimConf: (Float) -> Unit,
    val onSetAimNms: (Float) -> Unit,
    val onSetAimTargetClass: (Int) -> Unit,
    val onSetAimCrossHairX: (Float) -> Unit,
    val onSetAimCrossHairY: (Float) -> Unit,
    val onSetAimMappingFactor: (Float) -> Unit,
    val onSetAimVerticalOffsetRatio: (Float) -> Unit,
    val onSetAimDeadZone: (Float) -> Unit,
    val onSetAimMaxStepPx: (Float) -> Unit,
    val onSetAimCooldownMs: (Long) -> Unit,
    val onSetAimSwipeDurationMs: (Long) -> Unit,
)
