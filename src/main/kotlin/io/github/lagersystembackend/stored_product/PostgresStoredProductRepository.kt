package io.github.lagersystembackend.stored_product

import io.github.lagersystembackend.product.ProductEntity
import io.github.lagersystembackend.space.SpaceEntity
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class PostgresStoredProductRepository : StoredProductRepository {

    override fun createStoredProduct(
        productId: String,
        spaceId: String,
        quantity: Int
    ): StoredProduct = transaction {
        val product = ProductEntity.findById(UUID.fromString(productId)) ?: throw IllegalArgumentException("Product not found")
        val space = SpaceEntity.findById(UUID.fromString(spaceId)) ?: throw IllegalArgumentException("Space not found")
        StoredProductEntity.new {
            this.product = product
            this.space = space
            this.quantity = quantity
        }.toStoredProduct()

    }

    override fun getStoredProduct(id: String): StoredProduct? {
        TODO("Not yet implemented")
    }

    override fun getStoredProducts(): List<StoredProduct> {
        TODO("Not yet implemented")
    }

    override fun getStoredProductsBySpaceId(spaceId: String): List<StoredProduct> {
        TODO("Not yet implemented")
    }

    override fun updateStoredProduct(id: String, spaceId: String?, quantity: Int?): StoredProduct? {
        TODO("Not yet implemented")
    }

    override fun deleteStoredProduct(id: String): StoredProduct? {
        TODO("Not yet implemented")
    }


}