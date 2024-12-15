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
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID


data class Product(
    val id: String,
    val name: String,
    val description: String,
    val attributes: Map<String, Attribute>,
    val spaceId: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?
)

@Serializable
data class NetworkProduct(
    val id: String,
    val name: String,
    val description: String,
    val attributes: Map<String, Attribute>,
    val spaceId: String,
    val createdAt: String,
    val updatedAt: String?
)

@Serializable
data class AddProductNetworkRequest(
    val name: String,
    val description: String,
    val spaceId: String
)

@Serializable
data class UpdateProductNetworkRequest(
    val id: String,
    val name: String? = null,
    val description: String? = null,
)


object Products: UUIDTable() {
    val name = varchar("name", 255)
    val description = text("description")
    val spaceId = reference("spaceId", Spaces)
    val createdAt = datetime("createdAt").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updatedAt").nullable()
}

class ProductEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ProductEntity>(Products)

    var name by Products.name
    var description by Products.description
    val attributes by ProductAttributeEntity referrersOn ProductAttributes.productId
    var space by SpaceEntity referencedOn Products.spaceId
    var createdAt by Products.createdAt
    var updatedAt by Products.updatedAt
}

fun ProductEntity.toProduct() = Product(
    id.value.toString(),
    name,
    description,
    attributes.associate { it.key to it.toAttribute() },
    space.id.value.toString(),
    createdAt,
    updatedAt
)

fun Product.toNetworkProduct() = NetworkProduct(
    id,
    name,
    description,
    attributes,
    spaceId,
    createdAt.format(DateTimeFormatter.ISO_DATE_TIME),
    updatedAt?.format(DateTimeFormatter.ISO_DATE_TIME),
)