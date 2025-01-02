package io.github.lagersystembackend.stored_product

interface StoredProductRepository {
    fun createStoredProduct(productId: String, spaceId: String, quantity: Int): StoredProduct
    fun getStoredProduct(id: String): StoredProduct?
    fun getStoredProducts(): List<StoredProduct>
    fun getStoredProductsBySpaceId(spaceId: String): List<StoredProduct>
    fun updateStoredProduct(id: String, spaceId: String?, quantity: Int?): StoredProduct?
    fun deleteStoredProduct(id: String): StoredProduct?
    fun spaceExists(id: String): Boolean
    fun productExists(id: String): Boolean
    fun fitsInSpace(id: String, size: Double): Boolean
    fun copyStoredProduct(id: String, targetSpaceId: String): StoredProduct
    //TODO: check unit
}