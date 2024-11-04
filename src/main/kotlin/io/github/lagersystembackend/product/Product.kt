package io.github.lagersystembackend.product

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
    val price: Float?,
    val description: String,
    val spaceId: String
)

@Serializable
data class NetworkProduct(
    val id: String,
    val name: String,
    val price: Float?,
    val description: String,
    val spaceId: String
)

@Serializable
data class AddProductNetworkRequest(
    val name: String,
    val price: Float?,
    val description: String,
    val spaceId: String
)


object Products: UUIDTable() {
    val name = varchar("name", 255)
    val price = float("price").nullable()
    val description = text("description")
    val spaceId = reference("spaceId", Spaces)
}

class ProductEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ProductEntity>(Products)

    var name by Products.name
    var price by Products.price
    var description by Products.description
    var space by SpaceEntity referencedOn  Products.spaceId
}

fun ProductEntity.toProduct() = Product(
    id.value.toString(),
    name,
    price,
    description,
    space.id.value.toString()
)

fun Product.toNetworkProduct() = NetworkProduct(
    id,
    name,
    price,
    description,
    spaceId
)