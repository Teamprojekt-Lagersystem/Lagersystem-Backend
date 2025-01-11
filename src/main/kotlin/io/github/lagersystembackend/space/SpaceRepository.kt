package io.github.lagersystembackend.space

interface SpaceRepository {
    fun createSpace(name: String, description: String, size: Double?, unit: String?, storageId: String): Space
    fun getSpace(id: String): Space?
    fun getSpaces(): List<Space>
    fun updateSpace(id: String, name: String?, description: String?, size: Double?): Space?
    fun deleteSpace(id: String): Space?
    fun spaceExists(id: String): Boolean
    fun moveSpace(spaceId: String, targetStorageId: String): Space
    fun copySpace(spaceId: String, targetStorageId: String): Space
    fun isProductStored(spaceId: String): Boolean
}