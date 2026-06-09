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
import com.thinkuldeep.sdui.client.renderer.RenderWithRefresh
import com.thinkuldeep.sdui.client.tracing.Environment
import com.thinkuldeep.sdui.client.tracing.OpenTelemetryInit
import com.thinkuldeep.sdui.client.tracing.Span
import com.thinkuldeep.sdui.client.tracing.SpanContextHolder
import com.thinkuldeep.sdui.client.viewmodel.LandingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize OpenTelemetry (currently disabled due to dependency issues)
        // TODO: Fix OTEL initialization and re-enable
        // OpenTelemetryInit.initialize(this)

        setContent {
            // Create ViewModel inside setContent for proper lifecycle management
            val viewModel = remember {
                LandingViewModel().apply {
                    configureSampling(Environment.DEVELOPMENT)
                }
            }

            val state = viewModel.uiState.collectAsState()
            val span = viewModel.span.collectAsState()

            Scaffold { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    state.value?.let {
                        RenderWithRefresh(it, viewModel) {
                            refreshUI(viewModel)
                        }
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
}