package com.thinkuldeep.sdui.client.data

import com.thinkuldeep.sdui.client.PlatformConfig
import com.thinkuldeep.sdui.client.model.UiComponent
import com.thinkuldeep.sdui.client.network.HttpClientFactory
import com.thinkuldeep.sdui.client.tracing.TraceContext
import com.thinkuldeep.sdui.client.tracing.TraceContextHolder
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class UiRepository(private val client: HttpClient = HttpClientFactory.client) : UiDataSource {
    override suspend fun fetchLanding(): UiComponent {
        try {
            val currentContext = TraceContextHolder.current()
            val traceContext = currentContext ?: TraceContext.create()
            TraceContextHolder.set(traceContext)

            println("🔥 fetchLanding - ${PlatformConfig.baseUrl}/api/ui/landing")
            println("🔍 [TRACE] Traceparent: ${traceContext.toTraceparent()}")

            return client.get("${PlatformConfig.baseUrl}/api/ui/landing").body()
        } catch (e: Exception) {
            println("❌ ERROR: ${e.message}")
            throw e
        }
    }

    fun setTraceContext(context: TraceContext) {
        TraceContextHolder.set(context)
    }

    fun setTraceContext(traceparent: String, tracestate: String = "") {
        val context = TraceContext(
            traceId = extractTraceId(traceparent),
            spanId = extractSpanId(traceparent),
            traceState = tracestate
        )
        TraceContextHolder.set(context)
    }

    fun getCurrentTraceContext(): TraceContext? = TraceContextHolder.current()

    fun clearTraceContext() {
        TraceContextHolder.clear()
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