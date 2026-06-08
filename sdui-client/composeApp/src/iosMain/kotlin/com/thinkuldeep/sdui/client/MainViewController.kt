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
import com.thinkuldeep.sdui.client.tracing.Environment
import com.thinkuldeep.sdui.client.tracing.TraceContext
import com.thinkuldeep.sdui.client.tracing.TraceContextHolder

fun MainViewController() = ComposeUIViewController {

    val vm = remember {
        LandingViewModel().apply {
            configureSampling(Environment.PRODUCTION)
        }
    }

    val state = vm.uiState.collectAsState()
    val traceContext = vm.traceContext.collectAsState()

    Box(modifier = Modifier.fillMaxWidth()) {
        state.value?.let {
            RenderWithRefresh(it, vm) {
                refreshUI(vm)
            }
        }
    }

    // Log current trace
    traceContext.value?.let { trace ->
        val logMsg = "🔍 [TRACE] Current - TraceID: ${trace.traceId}, " +
                "Sampled: ${trace.isSampled}, " +
                "Flags: ${trace.traceFlags}"
        println(logMsg)
    }
}

private fun refreshUI(viewModel: LandingViewModel) {
    // Generate new trace context
    val newContext = TraceContext.create()
    TraceContextHolder.set(newContext)
    viewModel.setTraceContext(newContext)

    println("🔄 Refreshing UI with new trace")
    println("🔍 [TRACE] New TraceID: ${newContext.traceId}")
    println("🔍 [TRACE] New SpanID: ${newContext.spanId}")
    println("🔍 [TRACE] Sampled: ${newContext.isSampled}")
    println("🔍 [TRACE] Traceparent: ${newContext.toTraceparent()}")

    // Reload the UI (trigger API call with new trace)
    viewModel.reload()
}