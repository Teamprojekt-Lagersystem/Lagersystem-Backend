package io.github.lagersystembackend.depot

import io.github.lagersystembackend.storage.Storage
import io.github.lagersystembackend.storage.NetworkStorage
import io.github.lagersystembackend.storage.Storages
import io.github.lagersystembackend.storage.toStorage
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

data class Depot(
    val id: String,
    val name: String,
    val description: String,
    val storages: MutableList<Storage>
)

@Serializable
data class NetworkDepot(
    val id: String,
    val name: String,
    val description: String,
    val storages: MutableList<NetworkStorage>
)

@Serializable
data class AddDepotNetworkRequest(
    val name: String,
    val description: String,
    val storages: MutableList<NetworkStorage>
)

object Depots: UUIDTable() {
    val name = varchar("name", 255)
    val description = text("description")
}

class DepotEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<DepotEntity>(Depots)

    var name by Depots.name
    var description by Depots.description
}

fun DepotEntity.toDepot(): Depot {
    return Depot(
        id = id.value.toString(),
        name = name,
        description = description,
        storages = transaction {
            val uuid = UUID.fromString(id)
            val depotId = EntityID(uuid, Depots)
            Storages.selectAll().where { Storages.depotId eq depotId }
                .map { it.toStorage() }
        }.toMutableList()
    )
}