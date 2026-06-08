package com.thinkuldeep.sdui.client.network

import com.thinkuldeep.sdui.client.tracing.TracingPlugin
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

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
        install(HttpTimeout) {
            connectTimeoutMillis = 30_000L  // 30 seconds for connection
            requestTimeoutMillis = 60_000L  // 60 seconds for request
            socketTimeoutMillis = 30_000L   // 30 seconds for socket operations
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
        install(HttpTimeout) {
            connectTimeoutMillis = 30_000L
            requestTimeoutMillis = 60_000L
            socketTimeoutMillis = 30_000L
        }
        // No TracingPlugin to avoid infinite loop on export requests
    }
}