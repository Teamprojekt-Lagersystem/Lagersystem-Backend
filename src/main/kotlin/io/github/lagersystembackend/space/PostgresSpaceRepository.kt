package io.github.lagersystembackend.space

import io.github.lagersystembackend.storage.StorageEntity
import io.github.lagersystembackend.product.ProductEntity
import io.github.lagersystembackend.attribute.ProductAttributeEntity
import io.github.lagersystembackend.product.Product
import io.github.lagersystembackend.product.toProduct
import io.github.lagersystembackend.stored_product.StoredProductEntity
import io.github.lagersystembackend.stored_product.StoredProducts
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class PostgresSpaceRepository : SpaceRepository {
    override fun createSpace(
        name: String,
        description: String,
        size: Double?,
        unit: String?,
        storageId: String
    ): Space = transaction {
        val storage = StorageEntity.findById(UUID.fromString(storageId)) ?: throw IllegalArgumentException("Storage not found")
        SpaceEntity.new {
            this.name = name
            this.totalSize = size
            this.currentSize = if (size != null) 0.0 else null
            this.unit = unit
            this.description = description
            this.storage = storage
        }.toSpace()
    }

    override fun getSpace(id: String): Space? = transaction {
        SpaceEntity.findById(UUID.fromString(id))?.toSpace()
    }

    override fun getSpaces(): List<Space> = transaction {
        SpaceEntity.all().toList().map { it.toSpace() }
    }

    override fun updateSpace(
        id: String,
        name: String?,
        description: String?,
        size: Double?,
    ): Space? = transaction {
        SpaceEntity.findByIdAndUpdate(UUID.fromString(id)) { space ->
            val currentSize = space.currentSize
            if (size != null && currentSize != null) {
                if (size >= currentSize) {
                    size.let { space.totalSize = it }
                } else {
                    throw IllegalArgumentException("New size can not be smaller than current size of space.")
                }
            }
            name?.let { space.name = it }
            description?.let { space.description = it }
            space.updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
        }?.toSpace()
    }

    override fun deleteSpace(id: String): Space? = transaction {
        val spaceEntity = SpaceEntity.findById(UUID.fromString(id))

        spaceEntity?.delete()

        spaceEntity?.toSpace()
    }

    override fun spaceExists(id: String): Boolean = transaction {
        SpaceEntity.findById(UUID.fromString(id)) != null
    }

    override fun moveSpace(spaceId: String, targetStorageId: String): Space = transaction {
        val space = SpaceEntity.findById(UUID.fromString(spaceId))
            ?: throw IllegalArgumentException("Space with ID $spaceId not found")

        val targetStorage = StorageEntity.findById(UUID.fromString(targetStorageId))
            ?: throw IllegalArgumentException("Storage with ID $targetStorageId not found")

        space.storage = targetStorage
        space.updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
        space.toSpace()
    }

    override fun copySpace(spaceId: String, targetStorageId: String): Space {
        return transaction {

            val originalSpace = SpaceEntity.findById(UUID.fromString(spaceId))
                ?: throw IllegalArgumentException("Space with ID $spaceId not found")

            val targetStorage = StorageEntity.findById(UUID.fromString(targetStorageId))
                ?: throw IllegalArgumentException("Storage with ID $targetStorageId not found")

            val newSpaceEntity = SpaceEntity.new {
                name = originalSpace.name
                totalSize = originalSpace.totalSize
                currentSize = originalSpace.currentSize
                unit = originalSpace.unit
                description = originalSpace.description
                storage = targetStorage
            }

            originalSpace.storedProducts.forEach { product ->
                StoredProductEntity.new {
                    this.product = getProduct(product.id)
                    this.space = newSpaceEntity
                    this.quantity = product.quantity
                    this.createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
                }
            }
            newSpaceEntity.toSpace()
        }
    }

    private fun getProduct(id: String): ProductEntity = transaction {
        val storedProduct = StoredProductEntity.findById(UUID.fromString(id))
            ?: throw IllegalArgumentException("Stored product with ID $id not found")
        storedProduct.product
    }

    override fun isProductStored(spaceId: String): Boolean = transaction {
        StoredProductEntity.find { StoredProducts.spaceId eq UUID.fromString(spaceId) }.count() > 0
    }

}