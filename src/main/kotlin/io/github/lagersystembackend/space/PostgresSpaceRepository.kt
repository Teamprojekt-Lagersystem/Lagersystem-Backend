package io.github.lagersystembackend.space

import io.github.lagersystembackend.storage.StorageEntity
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class PostgresSpaceRepository : SpaceRepository {
    override fun createSpace(
        name: String,
        size: Float?,
        description: String,
        storageId: String
    ): Space = transaction {
        val storage = StorageEntity.findById(UUID.fromString(storageId)) ?: throw IllegalArgumentException("Storage not found")
        val createTime = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
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
}