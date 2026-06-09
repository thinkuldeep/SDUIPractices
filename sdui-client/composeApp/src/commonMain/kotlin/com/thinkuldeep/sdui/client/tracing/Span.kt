package com.thinkuldeep.sdui.client.tracing

import kotlinx.serialization.Serializable
import com.thinkuldeep.sdui.client.PlatformConfig
import com.thinkuldeep.sdui.client.threading.threadSafeExecute
import kotlin.random.Random

fun generateTraceId(): String {
    return (0 until 16).map { Random.nextInt(256).toString(16).padStart(2, '0') }.joinToString("")
}

fun generateSpanId(): String {
    return (0 until 8).map { Random.nextInt(256).toString(16).padStart(2, '0') }.joinToString("")
}

expect fun currentTimeMillis(): Long

@Serializable
data class Span(
    val traceId: String,
    val spanId: String,
    val parentSpanId: String? = null,
    val traceFlags: String = "01",
    val traceState: String = "",
    val name: String = "span",
    val startTime: Long = currentTimeMillis(),
    var endTime: Long? = null,
    var status: SpanStatus = SpanStatus.UNSET,
    var attributes: Map<String, String> = emptyMap()
) {
    val isSampled: Boolean get() = traceFlags == "01"

    fun toTraceparent(): String = "$TRACE_VERSION-$traceId-$spanId-$traceFlags"
    fun toTracestate(): String = traceState.ifEmpty { "" }
    fun isEnded(): Boolean = endTime != null
    fun duration(): Long? = endTime?.let { it - startTime }

    companion object {
        private const val TRACE_VERSION = "00"

        fun create(
            traceId: String? = null,
            spanId: String? = null,
            parentSpanId: String? = null,
            name: String = "span",
            traceState: String = "",
            traceFlags: String? = null
        ): Span {
            val finalTraceState = if (traceState.isEmpty()) {
                "device-id=${PlatformConfig.deviceId},device-os=${PlatformConfig.deviceOs}"
            } else {
                traceState
            }
            val flags = traceFlags ?: TraceSamplerHolder.getTraceFlags()

            return Span(
                traceId = traceId ?: generateTraceId(),
                spanId = spanId ?: generateSpanId(),
                parentSpanId = parentSpanId,
                name = name,
                traceState = finalTraceState,
                traceFlags = flags,
                startTime = currentTimeMillis()
            )
        }

        fun createContext(
            traceId: String? = null,
            spanId: String? = null,
            parentSpanId: String? = null,
            traceState: String = "",
            traceFlags: String? = null
        ): Span = create(
            traceId = traceId,
            spanId = spanId,
            parentSpanId = parentSpanId,
            name = "context",
            traceState = traceState,
            traceFlags = traceFlags
        )

        fun current(): Span? = SpanContextHolder.current()
    }
}

enum class SpanStatus {
    UNSET, OK, ERROR
}

object SpanContextHolder {
    private var currentSpan: Span? = null
    private val lock = Any()

    fun set(span: Span?) {
        threadSafeExecute(lock) {
            currentSpan = span
        }
    }

    fun current(): Span? = threadSafeExecute(lock) {
        currentSpan
    }

    fun clear() {
        threadSafeExecute(lock) {
            currentSpan = null
        }
    }
}

