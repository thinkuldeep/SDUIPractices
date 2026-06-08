package com.thinkuldeep.sdui.client.tracing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TraceContextTest {
    @Test
    fun testTraceContextCreation() {
        val context = TraceContext.create()
        assertNotNull(context)
        assertEquals(32, context.traceId.length)
        assertEquals(16, context.spanId.length)
        assertEquals("01", context.traceFlags)
    }

    @Test
    fun testTraceContextWithCustomValues() {
        val traceId = "4bf92f3577b34da6a3ce929d0e0e4736"
        val spanId = "00f067aa0ba902b7"
        val context = TraceContext.create(
            traceId = traceId,
            spanId = spanId
        )
        assertEquals(traceId, context.traceId)
        assertEquals(spanId, context.spanId)
    }

    @Test
    fun testTraceparentFormatting() {
        val context = TraceContext.create(
            traceId = "4bf92f3577b34da6a3ce929d0e0e4736",
            spanId = "00f067aa0ba902b7"
        )
        val traceparent = context.toTraceparent()
        assertEquals("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01", traceparent)
    }

    @Test
    fun testTraceStateFormatting() {
        val context = TraceContext(
            traceId = "4bf92f3577b34da6a3ce929d0e0e4736",
            spanId = "00f067aa0ba902b7",
            traceState = "vendor=data"
        )
        assertEquals("vendor=data", context.toTracestate())
    }

    @Test
    fun testTraceContextHolder() {
        TraceContextHolder.clear()
        assertNull(TraceContextHolder.current())

        val context = TraceContext.create()
        TraceContextHolder.set(context)
        assertEquals(context, TraceContextHolder.current())

        TraceContextHolder.clear()
        assertNull(TraceContextHolder.current())
    }

    @Test
    fun testGenerateTraceId() {
        val traceId1 = generateTraceId()
        val traceId2 = generateTraceId()

        assertEquals(32, traceId1.length)
        assertEquals(32, traceId2.length)
        assertNotNull(traceId1)
        assertNotNull(traceId2)
    }

    @Test
    fun testGenerateSpanId() {
        val spanId1 = generateSpanId()
        val spanId2 = generateSpanId()

        assertEquals(16, spanId1.length)
        assertEquals(16, spanId2.length)
        assertNotNull(spanId1)
        assertNotNull(spanId2)
    }
}