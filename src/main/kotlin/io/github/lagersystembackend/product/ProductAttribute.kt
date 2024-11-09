package io.github.lagersystembackend.product

import kotlinx.serialization.Serializable

@Serializable
sealed class ProductAttribute {
    data class StringAttribute(val value: String): ProductAttribute()
    data class NumberAttribute(val value: Float): ProductAttribute()
    data class BooleanAttribute(val value: Boolean): ProductAttribute()
}

@Serializable
data class NetworkProductAttribute(
    val key: String,
    val value: String,
    val type: String
)






