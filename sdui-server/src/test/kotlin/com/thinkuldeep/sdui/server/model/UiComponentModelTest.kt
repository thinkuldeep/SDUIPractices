package com.thinkuldeep.sdui.server.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UiComponentModelTest {

    @Test
    fun `Row has correct type and children`() {
        val row = Row(children = listOf(Text(value = "item")))
        assertEquals("row", row.type)
        assertEquals(1, row.children.size)
    }

    @Test
    fun `Row getChildren returns children list`() {
        val text = Text(value = "a")
        val row = Row(children = listOf(text))
        assertEquals(text, row.children[0])
    }

    @Test
    fun `Text uses default size and weight when not specified`() {
        val text = Text(value = "hello")
        assertEquals("text", text.type)
        assertEquals("hello", text.value)
        assertEquals("medium", text.size)
        assertEquals("normal", text.weight)
    }

    @Test
    fun `Image uses null width and height by default`() {
        val image = Image(url = "https://example.com/img.png")
        assertEquals("image", image.type)
        assertEquals("https://example.com/img.png", image.url)
        assertNull(image.width)
        assertNull(image.height)
    }

    @Test
    fun `Button uses default value when not specified`() {
        val button = Button(id = "btn1", action = "click")
        assertEquals("button", button.type)
        assertEquals("btn1", button.id)
        assertEquals("Click Me", button.value)
        assertEquals("click", button.action)
    }
}