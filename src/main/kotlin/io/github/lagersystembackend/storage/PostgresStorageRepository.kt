package io.github.lagersystembackend.storage

import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction
import io.github.lagersystembackend.product.ProductEntity
import io.github.lagersystembackend.space.SpaceEntity
import io.github.lagersystembackend.attribute.ProductAttributeEntity

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
        }?.toStorage()
    }

    override fun deleteStorage(id: String): Storage? = transaction {
        val storageEntity = StorageEntity.findById(UUID.fromString(id))

        storageEntity?.delete()

        storageEntity?.toStorage()
    }

    override fun storageExists(id: String): Boolean = transaction {
        StorageEntity.findById(UUID.fromString(id)) != null
    }

    override fun moveStorage(id: String, newParentId: String?): Storage = transaction {
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
        storage.toStorage()
    }

    override fun isCircularReference(storageId: String, targetParentId: String): Boolean = transaction {
        val storage = StorageEntity.findById(UUID.fromString(storageId)) ?: return@transaction false
        val targetParent = StorageEntity.findById(UUID.fromString(targetParentId)) ?: return@transaction false
        generateSequence(targetParent) { it.parent }.any { it.id.value.toString() == storageId }
    }

    override fun copyStorage(id: String, newParentId: String?): Storage {
        return transaction {
            val originalStorage = StorageEntity.findById(UUID.fromString(id))
                ?: throw IllegalArgumentException("Storage with ID $id not found")

            val newParent = newParentId?.let { parentId ->
                StorageEntity.findById(UUID.fromString(parentId))
                    ?: throw IllegalArgumentException("Parent storage with ID $parentId not found")
            }

            val newStorageEntity = StorageEntity.new {
                name = originalStorage.name + " (Copy)"
                description = originalStorage.description
            }

            newStorageEntity.parent = newParent

            originalStorage.spaces.forEach { space ->
                SpaceEntity.new {
                    name = space.name + " (Copy)"
                    description = space.description
                    storage = newStorageEntity
                }
            }

            originalStorage.subStorages.forEach { subStorage ->
                copyStorage(subStorage.id.value.toString(), newStorageEntity.id.value.toString()) // Recursive copy
            }

            newStorageEntity.toStorage()
        }
    }

    private fun copySpace(original: SpaceEntity, newStorage: StorageEntity): SpaceEntity {
        val newSpace = SpaceEntity.new {
            name = original.name + " (Copy)"
            size = original.size
            description = original.description
            storage = newStorage
        }

        original.products.forEach { product ->
            copyProduct(product, newSpace)
        }

        return newSpace
    }
    private fun copyProduct(original: ProductEntity, newSpace: SpaceEntity): ProductEntity {
        val newProduct = ProductEntity.new {
            name = original.name + " (Copy)"
            description = original.description
            space = newSpace
        }

        original.attributes.forEach { attribute ->
            ProductAttributeEntity.new {
                product = newProduct
                key = attribute.key
                type = attribute.type
                value = attribute.value
            }
        }

        return newProduct
    }

}