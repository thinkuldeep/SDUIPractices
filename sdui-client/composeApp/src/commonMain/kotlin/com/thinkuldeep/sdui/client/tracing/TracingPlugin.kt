package com.thinkuldeep.sdui.client.tracing

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.util.AttributeKey
import com.thinkuldeep.sdui.client.PlatformConfig

private val TRACE_CONTEXT_ATTRIBUTE = AttributeKey<TraceContext>("TraceContext")
private val SPAN_ATTRIBUTE = AttributeKey<Span>("Span")

val TracingPlugin = createClientPlugin("TracingPlugin") {
    onRequest { request, _ ->
        val requestUrl = request.url.toString()

        // Skip tracing for image requests (avoid timeouts)
        if (isImageUrl(requestUrl)) {
            return@onRequest
        }

        val currentContext = TraceContextHolder.current()
        val traceContext = currentContext ?: TraceContext.create()

        TraceContextHolder.set(traceContext)

        // Create a span for this HTTP request
        val requestMethod = request.method.value
        val spanName = "$requestMethod $requestUrl"
        val span = Span.create(
            traceId = traceContext.traceId,
            spanId = null,
            parentSpanId = TracingProvider.getCurrentSpan()?.spanId,
            name = spanName,
            traceState = traceContext.traceState,
            traceFlags = traceContext.traceFlags  // Use trace context's sampling decision
        )
        TracingProvider.addSpanToStack(span)

        // Add request attributes
        TracingProvider.addAttribute(span, "http.method", requestMethod)
        TracingProvider.addAttribute(span, "http.url", requestUrl)

        // Inject tracing headers
        request.headers["traceparent"] = span.toTraceparent()
        if (span.traceState.isNotEmpty()) {
            request.headers["tracestate"] = span.toTracestate()
        }

        request.attributes.put(TRACE_CONTEXT_ATTRIBUTE, traceContext)
        request.attributes.put(SPAN_ATTRIBUTE, span)

        println("🔍 [HTTP] Request started: $requestMethod $requestUrl")
    }

    onResponse { response ->
        val span = response.call.request.attributes.getOrNull(SPAN_ATTRIBUTE)
        span?.let {
            TracingProvider.addAttribute(it, "http.status_code", response.status.value.toString())
            TracingProvider.endSpan(it, SpanStatus.OK)
            println("🔍 [HTTP] Response: ${response.status.value}")
        }
    }
}

private fun isImageUrl(url: String): Boolean {
    val imageExtensions = listOf(".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg", ".bmp")
    val lowerUrl = url.lowercase()
    return imageExtensions.any { lowerUrl.contains(it) }
}

fun HttpRequestBuilder.setTraceContext(context: TraceContext) {
    headers["traceparent"] = context.toTraceparent()
    if (context.traceState.isNotEmpty()) {
        headers["tracestate"] = context.traceState
    }
    attributes.put(TRACE_CONTEXT_ATTRIBUTE, context)
}

fun HttpRequestBuilder.getTraceContext(): TraceContext? {
    return attributes.getOrNull(TRACE_CONTEXT_ATTRIBUTE)
}

fun HttpRequestBuilder.getSpan(): Span? {
    return attributes.getOrNull(SPAN_ATTRIBUTE)
}