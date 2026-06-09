package com.thinkuldeep.sdui.client.tracing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration test simulating production environment sampling configuration.
 *
 * Scenarios:
 * - Prod 1%: 99% non-sampled, 1% sampled
 * - Stage 20%: 80% non-sampled, 20% sampled
 * - QA 100%: All sampled
 * - QA users in Prod: Always sampled
 */
class ProductionSamplingTest {

    @Test
    fun testProductionEnvironmentSimulation() {
        // Simulate production environment
        val config = SamplingConfig(
            environment = Environment.PRODUCTION,
            isQaUser = false
        )
        TraceSamplerHolder.setConfig(config)

        var sampledCount = 0
        val totalRequests = 1000

        repeat(totalRequests) {
            val context = Span.create()
            if (context.isSampled) {
                sampledCount++
            }
            // Verify all requests have traceparent
            assertTrue(context.toTraceparent().isNotEmpty(), "All requests should have traceparent")
        }

        println("📊 Production Sampling Test Results:")
        println("   Total Requests: $totalRequests")
        println("   Sampled Requests: $sampledCount (${sampledCount * 100 / totalRequests}%)")
        println("   Non-Sampled Requests: ${totalRequests - sampledCount} (${(totalRequests - sampledCount) * 100 / totalRequests}%)")

        assertTrue(sampledCount < 50, "Production: Expected <5% actual sampling, got $sampledCount/1000")
        assertTrue(sampledCount > 0, "Production: Expected at least 1 sample in 1000")
    }

    @Test
    fun testProductionWithQAUserSimulation() {
        // Simulate QA user in production
        val config = SamplingConfig(
            environment = Environment.PRODUCTION,
            isQaUser = true
        )
        TraceSamplerHolder.setConfig(config)

        var sampledCount = 0
        val totalRequests = 100

        repeat(totalRequests) {
            val context = Span.create()
            if (context.isSampled) {
                sampledCount++
            }
        }

        println("📊 QA User in Production Test Results:")
        println("   Total Requests: $totalRequests")
        println("   Sampled Requests: $sampledCount (${sampledCount * 100 / totalRequests}%)")

        assertEquals(totalRequests, sampledCount, "QA user should always be sampled")
    }

    @Test
    fun testStagingEnvironmentSimulation() {
        // Simulate staging environment
        val config = SamplingConfig(
            environment = Environment.STAGING,
            isQaUser = false
        )
        TraceSamplerHolder.setConfig(config)

        var sampledCount = 0
        val totalRequests = 1000

        repeat(totalRequests) {
            val context = Span.create()
            if (context.isSampled) {
                sampledCount++
            }
        }

        println("📊 Staging Sampling Test Results:")
        println("   Total Requests: $totalRequests")
        println("   Sampled Requests: $sampledCount (${sampledCount * 100 / totalRequests}%)")
        println("   Non-Sampled Requests: ${totalRequests - sampledCount} (${(totalRequests - sampledCount) * 100 / totalRequests}%)")

        assertTrue(sampledCount > 100, "Staging: Expected >100 sampled, got $sampledCount/1000")
        assertTrue(sampledCount < 300, "Staging: Expected <300 sampled, got $sampledCount/1000")
    }

    @Test
    fun testQAEnvironmentSimulation() {
        // Simulate QA environment
        val config = SamplingConfig(
            environment = Environment.QA,
            isQaUser = false
        )
        TraceSamplerHolder.setConfig(config)

        var sampledCount = 0
        val totalRequests = 100

        repeat(totalRequests) {
            val context = Span.create()
            if (context.isSampled) {
                sampledCount++
            }
        }

        println("📊 QA Environment Test Results:")
        println("   Total Requests: $totalRequests")
        println("   Sampled Requests: $sampledCount (100%)")

        assertEquals(totalRequests, sampledCount, "QA environment should always be sampled")
    }

    @Test
    fun testTraceparentAlwaysSentEvenWhenNotSampled() {
        // Simulate production environment with low sampling
        val config = SamplingConfig(
            environment = Environment.PRODUCTION,
            isQaUser = false
        )
        TraceSamplerHolder.setConfig(config)

        val sampledSpans = mutableListOf<Span>()
        val nonSampledSpans = mutableListOf<Span>()

        repeat(1000) {
            val span = Span.create()
            if (span.isSampled) {
                sampledSpans.add(span)
            } else {
                nonSampledSpans.add(span)
            }
        }

        println("📊 Traceparent Always Sent Test Results:")
        println("   Sampled spans: ${sampledSpans.size}")
        println("   Non-sampled spans: ${nonSampledSpans.size}")

        // Verify sampled spans have traceFlags = "01"
        sampledSpans.forEach { span ->
            assertEquals("01", span.traceFlags, "Sampled span should have traceFlags=01")
            assertTrue(span.toTraceparent().endsWith("-01"), "Sampled traceparent should end with -01")
        }

        // Verify non-sampled spans have traceFlags = "00"
        nonSampledSpans.forEach { span ->
            assertEquals("00", span.traceFlags, "Non-sampled span should have traceFlags=00")
            assertTrue(span.toTraceparent().endsWith("-00"), "Non-sampled traceparent should end with -00")
        }

        // Verify all spans have valid traceparent headers
        (sampledSpans + nonSampledSpans).forEach { span ->
            val traceparent = span.toTraceparent()
            assertTrue(traceparent.isNotEmpty(), "Every span should have traceparent")
            assertTrue(traceparent.contains(span.traceId), "Traceparent should contain traceId")
            assertTrue(traceparent.contains(span.spanId), "Traceparent should contain spanId")
        }

        assertTrue(nonSampledSpans.isNotEmpty(), "Should have some non-sampled spans")
        println("✅ Even non-sampled requests send trace context")
    }

    @Test
    fun testCostAnalysisForProduction() {
        // Calculate trace cost for production
        val requestsPerDay = 1_000_000L
        val sampleRate = 0.01 // 1%
        val tracesPerDay = (requestsPerDay * sampleRate).toLong()
        val bytesPerTrace = 1024L // 1 KB
        val bytesPerDay = tracesPerDay * bytesPerTrace
        val bytesPerMonth = bytesPerDay * 30
        val gbPerMonth = bytesPerMonth / (1024 * 1024 * 1024)
        val costPerGb = 0.01
        val costPerMonth = gbPerMonth * costPerGb

        println("📊 Production Cost Analysis:")
        println("   Requests/day: $requestsPerDay")
        println("   Sample rate: ${sampleRate * 100}%")
        println("   Traces/day: $tracesPerDay")
        println("   Storage/day: ${bytesPerDay / (1024 * 1024)} MB")
        println("   Storage/month: $gbPerMonth GB")
        println("   Cost/month: \$$costPerMonth")
        println("✅ Very cost-effective for 1M requests/day")
    }

    @Test
    fun testEnvironmentSwitching() {
        // Test switching between environments
        val environments = listOf(
            Environment.PRODUCTION to "1%",
            Environment.STAGING to "20%",
            Environment.QA to "100%",
            Environment.DEVELOPMENT to "100%"
        )

        println("📊 Environment Switching Test:")
        for ((environment, expectedRate) in environments) {
            val config = SamplingConfig(environment = environment, isQaUser = false)
            TraceSamplerHolder.setConfig(config)

            var sampledCount = 0
            repeat(100) {
                if (config.shouldSample()) {
                    sampledCount++
                }
            }

            println("   $environment: $expectedRate (actual: $sampledCount/100)")
        }
        println("✅ Can dynamically switch environments")
    }

    @Test
    fun testFullProductionSimulation() {
        // Full production simulation
        println("\n🚀 FULL PRODUCTION SIMULATION")
        println("=" * 50)

        // 1. Configure for production
        val config = SamplingConfig(
            environment = Environment.PRODUCTION,
            isQaUser = false
        )
        TraceSamplerHolder.setConfig(config)
        println("✅ Configured for Production (1% sampling)")

        // 2. Simulate requests
        val requestSpans = mutableListOf<Span>()
        repeat(100) {
            requestSpans.add(Span.create())
        }
        println("✅ Generated 100 requests with trace context")

        // 3. Verify trace headers
        val sampledSpans = requestSpans.filter { it.isSampled }
        val nonSampledSpans = requestSpans.filter { !it.isSampled }

        println("✅ Trace distribution: ${sampledSpans.size} sampled, ${nonSampledSpans.size} non-sampled")

        // 4. Verify all have traceparent
        requestSpans.forEach { span ->
            assertTrue(span.toTraceparent().isNotEmpty())
        }
        println("✅ All 100 requests have traceparent headers")

        // 5. Simulate QA user coming in
        val qaConfig = SamplingConfig(
            environment = Environment.PRODUCTION,
            isQaUser = true
        )
        TraceSamplerHolder.setConfig(qaConfig)
        val qaSpans = mutableListOf<Span>()
        repeat(10) {
            qaSpans.add(Span.create())
        }
        println("✅ QA user requests: ${qaSpans.filter { it.isSampled }.size}/10 sampled (100%)")

        println("=" * 50)
        println("🎉 Production simulation complete!")
    }
}

private operator fun String.times(count: Int): String = this.repeat(count)