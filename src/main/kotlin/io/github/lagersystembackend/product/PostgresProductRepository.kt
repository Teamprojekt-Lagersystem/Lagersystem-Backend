package io.github.lagersystembackend.product

import io.github.lagersystembackend.space.SpaceEntity
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class PostgresProductRepository : ProductRepository {

    override fun createProduct(
        name: String,
        price: Float?,
        description: String,
        spaceId: String
    ): Product = transaction {
        val space = SpaceEntity.findById(UUID.fromString(spaceId)) ?: return@transaction throw IllegalArgumentException("Space not found")
        ProductEntity.new {
            this.name = name
            this.price = price
            this.description = description
            this.space = space
        }.toProduct()
    }

    override fun getProduct(id: String): Product? = transaction {
        ProductEntity.findById(UUID.fromString(id))?.toProduct()
    }

    override fun getProducts(): List<Product> = transaction {
        ProductEntity.all().toList().map { it.toProduct() }
    }

    override fun updateProduct(
        id: String,
        name: String?,
        price: Float?,
        description: String?,
        spaceId: String?
    ): Product? = transaction {
        ProductEntity.findByIdAndUpdate(UUID.fromString(id)) { product ->
            name?.let { product.name = it }
            price?.let { product.price = it }
            description?.let { product.description = it }
            spaceId?.let { product.space = SpaceEntity.findById(UUID.fromString(it)) ?: throw IllegalArgumentException("Space not found") }
        }?.toProduct()
    }

    override fun deleteProduct(id: String): Boolean = transaction {
        ProductEntity.findById(UUID.fromString(id)).also { it?.delete() } != null
    }

}