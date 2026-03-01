package com.thinkuldeep.sdui.client.model

import io.ktor.util.reflect.Type
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class UiComponent {

    @Serializable
    @SerialName("column")
    data class Column(
        val children: List<UiComponent>
    ) : UiComponent()

    @Serializable
    @SerialName("row")
    data class Row(
        val children: List<UiComponent>
    ) : UiComponent()

    @Serializable
    @SerialName("text")
    data class Text(
        val value: String,
        val size: String = "medium",
        val weight: String = "normal"
    ) : UiComponent()

    @Serializable
    @SerialName("image")
    data class Image(
        val url: String,
        val width: Int? = null,
        val height: Int? = null
    ) : UiComponent()

    @Serializable
    @SerialName("featuredItems")
    data class FeaturedItems(
        val button: Button,
        val children: List<UiComponent>
    ) : UiComponent()

    @Serializable
    @SerialName("button")
    data class Button(
        val  id: String,
        val value: String,
        val action: String
    ) : UiComponent()
}