package io.github.lagersystembackend.space

import io.github.lagersystembackend.product.NetworkProduct
import io.github.lagersystembackend.product.Product
import io.github.lagersystembackend.product.ProductEntity
import io.github.lagersystembackend.product.Products
import io.github.lagersystembackend.product.toNetworkProduct
import io.github.lagersystembackend.product.toProduct
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.UUID


data class Space(
    val id: String,
    val name: String,
    val size: Float?,
    val description: String,
    val products: List<Product>
)

@Serializable
data class NetworkSpace(
    val id: String,
    val name: String,
    val size: Float?,
    val description: String,
    val products: List<NetworkProduct>?
)

@Serializable
data class AddSpaceNetworkRequest(
    val name: String,
    val size: Float?,
    val description: String
)

object Spaces: UUIDTable() {
    val name = varchar("name", 255)
    val size = float("size").nullable()
    val description = text("description")
}

class SpaceEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<SpaceEntity>(Spaces)

    var name by Spaces.name
    var size by Spaces.size
    var description by Spaces.description
    val products by ProductEntity referrersOn Products.space
}

fun SpaceEntity.toSpace() = Space(
    id.value.toString(),
    name,
    size,
    description,
    products.map { it.toProduct() }
)

fun Space.toNetworkSpace() = NetworkSpace(
    id,
    name,
    size,
    description,
    products.map { it.toNetworkProduct() }
)
