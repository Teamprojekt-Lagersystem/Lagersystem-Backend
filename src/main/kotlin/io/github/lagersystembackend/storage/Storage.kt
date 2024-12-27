package io.github.lagersystembackend.storage

import io.github.lagersystembackend.space.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class Storage(
    val id: String,
    val name: String,
    val description: String,
    val spaces: List<Space>,
    val parentId: String?,
    val subStorages: List<Storage>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?
)

@Serializable
data class NetworkStorage(
    val id: String,
    val name: String,
    val description: String,
    val spaces: List<NetworkSpace>,
    val subStorages: List<NetworkStorage>,
    val createdAt: String,
    val updatedAt: String?
)

@Serializable
data class AddStorageNetworkRequest(
    val name: String,
    val description: String,
    val parentId: String? = null
)

@Serializable
data class UpdateStorageNetworkRequest(
    val name: String? = null,
    val description: String? = null
)

@Serializable
data class MoveStorageRequest(
    val newParentId: String?
)

@Serializable
data class CopyStorageRequest(
    val newParentId: String? = null
)

object Storages: UUIDTable() {
    val name = varchar("name", 255)
    val description = text("description")
    val createdAt = datetime("createdAt").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updatedAt").nullable()
}

object StorageToStorages: Table() {
    val parent = reference("parent_storage_id", Storages, onDelete = ReferenceOption.CASCADE)
    val child = reference("child_storage_id", Storages, onDelete = ReferenceOption.CASCADE).uniqueIndex()
}

class StorageEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<StorageEntity>(Storages)

    var name by Storages.name
    var description by Storages.description
    val spaces by SpaceEntity referrersOn Spaces.storageId
    var subStorages by StorageEntity.via(StorageToStorages.parent, StorageToStorages.child)
    var parent: StorageEntity?
        get() = StorageToStorages
            .selectAll().where { StorageToStorages.child eq  id.value }
            .firstOrNull()
            ?.let { findById(it[StorageToStorages.parent]) }
        set(value) {
            StorageToStorages.deleteWhere { child eq id.value }
            if (value != null) {
                StorageToStorages.insert {
                    it[parent] = value.id.value
                    it[child] = id.value
                }
            }
        }
    var createdAt by Storages.createdAt
    var updatedAt by Storages.updatedAt

    override fun delete() {
        spaces.forEach { it.delete() }
        subStorages.forEach { it.delete() }
        super.delete()
    }

}

fun StorageEntity.toStorage(): Storage {
    return Storage(
        id = id.value.toString(),
        name = name,
        description = description,
        spaces = spaces.map { it.toSpace() },
        parentId = parent?.id?.value?.toString(),
        subStorages = subStorages.map { it.toStorage() },
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun Storage.toNetworkStorage(maxDepth: Int? = null) = toNetworkStorage(0, maxDepth)

private fun Storage.toNetworkStorage(depth: Int, maxDepth: Int?): NetworkStorage {
    val subStorages = when {
        maxDepth == null -> subStorages.map { it.toNetworkStorage(null) }
        depth < maxDepth -> subStorages.map { it.toNetworkStorage(depth + 1, maxDepth) }
        else -> emptyList()
    }
    return NetworkStorage(
        id = id,
        name = name,
        description = description,
        spaces = spaces.map { it.toNetworkSpace() },
        subStorages = subStorages,
        createdAt = createdAt.format(DateTimeFormatter.ISO_DATE_TIME),
        updatedAt = updatedAt?.format(DateTimeFormatter.ISO_DATE_TIME),
    )
}