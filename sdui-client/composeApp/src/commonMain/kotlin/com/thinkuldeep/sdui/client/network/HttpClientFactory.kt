package com.thinkuldeep.sdui.client.network

import com.thinkuldeep.sdui.client.tracing.TracingPlugin
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClientFactory {
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
}