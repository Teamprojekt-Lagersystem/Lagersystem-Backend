package io.github.lagersystembackend.space

import io.github.lagersystembackend.attribute.Attribute
import io.github.lagersystembackend.attribute.toAttribute
import io.github.lagersystembackend.product.ProductEntity
import org. jetbrains. exposed. sql. ISqlExpressionBuilder
import io.github.lagersystembackend.storage.StorageEntity
import io.github.lagersystembackend.storage.Storages
import io.github.lagersystembackend.stored_product.*
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

data class ProductInSpace(
    val id: String,
    val name: String,
    val description: String,
    val size: Double?,
    val unit: String?,
    val attributes: Map<String, Attribute>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?,
    val quantity: Int,
    val storedAt: LocalDateTime,
    val modifiedAt: LocalDateTime?
)

@Serializable
data class NetworkProductInSpace(
    val id: String,
    val name: String,
    val description: String,
    val size: Double?,
    val unit: String?,
    val attributes: Map<String, Attribute>,
    val createdAt: String,
    val updatedAt: String?,
    val quantity: Int,
    val storedAt: String,
    val modifiedAt: String?
)

data class Space(
    val id: String,
    val name: String,
    val totalSize: Double?,
    val currentSize: Double?,
    val unit: String?,
    val description: String,
    val products: List<ProductInSpace>,
    val storageId: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?
) {
    init {
        val allNull = (currentSize == null && totalSize == null && unit == null)
        val allDefined = (currentSize != null && totalSize != null && unit != null)

        require(allNull || allDefined) {
            "Either all of currentSize, totalSize, and unit must be defined, or none of them."
        }
        if (currentSize != null && totalSize != null) {
            require(currentSize <= totalSize) {
                "currentSize cannot exceed totalSize."
            }
        }
    }
}

@Serializable
data class NetworkSpace(
    val id: String,
    val name: String,
    val totalSize: Double?,
    val currentSize: Double?,
    val unit: String?,
    val description: String,
    val products: List<NetworkProductInSpace>,
    val storageId: String,
    val createdAt: String,
    val updatedAt: String?
)

@Serializable
data class AddSpaceNetworkRequest(
    val name: String,
    val totalSize: Double?,
    val unit: String?,
    val description: String,
    val storageId: String
)

@Serializable
data class UpdateSpaceNetworkRequest(
    val name: String? = null,
    val totalSize: Double? = null,
    val description: String? = null
)

@Serializable
data class MoveSpaceNetworkRequest(
    val targetStorageId: String
)

@Serializable
data class CopySpaceRequest(
    val targetStorageId: String
)

object Spaces: UUIDTable() {
    val name = varchar("name", 255)
    val totalSize = double("totalSize").nullable()
    val currentSize = double("currentSize").nullable()
    val unit = varchar("unit", 255).nullable()
    val description = text("description")
    val storageId = reference("storageId", Storages)
    val createdAt = datetime("createdAt").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updatedAt").nullable()
}

class SpaceEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<SpaceEntity>(Spaces)

    var name by Spaces.name
    var totalSize by Spaces.totalSize
    var currentSize by Spaces.currentSize
    var unit by Spaces.unit
    var description by Spaces.description
    val productsInSpace: List<ProductInSpace>
        get() = StoredProductEntity.find { StoredProducts.spaceId eq this@SpaceEntity.id }
            .map { it.toProductInSpace() }
    var storage by StorageEntity referencedOn Spaces.storageId
    var createdAt by Spaces.createdAt
    var updatedAt by Spaces.updatedAt
}

fun SpaceEntity.toSpace() = Space(
    id.value.toString(),
    name,
    totalSize,
    currentSize,
    unit,
    description,
    productsInSpace,
    storage.id.value.toString(),
    createdAt,
    updatedAt
)

fun Space.toNetworkSpace() = NetworkSpace(
    id,
    name,
    totalSize,
    currentSize,
    unit,
    description,
    products.map { it.toNetworkProductInSpace() },
    storageId,
    createdAt.format(DateTimeFormatter.ISO_DATE_TIME),
    updatedAt?.format(DateTimeFormatter.ISO_DATE_TIME)
)

fun ProductInSpace.toNetworkProductInSpace() = NetworkProductInSpace(
    id,
    name,
    description,
    size,
    unit,
    attributes,
    createdAt.format(DateTimeFormatter.ISO_DATE_TIME),
    updatedAt?.format(DateTimeFormatter.ISO_DATE_TIME),
    quantity,
    storedAt.format(DateTimeFormatter.ISO_DATE_TIME),
    modifiedAt?.format(DateTimeFormatter.ISO_DATE_TIME)
)

fun StoredProductEntity.toProductInSpace(): ProductInSpace {
    return ProductInSpace(
        id = product.id.value.toString(),
        name = product.name,
        description = product.description,
        size = product.size,
        unit = product.unit,
        attributes = product.attributes.associate { it.key to it.toAttribute() },
        createdAt = product.createdAt,
        updatedAt = product.updatedAt,
        quantity = this.quantity,
        storedAt = this.createdAt,
        modifiedAt = this.updatedAt
    )
}
