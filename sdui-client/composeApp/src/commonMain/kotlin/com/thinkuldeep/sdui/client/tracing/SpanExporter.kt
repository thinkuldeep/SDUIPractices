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
    val endpoint: String = "http://localhost:14268/api/traces",
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
        // Filter to only sampled spans
        val sampledSpans = spans.filter { it.traceFlags == "01" }
        if (sampledSpans.isEmpty()) {
            println("🔍 [JAEGER] No sampled spans to export (${spans.size} total)")
            return
        }

        try {
            val payload = buildJaegerPayload(sampledSpans)
            println("📤 [JAEGER] Sending payload to ${config.endpoint}")
            println("📤 [JAEGER] Payload: ${payload.take(200)}...")

            httpClient.post(config.endpoint) {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            println("✅ [JAEGER] Exported ${sampledSpans.size}/${spans.size} sampled spans")
        } catch (e: Exception) {
            println("❌ [JAEGER] Failed to export spans: ${e.message}")
            e.printStackTrace()
            threadSafeExecute(bufferLock) {
                spanBuffer.addAll(sampledSpans)
            }
        }
    }

    override fun shutdown() {
        flushJob?.cancel()
        println("🔍 [JAEGER] Exporter shutdown")
    }

    private fun buildJaegerPayload(spans: List<Span>): String {
        val spanJson = spans.map { span ->
            """
            {
              "traceID": "${span.traceId}",
              "spanID": "${span.spanId}",
              "parentSpanID": "${span.parentSpanId ?: ""}",
              "operationName": "${span.name}",
              "startTime": ${span.startTime * 1000},
              "duration": ${(span.duration() ?: 0) * 1000},
              "tags": ${buildTagsJson(span)}
            }
            """.trimIndent()
        }

        return """
        {
          "data": [
            {
              "traceID": "${spans.first().traceId}",
              "spans": [${spanJson.joinToString(",")}],
              "processID": "p1"
            }
          ],
          "processes": {
            "p1": {
              "serviceName": "${config.serviceName}",
              "tags": [
                {
                  "key": "device.id",
                  "type": "string",
                  "value": "${PlatformConfig.deviceId}"
                },
                {
                  "key": "device.os",
                  "type": "string",
                  "value": "${PlatformConfig.deviceOs}"
                }
              ]
            }
          }
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
}

class NoOpSpanExporter : SpanExporter {
    override suspend fun export(spans: List<Span>) {}
    override fun shutdown() {}
}