package com.thinkuldeep.sdui.client.tracing

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.util.AttributeKey

private val TRACE_CONTEXT_ATTRIBUTE = AttributeKey<TraceContext>("TraceContext")
private val SPAN_ATTRIBUTE = AttributeKey<Span>("Span")

val TracingPlugin = createClientPlugin("TracingPlugin") {
    onRequest { request, _ ->
        val currentContext = TraceContextHolder.current()
        val traceContext = currentContext ?: TraceContext.create()

        TraceContextHolder.set(traceContext)

        // Create a span for this HTTP request
        val requestUrl = request.url.toString()
        val requestMethod = request.method.value
        val spanName = "$requestMethod ${request.url.host}"
        val span = TracingProvider.startSpan(
            name = spanName,
            parentSpan = TracingProvider.getCurrentSpan()
        )

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