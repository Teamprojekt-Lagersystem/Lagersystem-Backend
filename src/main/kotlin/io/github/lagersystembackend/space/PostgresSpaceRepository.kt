package io.github.lagersystembackend.space

import io.github.lagersystembackend.storage.StorageEntity
import io.github.lagersystembackend.product.ProductEntity
import io.github.lagersystembackend.attribute.ProductAttributeEntity
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class PostgresSpaceRepository : SpaceRepository {
    override fun createSpace(
        name: String,
        size: Float?,
        description: String,
        storageId: String
    ): Space = transaction {
        val storage = StorageEntity.findById(UUID.fromString(storageId)) ?: throw IllegalArgumentException("Storage not found")
        SpaceEntity.new {
            this.name = name
            this.size = size
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
        size: Float?,
        description: String?,
    ): Space? = transaction {
        SpaceEntity.findByIdAndUpdate(UUID.fromString(id)) { space ->
            name?.let { space.name = it }
            size?.let { space.size = it }
            description?.let { space.description = it }
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

        space.toSpace()
    }

    override fun copySpace(spaceId: String, targetStorageId: String): Space {
        return transaction {

            val originalSpace = SpaceEntity.findById(UUID.fromString(spaceId))
                ?: throw IllegalArgumentException("Space with ID $spaceId not found")

            val targetStorage = StorageEntity.findById(UUID.fromString(targetStorageId))
                ?: throw IllegalArgumentException("Storage with ID $targetStorageId not found")

            val newSpaceEntity = SpaceEntity.new {
                name = originalSpace.name + " (Copy)"
                size = originalSpace.size
                description = originalSpace.description
                storage = targetStorage
            }
            originalSpace.products.forEach { product ->
                val newProductEntity = ProductEntity.new {
                    name = product.name + " (Copy)"
                    description = product.description
                    this.space = newSpaceEntity
                }
                product.attributes.forEach { attribute ->
                    ProductAttributeEntity.new {
                        this.key = attribute.key
                        this.value = attribute.value
                        this.product = newProductEntity
                    }
                }
            }
            newSpaceEntity.toSpace()
        }
    }

}