package io.github.lagersystembackend.product

interface ProductRepository {
    fun createProduct(name: String, description: String, size: Double?, unit: String?, unique: Boolean): Product
    fun getProduct(id: String): Product?
    fun getProducts(): List<Product>
    fun updateProduct(id: String, name: String?, description: String?, size: Double?): Product?
    fun deleteProduct(id: String): Product?
    fun isProductInUse(productId: String): Boolean
}