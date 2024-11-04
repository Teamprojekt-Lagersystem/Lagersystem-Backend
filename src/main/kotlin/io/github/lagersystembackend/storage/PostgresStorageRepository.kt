package io.github.lagersystembackend.storage

import io.github.lagersystembackend.product.Product
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class PostgresStorageRepository: StorageRepository {
    override fun createStorage(
        name: String,
        description: String,
    ): Product {
        TODO("Not yet implemented")
    }

    override fun getStorage(id: String): Storage? {
        TODO("Not yet implemented")
    }

    override fun getStorages(): List<Storage> {
        TODO("Not yet implemented")
    }

    override fun updateStorage(
        id: String,
        description: String?,
        subStorages: List<Storage>
    ): Storage? {
        TODO("Not yet implemented")
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

    override fun addSpace(id: String, spaceId: String): Storage {
        TODO("Not yet implemented")
    }

    override fun deleteStorage(id: String): Boolean {
        TODO("Not yet implemented")
    }
}