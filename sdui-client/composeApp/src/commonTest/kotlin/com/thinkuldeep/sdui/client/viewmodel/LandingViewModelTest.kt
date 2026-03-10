package com.thinkuldeep.sdui.client.viewmodel

import com.thinkuldeep.sdui.client.data.FakeUiDataSource
import com.thinkuldeep.sdui.client.model.UiComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Tests use Dispatchers.Unconfined so that coroutines in LandingViewModel run
 * synchronously in the calling thread, making assertions straightforward without
 * needing a TestCoroutineScheduler.
 */
class LandingViewModelTest {

    private fun featuredItems(id: String, vararg children: UiComponent) = UiComponent.FeaturedItems(
        button = UiComponent.Button(id, "Next", "load_next_feature"),
        children = children.toList()
    )

    // ── load ─────────────────────────────────────────────────────────────────

    @Test
    fun state_isNull_whenLoadThrows() {
        val vm = LandingViewModel(
            repository = FakeUiDataSource(shouldThrow = true),
            dispatcher = Dispatchers.Unconfined
        )
        assertNull(vm.uiState.value)
    }

    @Test
    fun state_isSet_afterSuccessfulLoad() {
        val root = UiComponent.Column(emptyList())
        val vm = LandingViewModel(FakeUiDataSource(root), Dispatchers.Unconfined)
        assertEquals(root, vm.uiState.value)
    }

    // ── applyFeatureFilter ────────────────────────────────────────────────────

    @Test
    fun featureFilter_showsFirstItem_byDefault() {
        val item1 = UiComponent.Text("Item 1")
        val item2 = UiComponent.Text("Item 2")
        val root = UiComponent.Column(listOf(featuredItems("f1", item1, item2)))

        val vm = LandingViewModel(FakeUiDataSource(root), Dispatchers.Unconfined)

        val featured = assertIs<UiComponent.FeaturedItems>(
            assertIs<UiComponent.Column>(vm.uiState.value).children[0]
        )
        assertEquals(1, featured.children.size)
        assertEquals(item1, featured.children[0])
    }

    @Test
    fun featureFilter_withEmptyChildren_returnsUnchanged() {
        val featured = UiComponent.FeaturedItems(
            button = UiComponent.Button("btn", "Next", "load_next_feature"),
            children = emptyList()
        )
        val root = UiComponent.Column(listOf(featured))
        val vm = LandingViewModel(FakeUiDataSource(root), Dispatchers.Unconfined)

        val filtered = assertIs<UiComponent.FeaturedItems>(
            assertIs<UiComponent.Column>(vm.uiState.value).children[0]
        )
        assertEquals(0, filtered.children.size)
    }

    @Test
    fun featureFilter_appliesRecursivelyThroughNestedColumns() {
        val item = UiComponent.Text("Item")
        val outer = UiComponent.Column(
            listOf(UiComponent.Column(listOf(featuredItems("f1", item))))
        )
        val vm = LandingViewModel(FakeUiDataSource(outer), Dispatchers.Unconfined)

        val innerCol = assertIs<UiComponent.Column>(
            assertIs<UiComponent.Column>(vm.uiState.value).children[0]
        )
        val filtered = assertIs<UiComponent.FeaturedItems>(innerCol.children[0])
        assertEquals(1, filtered.children.size)
        assertEquals(item, filtered.children[0])
    }

    // ── dispatch ──────────────────────────────────────────────────────────────

    @Test
    fun dispatch_loadNextFeature_advancesToNextItem() {
        val item1 = UiComponent.Text("Item 1")
        val item2 = UiComponent.Text("Item 2")
        val root = UiComponent.Column(listOf(featuredItems("f1", item1, item2)))
        val vm = LandingViewModel(FakeUiDataSource(root), Dispatchers.Unconfined)

        vm.dispatch("load_next_feature", "f1")

        val featured = assertIs<UiComponent.FeaturedItems>(
            assertIs<UiComponent.Column>(vm.uiState.value).children[0]
        )
        assertEquals(item2, featured.children[0])
    }

    @Test
    fun dispatch_loadNextFeature_wrapsAroundToFirstItem() {
        val item1 = UiComponent.Text("Item 1")
        val item2 = UiComponent.Text("Item 2")
        val root = UiComponent.Column(listOf(featuredItems("f1", item1, item2)))
        val vm = LandingViewModel(FakeUiDataSource(root), Dispatchers.Unconfined)

        vm.dispatch("load_next_feature", "f1") // index 1 → item2
        vm.dispatch("load_next_feature", "f1") // index 2 % 2 = 0 → item1

        val featured = assertIs<UiComponent.FeaturedItems>(
            assertIs<UiComponent.Column>(vm.uiState.value).children[0]
        )
        assertEquals(item1, featured.children[0])
    }

    @Test
    fun dispatch_withNullComponentId_doesNotChangeState() {
        val root = UiComponent.Column(emptyList())
        val vm = LandingViewModel(FakeUiDataSource(root), Dispatchers.Unconfined)
        val before = vm.uiState.value

        vm.dispatch("load_next_feature", null)

        assertEquals(before, vm.uiState.value)
    }

    @Test
    fun dispatch_unknownAction_doesNotChangeState() {
        val root = UiComponent.Column(emptyList())
        val vm = LandingViewModel(FakeUiDataSource(root), Dispatchers.Unconfined)
        val before = vm.uiState.value

        vm.dispatch("unknown_action", "some_id")

        assertEquals(before, vm.uiState.value)
    }

    @Test
    fun usesDefaultDispatcher_whenNoneProvided() = runTest {
        // Exercises the default `Dispatchers.Default` parameter value (line 12)
        val root = UiComponent.Column(emptyList())
        val vm = LandingViewModel(FakeUiDataSource(root)) // no dispatcher arg — uses default
        val state = withTimeout(2000) { vm.uiState.first { it != null } }
        assertEquals(root, state)
    }

    @Test
    fun dispatch_whenOriginalTreeIsNull_doesNotUpdateState() {
        // Exercises the null path of `originalTree?.let { }` (line 46)
        val vm = LandingViewModel(
            repository = FakeUiDataSource(shouldThrow = true),
            dispatcher = Dispatchers.Unconfined
        )
        assertNull(vm.uiState.value) // load() failed, originalTree is null

        vm.dispatch("load_next_feature", "some_id") // componentId non-null but originalTree is null

        assertNull(vm.uiState.value) // state unchanged
    }

    @Test
    fun applyFeatureFilter_passesThrough_nonContainerChildren() {
        // Exercises the `else -> component` branch (line 78) for Row, Text, Image, Button
        val text = UiComponent.Text("Hello")
        val image = UiComponent.Image("https://example.com/img.png")
        val row = UiComponent.Row(listOf(UiComponent.Text("A")))
        val root = UiComponent.Column(listOf(text, image, row))

        val vm = LandingViewModel(FakeUiDataSource(root), Dispatchers.Unconfined)

        val state = assertIs<UiComponent.Column>(vm.uiState.value)
        assertEquals(text, state.children[0])
        assertEquals(image, state.children[1])
        assertEquals(row, state.children[2])
    }

    @Test
    fun dispatch_multipleFeatureSections_indexedIndependently() {
        val item1a = UiComponent.Text("1A")
        val item1b = UiComponent.Text("1B")
        val item2a = UiComponent.Text("2A")
        val item2b = UiComponent.Text("2B")
        val root = UiComponent.Column(
            listOf(featuredItems("f1", item1a, item1b), featuredItems("f2", item2a, item2b))
        )
        val vm = LandingViewModel(FakeUiDataSource(root), Dispatchers.Unconfined)

        vm.dispatch("load_next_feature", "f1") // only f1 advances

        val cols = assertIs<UiComponent.Column>(vm.uiState.value).children
        assertEquals(item1b, assertIs<UiComponent.FeaturedItems>(cols[0]).children[0])
        assertEquals(item2a, assertIs<UiComponent.FeaturedItems>(cols[1]).children[0])
    }
}