package io.github.lagersystembackend.stored_product

import io.github.lagersystembackend.product.ProductEntity
import io.github.lagersystembackend.space.SpaceEntity
import io.ktor.http.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class PostgresStoredProductRepository : StoredProductRepository {

    override fun createStoredProduct(
        productId: String,
        spaceId: String,
        quantity: Double
    ): StoredProduct = transaction {
        val product = ProductEntity.findById(UUID.fromString(productId)) ?: throw IllegalArgumentException("Product not found")
        val space = SpaceEntity.findById(UUID.fromString(spaceId)) ?: throw IllegalArgumentException("Space not found")
        if (space.unit != product.unit) {
            throw IllegalArgumentException("Unit of product and space must match")
        }
        if (product.size != null) {
            val currentSize = space.currentSize
            val totalSize = space.totalSize
            val size = product.size!! * quantity

            if (currentSize != null && totalSize != null) {
                if ((currentSize + size) > totalSize) {
                    throw IllegalArgumentException("product does not fit inside the space anymore")
                }
            }
            space.currentSize = space.currentSize?.plus(size)
        }
        StoredProductEntity.new {
            this.product = product
            this.space = space
            this.quantity = quantity
        }.toStoredProduct()

    }

    override fun getStoredProduct(id: String): StoredProduct? = transaction {
        StoredProductEntity.findById(UUID.fromString(id))?.toStoredProduct()
    }

    override fun getStoredProducts(): List<StoredProduct> = transaction {
        StoredProductEntity.all().toList().map { it.toStoredProduct() }
    }

    override fun getStoredProductsBySpaceId(spaceId: String): List<StoredProduct> {
        return transaction {
            StoredProductEntity.find { StoredProducts.spaceId eq UUID.fromString(spaceId) }.toList().map { it.toStoredProduct() }
        }
    }

    override fun updateStoredProduct(
        id: String,
        spaceId: String?,
        quantity: Double?
    ): StoredProduct? = transaction {
        val space = SpaceEntity.findById(UUID.fromString(spaceId)) ?: throw IllegalArgumentException("Space not found")
        StoredProductEntity.findByIdAndUpdate(UUID.fromString(id)) { storedProduct ->
            spaceId?.let {
                storedProduct.space = space
            }
            quantity?.let { storedProduct.quantity = it }
        }?.toStoredProduct()
    }

    override fun deleteStoredProduct(id: String): StoredProduct? = transaction {
        val storedProduct = StoredProductEntity.findById(UUID.fromString(id))

        storedProduct?.delete()

        storedProduct?.toStoredProduct()

    }

    override fun spaceExists(id: String): Boolean = transaction {
        SpaceEntity.findById(UUID.fromString(id)) != null
    }

    override fun productExists(id: String): Boolean = transaction {
        ProductEntity.findById(UUID.fromString(id)) != null
    }

    //TODO: test this function
    override fun fitsInSpace(id: String): Boolean = transaction {
        val storedProduct = StoredProductEntity.findById(UUID.fromString(id)) ?: throw IllegalArgumentException("Stored product not found")
        val currentSize = storedProduct.space.currentSize
        val totalSize = storedProduct.space.totalSize
        if (storedProduct.product.size != null) {
            val size = storedProduct.product.size!! * storedProduct.quantity
            if (currentSize != null && totalSize != null) {
                if ((currentSize + size) > totalSize) {
                    false
                }
            }
        }
        true
    }
}