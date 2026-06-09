package com.thinkuldeep.sdui.client.tracing

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.util.AttributeKey
import com.thinkuldeep.sdui.client.PlatformConfig

private val SPAN_CONTEXT_ATTRIBUTE = AttributeKey<Span>("Span")
private val SPAN_ATTRIBUTE = AttributeKey<Span>("Span")

val TracingPlugin = createClientPlugin("TracingPlugin") {
    onRequest { request, _ ->
        val requestUrl = request.url.toString()

        val currentSpan = SpanContextHolder.current()
        val span = currentSpan ?: Span.create()

        SpanContextHolder.set(span)

        // Create a span for this HTTP request
        val requestMethod = request.method.value
        val spanName = "$requestMethod $requestUrl"

        val requestSpan = TracingProvider.startSpan(spanName, span)
        // Add request attributes
        TracingProvider.addAttribute(requestSpan, "http.method", requestMethod)
        TracingProvider.addAttribute(requestSpan, "http.url", requestUrl)

        // Inject tracing headers
        request.headers["traceparent"] = requestSpan.toTraceparent()
        if (requestSpan.traceState.isNotEmpty()) {
            request.headers["tracestate"] = requestSpan.toTracestate()
        }

        request.attributes.put(SPAN_CONTEXT_ATTRIBUTE, span)
        request.attributes.put(SPAN_ATTRIBUTE, requestSpan)

        println("🔍 [HTTP] Request started: $requestMethod $requestUrl")
    }

    onResponse { response ->
        val requestSpan = response.call.request.attributes.getOrNull(SPAN_ATTRIBUTE)
        requestSpan?.let {
            TracingProvider.addAttribute(it, "http.status_code", response.status.value.toString())
            val status = if (response.status.value >= 400) SpanStatus.ERROR else SpanStatus.OK
            TracingProvider.endSpan(it, status)
            println("🔍 [HTTP] Response: ${response.status.value}")
        }
    }
}