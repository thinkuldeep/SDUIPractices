package com.thinkuldeep.sdui.client.data

import com.thinkuldeep.sdui.client.PlatformConfig
import com.thinkuldeep.sdui.client.model.UiComponent
import com.thinkuldeep.sdui.client.network.HttpClientFactory
import com.thinkuldeep.sdui.client.tracing.SpanContextHolder
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

class UiRepository(private val client: HttpClient = HttpClientFactory.client) : UiDataSource {
    override suspend fun fetchLanding(): UiComponent {
        try {
            val span = SpanContextHolder.current()
            println("🔥 fetchLanding - ${PlatformConfig.baseUrl}/api/ui/landing")
            if (span != null) {
                println("🔍 [TRACE] Traceparent: ${span.toTraceparent()}")
            }
            val response = client.get("${PlatformConfig.baseUrl}/api/ui/landing")
            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                throw Exception("HTTP ${response.status.value}: $errorBody")
            }
            return response.body()
        } catch (e: Exception) {
            println("❌ ERROR: ${e.message}")
            throw e
        }
    }
}