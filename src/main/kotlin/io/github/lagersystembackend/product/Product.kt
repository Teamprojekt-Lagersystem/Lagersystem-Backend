package io.github.lagersystembackend.product

import TsVectorColumnType
import io.github.lagersystembackend.attribute.Attribute
import io.github.lagersystembackend.attribute.ProductAttributeEntity
import io.github.lagersystembackend.attribute.ProductAttributes
import io.github.lagersystembackend.attribute.toAttribute
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID


data class Product(
    val id: String,
    val name: String,
    val description: String,
    val size: Double?,
    val unit: String?,
    val attributes: Map<String, Attribute>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?
) {
    init {
        val allNull = (size == null && unit == null)
        val allDefined = (size != null && unit != null)

        require(allNull || allDefined) {
            "Either all of currentSize, totalSize, and unit must be defined, or none of them."
        }
    }
}

@Serializable
data class NetworkProduct(
    val id: String,
    val name: String,
    val description: String,
    val size: Double?,
    val unit: String?,
    val attributes: Map<String, Attribute>,
    val createdAt: String,
    val updatedAt: String?
)

@Serializable
data class AddProductNetworkRequest(
    val name: String,
    val description: String,
    val size: Double? = null,
    val unit: String? = null,
)

@Serializable
data class UpdateProductNetworkRequest(
    val name: String? = null,
    val description: String? = null,
    val size: Double?,
)

/*
@Serializable
data class MoveProductNetworkRequest(
    val targetSpaceId: String
)

@Serializable
data class CopyProductRequest(
    val targetSpaceId: String
)

 */

object Products: UUIDTable() {
    val name = varchar("name", 255)
    val description = text("description")
    val size = double("size").nullable()
    val unit = varchar("unit", 255).nullable()
    val createdAt = datetime("createdAt").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updatedAt").nullable()
    val tsVector = registerColumn<String>("tsVector", TsVectorColumnType()).databaseGenerated()
}

class ProductEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ProductEntity>(Products)

    var name by Products.name
    var description by Products.description
    var size by Products.size
    var unit by Products.unit
    val attributes by ProductAttributeEntity referrersOn ProductAttributes.productId
    var createdAt by Products.createdAt
    var updatedAt by Products.updatedAt
}

fun ProductEntity.toProduct() = transaction {
    Product(
        this@toProduct.id.value.toString(),
        name,
        description,
        size,
        unit,
        attributes.associate { it.key to it.toAttribute() },
        createdAt,
        updatedAt
    )
}

fun Product.toNetworkProduct() = NetworkProduct(
    id,
    name,
    description,
    size,
    unit,
    attributes,
    createdAt.format(DateTimeFormatter.ISO_DATE_TIME),
    updatedAt?.format(DateTimeFormatter.ISO_DATE_TIME),
)