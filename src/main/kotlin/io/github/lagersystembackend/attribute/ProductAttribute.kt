package io.github.lagersystembackend.attribute

import io.github.lagersystembackend.product.Products
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption


object ProductAttributes : IntIdTable() {
    val productId = uuid("product_id").references(Products.id, onDelete = ReferenceOption.CASCADE)
    val key = varchar("key", 255)
    val type = varchar("type", 255)
    val value = text("value")
}

class ProductAttributeEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ProductAttributeEntity>(ProductAttributes)
    var productId by ProductAttributes.productId
    var key by ProductAttributes.key
    var type by ProductAttributes.type
    var value by ProductAttributes.value
}


fun ProductAttributeEntity.toAttribute() = when (type) {
    "string" -> Attribute.StringAttribute(value)
    "number" -> Attribute.NumberAttribute(value.toFloat())
    "boolean" -> Attribute.BooleanAttribute(value.toBoolean())
    else -> throw IllegalArgumentException("Unknown attribute type: $type")
}