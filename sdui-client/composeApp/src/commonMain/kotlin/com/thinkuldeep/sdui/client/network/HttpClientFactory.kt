package com.thinkuldeep.sdui.client.network

import com.thinkuldeep.sdui.client.tracing.TracingPlugin
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClientFactory {
    // Main client with tracing for API requests
    val client = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    classDiscriminator = "type"
                }
            )
        }
        install(TracingPlugin)
    }

    // Separate client for observability exports (without tracing to avoid loops)
    val exportClient = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }
        // No TracingPlugin to avoid infinite loop on export requests
    }
}