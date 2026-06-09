package com.thinkuldeep.sdui.client.tracing

import kotlinx.serialization.Serializable
import com.thinkuldeep.sdui.client.PlatformConfig

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
    }
}

enum class SpanStatus {
    UNSET, OK, ERROR
}