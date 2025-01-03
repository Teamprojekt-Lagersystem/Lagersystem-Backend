package io.github.lagersystembackend.stored_product

import io.github.lagersystembackend.product.ProductEntity
import io.github.lagersystembackend.product.Products
import io.github.lagersystembackend.space.SpaceEntity
import io.ktor.http.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class PostgresStoredProductRepository : StoredProductRepository {

    override fun createStoredProduct(
        productId: String,
        spaceId: String,
        quantity: Int
    ): StoredProductDTO = transaction {
        val product = ProductEntity.findById(UUID.fromString(productId)) ?: throw IllegalArgumentException("Product not found")
        val space = SpaceEntity.findById(UUID.fromString(spaceId)) ?: throw IllegalArgumentException("Space not found")
        if (space.unit != product.unit) {
            throw IllegalArgumentException("Unit of product and space must match")
        }

        if (fitsInSpace(product, space, quantity)) {
            space.currentSize = space.currentSize?.plus(product.size!! * quantity)
        }
        /*
        if (product.size != null && space.totalSize != null) {
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

         */
        StoredProductEntity.new {
            this.product = product
            this.space = space
            this.quantity = quantity
            this.createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
        }.toStoredProductDTO()

    }

    override fun getStoredProduct(id: String): StoredProductDTO? = transaction {
        StoredProductEntity.findById(UUID.fromString(id))?.toStoredProductDTO()
    }

    override fun getStoredProducts(): List<StoredProductDTO> = transaction {
        StoredProductEntity.all().toList().map { it.toStoredProductDTO() }
    }

    override fun getStoredProductsBySpaceId(spaceId: String): List<StoredProductDTO> {
        return transaction {
            StoredProductEntity.find { StoredProducts.spaceId eq UUID.fromString(spaceId) }.toList().map { it.toStoredProductDTO() }
        }
    }

    //TODO: update quantity space size and check if product fits in space
    /*
    override fun updateStoredProduct(
        id: String,
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

     */

    //TODO: moveStoreProduct
    /*
    override fun moveStoredProduct(id: String, targetSpaceId: String): StoredProductDTO = transaction {
        val storedProduct = StoredProductEntity

     */

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

    override fun fitsInSpace(product: ProductEntity, space: SpaceEntity, quantity: Int): Boolean = transaction {
        if (product.size != null && space.totalSize != null) {
            val currentSize = space.currentSize
            val totalSize = space.totalSize
            val size = product.size!! * quantity

            if (currentSize != null && totalSize != null) {
                if ((currentSize + size) > totalSize) {
                    throw IllegalArgumentException("product does not fit inside the space anymore")
                }
            }
            return@transaction true
        }
        return@transaction true
    }

    override fun isStored(productId: String, spaceId: String): Boolean = transaction {
        StoredProductEntity.find {
            (StoredProducts.productId eq UUID.fromString(productId)) and (StoredProducts.spaceId eq UUID.fromString(spaceId)) }.count() > 0
    }

    override fun copyStoredProduct(id: String, targetSpaceId: String): StoredProductDTO = transaction {
        val storedProduct = StoredProductEntity.findById(UUID.fromString(id)) ?: throw IllegalArgumentException("Stored product with ID $id not found")
        val targetSpace = SpaceEntity.findById(UUID.fromString(targetSpaceId)) ?: throw IllegalArgumentException("Space with ID $targetSpaceId not found")

        if (storedProduct.space.unit != storedProduct.product.unit) {
            throw IllegalArgumentException("Unit of product and space must match")
        }

        if (fitsInSpace(storedProduct.product, targetSpace, storedProduct.quantity)) {
            targetSpace.currentSize = targetSpace.currentSize?.plus(storedProduct.product.size!! * storedProduct.quantity)
        }

        StoredProductEntity.new {
            this.product = storedProduct.product
            this.space = targetSpace
            this.quantity = storedProduct.quantity
            this.createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
        }.toStoredProductDTO()
    }
}