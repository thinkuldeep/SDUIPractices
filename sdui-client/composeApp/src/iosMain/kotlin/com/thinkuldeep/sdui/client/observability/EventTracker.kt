package com.thinkuldeep.sdui.client.observability

actual class EventTracker {
    private val swiftEventTracker = SwiftEventTracker()

    actual fun trackEventStarted(
        operationName: String,
        properties: Map<String, String>
    ): EventSpan {
        return swiftEventTracker.trackEventStarted(operationName, properties)
    }

    actual fun trackEventCompleted(
        startedSpan: EventSpan,
        properties: Map<String, String>
    ): EventSpan {
        return swiftEventTracker.trackEventCompleted(startedSpan, properties)
    }

    actual fun trackError(
        operationName: String,
        errorMessage: String,
        startedSpan: EventSpan?,
        properties: Map<String, String>
    ): EventSpan {
        return swiftEventTracker.trackError(operationName, errorMessage, startedSpan, properties)
    }

    actual fun setUserId(userId: String) {
        swiftEventTracker.setUserId(userId)
    }
}

external class SwiftEventTracker {
    fun trackEventStarted(operationName: String, properties: Map<String, String>): EventSpan
    fun trackEventCompleted(startedSpan: EventSpan, properties: Map<String, String>): EventSpan
    fun trackError(
        operationName: String,
        errorMessage: String,
        startedSpan: EventSpan?,
        properties: Map<String, String>
    ): EventSpan
    fun setUserId(userId: String)
}
