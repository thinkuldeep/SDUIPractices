package com.thinkuldeep.sdui.client

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.window.ComposeUIViewController
import com.thinkuldeep.sdui.client.renderer.Render
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

    Column(modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight()) {
        Button(
            onClick = {
                refreshUI(vm)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text("🔄 New Trace")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()) {
            state.value?.let {
                Render(it, vm)
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