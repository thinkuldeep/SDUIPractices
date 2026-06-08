package com.thinkuldeep.sdui.client.tracing

import android.content.Context
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.exporter.jaeger.thrift.JaegerThriftSpanExporter
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.ResourceAttributes
import io.opentelemetry.api.GlobalOpenTelemetry

object OpenTelemetryInit {
    private var isInitialized = false

    fun initialize(context: Context, jaegerEndpoint: String = "http://10.0.2.2:14250") {
        if (isInitialized) return

        try {
            // Create Jaeger exporter
            val jaegerExporter = JaegerThriftSpanExporter.builder()
                .setEndpoint(jaegerEndpoint)
                .build()

            // Create resource with service name and device info
            val resource = Resource.getDefault()
                .merge(
                    Resource.builder()
                        .put(ResourceAttributes.SERVICE_NAME, "sdui-mobile-client")
                        .put("device.id", PlatformConfig.deviceId)
                        .put("device.os", "android")
                        .build()
                )

            // Create tracer provider with Jaeger exporter
            val tracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(SimpleSpanProcessor.create(jaegerExporter))
                .build()

            // Set global OpenTelemetry instance
            OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal()

            isInitialized = true
            println("🔍 [OTEL] OpenTelemetry initialized for Android")
        } catch (e: Exception) {
            println("❌ [OTEL] Failed to initialize OpenTelemetry: ${e.message}")
        }
    }
}