package com.thinkuldeep.sdui.client.tracing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TraceSamplerTest {
    @Test
    fun testProductionEnvironmentSamplesOnePercent() {
        val config = SamplingConfig(environment = Environment.PRODUCTION, isQaUser = false)
        val sampler = TraceSampler(config)

        var sampledCount = 0
        repeat(1000) {
            if (sampler.shouldSample()) {
                sampledCount++
            }
        }

        assertTrue(sampledCount < 50, "Expected less than 5% sampled, got $sampledCount/1000")
        assertTrue(sampledCount > 0, "Expected at least 1 sample in 1000")
    }

    @Test
    fun testStagingEnvironmentSamplesTwentyPercent() {
        val config = SamplingConfig(environment = Environment.STAGING, isQaUser = false)
        val sampler = TraceSampler(config)

        var sampledCount = 0
        repeat(1000) {
            if (sampler.shouldSample()) {
                sampledCount++
            }
        }

        assertTrue(sampledCount > 100, "Expected more than 100 sampled, got $sampledCount/1000")
        assertTrue(sampledCount < 300, "Expected less than 300 sampled, got $sampledCount/1000")
    }

    @Test
    fun testQAEnvironmentAlwaysSamples() {
        val config = SamplingConfig(environment = Environment.QA, isQaUser = false)
        val sampler = TraceSampler(config)

        repeat(100) {
            assertTrue(sampler.shouldSample(), "QA environment should always sample")
        }
    }

    @Test
    fun testDevelopmentEnvironmentAlwaysSamples() {
        val config = SamplingConfig(environment = Environment.DEVELOPMENT, isQaUser = false)
        val sampler = TraceSampler(config)

        repeat(100) {
            assertTrue(sampler.shouldSample(), "Development environment should always sample")
        }
    }

    @Test
    fun testQAUserAlwaysSampled() {
        val config = SamplingConfig(environment = Environment.PRODUCTION, isQaUser = true)
        val sampler = TraceSampler(config)

        repeat(100) {
            assertTrue(sampler.shouldSample(), "QA users should always be sampled")
        }
    }

    @Test
    fun testTraceFlagsForSampledRequest() {
        val config = SamplingConfig(environment = Environment.QA, isQaUser = false)
        assertEquals("01", config.getTraceFlags())
    }

    @Test
    fun testTraceFlagsForNonSampledRequest() {
        val config = SamplingConfig(environment = Environment.PRODUCTION, isQaUser = false)
        var nonSampledFlagsFound = false

        repeat(100) {
            if (config.getTraceFlags() == "00") {
                nonSampledFlagsFound = true
            }
        }

        assertTrue(nonSampledFlagsFound, "Should find at least some non-sampled requests in production")
    }

    @Test
    fun testSamplingConfigCurrent() {
        val config = SamplingConfig(environment = Environment.QA, isQaUser = true)
        SamplingConfig.setCurrent(config)

        val retrievedConfig = SamplingConfig.current()
        assertEquals(Environment.QA, retrievedConfig.environment)
        assertTrue(retrievedConfig.isQaUser)
        assertTrue(SamplingConfig.shouldSample())
    }

    @Test
    fun testSamplingConfigDefaultConfig() {
        SamplingConfig.setCurrent(SamplingConfig()) // Reset to default
        val config = SamplingConfig.current()

        assertEquals(Environment.QA, config.environment) // Default is QA
        assertFalse(config.isQaUser)
        assertTrue(SamplingConfig.shouldSample()) // QA environment always samples
    }
}