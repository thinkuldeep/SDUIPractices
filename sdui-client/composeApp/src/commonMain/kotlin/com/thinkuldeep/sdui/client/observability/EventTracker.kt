package com.thinkuldeep.sdui.client.observability

enum class EventType {
    EVENT_STARTED,
    EVENT_COMPLETED,
    ERROR
}

data class EventSpan(
    val traceId: String,
    val spanId: String,
    val eventType: EventType,
    val operationName: String,
    val traceFlags: String,
    val isSampled: Boolean,
    val environment: String,
    val durationMs: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toTraceContext(): TraceContext = TraceContext(
        traceId = traceId,
        spanId = spanId,
        traceFlags = traceFlags,
        isSampled = isSampled
    )
}

data class TraceContext(
    val traceId: String,
    val spanId: String,
    val traceFlags: String,
    val isSampled: Boolean
) {
    fun toTraceparent(): String = "00-$traceId-$spanId-$traceFlags"

    fun toHeaders(): Map<String, String> = mapOf(
        "traceparent" to toTraceparent(),
        "X-Trace-ID" to traceId,
        "X-Parent-Span-ID" to spanId
    )
}

expect class EventTracker {
    fun trackEventStarted(
        operationName: String,
        properties: Map<String, String> = emptyMap()
    ): EventSpan

    fun trackEventCompleted(
        startedSpan: EventSpan,
        properties: Map<String, String> = emptyMap()
    ): EventSpan

    fun trackError(
        operationName: String,
        errorMessage: String,
        startedSpan: EventSpan? = null,
        properties: Map<String, String> = emptyMap()
    ): EventSpan

    fun setUserId(userId: String)
}

object EventTrackerProvider {
    val instance: EventTracker by lazy { EventTracker() }
}
