package io.github.lagersystembackend.attribute

import kotlinx.serialization.Serializable

@Serializable
sealed class Attribute {
    @Serializable
    data class StringAttribute(val value: String): Attribute()
    @Serializable
    data class NumberAttribute(val value: Float): Attribute()
    @Serializable
    data class BooleanAttribute(val value: Boolean): Attribute()

    fun type() = when (this) {
        is StringAttribute -> "string"
        is NumberAttribute -> "number"
        is BooleanAttribute -> "boolean"
    }

    fun value() = when (this) {
        is StringAttribute -> value
        is NumberAttribute -> value.toString()
        is BooleanAttribute -> value.toString()
    }
}



@Serializable
data class NetworkProductAttribute(
    val key: String,
    val value: String,
    val type: String
)








