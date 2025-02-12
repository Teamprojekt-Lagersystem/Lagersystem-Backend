package io.github.lagersystembackend.stored_product

import io.github.lagersystembackend.product.ProductEntity
import io.github.lagersystembackend.product.Products
import io.github.lagersystembackend.space.Space
import io.github.lagersystembackend.space.SpaceEntity
import io.ktor.http.*
import org.jetbrains.exposed.sql.and
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
    ): StoredProductDTO = transaction {
        val product = ProductEntity.findById(UUID.fromString(productId)) ?: throw IllegalArgumentException("Product not found")
        val space = SpaceEntity.findById(UUID.fromString(spaceId)) ?: throw IllegalArgumentException("Space not found")

        space.currentSize = space.currentSize?.plus(product.size!! * quantity)

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

    override fun updateStoredProduct(
        id: String,
        quantity: Int
    ): StoredProductDTO = transaction {
        val storedProduct = StoredProductEntity.findById(UUID.fromString(id)) ?: throw IllegalArgumentException("Stored product not found")

        if (fitsInSpace(storedProduct.product.id.toString(), storedProduct.space.id.toString(), quantity - storedProduct.quantity)) {
            storedProduct.space.currentSize = storedProduct.space.currentSize?.plus(storedProduct.product.size!! * (quantity - storedProduct.quantity))
            storedProduct.quantity = quantity
            storedProduct.updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
        }
        storedProduct.toStoredProductDTO()
    }

    override fun moveStoredProduct(id: String, targetSpaceId: String): StoredProductDTO = transaction {
        val storedProduct = StoredProductEntity.findById(UUID.fromString(id)) ?: throw IllegalArgumentException("StoredProduct not found")
        val targetSpace = SpaceEntity.findById(UUID.fromString(targetSpaceId)) ?: throw IllegalArgumentException("Space not found")

        storedProduct.space.currentSize = storedProduct.space.currentSize?.minus(storedProduct.product.size!! * storedProduct.quantity)
        targetSpace.currentSize = targetSpace.currentSize?.plus(storedProduct.product.size!! * storedProduct.quantity)
        storedProduct.space = targetSpace
        storedProduct.updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
        storedProduct.toStoredProductDTO()
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

    override fun fitsInSpace(productId: String, spaceId: String, quantity: Int): Boolean = transaction {
        val product = ProductEntity.findById(UUID.fromString(productId)) ?: throw IllegalArgumentException("Product not found")
        val space = SpaceEntity.findById(UUID.fromString(spaceId)) ?: throw IllegalArgumentException("Space not found")
        if (product.size != null && space.totalSize != null) {
            val currentSize = space.currentSize
            val totalSize = space.totalSize
            val size = product.size!! * quantity

            if (currentSize != null && totalSize != null) {
                if ((currentSize + size) > totalSize) {
                    return@transaction false
                }
            }
            return@transaction true
        }
        else if (product.size == null && space.totalSize != null || product.size != null && space.totalSize == null) {
            return@transaction false
        }
        return@transaction true
    }

    override fun checkUnit(productId: String, spaceId: String): Boolean = transaction {
        val product = ProductEntity.findById(UUID.fromString(productId)) ?: throw IllegalArgumentException("Product not found")
        val space = SpaceEntity.findById(UUID.fromString(spaceId)) ?: throw IllegalArgumentException("Space not found")
        if (product.unit != null && space.unit != null) {
            if (product.unit != space.unit) {
                return@transaction false
            }
        }
        else if (product.unit == null && space.unit != null || product.unit != null && space.unit == null) {
            return@transaction false
        }
        return@transaction true
    }

    override fun isStored(productId: String, spaceId: String): Boolean = transaction {
        StoredProductEntity.find {
            (StoredProducts.productId eq UUID.fromString(productId)) and (StoredProducts.spaceId eq UUID.fromString(spaceId)) }.count() > 0
    }

    override fun getId(productId: String, spaceId: String): String = transaction {
        StoredProductEntity.find {
            (StoredProducts.productId eq UUID.fromString(productId)) and (StoredProducts.spaceId eq UUID.fromString(spaceId))
        }.firstOrNull()?.id?.value.toString()
    }

    override fun copyStoredProduct(id: String, targetSpaceId: String): StoredProductDTO = transaction {
        val storedProduct = StoredProductEntity.findById(UUID.fromString(id)) ?: throw IllegalArgumentException("Stored product with ID $id not found")
        val targetSpace = SpaceEntity.findById(UUID.fromString(targetSpaceId)) ?: throw IllegalArgumentException("Space with ID $targetSpaceId not found")

        targetSpace.currentSize = targetSpace.currentSize?.plus(storedProduct.product.size!! * storedProduct.quantity)

        StoredProductEntity.new {
            this.product = storedProduct.product
            this.space = targetSpace
            this.quantity = storedProduct.quantity
            this.createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
        }.toStoredProductDTO()
    }
}