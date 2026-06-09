package com.thinkuldeep.sdui.client.tracing

import kotlin.random.Random

enum class Environment {
    PRODUCTION, STAGING, QA, DEVELOPMENT
}

data class SamplingConfig(
    val environment: Environment = Environment.QA,
    val isQaUser: Boolean = false
) {
    fun shouldSample(): Boolean = when {
        isQaUser -> true
        environment == Environment.QA -> true
        environment == Environment.STAGING -> Random.nextDouble() < 0.2
        environment == Environment.PRODUCTION -> Random.nextDouble() < 0.01
        else -> true
    }

    fun getTraceFlags(): String = if (shouldSample()) "01" else "00"

    companion object {
        private var instance: SamplingConfig = SamplingConfig()

        fun current(): SamplingConfig = instance

        fun setCurrent(config: SamplingConfig) {
            instance = config
        }

        fun shouldSample(): Boolean = instance.shouldSample()

        fun getTraceFlags(): String = instance.getTraceFlags()
    }
}