package com.thinkuldeep.sdui.client.data

import com.thinkuldeep.sdui.client.PlatformConfig
import com.thinkuldeep.sdui.client.model.UiComponent
import com.thinkuldeep.sdui.client.network.HttpClientFactory
import com.thinkuldeep.sdui.client.tracing.SpanContextHolder
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class UiRepository(private val client: HttpClient = HttpClientFactory.client) : UiDataSource {
    override suspend fun fetchLanding(): UiComponent {
        try {
            val span = SpanContextHolder.current()
            println("🔥 fetchLanding - ${PlatformConfig.baseUrl}/api/ui/landing")
            if (span != null) {
                println("🔍 [TRACE] Traceparent: ${span.toTraceparent()}")
            }
            return client.get("${PlatformConfig.baseUrl}/api/ui/landing").body()
        } catch (e: Exception) {
            println("❌ ERROR: ${e.message}")
            throw e
        }
    }
}