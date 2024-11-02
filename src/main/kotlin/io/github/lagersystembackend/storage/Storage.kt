package io.github.lagersystembackend.storage

import io.github.lagersystembackend.depot.DepotEntity
import io.github.lagersystembackend.depot.Depots
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.UUID
import io.github.lagersystembackend.space.*
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.transactions.transaction

/** Todo: integrate substorages
 *
 */

data class Storage(
    val id: String,
    val name: String,
    val description: String,
    //val subStorages: MutableList<Storage>?,
    val depotId: String
)

@Serializable
data class NetworkStorage(
    val id: String,
    val name: String,
    val description: String,
    //val subStorages: MutableList<NetworkStorage>?,
    val depotId: String
)

@Serializable
data class AddStorageNetworkRequest(
    val name: String,
    val description: String,
    //val subStorages: MutableList<NetworkStorage>?,
    val depotId: String
)

object Storages: UUIDTable() {
    val name = varchar("name", 255)
    val description = text("description")
    val parentStorage = optReference("parentStorageId", Storages)
    val depotId = reference("depotId", Depots)
}

class StorageEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<StorageEntity>(Storages)

    var name by Storages.name
    var description by Storages.description
    val parentStorage by StorageEntity optionalReferencedOn Storages.parentStorage
    val depot by DepotEntity referencedOn Storages.depotId
}

fun StorageEntity.toStorage(): Storage {
    return Storage(
        id.value.toString(),
        name,
        description,
        depot.id.value.toString()
        // subStorages...
    )
}

fun ResultRow.toStorage(): Storage {
    return Storage(
        id = this[Storages.id].value.toString(),  // Assuming id is an EntityID
        name = this[Storages.name],
        description = this[Storages.description],
        depotId = this[Storages.depotId].value.toString()
        //subStorages...
    )
}