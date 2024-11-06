package io.github.lagersystembackend.storage

import io.github.lagersystembackend.space.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

data class Storage(
    val id: String,
    val name: String,
    val description: String,
    val spaces: List<Space>,
    val parentId: String?,
    val subStorages: List<Storage>
)

@Serializable
data class NetworkStorage(
    val id: String,
    val name: String,
    val description: String,
    val spaces: List<NetworkSpace>,
    val subStorages: List<NetworkStorage>
)

@Serializable
data class AddStorageNetworkRequest(
    val name: String,
    val description: String,
    val parentId: String?
)

object Storages: UUIDTable() {
    val name = varchar("name", 255)
    val description = text("description")
}

object StorageToStorages: Table() {
    val parent = reference("parent_storage_id", Storages)
    val child = reference("child_storage_id", Storages).uniqueIndex()
}

class StorageEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<StorageEntity>(Storages)

    var name by Storages.name
    var description by Storages.description
    val spaces by SpaceEntity referrersOn Spaces.storageId
    var subStorages by StorageEntity.via(StorageToStorages.parent, StorageToStorages.child)
    var parent: StorageEntity?
        get() = StorageToStorages
            .selectAll().where { StorageToStorages.child eq  id }
            .firstOrNull()
            ?.let { findById(it[StorageToStorages.parent]) }
        set(value) {
            StorageToStorages.deleteWhere { child eq id }
            if (value != null) {
                StorageToStorages.insert {
                    it[parent] = value.id
                    it[child] = id
                }
            }
        }
}

fun StorageEntity.toStorage(depth: Int = 0, maxDepth: Int = 3): Storage {
    return Storage(
        id = id.value.toString(),
        name = name,
        description = description,
        spaces = spaces.map { it.toSpace() },
        parentId = parent?.id.toString(),
        subStorages = subStorages.map { it.toStorage(depth + 1, maxDepth) }
    )
}

fun NetworkStorage.toStorage(depth: Int = 0, maxDepth: Int = 3): Storage {
    return Storage(
        id = id,
        name = name,
        description = description,
        spaces = spaces.map { it.toSpace() },
        parentId = null,
        subStorages = subStorages.map { it.toStorage(depth + 1, maxDepth) }
    )
}

fun Storage.toNetworkStorage(depth: Int = 0, maxDepth: Int = 3): NetworkStorage {
    return NetworkStorage(
        id = id,
        name = name,
        description = description,
        spaces = spaces.map { it.toNetworkSpace() },
        subStorages = subStorages.map { it.toNetworkStorage(depth + 1, maxDepth) }
    )
}