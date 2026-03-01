package com.thinkuldeep.sdui.server.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = Column::class, name = "column"),
    JsonSubTypes.Type(value = Row::class, name = "row"),
    JsonSubTypes.Type(value = Text::class, name = "text"),
    JsonSubTypes.Type(value = Image::class, name = "image"),
    JsonSubTypes.Type(value = Button::class, name = "button"),
    JsonSubTypes.Type(value = FeaturedItems::class, name = "featuredItems"),
)
sealed interface UiComponent {
    val type: String
}

data class Column(
    override val type: String = "column",
    val children: List<UiComponent>
) : UiComponent

data class Row(
    override val type: String = "row",
    val children: List<UiComponent>
) : UiComponent

data class Text(
    override val type: String = "text",
    val value: String,
    val size: String = "medium",
    val weight: String = "normal"
) : UiComponent

data class Image(
    override val type: String = "image",
    val url: String,
    val width: Int? = null,
    val height: Int? = null
) : UiComponent

data class Button(
    override val type: String = "button",
    val id: String,
    val value: String = "Click Me",
    val action: String
) : UiComponent

data class FeaturedItems(
    override val type: String = "featuredItems",
    val button : Button,
    val children: List<UiComponent>
) : UiComponent

