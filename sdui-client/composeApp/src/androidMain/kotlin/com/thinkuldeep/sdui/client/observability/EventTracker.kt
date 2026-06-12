package com.thinkuldeep.sdui.client.observability

import com.google.firebase.analytics.FirebaseAnalytics

actual class EventTracker {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance()
    private val activeSpans = mutableMapOf<String, Long>() // spanKey -> startTime

    actual fun trackEventStarted(
        operationName: String,
        properties: Map<String, String>
    ): EventSpan {
        val traceId = properties["traceId"] ?: generateTraceId()
        val spanId = generateSpanId()
        val traceFlags = properties["traceFlags"] ?: "01"
        val environment = getEnvironment()

        val spanKey = "$traceId-$spanId"
        activeSpans[spanKey] = System.currentTimeMillis()

        firebaseAnalytics.logEvent("event_started") {
            param("operationName", operationName)
            param("traceId", traceId)
            param("spanId", spanId)
            properties.forEach { (k, v) -> param(k, v) }
        }

        return EventSpan(
            traceId = traceId,
            spanId = spanId,
            eventType = EventType.EVENT_STARTED,
            operationName = operationName,
            traceFlags = traceFlags,
            isSampled = traceFlags == "01",
            environment = environment
        )
    }

    actual fun trackEventCompleted(
        startedSpan: EventSpan,
        properties: Map<String, String>
    ): EventSpan {
        val spanKey = "${startedSpan.traceId}-${startedSpan.spanId}"
        val durationMs = activeSpans.remove(spanKey)?.let {
            System.currentTimeMillis() - it
        }

        firebaseAnalytics.logEvent("event_completed") {
            param("operationName", startedSpan.operationName)
            param("traceId", startedSpan.traceId)
            param("spanId", startedSpan.spanId)
            if (durationMs != null) param("durationMs", durationMs)
            properties.forEach { (k, v) -> param(k, v) }
        }

        return EventSpan(
            traceId = startedSpan.traceId,
            spanId = startedSpan.spanId,
            eventType = EventType.EVENT_COMPLETED,
            operationName = startedSpan.operationName,
            traceFlags = startedSpan.traceFlags,
            isSampled = startedSpan.isSampled,
            environment = startedSpan.environment,
            durationMs = durationMs
        )
    }

    actual fun trackError(
        operationName: String,
        errorMessage: String,
        startedSpan: EventSpan?,
        properties: Map<String, String>
    ): EventSpan {
        val traceId = startedSpan?.traceId ?: generateTraceId()
        val spanId = startedSpan?.spanId ?: generateSpanId()
        val environment = startedSpan?.environment ?: getEnvironment()

        // End the span if it was started
        if (startedSpan != null) {
            val spanKey = "${startedSpan.traceId}-${startedSpan.spanId}"
            activeSpans.remove(spanKey)
        }

        firebaseAnalytics.logEvent("event_error") {
            param("operationName", operationName)
            param("traceId", traceId)
            param("spanId", spanId)
            param("errorMessage", errorMessage)
            properties.forEach { (k, v) -> param(k, v) }
        }

        return EventSpan(
            traceId = traceId,
            spanId = spanId,
            eventType = EventType.ERROR,
            operationName = operationName,
            traceFlags = "01",
            isSampled = true,
            environment = environment
        )
    }

    actual fun setUserId(userId: String) {
        firebaseAnalytics.setUserId(userId)
    }

    private fun getEnvironment(): String = "PRODUCTION"

    private fun generateTraceId(): String {
        return (0 until 32)
            .map { java.util.Random().nextInt(16).toString(16) }
            .joinToString("")
    }

    private fun generateSpanId(): String {
        return (0 until 16)
            .map { java.util.Random().nextInt(16).toString(16) }
            .joinToString("")
    }
}
