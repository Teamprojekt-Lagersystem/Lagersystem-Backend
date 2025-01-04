package io.github.lagersystembackend.stored_product

import io.github.lagersystembackend.product.ProductEntity
import io.github.lagersystembackend.space.SpaceEntity
import io.ktor.http.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.reflect.jvm.internal.impl.descriptors.Visibilities.Local

class PostgresStoredProductRepository : StoredProductRepository {

    override fun createStoredProduct(
        productId: String,
        spaceId: String,
        quantity: Int
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
            this.createdAt = LocalDateTime.now()
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

    //TODO: update space size and check if product fits in space
    override fun updateStoredProduct(
        id: String,
        spaceId: String?,
        quantity: Int?
    ): StoredProduct? = transaction {
        val product = ProductEntity.findById(UUID.fromString(id)) ?: throw IllegalArgumentException("Product not found")
        val space = SpaceEntity.findById(UUID.fromString(spaceId)) ?: throw IllegalArgumentException("Space not found")
        val storedProduct = StoredProductEntity.findById(UUID.fromString(id)) ?: throw IllegalArgumentException("Stored product not found")

        if (product.size != null && quantity != null) {
            val currentSize = space.currentSize
            val totalSize = space.totalSize
            val size = product.size!! * quantity

            /*
            if (currentSize != null && totalSize != null) {
                if (storedProduct.space.id.toString() != spaceId) {
                    if (fitsInSpace(spaceId, size)) {
                        storedProduct.space.currentSize = storedProduct.space.currentSize.minus(size)
                        space.currentSize = space.currentSize?.plus(size)
                    } else {
                        throw IllegalArgumentException("product does not fit inside the space anymore")
                    }
                } else {
                    if (currentSize + size > totalSize) {
                        throw IllegalArgumentException("product does not fit inside the space anymore")
                    }
                }
            }
            space.currentSize = space.currentSize?.plus(size)

             */
        }
        storedProduct.toStoredProduct()
    }

    override fun deleteStoredProduct(id: String): StoredProduct? = transaction {
        val storedProduct = StoredProductEntity.findById(UUID.fromString(id))
        val size = storedProduct?.product?.size?.times(storedProduct.quantity)
        storedProduct?.space?.currentSize = storedProduct?.space?.currentSize?.minus(size!!)

        storedProduct?.delete()

        storedProduct?.toStoredProduct()

    }

    override fun spaceExists(id: String): Boolean = transaction {
        SpaceEntity.findById(UUID.fromString(id)) != null
    }

    override fun productExists(id: String): Boolean = transaction {
        ProductEntity.findById(UUID.fromString(id)) != null
    }


    /*
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

     */
    override fun fitsInSpace(spaceId: String, size: Double): Boolean = transaction {
        val space = SpaceEntity.findById(UUID.fromString(spaceId))
        if (space != null) {
            val totalSize = space.totalSize
            val currentSize = space.currentSize
            totalSize != null && currentSize != null && currentSize + size <= totalSize
        } else {
            false
        }
    }

    //TODO: update space size and check if product fits in space
    override fun copyStoredProduct(id: String, targetSpaceId: String): StoredProduct {
        return transaction {
            val storedProduct = StoredProductEntity.findById(UUID.fromString(id))
                ?: throw IllegalArgumentException("Stored product with ID $id not found")

            val targetSpace = SpaceEntity.findById(UUID.fromString(targetSpaceId))
                ?: throw IllegalArgumentException("Space with ID $targetSpaceId not found")

            val newStoredProductEntity = StoredProductEntity.new {
                product = storedProduct.product
                quantity = storedProduct.quantity
                space = targetSpace
            }

            newStoredProductEntity.toStoredProduct()
        }
    }
}