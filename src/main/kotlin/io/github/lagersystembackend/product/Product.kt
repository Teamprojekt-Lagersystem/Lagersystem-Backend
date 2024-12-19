package io.github.lagersystembackend.product

import io.github.lagersystembackend.attribute.Attribute
import io.github.lagersystembackend.attribute.ProductAttributeEntity
import io.github.lagersystembackend.attribute.ProductAttributes
import io.github.lagersystembackend.attribute.toAttribute
import io.github.lagersystembackend.space.SpaceEntity
import io.github.lagersystembackend.space.Spaces
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.UUID


data class Product(
    val id: String,
    val name: String,
    val description: String,
    val attributes: Map<String, Attribute>,
    val spaceId: String
)

@Serializable
data class NetworkProduct(
    val id: String,
    val name: String,
    val description: String,
    val attributes: Map<String, Attribute>,
    val spaceId: String
)

@Serializable
data class AddProductNetworkRequest(
    val name: String,
    val description: String,
    val spaceId: String
)

@Serializable
data class CopyProductRequest(
    val targetSpaceId: String
)

object Products: UUIDTable() {
    val name = varchar("name", 255)
    val description = text("description")
    val spaceId = reference("spaceId", Spaces)
}

class ProductEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ProductEntity>(Products)

    var name by Products.name
    var description by Products.description
    val attributes by ProductAttributeEntity referrersOn ProductAttributes.productId
    var space by SpaceEntity referencedOn Products.spaceId
}

fun ProductEntity.toProduct() = Product(
    id.value.toString(),
    name,
    description,
    attributes.associate { it.key to it.toAttribute() },
    space.id.value.toString()
)

fun Product.toNetworkProduct() = NetworkProduct(
    id,
    name,
    description,
    attributes,
    spaceId
)