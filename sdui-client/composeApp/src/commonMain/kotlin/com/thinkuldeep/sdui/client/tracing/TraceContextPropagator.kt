package com.thinkuldeep.sdui.client.tracing

object TraceContextPropagator {
    private const val TRACEPARENT_HEADER = "traceparent"
    private const val TRACESTATE_HEADER = "tracestate"
    private const val TRACE_VERSION = "00"

    fun extractContext(headers: Map<String, String>): TraceContext? {
        val traceparent = headers[TRACEPARENT_HEADER] ?: return null
        val parts = traceparent.split("-")

        if (parts.size < 4 || parts[0] != TRACE_VERSION) return null

        return TraceContext(
            traceId = parts[1],
            spanId = parts[2],
            traceFlags = parts[3],
            traceState = headers[TRACESTATE_HEADER] ?: ""
        )
    }

    fun injectContext(context: TraceContext, headers: MutableMap<String, String>) {
        headers[TRACEPARENT_HEADER] = context.toTraceparent()
        if (context.traceState.isNotEmpty()) {
            headers[TRACESTATE_HEADER] = context.traceState
        }
    }

    fun createChildContext(
        parentContext: TraceContext? = TraceContextHolder.current(),
        traceState: String = parentContext?.traceState ?: ""
    ): TraceContext {
        return if (parentContext != null) {
            TraceContext(
                traceId = parentContext.traceId,
                spanId = generateSpanId(),
                parentSpanId = parentContext.spanId,
                traceState = traceState
            )
        } else {
            TraceContext.create(traceState = traceState)
        }
    }
}