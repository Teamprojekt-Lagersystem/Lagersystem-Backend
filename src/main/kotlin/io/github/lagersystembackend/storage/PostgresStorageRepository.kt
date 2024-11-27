package io.github.lagersystembackend.storage

import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction

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

        val newParent = newParentId?.let { StorageEntity.findById(UUID.fromString(it)) }
        storage.parent = newParent
        storage.toStorage()
    }

    override fun isCircularReference(storageId: String, targetParentId: String): Boolean = transaction {
        val storage = StorageEntity.findById(UUID.fromString(storageId)) ?: return@transaction false
        val targetParent = StorageEntity.findById(UUID.fromString(targetParentId)) ?: return@transaction false

        generateSequence(targetParent) { it.parent }.any { it.id.value.toString() == storageId }
    }
}