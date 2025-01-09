package io.github.lagersystembackend.product

interface ProductRepository {
    fun createProduct(name: String, description: String, size: Double?, unit: String?, depotId: String): Product
    fun getProduct(id: String): Product?
    fun getProducts(): List<Product>
    fun getProductsInDepot(depotId: String): List<Product>
    fun updateProduct(id: String, name: String?, description: String?, size: Double?): Product?
    fun deleteProduct(id: String): Product?
    fun isProductInUse(productId: String): Boolean
    fun isRootStorage(storageId: String): Boolean
    fun depotExists(id: String): Boolean
}