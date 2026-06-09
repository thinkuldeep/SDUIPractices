package com.thinkuldeep.sdui.client.tracing

import com.thinkuldeep.sdui.client.PlatformConfig
import com.thinkuldeep.sdui.client.threading.threadSafeExecute
import kotlin.random.Random

// TraceContext is now an alias to Span - use Span for both context and span tracking
typealias TraceContext = Span

object TraceContextHolder {
    private var currentContext: Span? = null
    private val lock = Any()

    fun set(context: Span?) {
        threadSafeExecute(lock) {
            currentContext = context
        }
    }

    fun current(): Span? = threadSafeExecute(lock) {
        currentContext
    }

    fun clear() {
        threadSafeExecute(lock) {
            currentContext = null
        }
    }
}

fun generateTraceId(): String {
    return (0 until 16).map { Random.nextInt(256).toString(16).padStart(2, '0') }.joinToString("")
}

fun generateSpanId(): String {
    return (0 until 8).map { Random.nextInt(256).toString(16).padStart(2, '0') }.joinToString("")
}

expect fun currentTimeMillis(): Long