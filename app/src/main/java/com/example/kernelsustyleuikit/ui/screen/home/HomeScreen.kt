package com.example.kernelsustyleuikit.ui.screen.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kernelsustyleuikit.ui.navigation3.Navigator
import com.example.kernelsustyleuikit.ui.navigation3.Route
import com.example.kernelsustyleuikit.ui.viewmodel.HomeViewModel

@Composable
fun HomePager(
    navigator: Navigator,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true
) {
    val viewModel = viewModel<HomeViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        viewModel.refresh(context)
        viewModel.ensureModel(context)
        onPauseOrDispose { }
    }

    val actions = HomeActions(
        onShizukuClick = { navigator.push(Route.Permissions) },
        onStartAim = { viewModel.startAim() },
        onStopAim = { viewModel.stopAim() },
    )

    HomePagerMiuix(
        state = uiState,
        actions = actions,
        bottomInnerPadding = bottomInnerPadding,
    )
}