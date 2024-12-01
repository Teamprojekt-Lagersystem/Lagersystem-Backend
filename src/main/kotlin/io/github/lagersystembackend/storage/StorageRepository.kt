package io.github.lagersystembackend.storage

interface StorageRepository {
    fun createStorage(name: String, description: String, parentId: String?): Storage
    fun getStorage(id: String): Storage?
    fun getStorages(): List<Storage>
    fun updateStorage(id: String, name: String? = null, description: String? = null): Storage?
    fun deleteStorage(id: String): Storage?
    fun storageExists(id: String): Boolean
    fun moveStorage(id: String, newParentId: String?): Storage
}