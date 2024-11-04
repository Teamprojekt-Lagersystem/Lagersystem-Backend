package io.github.lagersystembackend.depot

import io.github.lagersystembackend.depot.DepotEntity
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class PostgresDepotRepository : DepotRepository {

    override fun createDepot(
        name: String,
        description: String
    ): Depot = transaction {
        DepotEntity.new {
            this.name = name
            this.description = description
        }.toDepot()
    }

    override fun getDepot(id: String): Depot? = transaction {
        DepotEntity.findById(UUID.fromString(id))?.toDepot()
    }

    override fun getDepots(): List<Depot> = transaction {
        DepotEntity.all().toList().map { it.toDepot() }
    }

    override fun updateDepot(
        id: String,
        name: String?,
        description: String?
    ): Depot? = transaction {
        DepotEntity.findByIdAndUpdate(UUID.fromString(id)) { depot ->
            name?.let { depot.name = it }
            description?.let { depot.description = it }
        }?.toDepot()
    }

    override fun deleteDepot(id: String): Boolean = transaction {
        DepotEntity.findById(UUID.fromString(id)).also { it?.delete() } != null
    }
}