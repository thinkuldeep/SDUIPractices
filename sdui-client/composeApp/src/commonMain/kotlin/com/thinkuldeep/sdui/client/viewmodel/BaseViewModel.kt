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

    fun configureSampling(config: SamplingConfig) {
        SamplingConfig.setCurrent(config)
        println("🔍 [TRACE] Sampling configured - Environment: ${config.environment}, IsQaUser: ${config.isQaUser}")

        val jaegerConfig = JaegerExporterConfig(
            endpoint = PlatformConfig.jaegerEndpoint,
            serviceName = "sdui-mobile-client"
        )
        val exporter = JaegerSpanExporter(jaegerConfig, HttpClientFactory.exportClient, scope)
        TracingProvider.initialize(exporter, scope)

        if (_span.value == null) {
            val newSpan = Span.create()
            SpanContextHolder.set(newSpan)
            _span.value = newSpan
            onSamplingConfigured()
        }
    }

    fun configureSampling(environment: Environment, isQaUser: Boolean = false) {
        val config = SamplingConfig(environment = environment, isQaUser = isQaUser)
        configureSampling(config)
    }

    protected open fun onSamplingConfigured() {}
}
