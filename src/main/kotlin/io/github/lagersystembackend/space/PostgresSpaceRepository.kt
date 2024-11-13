package io.github.lagersystembackend.space

import io.github.lagersystembackend.storage.StorageEntity
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

    override fun storageExists(storageId: String): Boolean = transaction {
        StorageEntity.findById(UUID.fromString(storageId)) != null
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
        SpaceEntity.findById(UUID.fromString(id)).also { it?.delete() }?.toSpace()
    }
}