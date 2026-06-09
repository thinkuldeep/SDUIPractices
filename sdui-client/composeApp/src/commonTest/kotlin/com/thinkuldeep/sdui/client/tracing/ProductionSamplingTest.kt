package com.thinkuldeep.sdui.client.tracing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProductionSamplingTest {

    @Test
    fun testProductionEnvironmentSimulation() {
        val config = SamplingConfig(environment = Environment.PRODUCTION, isQaUser = false)
        SamplingConfig.setCurrent(config)

        var sampledCount = 0
        repeat(1000) {
            val span = Span.create()
            if (span.isSampled) sampledCount++
            assertTrue(span.toTraceparent().isNotEmpty())
        }

        assertTrue(sampledCount < 50)
        assertTrue(sampledCount > 0)
    }

    @Test
    fun testProductionWithQAUserSimulation() {
        val config = SamplingConfig(environment = Environment.PRODUCTION, isQaUser = true)
        SamplingConfig.setCurrent(config)

        var sampledCount = 0
        repeat(100) {
            val span = Span.create()
            if (span.isSampled) sampledCount++
        }
        assertEquals(100, sampledCount)
    }

    @Test
    fun testStagingEnvironmentSimulation() {
        val config = SamplingConfig(environment = Environment.STAGING, isQaUser = false)
        SamplingConfig.setCurrent(config)

        var sampledCount = 0
        repeat(1000) {
            val span = Span.create()
            if (span.isSampled) sampledCount++
        }
        assertTrue(sampledCount > 100)
        assertTrue(sampledCount < 300)
    }

    @Test
    fun testQAEnvironmentSimulation() {
        val config = SamplingConfig(environment = Environment.QA, isQaUser = false)
        SamplingConfig.setCurrent(config)

        var sampledCount = 0
        repeat(100) {
            val span = Span.create()
            if (span.isSampled) sampledCount++
        }
        assertEquals(100, sampledCount)
    }

    @Test
    fun testTraceparentAlwaysSentEvenWhenNotSampled() {
        val config = SamplingConfig(environment = Environment.PRODUCTION, isQaUser = false)
        SamplingConfig.setCurrent(config)

        val sampledSpans = mutableListOf<Span>()
        val nonSampledSpans = mutableListOf<Span>()

        repeat(1000) {
            val span = Span.create()
            if (span.isSampled) sampledSpans.add(span) else nonSampledSpans.add(span)
        }

        sampledSpans.forEach { span ->
            assertEquals("01", span.traceFlags)
            assertTrue(span.toTraceparent().endsWith("-01"))
        }

        nonSampledSpans.forEach { span ->
            assertEquals("00", span.traceFlags)
            assertTrue(span.toTraceparent().endsWith("-00"))
        }

        (sampledSpans + nonSampledSpans).forEach { span ->
            val traceparent = span.toTraceparent()
            assertTrue(traceparent.isNotEmpty())
            assertTrue(traceparent.contains(span.traceId))
            assertTrue(traceparent.contains(span.spanId))
        }

        assertTrue(nonSampledSpans.isNotEmpty())
    }

    @Test
    fun testCostAnalysisForProduction() {
        val requestsPerDay = 1_000_000L
        val sampleRate = 0.01
        val tracesPerDay = (requestsPerDay * sampleRate).toLong()
        val bytesPerDay = tracesPerDay * 1024L
        val gbPerMonth = (bytesPerDay * 30) / (1024 * 1024 * 1024)
        val costPerMonth = gbPerMonth * 0.01
        assertTrue(costPerMonth < 100)
    }

    @Test
    fun testEnvironmentSwitching() {
        val environments = listOf(
            Environment.PRODUCTION, Environment.STAGING, Environment.QA, Environment.DEVELOPMENT
        )

        for (env in environments) {
            val config = SamplingConfig(environment = env, isQaUser = false)
            SamplingConfig.setCurrent(config)
            // Just verify the config can be set without errors
            assertFalse(SamplingConfig.current().environment.name.isEmpty())
        }
    }

    @Test
    fun testFullProductionSimulation() {
        val config = SamplingConfig(environment = Environment.PRODUCTION, isQaUser = false)
        SamplingConfig.setCurrent(config)

        val requestSpans = mutableListOf<Span>()
        repeat(100) { requestSpans.add(Span.create()) }
        requestSpans.forEach { assertTrue(it.toTraceparent().isNotEmpty()) }

        val qaConfig = SamplingConfig(environment = Environment.PRODUCTION, isQaUser = true)
        SamplingConfig.setCurrent(qaConfig)
        val qaSpans = mutableListOf<Span>()
        repeat(10) { qaSpans.add(Span.create()) }
        assertEquals(10, qaSpans.filter { it.isSampled }.size)
    }
}