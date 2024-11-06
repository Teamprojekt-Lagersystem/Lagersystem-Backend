package io.github.lagersystembackend.storage

import io.github.lagersystembackend.product.Product

interface StorageRepository {
    fun createStorage(name: String, description: String, parentId: String?): Storage
    fun getStorage(id: String): Storage?
    fun getStorages(): List<Storage>
    fun updateStorage(id: String, name: String?, description: String?, parentId: String?, subStorages: List<Storage>?): Storage?
    fun addSubStorage(id: String, subStorageId: String): Storage
    fun deleteStorage(id: String): Boolean
}