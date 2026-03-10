package com.thinkuldeep.sdui.client.data

import com.thinkuldeep.sdui.client.model.UiComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class UiRepositoryTest {

    @Test
    fun fetchLanding_returnsDeserializedComponent() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"type":"column","children":[{"type":"text","value":"Hello"}]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; classDiscriminator = "type" })
            }
        }

        val column = assertIs<UiComponent.Column>(UiRepository(client).fetchLanding())
        assertEquals(1, column.children.size)
        assertEquals("Hello", assertIs<UiComponent.Text>(column.children[0]).value)
    }

    @Test
    fun fetchLanding_propagatesNetworkException() = runTest {
        val engine = MockEngine { _ -> throw Exception("Network failure") }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; classDiscriminator = "type" })
            }
        }

        assertFailsWith<Exception> {
            UiRepository(client).fetchLanding()
        }
    }
}