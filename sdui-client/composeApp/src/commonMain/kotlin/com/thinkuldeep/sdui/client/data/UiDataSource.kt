package com.thinkuldeep.sdui.client.data

import com.thinkuldeep.sdui.client.model.UiComponent

interface UiDataSource {
    suspend fun fetchLanding(): UiComponent
}