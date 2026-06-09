package com.thinkuldeep.sdui.client.data

import com.thinkuldeep.sdui.client.PlatformConfig
import com.thinkuldeep.sdui.client.model.UiComponent
import com.thinkuldeep.sdui.client.network.HttpClientFactory
import com.thinkuldeep.sdui.client.tracing.Span
import com.thinkuldeep.sdui.client.tracing.SpanContextHolder
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class UiRepository(private val client: HttpClient = HttpClientFactory.client) : UiDataSource {
    override suspend fun fetchLanding(): UiComponent {
        try {
            val currentSpan = SpanContextHolder.current()
            val span = currentSpan ?: Span.create()
            SpanContextHolder.set(span)

            println("🔥 fetchLanding - ${PlatformConfig.baseUrl}/api/ui/landing")
            println("🔍 [TRACE] Traceparent: ${span.toTraceparent()}")

            return client.get("${PlatformConfig.baseUrl}/api/ui/landing").body()
        } catch (e: Exception) {
            println("❌ ERROR: ${e.message}")
            throw e
        }
    }

    fun setSpan(span: Span) {
        SpanContextHolder.set(span)
    }

    fun setSpan(traceparent: String, tracestate: String = "") {
        val span = Span(
            traceId = extractTraceId(traceparent),
            spanId = extractSpanId(traceparent),
            traceState = tracestate
        )
        SpanContextHolder.set(span)
    }

    fun getCurrentSpan(): Span? = SpanContextHolder.current()

    fun clearSpan() {
        SpanContextHolder.clear()
    }

    private fun extractTraceId(traceparent: String): String {
        val parts = traceparent.split("-")
        return if (parts.size >= 2) parts[1] else ""
    }

    private fun extractSpanId(traceparent: String): String {
        val parts = traceparent.split("-")
        return if (parts.size >= 3) parts[2] else ""
    }
}