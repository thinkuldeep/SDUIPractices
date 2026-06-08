package com.thinkuldeep.sdui.client.tracing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TraceContextPropagatorTest {
    @Test
    fun testInjectContext() {
        val context = TraceContext.create(
            traceId = "4bf92f3577b34da6a3ce929d0e0e4736",
            spanId = "00f067aa0ba902b7"
        )
        val headers = mutableMapOf<String, String>()

        TraceContextPropagator.injectContext(context, headers)

        assertEquals("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01", headers["traceparent"])
    }

    @Test
    fun testInjectContextWithTraceState() {
        val context = TraceContext.create(
            traceId = "4bf92f3577b34da6a3ce929d0e0e4736",
            spanId = "00f067aa0ba902b7",
            traceState = "vendor=data"
        )
        val headers = mutableMapOf<String, String>()

        TraceContextPropagator.injectContext(context, headers)

        assertEquals("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01", headers["traceparent"])
        assertEquals("vendor=data", headers["tracestate"])
    }

    @Test
    fun testExtractContext() {
        val headers = mapOf(
            "traceparent" to "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
        )

        val context = TraceContextPropagator.extractContext(headers)

        assertNotNull(context)
        assertEquals("4bf92f3577b34da6a3ce929d0e0e4736", context!!.traceId)
        assertEquals("00f067aa0ba902b7", context.spanId)
        assertEquals("01", context.traceFlags)
    }

    @Test
    fun testExtractContextWithTraceState() {
        val headers = mapOf(
            "traceparent" to "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
            "tracestate" to "vendor=data"
        )

        val context = TraceContextPropagator.extractContext(headers)

        assertNotNull(context)
        assertEquals("vendor=data", context!!.traceState)
    }

    @Test
    fun testExtractContextMissingHeader() {
        val headers = mapOf<String, String>()

        val context = TraceContextPropagator.extractContext(headers)

        assertNull(context)
    }

    @Test
    fun testExtractContextInvalidFormat() {
        val headers = mapOf(
            "traceparent" to "invalid-format"
        )

        val context = TraceContextPropagator.extractContext(headers)

        assertNull(context)
    }

    @Test
    fun testCreateChildContext() {
        val parentContext = TraceContext.create(
            traceId = "4bf92f3577b34da6a3ce929d0e0e4736",
            spanId = "00f067aa0ba902b7"
        )

        val childContext = TraceContextPropagator.createChildContext(parentContext)

        assertEquals(parentContext.traceId, childContext.traceId)
        assertEquals(parentContext.spanId, childContext.parentSpanId)
        assertNotNull(childContext.spanId)
    }

    @Test
    fun testCreateChildContextWithoutParent() {
        val childContext = TraceContextPropagator.createChildContext()

        assertNotNull(childContext)
        assertNotNull(childContext.traceId)
        assertNotNull(childContext.spanId)
    }

    @Test
    fun testRoundTripContextPropagation() {
        val originalContext = TraceContext.create(
            traceId = "4bf92f3577b34da6a3ce929d0e0e4736",
            spanId = "00f067aa0ba902b7",
            traceState = "app=myapp"
        )

        val headers = mutableMapOf<String, String>()
        TraceContextPropagator.injectContext(originalContext, headers)

        val extractedContext = TraceContextPropagator.extractContext(headers)

        assertNotNull(extractedContext)
        assertEquals(originalContext.traceId, extractedContext!!.traceId)
        assertEquals(originalContext.spanId, extractedContext.spanId)
        assertEquals(originalContext.traceState, extractedContext.traceState)
    }
}