package com.thinkuldeep.sdui.server.controller

import com.thinkuldeep.sdui.server.model.Column
import com.thinkuldeep.sdui.server.model.FeaturedItems
import com.thinkuldeep.sdui.server.model.Text
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@WebMvcTest(LandingPageController::class)
class LandingPageControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    private val controller = LandingPageController()

    // --- Unit tests ---

    @Test
    fun `landingPage returns a Column as root component`() {
        val result = controller.landingPage()
        assertIs<Column>(result)
    }

    @Test
    fun `landingPage root column has four children`() {
        val result = controller.landingPage() as Column
        assertEquals(4, result.children.size)
    }

    @Test
    fun `landingPage first child is title Text with large bold style`() {
        val root = controller.landingPage() as Column
        val title = assertIs<Text>(root.children[0])
        assertEquals("Welcome to Kuldeep's Space", title.value)
        assertEquals("large", title.size)
        assertEquals("bold", title.weight)
    }

    @Test
    fun `landingPage second child is subtitle Text with medium style`() {
        val root = controller.landingPage() as Column
        val subtitle = assertIs<Text>(root.children[1])
        assertEquals("The world of learning, sharing, and caring", subtitle.value)
        assertEquals("medium", subtitle.size)
        assertEquals("medium", subtitle.weight)
    }

    @Test
    fun `landingPage third child is books FeaturedItems`() {
        val root = controller.landingPage() as Column
        val books = assertIs<FeaturedItems>(root.children[2])
        assertEquals("books", books.button.id)
        assertEquals("load_next_feature", books.button.action)
        assertTrue(books.button.value.contains("Books"))
    }

    @Test
    fun `landingPage books FeaturedItems has four children`() {
        val root = controller.landingPage() as Column
        val books = assertIs<FeaturedItems>(root.children[2])
        assertEquals(4, books.children.size)
    }

    @Test
    fun `landingPage fourth child is articles FeaturedItems`() {
        val root = controller.landingPage() as Column
        val articles = assertIs<FeaturedItems>(root.children[3])
        assertEquals("articles", articles.button.id)
        assertEquals("load_next_feature", articles.button.action)
        assertTrue(articles.button.value.contains("Articles"))
    }

    @Test
    fun `landingPage articles FeaturedItems has two children`() {
        val root = controller.landingPage() as Column
        val articles = assertIs<FeaturedItems>(root.children[3])
        assertEquals(2, articles.children.size)
    }

    // --- Integration tests via MockMvc ---

    @Test
    fun `GET landing endpoint returns 200 OK`() {
        mockMvc.get("/api/ui/landing")
            .andExpect { status { isOk() } }
    }

    @Test
    fun `GET landing endpoint returns JSON content type`() {
        mockMvc.get("/api/ui/landing")
            .andExpect { content { contentTypeCompatibleWith("application/json") } }
    }

    @Test
    fun `GET landing endpoint root type is column`() {
        mockMvc.get("/api/ui/landing")
            .andExpect { jsonPath("$.type") { value("column") } }
    }

    @Test
    fun `GET landing endpoint root column has four children`() {
        mockMvc.get("/api/ui/landing")
            .andExpect { jsonPath("$.children.length()") { value(4) } }
    }

    @Test
    fun `GET landing endpoint title text has correct value and style`() {
        mockMvc.get("/api/ui/landing")
            .andExpect { jsonPath("$.children[0].value") { value("Welcome to Kuldeep's Space") } }
            .andExpect { jsonPath("$.children[0].size") { value("large") } }
            .andExpect { jsonPath("$.children[0].weight") { value("bold") } }
    }

    @Test
    fun `GET landing endpoint books section has correct button id`() {
        mockMvc.get("/api/ui/landing")
            .andExpect { jsonPath("$.children[2].type") { value("featuredItems") } }
            .andExpect { jsonPath("$.children[2].button.id") { value("books") } }
            .andExpect { jsonPath("$.children[2].button.action") { value("load_next_feature") } }
    }

    @Test
    fun `GET landing endpoint articles section has correct button id`() {
        mockMvc.get("/api/ui/landing")
            .andExpect { jsonPath("$.children[3].type") { value("featuredItems") } }
            .andExpect { jsonPath("$.children[3].button.id") { value("articles") } }
            .andExpect { jsonPath("$.children[3].button.action") { value("load_next_feature") } }
    }

    @Test
    fun `GET landing endpoint with query string returns 200 OK`() {
        mockMvc.get("/api/ui/landing?page=1")
            .andExpect { status { isOk() } }
    }
}