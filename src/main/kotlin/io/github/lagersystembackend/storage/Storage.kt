package io.github.lagersystembackend.storage

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.UUID

data class Storage(
    val id: String,
    val name: String,
    val description: String,
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


class StorageEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<StorageEntity>(Storages)

    var name by Storages.name
    var description by Storages.description
}
