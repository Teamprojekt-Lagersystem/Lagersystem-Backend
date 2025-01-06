package io.github.lagersystembackend.storage

import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction
import io.github.lagersystembackend.product.ProductEntity
import io.github.lagersystembackend.space.SpaceEntity
import io.github.lagersystembackend.attribute.ProductAttributeEntity
import io.github.lagersystembackend.stored_product.StoredProductEntity
import io.github.lagersystembackend.stored_product.StoredProducts
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import java.util.UUID

class PostgresStorageRepository: StorageRepository {
    override fun createStorage(
        name: String,
        description: String,
        parentId: String?,
    ): Storage = transaction {
        val parent = parentId?.let { StorageEntity.findById(UUID.fromString(it)) }
        val newStorage = StorageEntity.new {
            this.name = name
            this.description = description
        }
        parent?.run { subStorages = SizedCollection(subStorages + newStorage) }
        newStorage.parent = parent
        newStorage.toStorage()
    }

    override fun getStorage(id: String): Storage? = transaction {
        StorageEntity.findById(UUID.fromString(id))?.toStorage()

    }

    override fun getStorages(): List<Storage> = transaction {
        StorageEntity.all().toList().map { it.toStorage() }
    }

    override fun updateStorage(
        id: String,
        name: String?,
        description: String?,
    ): Storage? = transaction {
        StorageEntity.findByIdAndUpdate(UUID.fromString(id)) { storage ->
            name?.let { storage.name = it }
            description?.let { storage.description = it }
            storage.updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
        }?.toStorage()
    }

    override fun deleteStorage(id: String): Storage? = transaction {
        val storageEntity = StorageEntity.findById(UUID.fromString(id))

        //delete dependencies stored products
        storageEntity?.subStorages?.forEach { subStorage ->
            subStorage.spaces.forEach { space ->
                deleteStoredProductDependencies(space.id.value.toString())
            }
        }

        storageEntity?.spaces?.forEach { space ->
            deleteStoredProductDependencies(space.id.value.toString())
        }

        storageEntity?.delete()

        storageEntity?.toStorage()
    }

    override fun storageExists(id: String): Boolean = transaction {
        StorageEntity.findById(UUID.fromString(id)) != null
    }

    override fun moveStorage(id: String, newParentId: String?): Storage = transaction {
        if (id == newParentId) {
            throw IllegalArgumentException("Cannot move storage to itself")
        }

        val storage = StorageEntity.findById(UUID.fromString(id))
            ?: throw IllegalArgumentException("Storage with ID $id not found")

        val newParent = newParentId?.let {
            StorageEntity.findById(UUID.fromString(it))
                ?: throw IllegalArgumentException("Storage with ID $newParentId not found")
        }

        if (newParent != null && isCircularReference(id, newParentId)) {
            storage.subStorages.forEach { child ->
                child.parent = if (storage.parent == null) {
                    null
                } else {
                    storage.parent
                }
            }
        }

        storage.parent = newParent
        storage.updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)

        storage.toStorage()
    }

    override fun isCircularReference(storageId: String, targetParentId: String): Boolean = transaction {
        StorageEntity.findById(UUID.fromString(storageId)) ?: return@transaction false
        val targetParent = StorageEntity.findById(UUID.fromString(targetParentId)) ?: return@transaction false
        generateSequence(targetParent) { it.parent }.any { it.id.value.toString() == storageId }
    }

    override fun copyStorage(id: String, newParentId: String?): Storage {
        return transaction {

            if (id == newParentId) {
                throw IllegalArgumentException("Cannot copy storage to itself")
            }

            val originalStorage = StorageEntity.findById(UUID.fromString(id))
                ?: throw IllegalArgumentException("Storage with ID $id not found")

            val newParent = newParentId?.let { parentId ->
                StorageEntity.findById(UUID.fromString(parentId))
                    ?: throw IllegalArgumentException("Parent storage with ID $parentId not found")
            }

            val newStorageEntity = StorageEntity.new {
                name = originalStorage.name
                description = originalStorage.description
            }

            newStorageEntity.parent = newParent

            originalStorage.spaces.forEach { space ->
                val newSpaceEntity = SpaceEntity.new {
                    name = space.name 
                    totalSize = space.totalSize
                    currentSize = space.currentSize
                    unit = space.unit
                    description = space.description
                    storage = newStorageEntity
                }

                space.storedProducts.forEach { product ->
                    StoredProductEntity.new {
                        this.product = getProduct(product.id)
                        this.space = newSpaceEntity
                        this.quantity = product.quantity
                        this.createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
                    }
                }
            }

            originalStorage.subStorages.forEach { subStorage ->
                copyStorage(subStorage.id.value.toString(), newStorageEntity.id.value.toString()) // Recursive copy
            }

            newStorageEntity.toStorage()
        }
    }

    private fun getProduct(id: String): ProductEntity = transaction {
        val storedProduct = StoredProductEntity.findById(UUID.fromString(id))
            ?: throw IllegalArgumentException("Stored product with ID $id not found")
        storedProduct.product
    }

    private fun deleteStoredProductDependencies(spaceId: String) {
        transaction {
            StoredProductEntity.find { StoredProducts.spaceId eq UUID.fromString(spaceId) }.forEach { it.delete() }
        }
    }
}