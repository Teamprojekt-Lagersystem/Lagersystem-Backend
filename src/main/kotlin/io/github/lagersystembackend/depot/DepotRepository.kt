package io.github.lagersystembackend.depot

interface DepotRepository {
    fun createDepot(name: String, description: String): Depot
    fun getDepot(id: String): Depot?
    fun getDepots(): List<Depot>
    fun updateDepot(id: String, name: String?, description: String?): Depot?
    fun deleteDepot(id: String): Boolean
}