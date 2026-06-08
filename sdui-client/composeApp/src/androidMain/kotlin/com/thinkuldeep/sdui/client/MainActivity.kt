package com.thinkuldeep.sdui.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.thinkuldeep.sdui.client.renderer.Render
import com.thinkuldeep.sdui.client.tracing.Environment
import com.thinkuldeep.sdui.client.tracing.TraceContext
import com.thinkuldeep.sdui.client.tracing.TraceContextHolder
import com.thinkuldeep.sdui.client.viewmodel.LandingViewModel

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: LandingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = LandingViewModel()
        viewModel.configureSampling(Environment.STAGING)

        setContent {
            val vm = remember { viewModel }
            val state = vm.uiState.collectAsState()
            val traceContext = vm.traceContext.collectAsState()

            Scaffold(
                floatingActionButton = {
                    Button(
                        onClick = {
                            refreshUI(vm)
                        }
                    ) {
                        Text("🔄 New Trace")
                    }
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    state.value?.let {
                        Render(it, vm)
                    }
                }
            }

            // Log current trace
            traceContext.value?.let { trace ->
                val logMsg = "🔍 [TRACE] Current - TraceID: ${trace.traceId.take(8)}..., " +
                        "Sampled: ${trace.isSampled}, " +
                        "Flags: ${trace.traceFlags}"
                println(logMsg)
            }
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
}