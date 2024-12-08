package io.github.lagersystembackend.attribute

import io.github.lagersystembackend.product.ProductEntity
import io.github.lagersystembackend.product.Products
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption


object ProductAttributes : IntIdTable() {
    val key = varchar("key", 255)
    val type = varchar("type", 255)
    val value = text("value")
    val productId = reference("product_id", Products, onDelete = ReferenceOption.CASCADE)
}

class ProductAttributeEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ProductAttributeEntity>(ProductAttributes)
    var key by ProductAttributes.key
    var type by ProductAttributes.type
    var value by ProductAttributes.value
    var product by ProductEntity referencedOn ProductAttributes.productId
}


fun ProductAttributeEntity.toAttribute() = when (type) {
    Attribute.StringAttribute.TYPE -> Attribute.StringAttribute(value)
    Attribute.NumberAttribute.TYPE -> Attribute.NumberAttribute(value.toFloat())
    Attribute.BooleanAttribute.TYPE -> Attribute.BooleanAttribute(value.toBoolean())
    Attribute.ListAttribute.type(Attribute.StringAttribute.TYPE) -> Attribute.ListAttribute.fromStings(value.split(","))
    Attribute.ListAttribute.type(Attribute.NumberAttribute.TYPE) -> Attribute.ListAttribute.fromNumbers(value.split(",").map { it.toFloat() })
    Attribute.ListAttribute.type(Attribute.BooleanAttribute.TYPE) -> Attribute.ListAttribute.fromBooleans(value.split(",").map { it.toBoolean() })
    else -> throw IllegalArgumentException("Unknown attribute type: $type")
}