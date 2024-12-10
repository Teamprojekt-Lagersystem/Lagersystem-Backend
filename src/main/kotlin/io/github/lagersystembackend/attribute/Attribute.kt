package io.github.lagersystembackend.attribute

import kotlinx.serialization.Serializable

@Serializable
sealed class Attribute {
    abstract fun value(): String

    fun type() = when (this) {
        is StringAttribute -> StringAttribute.TYPE
        is NumberAttribute -> NumberAttribute.TYPE
        is BooleanAttribute -> BooleanAttribute.TYPE
        is ListAttribute<*> -> ListAttribute.type(elementType)
    }


    @Serializable
    data class StringAttribute(val value: String) : Attribute() {
        override fun value() = value
        companion object { const val TYPE = "string" }
    }

    @Serializable
    data class NumberAttribute(val value: Float) : Attribute() {
        override fun value() = value.toString()
        companion object { const val TYPE = "number" }
    }

    @Serializable
    data class BooleanAttribute(val value: Boolean) : Attribute() {
        override fun value() = value.toString()
        companion object { const val TYPE = "boolean" }
    }

    @ConsistentCopyVisibility
    @Serializable
    data class ListAttribute<T : Attribute> private constructor(val value: List<T>, val elementType: String): Attribute() {
        override fun value() = value.joinToString(",") { it.value() }

        companion object {
            fun type(elementType: String) = "${elementType}List"
            fun fromStings(values: List<String>) = ListAttribute(values.map { StringAttribute(it) }, StringAttribute.TYPE)
            fun fromNumbers(values: List<Float>) = ListAttribute(values.map { NumberAttribute(it) }, NumberAttribute.TYPE)
            fun fromBooleans(values: List<Boolean>) = ListAttribute(values.map { BooleanAttribute(it) }, BooleanAttribute.TYPE)
        }
    }
}