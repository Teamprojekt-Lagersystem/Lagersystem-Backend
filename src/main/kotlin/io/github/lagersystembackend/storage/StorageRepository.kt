package io.github.lagersystembackend.storage

interface StorageRepository {
    fun createStorage(name: String, description: String, parentId: String?): Storage
    fun getStorage(id: String?): Storage?
    fun getStorages(): List<Storage>
    fun updateStorage(id: String, name: String?, description: String?, parentId: String?, subStorages: List<Storage>?): Storage?
    fun addSubStorage(parentId: String, subStorageId: String): Storage
    fun deleteStorage(id: String): Boolean
    fun storageExists(id: String): Boolean
}