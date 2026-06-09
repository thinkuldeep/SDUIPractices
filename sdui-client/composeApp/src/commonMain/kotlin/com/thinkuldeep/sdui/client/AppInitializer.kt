package com.thinkuldeep.sdui.client

import com.thinkuldeep.sdui.client.network.HttpClientFactory
import com.thinkuldeep.sdui.client.tracing.Environment
import com.thinkuldeep.sdui.client.tracing.JaegerExporterConfig
import com.thinkuldeep.sdui.client.tracing.JaegerSpanExporter
import com.thinkuldeep.sdui.client.tracing.SamplingConfig
import com.thinkuldeep.sdui.client.tracing.Span
import com.thinkuldeep.sdui.client.tracing.SpanContextHolder
import com.thinkuldeep.sdui.client.tracing.TracingProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

object AppInitializer {
    private var initialized = false

    fun initializeApp(isQaUser: Boolean = false) {
        initializeSampling(Environment.STAGING, isQaUser)
    }

    fun initializeSampling(environment: Environment, isQaUser: Boolean = false) {
        if (initialized) return

        val config = SamplingConfig(environment = environment, isQaUser = isQaUser)
        SamplingConfig.setCurrent(config)
        println("🔍 [TRACE] Sampling configured - Environment: ${config.environment}, IsQaUser: ${config.isQaUser}")

        val jaegerConfig = JaegerExporterConfig(
            endpoint = PlatformConfig.jaegerEndpoint,
            serviceName = "sdui-mobile-client"
        )
        val scope = CoroutineScope(Dispatchers.Default)
        val exporter = JaegerSpanExporter(jaegerConfig, HttpClientFactory.exportClient, scope)
        TracingProvider.initialize(exporter, scope)

        val initialSpan = Span.create()
        SpanContextHolder.set(initialSpan)
        println("🔍 [TRACE] Initial Span set - TraceID: ${initialSpan.traceId}")

        initialized = true
    }
}