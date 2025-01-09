package io.github.lagersystembackend.stored_product

import io.github.lagersystembackend.product.ProductEntity
import io.github.lagersystembackend.space.SpaceEntity

interface StoredProductRepository {
    fun createStoredProduct(productId: String, spaceId: String, quantity: Int): StoredProductDTO
    fun getStoredProduct(id: String): StoredProductDTO?
    fun getStoredProducts(): List<StoredProductDTO>
    fun getStoredProductsBySpaceId(spaceId: String): List<StoredProductDTO>
    fun updateStoredProduct(id: String, quantity: Int): StoredProductDTO
    fun moveStoredProduct(id: String, targetSpaceId: String): StoredProductDTO
    fun deleteStoredProduct(id: String): StoredProduct?
    fun spaceExists(id: String): Boolean
    fun productExists(id: String): Boolean
    fun fitsInSpace(productId: String, spaceId: String, quantity: Int): Boolean
    fun checkUnit(productId: String, spaceId: String): Boolean
    fun isStored(productId: String, spaceId: String): Boolean
    fun getId(productId: String, spaceId: String): String
    fun copyStoredProduct(id: String, targetSpaceId: String): StoredProductDTO
}