package io.github.lagersystembackend.stored_product

import io.github.lagersystembackend.product.ProductEntity
import io.github.lagersystembackend.product.Products
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
import java.util.*

data class StoredProduct(
    val id: String,
    val productId: String,
    val spaceId: String,
    val quantity: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?
)

@Serializable
data class NetworkStoredProduct(
    val id: String,
    val productId: String,
    val spaceId: String,
    val quantity: Int,
    val createdAt: String,
    val updatedAt: String?
)

@Serializable
data class AddStoredProductNetworkRequest(
    val productId: String,
    val spaceId: String,
    val quantity: Int
)

@Serializable
data class UpdateStoredProductNetworkRequest(
    /*
    TODO: ersetzt move product
    val productId: String? = null,
     */
    val spaceId: String? = null,
    val quantity: Int? = null
)

object StoredProducts: UUIDTable() {
    val productId = reference("productId", Products)
    val spaceId = reference("spaceId", Spaces)
    val quantity = integer("quantity")
    val createdAt = datetime("createdAt").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updatedAt").nullable()
}

class StoredProductEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<StoredProductEntity>(StoredProducts)

    var product by ProductEntity referencedOn StoredProducts.productId
    var space by SpaceEntity referencedOn StoredProducts.spaceId
    var quantity by StoredProducts.quantity
    var createdAt by StoredProducts.createdAt
    var updatedAt by StoredProducts.updatedAt
}

fun StoredProductEntity.toStoredProduct() = StoredProduct(
    id.value.toString(),
    product.id.value.toString(),
    space.id.value.toString(),
    quantity,
    createdAt,
    updatedAt
)

fun StoredProduct.toNetworkStoredProduct() = NetworkStoredProduct(
    id,
    productId,
    spaceId,
    quantity,
    createdAt.format(DateTimeFormatter.ISO_DATE_TIME),
    updatedAt?.format(DateTimeFormatter.ISO_DATE_TIME)
)











