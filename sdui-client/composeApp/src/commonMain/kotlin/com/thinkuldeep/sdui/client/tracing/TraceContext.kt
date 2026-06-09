package com.thinkuldeep.sdui.client.tracing

// TraceContext is now an alias to Span - use Span for both context and span tracking
typealias TraceContext = Span

// Backward compatibility: TraceContextHolder delegates to SpanContextHolder
object TraceContextHolder {
    fun set(context: Span?) = SpanContextHolder.set(context)
    fun current(): Span? = SpanContextHolder.current()
    fun clear() = SpanContextHolder.clear()
}