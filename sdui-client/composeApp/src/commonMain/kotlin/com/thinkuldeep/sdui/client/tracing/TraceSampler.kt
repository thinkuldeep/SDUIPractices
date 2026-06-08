package com.thinkuldeep.sdui.client.tracing

import kotlin.random.Random

enum class Environment {
    PRODUCTION,
    STAGING,
    QA,
    DEVELOPMENT
}

data class SamplingConfig(
    val environment: Environment = Environment.QA,
    val isQaUser: Boolean = false
) {
    fun shouldSample(): Boolean = when {
        isQaUser -> true // QA users always sampled
        environment == Environment.QA -> true // QA environment always sampled
        environment == Environment.STAGING -> Random.nextDouble() < 0.2 // 20% sampling
        environment == Environment.PRODUCTION -> Random.nextDouble() < 0.01 // 1% sampling
        else -> true // Development: always sample
    }

    fun getTraceFlags(): String = if (shouldSample()) "01" else "00"
}

object TraceSamplerHolder {
    private var config: SamplingConfig = SamplingConfig()

    fun setConfig(newConfig: SamplingConfig) {
        config = newConfig
    }

    fun getConfig(): SamplingConfig = config

    fun shouldSample(): Boolean = config.shouldSample()

    fun getTraceFlags(): String = config.getTraceFlags()
}

class TraceSampler(private val config: SamplingConfig = SamplingConfig()) {
    fun shouldSample(): Boolean = config.shouldSample()

    fun getTraceFlags(): String = config.getTraceFlags()
}