package io.github.lagersystembackend.space

interface SpaceRepository {
    fun createSpace(name: String, size: Float?, description: String, storageId: String): Space
    fun getSpace(id: String): Space?
    fun getSpaces(): List<Space>
    fun updateSpace(id: String, name: String?, size: Float?, description: String?): Space?
    fun deleteSpace(id: String): Boolean
    fun storageExists(storageId: String): Boolean
}