package com.thinkuldeep.sdui.client.tracing

import com.thinkuldeep.sdui.client.PlatformConfig
import com.thinkuldeep.sdui.client.threading.threadSafeExecute
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface SpanExporter {
    suspend fun export(spans: List<Span>)
    fun shutdown()
}

data class JaegerExporterConfig(
    val endpoint: String = "http://localhost:4318/v1/traces",
    val serviceName: String = "sdui-mobile-client",
    val batchSize: Int = 10,
    val flushIntervalMs: Long = 5000L
)

class JaegerSpanExporter(
    private val config: JaegerExporterConfig,
    private val httpClient: HttpClient,  // Use exportClient (without tracing) to avoid infinite loops
    private val scope: CoroutineScope
) : SpanExporter {

    private val spanBuffer = mutableListOf<Span>()
    private val bufferLock = Any()
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
        threadSafeExecute(bufferLock) {
            if (spanBuffer.size >= config.batchSize) {
                val spansToExport = spanBuffer.take(config.batchSize)
                spanBuffer.removeAll(spansToExport.toSet())
                spansToExport
            } else {
                null
            }
        }?.let { spansToExport ->
            doExport(spansToExport)
        }
    }

    override suspend fun export(spans: List<Span>) {
        doExport(spans)
    }

    private suspend fun doExport(spans: List<Span>) {
        // Export sampled spans OR spans with errors (errors always exported)
        val spansToExport = spans.filter { it.traceFlags == "01" || it.status == SpanStatus.ERROR }
        if (spansToExport.isEmpty()) {
            println("🔍 [JAEGER] No sampled spans to export (${spans.size} total)")
            return
        }

        try {
            val payload = buildJaegerPayload(spansToExport)
            println("📤 [JAEGER] Sending payload to ${config.endpoint}")
            println("📤 [JAEGER] Payload: ${payload}")

            httpClient.post(config.endpoint) {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            println("✅ [JAEGER] Exported ${spansToExport.size}/${spans.size} sampled spans")
        } catch (e: Exception) {
            println("❌ [JAEGER] Failed to export spans: ${e.message}")
            //e.printStackTrace()
            threadSafeExecute(bufferLock) {
                spanBuffer.addAll(spansToExport)
            }
        }
    }

    override fun shutdown() {
        flushJob?.cancel()
        println("🔍 [JAEGER] Exporter shutdown")
    }

    private fun buildJaegerPayload(spans: List<Span>): String {
        // Build OTLP (OpenTelemetry Protocol) JSON format - validated working format
        val spansJson = spans.joinToString(",") { span ->
            val attributes = mutableListOf<String>()
            attributes.add("""{"key":"device.id","value":{"stringValue":"${PlatformConfig.deviceId}"}}""")
            attributes.add("""{"key":"device.os","value":{"stringValue":"${PlatformConfig.deviceOs}"}}""")

            span.attributes.forEach { (k, v) ->
                val safeVal = v.replace("\"", "\\\"")
                attributes.add("""{"key":"$k","value":{"stringValue":"$safeVal"}}""")
            }

            """
            {
              "traceId": "${span.traceId}",
              "spanId": "${span.spanId}",
              "parentSpanId": "${span.parentSpanId ?: ""}",
              "name": "${span.name.replace("\"", "\\\"")}",
              "startTimeUnixNano": "${span.startTime * 1_000_000}",
              "endTimeUnixNano": "${(span.endTime ?: span.startTime + (span.duration() ?: 0)) * 1_000_000}",
              "attributes": [${attributes.joinToString(",")}]
            }
            """.trimIndent()
        }

        return """
        {
          "resourceSpans": [
            {
              "resource": {
                "attributes": [
                  {"key": "service.name", "value": {"stringValue": "${config.serviceName}"}},
                  {"key": "device.id", "value": {"stringValue": "${PlatformConfig.deviceId}"}},
                  {"key": "device.os", "value": {"stringValue": "${PlatformConfig.deviceOs}"}}
                ]
              },
              "scopeSpans": [
                {
                  "spans": [$spansJson]
                }
              ]
            }
          ]
        }
        """.trimIndent()
    }

    private fun buildTagsJson(span: Span): String {
        val tags = mutableListOf<String>()
        tags.add("""{"key":"span.kind","type":"string","value":"INTERNAL"}""")
        tags.add("""{"key":"device.id","type":"string","value":"${PlatformConfig.deviceId}"}""")
        tags.add("""{"key":"device.os","type":"string","value":"${PlatformConfig.deviceOs}"}""")
        tags.add("""{"key":"status","type":"string","value":"${span.status.name}"}""")
        span.attributes.forEach { (k, v) ->
            val safeVal = v.replace("\"", "\\\"")
            tags.add("""{"key":"$k","type":"string","value":"$safeVal"}""")
        }
        return "[${tags.joinToString(",")}]"
    }

    private fun buildTagsList(span: Span): List<Map<String, String>> {
        val tags = mutableListOf<Map<String, String>>()
        tags.add(mapOf("key" to "span.kind", "type" to "string", "value" to "INTERNAL"))
        tags.add(mapOf("key" to "device.id", "type" to "string", "value" to PlatformConfig.deviceId))
        tags.add(mapOf("key" to "device.os", "type" to "string", "value" to PlatformConfig.deviceOs))
        tags.add(mapOf("key" to "status", "type" to "string", "value" to span.status.name))
        span.attributes.forEach { (k, v) ->
            tags.add(mapOf("key" to k, "type" to "string", "value" to v))
        }
        return tags
    }
}

class NoOpSpanExporter : SpanExporter {
    override suspend fun export(spans: List<Span>) {}
    override fun shutdown() {}
}