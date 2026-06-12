package com.thinkuldeep.sdui.client

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.window.ComposeUIViewController
import com.thinkuldeep.sdui.client.renderer.RenderWithRefresh
import com.thinkuldeep.sdui.client.viewmodel.LandingViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.thinkuldeep.sdui.client.tracing.Span
import com.thinkuldeep.sdui.client.tracing.SpanContextHolder
import com.thinkuldeep.sdui.client.tracing.TracingProvider

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
    var span = Span.create()
    SpanContextHolder.set(span)

    span = TracingProvider.startSpan(name = "UI Action - Refreshing", span)
    viewModel.setSpan(span)

    println("🔄 Refreshing UI with new span")
    println("🔍 [TRACE] New TraceID: ${span.traceId}")
    println("🔍 [TRACE] New SpanID: ${span.spanId}")
    println("🔍 [TRACE] Sampled: ${span.isSampled}")
    println("🔍 [TRACE] Traceparent: ${span.toTraceparent()}")

    // Reload the UI (trigger API call with new span)
    viewModel.reload()
}