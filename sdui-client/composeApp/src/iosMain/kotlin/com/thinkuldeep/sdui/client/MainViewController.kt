package com.thinkuldeep.sdui.client

import androidx.compose.ui.window.ComposeUIViewController
import com.thinkuldeep.sdui.client.renderer.Render
import com.thinkuldeep.sdui.client.viewmodel.LandingViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember

fun MainViewController() = ComposeUIViewController {

    val vm = remember { LandingViewModel() }

    val state = vm.uiState.collectAsState()

    state.value?.let {
        Render(it, vm)
    }
}