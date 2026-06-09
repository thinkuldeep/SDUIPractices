package com.thinkuldeep.sdui.client.tracing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SpanTest {
    @Test
    fun testSpanCreation() {
        val span = Span.create()
        assertNotNull(span)
        assertEquals(32, span.traceId.length)
        assertEquals(16, span.spanId.length)
        assertEquals("01", span.traceFlags)
    }

    @Test
    fun testSpanWithCustomValues() {
        val traceId = "4bf92f3577b34da6a3ce929d0e0e4736"
        val spanId = "00f067aa0ba902b7"
        val span = Span.create(
            traceId = traceId,
            spanId = spanId
        )
        assertEquals(traceId, span.traceId)
        assertEquals(spanId, span.spanId)
    }

    @Test
    fun testTraceparentFormatting() {
        val span = Span.create(
            traceId = "4bf92f3577b34da6a3ce929d0e0e4736",
            spanId = "00f067aa0ba902b7"
        )
        val traceparent = span.toTraceparent()
        assertEquals("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01", traceparent)
    }

    @Test
    fun testTraceStateFormatting() {
        val span = Span(
            traceId = "4bf92f3577b34da6a3ce929d0e0e4736",
            spanId = "00f067aa0ba902b7",
            traceState = "vendor=data"
        )
        assertEquals("vendor=data", span.toTracestate())
    }

    @Test
    fun testSpanContextHolder() {
        SpanContextHolder.clear()
        assertNull(SpanContextHolder.current())

        val span = Span.create()
        SpanContextHolder.set(span)
        assertEquals(span, SpanContextHolder.current())

        SpanContextHolder.clear()
        assertNull(SpanContextHolder.current())
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