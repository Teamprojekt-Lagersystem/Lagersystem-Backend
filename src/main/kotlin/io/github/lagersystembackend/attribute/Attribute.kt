package io.github.lagersystembackend.attribute

import kotlinx.serialization.Serializable

@Serializable
sealed class Attribute {
    data class StringAttribute(val value: String): Attribute()
    data class NumberAttribute(val value: Float): Attribute()
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








