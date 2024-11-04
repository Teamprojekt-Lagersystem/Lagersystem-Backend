package io.github.lagersystembackend.storage

import io.github.lagersystembackend.product.Product

interface StorageRepository {
    fun createStorage(name: String, description: String): Product
    fun getStorage(id: String): Storage?
    fun getStorages(): List<Storage>
    fun updateStorage(id: String, description: String?, subStorages: List<Storage>): Storage?
    fun addSubStorage(id: String, subStorageId: String): Storage
    fun addSpace(id: String, spaceId: String): Storage
    fun deleteStorage(id: String): Boolean
}