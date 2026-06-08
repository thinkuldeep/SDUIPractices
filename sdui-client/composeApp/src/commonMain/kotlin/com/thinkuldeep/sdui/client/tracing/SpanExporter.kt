package com.thinkuldeep.sdui.client.tracing

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.putJsonArray

interface SpanExporter {
    suspend fun export(spans: List<Span>)
    fun shutdown()
}

data class JaegerExporterConfig(
    val endpoint: String = "http://localhost:14268/api/traces",
    val serviceName: String = "sdui-mobile-client",
    val batchSize: Int = 10,
    val flushIntervalMs: Long = 5000L
)

class JaegerSpanExporter(
    private val config: JaegerExporterConfig,
    private val httpClient: HttpClient,
    private val scope: CoroutineScope
) : SpanExporter {

    private val spanBuffer = mutableListOf<Span>()
    private var flushJob: Job? = null

    init {
        startFlusher()
    }

    private fun startFlusher() {
        flushJob = scope.launch {
            while (true) {
                delay(config.flushIntervalMs)
                flushIfNeeded()
            }
        }
    }

    private suspend fun flushIfNeeded() {
        synchronized(spanBuffer) {
            if (spanBuffer.size >= config.batchSize) {
                val spansToExport = spanBuffer.take(config.batchSize)
                spanBuffer.removeAll(spansToExport.toSet())
                export(spansToExport)
            }
        }
    }

    override suspend fun export(spans: List<Span>) {
        if (spans.isEmpty()) return

        try {
            val payload = buildJaegerPayload(spans)
            httpClient.post(config.endpoint) {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            println("✅ [JAEGER] Exported ${spans.size} spans to ${config.endpoint}")
        } catch (e: Exception) {
            println("❌ [JAEGER] Failed to export spans: ${e.message}")
            // Buffer failed spans for retry
            synchronized(spanBuffer) {
                spanBuffer.addAll(spans)
            }
        }
    }

    override fun shutdown() {
        flushJob?.cancel()
        println("🔍 [JAEGER] Exporter shutdown")
    }

    fun addSpan(span: Span) {
        synchronized(spanBuffer) {
            spanBuffer.add(span)
            if (spanBuffer.size >= config.batchSize) {
                scope.launch {
                    flushIfNeeded()
                }
            }
        }
    }

    private fun buildJaegerPayload(spans: List<Span>): String {
        val json = buildJsonObject {
            putJsonArray("data") {
                spans.forEach { span ->
                    addJsonObject {
                        put("traceID", span.traceId)
                        putJsonArray("spans") {
                            addJsonObject {
                                put("traceID", span.traceId)
                                put("spanID", span.spanId)
                                put("parentSpanID", span.parentSpanId ?: "")
                                put("operationName", span.name)
                                put("startTime", span.startTime * 1_000)
                                put("duration", (span.duration() ?: 0) * 1_000)
                                put("flags", span.traceFlags.toInt(16))

                                putJsonArray("tags") {
                                    addJsonObject {
                                        put("key", "span.kind")
                                        put("type", "string")
                                        put("value", "INTERNAL")
                                    }
                                    addJsonObject {
                                        put("key", "device.id")
                                        put("type", "string")
                                        put("value", PlatformConfig.deviceId)
                                    }
                                    addJsonObject {
                                        put("key", "device.os")
                                        put("type", "string")
                                        put("value", PlatformConfig.deviceOs)
                                    }
                                    addJsonObject {
                                        put("key", "status")
                                        put("type", "string")
                                        put("value", span.status.name)
                                    }

                                    span.attributes.forEach { (k, v) ->
                                        addJsonObject {
                                            put("key", k)
                                            put("type", "string")
                                            put("value", v)
                                        }
                                    }
                                }
                            }
                        }
                        put("processID", "p1")
                    }
                }
            }

            putJsonObject("processes") {
                putJsonObject("p1") {
                    put("serviceName", config.serviceName)
                    putJsonArray("tags") {
                        addJsonObject {
                            put("key", "device.id")
                            put("type", "string")
                            put("value", PlatformConfig.deviceId)
                        }
                        addJsonObject {
                            put("key", "device.os")
                            put("type", "string")
                            put("value", PlatformConfig.deviceOs)
                        }
                    }
                }
            }
        }

        return json.toString()
    }
}

class NoOpSpanExporter : SpanExporter {
    override suspend fun export(spans: List<Span>) {
        // No-op for testing
    }

    override fun shutdown() {
        // No-op
    }
}