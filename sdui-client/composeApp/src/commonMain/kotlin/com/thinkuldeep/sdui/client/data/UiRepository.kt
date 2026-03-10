package com.thinkuldeep.sdui.client.data

import com.thinkuldeep.sdui.client.PlatformConfig
import com.thinkuldeep.sdui.client.model.UiComponent
import com.thinkuldeep.sdui.client.network.HttpClientFactory
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class UiRepository(private val client: HttpClient = HttpClientFactory.client) : UiDataSource {
    override suspend fun fetchLanding(): UiComponent {
        try {
            println("🔥 fetchLanding - ${PlatformConfig.baseUrl}/api/ui/landing")
            return client.get("${PlatformConfig.baseUrl}/api/ui/landing").body()
        } catch (e: Exception) {
            println("❌ ERROR: ${e.message}")
            throw e
        }
    }
}