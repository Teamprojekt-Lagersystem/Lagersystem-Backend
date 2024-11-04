package io.github.lagersystembackend.space

import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class PostgresSpaceRepository : SpaceRepository {
    override fun createSpace(
        name: String,
        size: Float?,
        description: String,
    ): Space = transaction {
        SpaceEntity.new {
            this.name = name
            this.size = size
            this.description = description
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

    override fun deleteSpace(id: String): Boolean = transaction {
        SpaceEntity.findById(UUID.fromString(id)).also { it?.delete() } != null
    }
}