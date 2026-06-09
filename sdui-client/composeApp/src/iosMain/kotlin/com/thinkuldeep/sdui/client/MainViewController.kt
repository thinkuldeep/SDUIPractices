package com.thinkuldeep.sdui.client

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.window.ComposeUIViewController
import com.thinkuldeep.sdui.client.renderer.RenderWithRefresh
import com.thinkuldeep.sdui.client.viewmodel.LandingViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thinkuldeep.sdui.client.tracing.Span
import com.thinkuldeep.sdui.client.tracing.SpanContextHolder
fun MainViewController() = ComposeUIViewController {
    val vm = remember {
        AppInitializer.initializeApp()
        LandingViewModel()
    }

    val state = vm.uiState.collectAsState()
    val span = vm.span.collectAsState()

    Box(modifier = Modifier.fillMaxWidth()) {
        state.value?.let {
            RenderWithRefresh(it, vm) {
                refreshUI(vm)
            }
        }
    }

    // Log current span
    span.value?.let { s ->
        val logMsg = "🔍 [TRACE] Current - TraceID: ${s.traceId}, " +
                "Sampled: ${s.isSampled}, " +
                "Flags: ${s.traceFlags}"
        println(logMsg)
    }
}

private fun refreshUI(viewModel: LandingViewModel) {
    // Generate new span context
    val newSpan = Span.create()
    SpanContextHolder.set(newSpan)
    viewModel.setSpan(newSpan)

    println("🔄 Refreshing UI with new span")
    println("🔍 [TRACE] New TraceID: ${newSpan.traceId}")
    println("🔍 [TRACE] New SpanID: ${newSpan.spanId}")
    println("🔍 [TRACE] Sampled: ${newSpan.isSampled}")
    println("🔍 [TRACE] Traceparent: ${newSpan.toTraceparent()}")

    // Reload the UI (trigger API call with new span)
    viewModel.reload()
}