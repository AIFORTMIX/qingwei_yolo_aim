package com.example.kernelsustyleuikit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.kernelsustyleuikit.aim.AimRuntime
import com.example.kernelsustyleuikit.data.repository.SettingsRepository
import com.example.kernelsustyleuikit.data.repository.SettingsRepositoryImpl
import com.example.kernelsustyleuikit.ui.screen.settings.AimSettingsUiState
import com.example.kernelsustyleuikit.ui.screen.settings.SettingsUiState
import com.example.kernelsustyleuikit.ui.theme.ColorMode

class SettingsViewModel(
    private val repo: SettingsRepository = SettingsRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val checkUpdate = repo.checkUpdate
            val themeMode = repo.themeMode
            val miuixMonet = repo.miuixMonet
            val keyColor = repo.keyColor
            val enablePredictiveBack = repo.enablePredictiveBack
            val enableBlur = repo.enableBlur
            val enableFloatingBottomBar = repo.enableFloatingBottomBar
            val enableFloatingBottomBarBlur = repo.enableFloatingBottomBarBlur
            val pageScale = repo.pageScale
            val colorStyle = repo.colorStyle
            val colorSpec = repo.colorSpec
            val uiMode = repo.uiMode

            _uiState.update {
                it.copy(
                    uiMode = uiMode,
                    checkUpdate = checkUpdate,
                    themeMode = themeMode,
                    miuixMonet = miuixMonet,
                    keyColor = keyColor,
                    enablePredictiveBack = enablePredictiveBack,
                    enableBlur = enableBlur,
                    enableFloatingBottomBar = enableFloatingBottomBar,
                    enableFloatingBottomBarBlur = enableFloatingBottomBarBlur,
                    pageScale = pageScale,
                    colorStyle = colorStyle,
                    colorSpec = colorSpec,
                    aim = AimSettingsUiState(
                        conf = AimRuntime.config.conf,
                        nms = AimRuntime.config.nms,
                        targetClass = AimRuntime.config.targetClass,
                        crossHairX = AimRuntime.config.crossHairX,
                        crossHairY = AimRuntime.config.crossHairY,
                        mappingFactor = AimRuntime.config.mappingFactor,
                        verticalOffsetRatio = AimRuntime.config.verticalOffsetRatio,
                        deadZone = AimRuntime.config.deadZone,
                        maxStepPx = AimRuntime.config.maxStepPx,
                        cooldownMs = AimRuntime.config.cooldownMs,
                        swipeDurationMs = AimRuntime.config.swipeDurationMs,
                    ),
                )
            }
        }
    }

    // region 瞄准参数（读写 AimRuntime 全局配置）

    fun setAimConf(v: Float) {
        AimRuntime.config.conf = v.coerceIn(0.01f, 0.99f)
        _uiState.update { it.copy(aim = it.aim.copy(conf = AimRuntime.config.conf)) }
    }

    fun setAimNms(v: Float) {
        AimRuntime.config.nms = v.coerceIn(0.01f, 0.99f)
        _uiState.update { it.copy(aim = it.aim.copy(nms = AimRuntime.config.nms)) }
    }

    fun setAimTargetClass(v: Int) {
        AimRuntime.config.targetClass = v.coerceAtLeast(0)
        _uiState.update { it.copy(aim = it.aim.copy(targetClass = AimRuntime.config.targetClass)) }
    }

    fun setAimCrossHairX(v: Float) {
        AimRuntime.config.crossHairX = v.coerceIn(0.001f, 0.999f)
        _uiState.update { it.copy(aim = it.aim.copy(crossHairX = AimRuntime.config.crossHairX)) }
    }

    fun setAimCrossHairY(v: Float) {
        AimRuntime.config.crossHairY = v.coerceIn(0.001f, 0.999f)
        _uiState.update { it.copy(aim = it.aim.copy(crossHairY = AimRuntime.config.crossHairY)) }
    }

    fun setAimMappingFactor(v: Float) {
        AimRuntime.config.mappingFactor = v.coerceIn(0.1f, 100f)
        _uiState.update { it.copy(aim = it.aim.copy(mappingFactor = AimRuntime.config.mappingFactor)) }
    }

    fun setAimVerticalOffsetRatio(v: Float) {
        AimRuntime.config.verticalOffsetRatio = v.coerceIn(0f, 0.99f)
        _uiState.update { it.copy(aim = it.aim.copy(verticalOffsetRatio = AimRuntime.config.verticalOffsetRatio)) }
    }

    fun setAimDeadZone(v: Float) {
        AimRuntime.config.deadZone = v.coerceIn(0f, 0.9f)
        _uiState.update { it.copy(aim = it.aim.copy(deadZone = AimRuntime.config.deadZone)) }
    }

    fun setAimMaxStepPx(v: Float) {
        AimRuntime.config.maxStepPx = v.coerceIn(1f, 1000f)
        _uiState.update { it.copy(aim = it.aim.copy(maxStepPx = AimRuntime.config.maxStepPx)) }
    }

    fun setAimCooldownMs(v: Long) {
        AimRuntime.config.cooldownMs = v.coerceAtLeast(0L)
        _uiState.update { it.copy(aim = it.aim.copy(cooldownMs = AimRuntime.config.cooldownMs)) }
    }

    fun setAimSwipeDurationMs(v: Long) {
        AimRuntime.config.swipeDurationMs = v.coerceIn(1L, 500L)
        _uiState.update { it.copy(aim = it.aim.copy(swipeDurationMs = AimRuntime.config.swipeDurationMs)) }
    }

    // endregion

    fun setCheckUpdate(enabled: Boolean) {
        repo.checkUpdate = enabled
        _uiState.update { it.copy(checkUpdate = enabled) }
    }

    fun setUiMode(mode: String) {
        val oldMode = repo.uiMode
        val currentThemeMode = repo.themeMode

        val newThemeMode = when (oldMode) {
            "material" if mode == "miuix" -> {
                val colorMode = ColorMode.fromValue(currentThemeMode)
                val baseMode = if (colorMode == ColorMode.DARK_AMOLED) 2 else currentThemeMode
                if (repo.miuixMonet && !colorMode.isMonet) {
                    ColorMode.fromValue(baseMode).toMonetMode()
                } else if (!repo.miuixMonet && colorMode.isMonet) {
                    ColorMode.fromValue(baseMode).toNonMonetMode()
                } else baseMode
            }

            "miuix" if mode == "material" -> {
                val colorMode = ColorMode.fromValue(currentThemeMode)
                if (colorMode.isMonet) {
                    colorMode.toNonMonetMode()
                } else currentThemeMode
            }

            else -> currentThemeMode
        }

        repo.uiMode = mode
        repo.themeMode = newThemeMode
        _uiState.update { it.copy(uiMode = mode, themeMode = newThemeMode) }
    }

    fun setThemeMode(mode: Int) {
        val currentUiMode = repo.uiMode
        val effectiveMode = if (currentUiMode == "miuix" && _uiState.value.miuixMonet) {
            mode + 3
        } else {
            mode
        }
        repo.themeMode = effectiveMode
        _uiState.update { it.copy(themeMode = effectiveMode) }
    }

    fun setColorMode(mode: ColorMode) {
        repo.themeMode = mode.value
        _uiState.update { it.copy(themeMode = mode.value) }
    }

    fun setMiuixMonet(enabled: Boolean) {
        val currentThemeMode = repo.themeMode
        val colorMode = ColorMode.fromValue(currentThemeMode)
        val newThemeMode = if (enabled) {
            if (!colorMode.isMonet) colorMode.toMonetMode() else currentThemeMode
        } else {
            if (colorMode.isMonet) colorMode.toNonMonetMode() else currentThemeMode
        }
        repo.miuixMonet = enabled
        repo.themeMode = newThemeMode
        _uiState.update { it.copy(miuixMonet = enabled, themeMode = newThemeMode) }
    }

    fun setKeyColor(color: Int) {
        repo.keyColor = color
        _uiState.update { it.copy(keyColor = color) }
    }

    fun setColorStyle(style: String) {
        repo.colorStyle = style
        _uiState.update { it.copy(colorStyle = style) }
    }

    fun setColorSpec(spec: String) {
        repo.colorSpec = spec
        _uiState.update { it.copy(colorSpec = spec) }
    }

    fun setEnablePredictiveBack(enabled: Boolean) {
        repo.enablePredictiveBack = enabled
        _uiState.update { it.copy(enablePredictiveBack = enabled) }
    }

    fun setEnableBlur(enabled: Boolean) {
        repo.enableBlur = enabled
        _uiState.update { it.copy(enableBlur = enabled) }
    }

    fun setEnableFloatingBottomBar(enabled: Boolean) {
        repo.enableFloatingBottomBar = enabled
        _uiState.update { it.copy(enableFloatingBottomBar = enabled) }
    }

    fun setEnableFloatingBottomBarBlur(enabled: Boolean) {
        repo.enableFloatingBottomBarBlur = enabled
        _uiState.update { it.copy(enableFloatingBottomBarBlur = enabled) }
    }

    fun setPageScale(scale: Float) {
        repo.pageScale = scale
        _uiState.update { it.copy(pageScale = scale) }
    }

}
