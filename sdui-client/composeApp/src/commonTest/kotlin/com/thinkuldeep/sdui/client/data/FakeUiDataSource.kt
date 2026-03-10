package com.thinkuldeep.sdui.client.data

import com.thinkuldeep.sdui.client.model.UiComponent

class FakeUiDataSource(
    private val component: UiComponent = UiComponent.Column(emptyList()),
    val shouldThrow: Boolean = false
) : UiDataSource {
    override suspend fun fetchLanding(): UiComponent {
        if (shouldThrow) throw Exception("Network error")
        return component
    }
}