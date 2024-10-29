package io.github.lagersystembackend.storage

import io.github.lagersystembackend.product.NetworkProduct
import io.github.lagersystembackend.product.Product
import io.github.lagersystembackend.product.ProductEntity
import io.github.lagersystembackend.product.Products
import io.github.lagersystembackend.product.toNetworkProduct
import io.github.lagersystembackend.product.toProduct
import io.github.lagersystembackend.space.NetworkSpace
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.UUID
import io.github.lagersystembackend.space.*

data class Storage(
    val id: String,
    val name: String,
    val description: String,
    val spaces: MutableList<Space>,
    val subStorages: MutableList<Storage>
)

@Serializable
data class NetworkStorage(
    val id: String,
    val name: String,
    val description: String,
    val spaces: MutableList<NetworkSpace>,
    val subStorages: MutableList<NetworkStorage>
)

@Serializable
data class AddStorageNetworkRequest(
    val name: String,
    val description: String,
    val spaces: MutableList<NetworkSpace>,
    val subStorages: MutableList<NetworkStorage>
)

object Storages: UUIDTable() {
    val name = varchar("name", 255)
    val description = text("description")
}


class StorageEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<StorageEntity>(Storages)

    var name by Storages.name
    var description by Storages.description
    val subStorages by StorageEntity  Storages
}
