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
    val creationTime: LocalDateTime
)

@Serializable
data class NetworkProduct(
    val id: String,
    val name: String,
    val description: String,
    val attributes: Map<String, Attribute>,
    val spaceId: String,
    val creationTime: String
)

@Serializable
data class AddProductNetworkRequest(
    val name: String,
    val description: String,
    val spaceId: String
)


object Products: UUIDTable() {
    val name = varchar("name", 255)
    val description = text("description")
    val spaceId = reference("spaceId", Spaces)
    val creationTime = datetime("creationTime").defaultExpression(CurrentDateTime)
}

class ProductEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ProductEntity>(Products)

    var name by Products.name
    var description by Products.description
    val attributes by ProductAttributeEntity referrersOn ProductAttributes.productId
    var space by SpaceEntity referencedOn Products.spaceId
    var creationTime by Products.creationTime
}

fun ProductEntity.toProduct() = Product(
    id.value.toString(),
    name,
    description,
    attributes.associate { it.key to it.toAttribute() },
    space.id.value.toString(),
    creationTime
)

fun Product.toNetworkProduct() = NetworkProduct(
    id,
    name,
    description,
    attributes,
    spaceId,
    creationTime.format(DateTimeFormatter.ISO_DATE_TIME)
)