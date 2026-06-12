package com.thinkuldeep.sdui.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.thinkuldeep.sdui.client.renderer.RenderWithRefresh
import com.thinkuldeep.sdui.client.tracing.Span
import com.thinkuldeep.sdui.client.tracing.SpanContextHolder
import com.thinkuldeep.sdui.client.tracing.TracingProvider
import com.thinkuldeep.sdui.client.viewmodel.LandingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize sampling at app startup (before any page loads)
        AppInitializer.initializeApp()

        setContent {
            // Create ViewModel inside setContent for proper lifecycle management
            val viewModel = remember {
                LandingViewModel()
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
        // Generate parent span
        var span = Span.create()
        SpanContextHolder.set(span)

        span = TracingProvider.startSpan(name = "UI Action - Refreshing", span)
        viewModel.setSpan(span)
        println("🔄 Refreshing UI with new span")
        println("🔍 [TRACE] New TraceID: ${span.traceId}")
        println("🔍 [TRACE] New SpanID: ${span.spanId}")
        println("🔍 [TRACE] Sampled: ${span.isSampled}")
        println("🔍 [TRACE] Traceparent: ${span.toTraceparent()}");

        // Reload the UI (trigger API call with new span)
        viewModel.reload()

        TracingProvider.endSpan(span)
    }
}