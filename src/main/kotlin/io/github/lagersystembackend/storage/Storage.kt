package io.github.lagersystembackend.storage

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.UUID
import io.github.lagersystembackend.space.*
import org.jetbrains.exposed.sql.transactions.transaction

data class Storage(
    val id: String,
    val name: String,
    val description: String,
    val subStorages: MutableList<Storage>?
)

@Serializable
data class NetworkStorage(
    val id: String,
    val name: String,
    val description: String,
    val subStorages: MutableList<NetworkStorage>?
)

@Serializable
data class AddStorageNetworkRequest(
    val name: String,
    val description: String,
    val subStorages: MutableList<NetworkStorage>?
)

object Storages: UUIDTable() {
    val name = varchar("name", 255)
    val description = text("description")
    val parentStorageId = optReference("parentStorageId", Storages)
}


class StorageEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<StorageEntity>(Storages)

    var name by Storages.name
    var description by Storages.description
    val parentStorage by StorageEntity optionalReferencedOn Storages.parentStorageId
}

fun StorageEntity.toStorage(): Storage {
    return Storage(
        id.value.toString(),
        name,
        description,

    )
}



