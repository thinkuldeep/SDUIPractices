package com.thinkuldeep.sdui.client.tracing

import com.thinkuldeep.sdui.client.threading.threadSafeExecute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object TracingProvider {
    private val lock = Any()
    private var currentSpan: Span? = null
    private val spanStack = mutableListOf<Span>()
    private val completedSpans = mutableListOf<Span>()
    private var exporter: SpanExporter? = null
    private var scope: CoroutineScope? = null

    fun initialize(exporter: SpanExporter, scope: CoroutineScope) {
        threadSafeExecute(lock) {
            this.exporter = exporter
            this.scope = scope
            println("🔍 [TRACER] Initialized with exporter: ${exporter::class.simpleName}")
        }
    }

    fun startSpan(name: String, parentSpan: Span? = null): Span {
        return threadSafeExecute(lock) {
            val parent = parentSpan ?: currentSpan
            val span = Span.create(
                traceId = parent?.traceId,
                spanId = null,
                parentSpanId = parent?.spanId,
                name = name,
                traceState = parent?.traceState ?: ""
            )
            spanStack.add(span)
            currentSpan = span
            println("🔍 [SPAN] Started: ${span.name} (${span.spanId})")
            span
        }
    }

    fun endSpan(span: Span, status: SpanStatus = SpanStatus.OK) {
        threadSafeExecute(lock) {
            span.endTime = currentTimeMillis()
            span.status = status
            spanStack.remove(span)
            completedSpans.add(span)

            if (spanStack.isNotEmpty()) {
                currentSpan = spanStack.last()
            } else {
                currentSpan = null
            }
            val duration = span.duration() ?: 0
            println("🔍 [SPAN] Ended: ${span.name} (${span.spanId}) - ${duration}ms - Status: $status")

            // Export only sampled spans
            if (span.traceFlags == "01") {
                scope?.let { s ->
                    s.launch {
                        exporter?.export(listOf(span))
                    }
                }
            } else {
                println("🔍 [SPAN] Skipping export - not sampled: ${span.name}")
            }
        }
    }

    fun getCurrentSpan(): Span? = threadSafeExecute(lock) {
        currentSpan
    }

    fun addAttribute(span: Span, key: String, value: String) {
        threadSafeExecute(lock) {
            val attributes = span.attributes.toMutableMap()
            attributes[key] = value
            span.attributes = attributes
        }
    }

    fun injectHeaders(span: Span? = getCurrentSpan()): Map<String, String> {
        return threadSafeExecute(lock) {
            span?.let {
                mapOf(
                    "traceparent" to it.toTraceparent(),
                    "tracestate" to it.toTracestate()
                )
            } ?: emptyMap()
        }
    }

    fun clear() {
        threadSafeExecute(lock) {
            currentSpan = null
            spanStack.clear()
            println("🔍 [SPAN] Cleared all spans")
        }
    }
}

suspend inline fun <T> traceBlock(
    name: String,
    block: suspend (span: Span) -> T
): T {
    val span = TracingProvider.startSpan(name)
    return try {
        block(span).also {
            TracingProvider.endSpan(span, SpanStatus.OK)
        }
    } catch (e: Exception) {
        TracingProvider.endSpan(span, SpanStatus.ERROR)
        throw e
    }
}