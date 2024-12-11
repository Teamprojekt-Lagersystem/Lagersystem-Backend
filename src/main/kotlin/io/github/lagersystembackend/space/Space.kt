package io.github.lagersystembackend.space

import io.github.lagersystembackend.product.NetworkProduct
import io.github.lagersystembackend.product.Product
import io.github.lagersystembackend.product.ProductEntity
import io.github.lagersystembackend.product.Products
import io.github.lagersystembackend.product.toNetworkProduct
import io.github.lagersystembackend.product.toProduct
import io.github.lagersystembackend.storage.StorageEntity
import io.github.lagersystembackend.storage.Storages
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


data class Space(
    val id: String,
    val name: String,
    val size: Float?,
    val description: String,
    val products: List<Product>,
    val storageId: String,
    val creationTime: LocalDateTime,
    val updatedAt: LocalDateTime
)

@Serializable
data class NetworkSpace(
    val id: String,
    val name: String,
    val size: Float?,
    val description: String,
    val products: List<NetworkProduct>?,
    val storageId: String,
    val creationTime: String,
    val updatedAt: String
)

@Serializable
data class AddSpaceNetworkRequest(
    val name: String,
    val size: Float?,
    val description: String,
    val storageId: String
)

@Serializable
data class MoveSpaceRequest(
    val targetStorageId: String
)

object Spaces: UUIDTable() {
    val name = varchar("name", 255)
    val size = float("size").nullable()
    val description = text("description")
    val storageId = reference("storageId", Storages)
    val creationTime = datetime("creationTime").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updatedAt").defaultExpression(CurrentDateTime)
}

class SpaceEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<SpaceEntity>(Spaces)

    var name by Spaces.name
    var size by Spaces.size
    var description by Spaces.description
    val products by ProductEntity referrersOn Products.spaceId
    var storage by StorageEntity referencedOn Spaces.storageId
    var creationTime by Spaces.creationTime
    var updatedAt by Spaces.updatedAt

    override fun delete() {
        products.forEach { it.delete() }
        super.delete()
    }
}

fun SpaceEntity.toSpace() = Space(
    id.value.toString(),
    name,
    size,
    description,
    products.map { it.toProduct() },
    storage.id.value.toString(),
    creationTime,
    updatedAt
)

fun Space.toNetworkSpace() = NetworkSpace(
    id,
    name,
    size,
    description,
    products.map { it.toNetworkProduct() },
    storageId,
    creationTime.format(DateTimeFormatter.ISO_DATE_TIME),
    updatedAt.format(DateTimeFormatter.ISO_DATE_TIME)
)
