package com.thinkuldeep.sdui.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.thinkuldeep.sdui.client.renderer.Render
import com.thinkuldeep.sdui.client.viewmodel.LandingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val vm = remember { LandingViewModel() }
            val state = vm.uiState.collectAsState()

            state.value?.let {
                Render(it, vm)
            }
        }
    }
}