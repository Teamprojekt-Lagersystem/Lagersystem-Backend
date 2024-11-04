package io.github.lagersystembackend.storage

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table
import java.util.UUID

data class Storage(
    val id: String,
    val name: String,
    val description: String,
    val parentId: String?,
    val subStorages: List<Storage>
)

@Serializable
data class NetworkStorage(
    val id: String,
    val name: String,
    val description: String,
    val subStorages: List<NetworkStorage>
)

@Serializable
data class AddStorageNetworkRequest(
    val name: String,
    val description: String,
)

object Storages: UUIDTable() {
    val name = varchar("name", 255)
    val description = text("description")
}

object StorageToStorages: Table() {
    val parent = reference("parent_storage_id", Storages)
    val child = reference("child_storage_id", Storages)
}

class StorageEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<StorageEntity>(Storages)

    var name by Storages.name
    var description by Storages.description
    var parents by StorageEntity.via(StorageToStorages.child, StorageToStorages.parent)
    var subStorages by StorageEntity.via(StorageToStorages.parent, StorageToStorages.child)
}

fun StorageEntity.toStorage(depth: Int = 0, maxDepth: Int = 3): Storage {
    return Storage(
        id = id.value.toString(),
        name = name,
        description = description,
        parentId = parents.firstOrNull()?.id.toString(),
        subStorages = subStorages.map { it.toStorage(depth + 1, maxDepth) }
    )
}