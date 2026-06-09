package com.thinkuldeep.sdui.client.viewmodel

import com.thinkuldeep.sdui.client.PlatformConfig
import com.thinkuldeep.sdui.client.network.HttpClientFactory
import com.thinkuldeep.sdui.client.tracing.Environment
import com.thinkuldeep.sdui.client.tracing.JaegerExporterConfig
import com.thinkuldeep.sdui.client.tracing.JaegerSpanExporter
import com.thinkuldeep.sdui.client.tracing.SamplingConfig
import com.thinkuldeep.sdui.client.tracing.Span
import com.thinkuldeep.sdui.client.tracing.SpanContextHolder
import com.thinkuldeep.sdui.client.tracing.TracingProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseViewModel(dispatcher: CoroutineDispatcher = Dispatchers.Default) {
    protected val scope = CoroutineScope(dispatcher)

    private val _span = MutableStateFlow<Span?>(null)
    val span: StateFlow<Span?> = _span

    fun setSpan(span: Span) {
        SpanContextHolder.set(span)
        _span.value = span
        println("🔍 [TRACE] Span set - TraceID: ${span.traceId}")
    }

    protected fun recordError(error: Throwable) {
        val currentSpan = _span.value
        if (currentSpan != null) {
            TracingProvider.recordError(currentSpan, error)
        } else {
            println("❌ Error occurred (no span): ${error.message}")
        }
    }
}
