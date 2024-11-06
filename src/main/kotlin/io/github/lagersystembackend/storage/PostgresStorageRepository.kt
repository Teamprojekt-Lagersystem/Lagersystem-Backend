package io.github.lagersystembackend.storage

import io.github.lagersystembackend.product.Product
import io.github.lagersystembackend.space.Space
import io.github.lagersystembackend.space.SpaceEntity
import io.github.lagersystembackend.storage.StorageEntity
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction

import java.util.UUID

class PostgresStorageRepository: StorageRepository {
    override fun createStorage(
        name: String,
        description: String,
        parentId: String?,
    ): Storage = transaction {
        val newStorage = StorageEntity.new {
            this.name = name
            this.description = description
        }

        parentId?.let {
            val parentStorage = StorageEntity.findById(UUID.fromString(it))
            if (parentStorage != null) {
                // Add newStorage to parent's subStorages and parentStorage to newStorage's parents
                parentStorage.subStorages = SizedCollection(parentStorage.subStorages + newStorage)
                newStorage.parents = SizedCollection(parentStorage)
            }
        }
        newStorage.toStorage()
    }

    override fun getStorage(id: String): Storage? = transaction {
        StorageEntity.findById(UUID.fromString(id))?.toStorage();
    }

    override fun getStorages(): List<Storage> = transaction {
        StorageEntity.all().toList().map { it.toStorage() }
    }

    //TODO: update storage rework substrage and parent id function
    override fun updateStorage(
        id: String,
        name: String?,
        description: String?,
        parentId: String?,
        subStorages: List<Storage>?
    ): Storage? = transaction {
        StorageEntity.findByIdAndUpdate(UUID.fromString(id)) { storage ->
            name?.let { storage.name = it }
            description?.let { storage.description = it }
            parentId?.let {
                val newParentStorage = StorageEntity.findById(UUID.fromString(it))
                if (newParentStorage != null && newParentStorage.id.toString() != it) {
                    storage.parents = SizedCollection(emptyList())
                    storage.parents = SizedCollection(newParentStorage)
                    newParentStorage.subStorages = SizedCollection(newParentStorage.subStorages + storage)
                }
            }
            subStorages?.forEach { subStorage ->
                val subStorageEntity = StorageEntity.findById(UUID.fromString(subStorage.id))
                if (subStorageEntity != null) {
                    storage.subStorages = SizedCollection(storage.subStorages + subStorageEntity)
                }
            }
        }?.toStorage()
    }

    override fun addSubStorage(
        id: String,
        subStorageId: String
    ): Storage = transaction {
        val storage = StorageEntity.findById(UUID.fromString(id)) ?:  throw IllegalArgumentException("Storage not found")
        val subStorage = StorageEntity.findById(UUID.fromString(subStorageId)) ?:  throw IllegalArgumentException("Storage not found")
        storage.subStorages = SizedCollection(storage.subStorages + subStorage)
        storage.toStorage()
    }

    override fun deleteStorage(id: String): Boolean = transaction {
        SpaceEntity.findById(UUID.fromString(id)).also { it?.delete() } != null
    }
}