package com.thinkuldeep.sdui.client.tracing

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.util.AttributeKey

val TracingPlugin = createClientPlugin("TracingPlugin") {
    onRequest { request, _ ->
        val currentContext = TraceContextHolder.current()
        val traceContext = currentContext ?: TraceContext.create()

        TraceContextHolder.set(traceContext)

        request.headers["traceparent"] = traceContext.toTraceparent()
        if (traceContext.traceState.isNotEmpty()) {
            request.headers["tracestate"] = traceContext.traceState
        }

        request.attributes.put(TRACE_CONTEXT_ATTRIBUTE, traceContext)
    }
}

private val TRACE_CONTEXT_ATTRIBUTE = AttributeKey<TraceContext>("TraceContext")

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