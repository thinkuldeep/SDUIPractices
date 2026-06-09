package com.thinkuldeep.sdui.client.tracing

import com.thinkuldeep.sdui.client.threading.threadSafeExecute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object TracingProvider {
    private val lock = Any()
    private var currentSpan: Span? = null
    private val spanStack = mutableListOf<Span>()
    private val completedSpans = mutableListOf<Span>()
    private val exportedSpans = mutableSetOf<String>()
    private var exporter: SpanExporter? = null
    private var scope: CoroutineScope? = null

    fun initialize(exporter: SpanExporter, scope: CoroutineScope) {
        threadSafeExecute(lock) {
            this.exporter = exporter
            this.scope = scope
            println("🔍 [TRACER] Initialized with exporter: ${exporter::class.simpleName}")
        }
    }

    fun startSpan(name: String, parentSpan: Span): Span {
        return threadSafeExecute(lock) {
            val span = Span.create(
                traceId = parentSpan.traceId,
                spanId = null,
                parentSpanId = currentSpan?.spanId,
                name = name,
                traceState = parentSpan.traceState ?: "",
                traceFlags = parentSpan.traceFlags
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

            exportIfSampled(span)
        }
    }

    fun recordError(parentSpan: Span, error: Throwable) {
        threadSafeExecute(lock) {
            println("🔍 [ERROR] ${error::class.simpleName}: ${error.message}")

            // Record error as attributes on parent span
            val attributes = parentSpan.attributes.toMutableMap()
            attributes["error.type"] = error::class.simpleName ?: "Unknown"
            attributes["error.message"] = error.message ?: "No message"
            parentSpan.attributes = attributes
            parentSpan.status = SpanStatus.ERROR

            // If span wasn't exported yet (was skipped due to not sampled), export now because of error
            if (parentSpan.spanId !in exportedSpans) {
                println("🔍 [SPAN] Exporting previously skipped span due to error: ${parentSpan.name}")
                scope?.let { s ->
                    s.launch {
                        exporter?.export(listOf(parentSpan))
                    }
                }
            }
        }
    }

    private fun exportIfSampled(span: Span) {
        val shouldExport = span.isSampled || span.status == SpanStatus.ERROR
        if (shouldExport) {
            exportedSpans.add(span.spanId)
            scope?.let { s ->
                s.launch {
                    exporter?.export(listOf(span))
                }
            }
        } else {
            println("🔍 [SPAN] Skipping export - not sampled: ${span.name}")
        }
    }

    fun addAttribute(span: Span, key: String, value: String) {
        threadSafeExecute(lock) {
            val attributes = span.attributes.toMutableMap()
            attributes[key] = value
            span.attributes = attributes
        }
    }
}