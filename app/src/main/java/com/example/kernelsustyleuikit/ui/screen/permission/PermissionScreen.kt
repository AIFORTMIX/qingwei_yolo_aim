package com.example.kernelsustyleuikit.ui.screen.permission

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.example.kernelsustyleuikit.R
import com.example.kernelsustyleuikit.permission.PermissionManager
import com.example.kernelsustyleuikit.permission.PermissionState
import com.example.kernelsustyleuikit.ui.navigation3.LocalNavigator
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun PermissionScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val manager = remember(context) { PermissionManager(context) }
    val state by manager.state.collectAsStateWithLifecycle()
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { manager.refresh() }
    val shizukuLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { manager.refresh() }

    LifecycleResumeEffect(manager) {
        manager.refresh()
        onPauseOrDispose { }
    }

    val actions = PermissionActions(
        onShizuku = { manager.requestShizuku() },
        onOpenShizuku = { manager.shizukuLauncherIntent()?.let(shizukuLauncher::launch) },
        onNotification = {
            val perm = manager.notificationRuntimePermission()
            if (perm != null) notificationLauncher.launch(perm)
            else shizukuLauncher.launch(manager.notificationSettingsIntent())
        },
    )
    val onBack = dropUnlessResumed { navigator.pop() }

    PermissionScreenMiuix(state, actions, onBack)
}

private data class PermissionActions(
    val onShizuku: () -> Unit,
    val onOpenShizuku: () -> Unit,
    val onNotification: () -> Unit,
)

@Composable
private fun PermissionScreenMiuix(
    state: PermissionState,
    actions: PermissionActions,
    onBack: () -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    MiuixScaffold(
        topBar = {
            MiuixTopAppBar(
                title = stringResource(R.string.permission_section),
                navigationIcon = {
                    MiuixIconButton(onClick = onBack) {
                        MiuixIcon(MiuixIcons.Back, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        popupHost = { },
        contentWindowInsets =
            WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .scrollEndHaptic()
                .overScrollVertical()
                .padding(horizontal = 12.dp),
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            item {
                PermissionStatusCardMiuix(state, actions)
            }
            item {
                MiuixCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    PermissionRowMiuix(
                        stringResource(R.string.permission_notification),
                        state.notification,
                        Icons.Rounded.Notifications,
                        actions.onNotification,
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun PermissionStatusCardMiuix(
    state: PermissionState,
    actions: PermissionActions,
) {
    val title: String
    val summary: String
    val icon: ImageVector
    val iconTint: androidx.compose.ui.graphics.Color
    when {
        state.shizukuGranted -> {
            title = stringResource(R.string.permission_status_ready_title)
            summary = stringResource(R.string.permission_shizuku_granted)
            icon = Icons.Rounded.VerifiedUser
            iconTint = androidx.compose.ui.graphics.Color(0xFF36D167)
        }

        state.shizukuAvailable -> {
            title = stringResource(R.string.permission_shizuku)
            summary = stringResource(R.string.permission_shizuku_available)
            icon = Icons.Rounded.Security
            iconTint = MiuixTheme.colorScheme.primary
        }

        else -> {
            title = stringResource(R.string.permission_status_missing_title)
            summary = stringResource(R.string.permission_shizuku_not_installed)
            icon = Icons.Rounded.Security
            iconTint = MiuixTheme.colorScheme.error
        }
    }

    MiuixCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            if (state.shizukuGranted) return@MiuixCard
            if (state.shizukuAvailable) actions.onShizuku()
            else actions.onOpenShizuku()
        },
        showIndication = true,
    ) {
        BasicComponent(
            title = title,
            summary = summary,
            endActions = {
                MiuixIcon(
                    imageVector = icon,
                    tint = iconTint,
                    contentDescription = null,
                )
            },
        )
    }

    if (state.shizukuGranted) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        MiuixCard(modifier = Modifier.fillMaxWidth()) {
            ArrowPreference(
                title = stringResource(R.string.permission_request_shizuku),
                summary = stringResource(R.string.permission_shizuku_summary),
                onClick = {
                    if (state.shizukuAvailable) actions.onShizuku()
                    else actions.onOpenShizuku()
                },
                endActions = {
                    if (state.shizukuAvailable) {
                        MiuixText(
                            text = stringResource(R.string.permission_request_shizuku),
                            color = MiuixTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                },
                holdDownState = false,
                enabled = true,
            )
        }
    }
}

@Composable
private fun PermissionRowMiuix(
    title: String,
    granted: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    ArrowPreference(
        title = title,
        summary = when {
            granted -> stringResource(R.string.permission_granted)
            else -> stringResource(R.string.permission_required)
        },
        startAction = {
            MiuixIcon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
        },
        endActions = {
            MiuixText(
                text =
                    if (granted) {
                        stringResource(R.string.permission_granted)
                    } else {
                        stringResource(R.string.permission_grant_action)
                    },
                color =
                    if (granted) MiuixTheme.colorScheme.primary
                    else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontWeight = FontWeight.Medium,
            )
        },
        onClick = onClick,
        holdDownState = false,
        enabled = true,
    )
}