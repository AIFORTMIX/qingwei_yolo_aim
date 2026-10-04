package com.example.kernelsustyleuikit.ui.screen.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DonutSmall
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FilterCenterFocus
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Pinch
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Update
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kernelsustyleuikit.R
import com.example.kernelsustyleuikit.ui.UiMode
import com.example.kernelsustyleuikit.ui.component.dialog.rememberLoadingDialog
import com.example.kernelsustyleuikit.ui.component.miuix.SendLogDialog
import com.example.kernelsustyleuikit.ui.component.miuix.SuperEditArrow
import com.example.kernelsustyleuikit.ui.component.miuix.SuperEditFloat
import com.example.kernelsustyleuikit.ui.theme.LocalEnableBlur
import com.example.kernelsustyleuikit.ui.util.BlurredBar
import com.example.kernelsustyleuikit.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * @author weishu
 * @date 2023/1/1.
 */
@Composable
private fun SectionTitle(title: String, summary: String) {
    Column(modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 4.dp)) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurface
        )
        Text(
            text = summary,
            fontSize = 13.sp,
            color = colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun SettingPagerMiuix(
    uiState: SettingsUiState,
    actions: SettingsScreenActions,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface
    val loadingDialog = rememberLoadingDialog()
    val showSendLogDialog = rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = barColor,
                    title = stringResource(R.string.settings),
                    scrollBehavior = scrollBehavior
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(horizontal = 12.dp),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        SwitchPreference(
                            title = stringResource(id = R.string.settings_check_update),
                            summary = stringResource(id = R.string.settings_check_update_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Update,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_check_update),
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = uiState.checkUpdate,
                            onCheckedChange = actions.onSetCheckUpdate
                        )
                    }

                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        OverlayDropdownPreference(
                            title = stringResource(id = R.string.settings_ui_mode),
                            summary = stringResource(id = R.string.settings_ui_mode_summary),
                            items = UiMode.entries.map { it.name },
                            startAction = {
                                Icon(
                                    Icons.Rounded.Dashboard,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_ui_mode),
                                    tint = colorScheme.onBackground
                                )
                            },
                            selectedIndex = if (uiState.uiMode == UiMode.Material.value) 1 else 0,
                            onSelectedIndexChange = actions.onSetUiModeIndex
                        )
                        ArrowPreference(
                            title = stringResource(id = R.string.settings_theme),
                            summary = stringResource(id = R.string.settings_theme_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Palette,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_theme),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onClick = actions.onOpenTheme
                        )
                    }

                    Card(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        SectionTitle(stringResource(id = R.string.aim_settings), stringResource(id = R.string.aim_settings_summary))
                        SuperEditFloat(
                            title = stringResource(id = R.string.aim_conf),
                            summary = stringResource(id = R.string.aim_conf_summary),
                            value = uiState.aim.conf,
                            startAction = {
                                Icon(
                                    Icons.Rounded.Explore,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.aim_conf),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onValueChange = actions.onSetAimConf
                        )
                        SuperEditFloat(
                            title = stringResource(id = R.string.aim_nms),
                            summary = stringResource(id = R.string.aim_nms_summary),
                            value = uiState.aim.nms,
                            startAction = {
                                Icon(
                                    Icons.Rounded.FilterCenterFocus,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.aim_nms),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onValueChange = actions.onSetAimNms
                        )
                        SuperEditArrow(
                            title = stringResource(id = R.string.aim_target_class),
                            defaultValue = uiState.aim.targetClass,
                            startAction = {
                                Icon(
                                    Icons.Rounded.Person,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.aim_target_class),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onValueChange = actions.onSetAimTargetClass
                        )
                        SuperEditFloat(
                            title = stringResource(id = R.string.aim_mapping_factor),
                            summary = stringResource(id = R.string.aim_mapping_factor_summary),
                            value = uiState.aim.mappingFactor,
                            startAction = {
                                Icon(
                                    Icons.Rounded.Speed,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.aim_mapping_factor),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onValueChange = actions.onSetAimMappingFactor
                        )
                        SuperEditFloat(
                            title = stringResource(id = R.string.aim_vertical_offset),
                            summary = stringResource(id = R.string.aim_vertical_offset_summary),
                            value = uiState.aim.verticalOffsetRatio,
                            startAction = {
                                Icon(
                                    Icons.Rounded.TrendingUp,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.aim_vertical_offset),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onValueChange = actions.onSetAimVerticalOffsetRatio
                        )
                        SuperEditFloat(
                            title = stringResource(id = R.string.aim_dead_zone),
                            summary = stringResource(id = R.string.aim_dead_zone_summary),
                            value = uiState.aim.deadZone,
                            startAction = {
                                Icon(
                                    Icons.Rounded.DonutSmall,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.aim_dead_zone),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onValueChange = actions.onSetAimDeadZone
                        )
                        SuperEditFloat(
                            title = stringResource(id = R.string.aim_max_step),
                            summary = stringResource(id = R.string.aim_max_step_summary),
                            value = uiState.aim.maxStepPx,
                            startAction = {
                                Icon(
                                    Icons.Rounded.Pinch,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.aim_max_step),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onValueChange = actions.onSetAimMaxStepPx
                        )
                        SuperEditArrow(
                            title = stringResource(id = R.string.aim_cooldown),
                            defaultValue = uiState.aim.cooldownMs.toInt(),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Timer,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.aim_cooldown),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onValueChange = { actions.onSetAimCooldownMs(it.toLong()) }
                        )
                        SuperEditArrow(
                            title = stringResource(id = R.string.aim_swipe_duration),
                            defaultValue = uiState.aim.swipeDurationMs.toInt(),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Gesture,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.aim_swipe_duration),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onValueChange = { actions.onSetAimSwipeDurationMs(it.toLong()) }
                        )
                    }

                    Card(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        ArrowPreference(
                            title = stringResource(id = R.string.send_log),
                            startAction = {
                                Icon(
                                    Icons.Rounded.BugReport,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.send_log),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onClick = { showSendLogDialog.value = true },
                        )
                        SendLogDialog(
                            show = showSendLogDialog.value,
                            onDismissRequest = { showSendLogDialog.value = false },
                            loadingDialog = loadingDialog
                        )
                        val about = stringResource(id = R.string.about)
                        ArrowPreference(
                            title = about,
                            startAction = {
                                Icon(
                                    Icons.Rounded.ContactPage,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = about,
                                    tint = colorScheme.onBackground
                                )
                            },
                            onClick = actions.onOpenAbout,
                        )
                    }
                    Spacer(Modifier.height(bottomInnerPadding))
                }
            }
        }
    }
}
