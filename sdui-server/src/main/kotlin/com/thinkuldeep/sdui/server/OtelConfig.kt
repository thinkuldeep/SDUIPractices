package com.thinkuldeep.sdui.server

import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.samplers.Sampler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OtelConfig {

	@Bean
	fun traceSampler(): Sampler {
		return Sampler.parentBased(Sampler.traceIdRatioBased(0.01))
	}
}