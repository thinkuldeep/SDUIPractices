package com.thinkuldeep.sdui.client.tracing

import kotlinx.serialization.Serializable

@Serializable
data class TraceContext(
    val traceId: String,
    val spanId: String,
    val parentSpanId: String? = null,
    val traceFlags: String = "01",
    val traceState: String = "",
    val timestamp: Long = currentTimeMillis()
) {
    fun toTraceparent(): String = "$TRACE_VERSION-$traceId-$spanId-$traceFlags"

    fun toTracestate(): String = traceState.ifEmpty { "" }

    companion object {
        private const val TRACE_VERSION = "00"

        fun current(): TraceContext? = TraceContextHolder.current()

        fun create(
            traceId: String? = null,
            spanId: String? = null,
            parentSpanId: String? = null,
            traceState: String = ""
        ): TraceContext {
            return TraceContext(
                traceId = traceId ?: generateTraceId(),
                spanId = spanId ?: generateSpanId(),
                parentSpanId = parentSpanId,
                traceState = traceState
            )
        }
    }
}

object TraceContextHolder {
    private var currentContext: TraceContext? = null

    fun set(context: TraceContext?) {
        currentContext = context
    }

    fun current(): TraceContext? = currentContext

    fun clear() {
        currentContext = null
    }
}

fun generateTraceId(): String {
    val random = kotlin.random.Random
    return (0 until 16).map { (random.nextInt(256)).toString(16).padStart(2, '0') }.joinToString("")
}

fun generateSpanId(): String {
    val random = kotlin.random.Random
    return (0 until 8).map { (random.nextInt(256)).toString(16).padStart(2, '0') }.joinToString("")
}

expect fun currentTimeMillis(): Long