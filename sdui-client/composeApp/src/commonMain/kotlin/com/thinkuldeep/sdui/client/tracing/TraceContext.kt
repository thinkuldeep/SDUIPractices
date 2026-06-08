package com.thinkuldeep.sdui.client.tracing

import kotlinx.serialization.Serializable
import com.thinkuldeep.sdui.client.PlatformConfig
import com.thinkuldeep.sdui.client.threading.threadSafeExecute

@Serializable
data class TraceContext(
    val traceId: String,
    val spanId: String,
    val parentSpanId: String? = null,
    val traceFlags: String = TraceSamplerHolder.getTraceFlags(),
    val traceState: String = "",
    val timestamp: Long = currentTimeMillis(),
    val isSampled: Boolean = traceFlags == "01"
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
            traceState: String = "",
            traceFlags: String? = null
        ): TraceContext {
            val flags = traceFlags ?: TraceSamplerHolder.getTraceFlags()
            val finalTraceState = if (traceState.isEmpty()) {
                "device-id=${PlatformConfig.deviceId},device-os=${PlatformConfig.deviceOs}"
            } else {
                traceState
            }
            return TraceContext(
                traceId = traceId ?: generateTraceId(),
                spanId = spanId ?: generateSpanId(),
                parentSpanId = parentSpanId,
                traceState = finalTraceState,
                traceFlags = flags,
                isSampled = flags == "01"
            )
        }
    }
}

object TraceContextHolder {
    private var currentContext: TraceContext? = null
    private val lock = Any()

    fun set(context: TraceContext?) {
        threadSafeExecute(lock) {
            currentContext = context
        }
    }

    fun current(): TraceContext? = threadSafeExecute(lock) {
        currentContext
    }

    fun clear() {
        threadSafeExecute(lock) {
            currentContext = null
        }
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