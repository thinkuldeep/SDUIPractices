package com.thinkuldeep.sdui.client.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

private val json = Json {
    ignoreUnknownKeys = true
    classDiscriminator = "type"
}

class UiComponentSerializationTest {

    @Test
    fun deserialize_text_withAllFields() {
        val result = json.decodeFromString<UiComponent>(
            """{"type":"text","value":"Hello","size":"large","weight":"bold"}"""
        )
        assertIs<UiComponent.Text>(result)
        assertEquals("Hello", result.value)
        assertEquals("large", result.size)
        assertEquals("bold", result.weight)
    }

    @Test
    fun deserialize_text_defaultSizeAndWeight() {
        val result = json.decodeFromString<UiComponent>(
            """{"type":"text","value":"World"}"""
        )
        assertIs<UiComponent.Text>(result)
        assertEquals("World", result.value)
        assertEquals("medium", result.size)
        assertEquals("normal", result.weight)
    }

    @Test
    fun deserialize_column_withChildren() {
        val result = json.decodeFromString<UiComponent>(
            """{"type":"column","children":[{"type":"text","value":"Child"}]}"""
        )
        assertIs<UiComponent.Column>(result)
        assertEquals(1, result.children.size)
        assertIs<UiComponent.Text>(result.children[0])
        assertEquals("Child", (result.children[0] as UiComponent.Text).value)
    }

    @Test
    fun deserialize_column_empty() {
        val result = json.decodeFromString<UiComponent>(
            """{"type":"column","children":[]}"""
        )
        assertIs<UiComponent.Column>(result)
        assertEquals(0, result.children.size)
    }

    @Test
    fun deserialize_row_withMultipleChildren() {
        val result = json.decodeFromString<UiComponent>(
            """{"type":"row","children":[{"type":"text","value":"A"},{"type":"text","value":"B"}]}"""
        )
        assertIs<UiComponent.Row>(result)
        assertEquals(2, result.children.size)
    }

    @Test
    fun deserialize_image_withDimensions() {
        val result = json.decodeFromString<UiComponent>(
            """{"type":"image","url":"https://example.com/img.png","width":200,"height":150}"""
        )
        assertIs<UiComponent.Image>(result)
        assertEquals("https://example.com/img.png", result.url)
        assertEquals(200, result.width)
        assertEquals(150, result.height)
    }

    @Test
    fun deserialize_image_withoutDimensions() {
        val result = json.decodeFromString<UiComponent>(
            """{"type":"image","url":"https://example.com/img.png"}"""
        )
        assertIs<UiComponent.Image>(result)
        assertEquals("https://example.com/img.png", result.url)
        assertNull(result.width)
        assertNull(result.height)
    }

    @Test
    fun deserialize_button() {
        val result = json.decodeFromString<UiComponent>(
            """{"type":"button","id":"btn1","value":"Click Me","action":"load_next_feature"}"""
        )
        assertIs<UiComponent.Button>(result)
        assertEquals("btn1", result.id)
        assertEquals("Click Me", result.value)
        assertEquals("load_next_feature", result.action)
    }

    @Test
    fun deserialize_featuredItems_withChildren() {
        val result = json.decodeFromString<UiComponent>(
            """
            {
                "type": "featuredItems",
                "button": {"type":"button","id":"f1","value":"Next","action":"load_next_feature"},
                "children": [
                    {"type":"text","value":"Item 1"},
                    {"type":"text","value":"Item 2"}
                ]
            }
            """
        )
        assertIs<UiComponent.FeaturedItems>(result)
        assertEquals("f1", result.button.id)
        assertEquals("Next", result.button.value)
        assertEquals(2, result.children.size)
        assertEquals("Item 1", (result.children[0] as UiComponent.Text).value)
    }

    @Test
    fun deserialize_ignoresUnknownFields() {
        val result = json.decodeFromString<UiComponent>(
            """{"type":"text","value":"OK","unknownField":"ignored","anotherExtra":42}"""
        )
        assertIs<UiComponent.Text>(result)
        assertEquals("OK", result.value)
    }

    @Test
    fun serialize_then_deserialize_roundtrip() {
        val original = UiComponent.Column(
            children = listOf(
                UiComponent.Text("Title", "large", "bold"),
                UiComponent.Image("https://example.com/img.png", 100, 100),
                UiComponent.Button("btn", "Click", "some_action")
            )
        )
        val serialized = json.encodeToString(UiComponent.serializer(), original)
        val deserialized = json.decodeFromString<UiComponent>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun deserialize_nestedColumnAndRow() {
        val result = json.decodeFromString<UiComponent>(
            """
            {
                "type": "column",
                "children": [
                    {"type": "text", "value": "Header"},
                    {
                        "type": "row",
                        "children": [
                            {"type": "text", "value": "Left"},
                            {"type": "text", "value": "Right"}
                        ]
                    }
                ]
            }
            """
        )
        assertIs<UiComponent.Column>(result)
        assertEquals(2, result.children.size)
        val row = assertIs<UiComponent.Row>(result.children[1])
        assertEquals(2, row.children.size)
        assertEquals("Left", (row.children[0] as UiComponent.Text).value)
    }
}