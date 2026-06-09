package com.thinkuldeep.sdui.client.tracing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TraceSamplerTest {
    @Test
    fun testProductionEnvironmentSamplesOnePercent() {
        val config = SamplingConfig(environment = Environment.PRODUCTION, isQaUser = false)
        var sampledCount = 0
        repeat(1000) {
            if (config.shouldSample()) sampledCount++
        }
        assertTrue(sampledCount < 50)
        assertTrue(sampledCount > 0)
    }

    @Test
    fun testStagingEnvironmentSamplesTwentyPercent() {
        val config = SamplingConfig(environment = Environment.STAGING, isQaUser = false)
        var sampledCount = 0
        repeat(1000) {
            if (config.shouldSample()) sampledCount++
        }
        assertTrue(sampledCount > 100)
        assertTrue(sampledCount < 300)
    }

    @Test
    fun testQAEnvironmentAlwaysSamples() {
        val config = SamplingConfig(environment = Environment.QA, isQaUser = false)
        repeat(100) {
            assertTrue(config.shouldSample())
        }
    }

    @Test
    fun testDevelopmentEnvironmentAlwaysSamples() {
        val config = SamplingConfig(environment = Environment.DEVELOPMENT, isQaUser = false)
        repeat(100) {
            assertTrue(config.shouldSample())
        }
    }

    @Test
    fun testQAUserAlwaysSampled() {
        val config = SamplingConfig(environment = Environment.PRODUCTION, isQaUser = true)
        repeat(100) {
            assertTrue(config.shouldSample())
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
            if (config.getTraceFlags() == "00") nonSampledFlagsFound = true
        }
        assertTrue(nonSampledFlagsFound)
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