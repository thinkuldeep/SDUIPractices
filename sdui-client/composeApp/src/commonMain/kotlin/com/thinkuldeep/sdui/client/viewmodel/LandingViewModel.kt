package com.thinkuldeep.sdui.client.viewmodel

import com.thinkuldeep.sdui.client.data.UiDataSource
import com.thinkuldeep.sdui.client.data.UiRepository
import com.thinkuldeep.sdui.client.model.UiComponent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LandingViewModel(
    private val repository: UiDataSource = UiRepository(),
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val scope = CoroutineScope(dispatcher)
    private val _uiState = MutableStateFlow<UiComponent?>(null)
    val uiState: StateFlow<UiComponent?> = _uiState

    private val featureIndexes = mutableMapOf<String, Int>()
    private var originalTree: UiComponent? = null

    init {
        println("🔥 ViewModel INIT")
        load()
    }

    private fun load() {
        scope.launch {
            println("🔥 Calling API...")
            try {
                val root = repository.fetchLanding()
                originalTree = root
                _uiState.value = applyFeatureFilter(root)
            } catch (e: Exception) {
                println("❌ ViewModel error: ${e.message}")
            }
        }
    }

    fun dispatch(action: String, componentId: String?) {
        when (action) {
            "load_next_feature" -> {
                componentId?.let { id ->
                    val current = featureIndexes[id] ?: 0
                    featureIndexes[id] = current + 1

                    originalTree?.let {
                        _uiState.value = applyFeatureFilter(it)
                    }
                }
            }
        }
    }


    private fun applyFeatureFilter(component: UiComponent): UiComponent {
        return when (component) {

            is UiComponent.Column -> {
                component.copy(
                    children = component.children.map {
                        applyFeatureFilter(it)
                    }
                )
            }

            is UiComponent.FeaturedItems -> {
                val items = component.children
                if (items.isEmpty()) return component

                val index = featureIndexes[component.button.id] ?: 0
                val safeIndex = index % items.size

                component.copy(
                    children = listOf(items[safeIndex])
                )
            }

            else -> component
        }
    }
}