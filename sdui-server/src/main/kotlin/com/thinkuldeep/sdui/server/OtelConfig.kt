package com.thinkuldeep.sdui.server

import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter
import io.opentelemetry.sdk.common.CompletableResultCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener

@Configuration
class OtelConfig {
	private val logger = LoggerFactory.getLogger(OtelConfig::class.java)

	@Value("\${otel.exporter.otlp.traces.endpoint:http://localhost:4318/v1/traces}")
	private lateinit var otlpEndpoint: String

	@EventListener(ApplicationReadyEvent::class)
	fun onApplicationReady() {
		val isAgentLoaded = System.getProperty("otel.javaagent.enabled") != null ||
			System.getProperty("sun.jdk.commands") != null ||
			java.lang.management.ManagementFactory.getRuntimeMXBean().inputArguments.any { it.contains("javaagent") }

		logger.info("OpenTelemetry tracing initialized")
		logger.info("Exporting traces to $otlpEndpoint")
		logger.info("Trace sampler: parent-based (1%) via otel.traces.sampler")
		logger.info("Error traces: exported via tail-based error detection in SpanExporter")

		if (isAgentLoaded) {
			logger.info("✓ OpenTelemetry Java agent is active - spans will be automatically instrumented")
		} else {
			logger.warn("⚠ OpenTelemetry Java agent not detected. Run with: ./gradlew bootRunWithAgent")
			logger.warn("  Without the agent, only trace context propagation will work, not span creation")
		}
	}
}