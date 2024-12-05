package io.github.lagersystembackend.product

interface ProductRepository {
    fun createProduct(name: String, price: Float?, description: String, spaceId: String): Product
    fun getProduct(id: String): Product?
    fun getProducts(): List<Product>
    fun updateProduct(id: String, name: String?, price: Float?, description: String?, spaceId: String?): Product?
    fun deleteProduct(id: String): Product?
    fun moveProduct(id: String, spaceId: String): Product?
    fun spaceExists(id: String): Boolean
}