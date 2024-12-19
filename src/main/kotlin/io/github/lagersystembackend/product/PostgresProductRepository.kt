package io.github.lagersystembackend.product
import io.github.lagersystembackend.space.SpaceEntity
import io.github.lagersystembackend.attribute.ProductAttributeEntity
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class PostgresProductRepository : ProductRepository {

    override fun createProduct(
        name: String,
        description: String,
        spaceId: String
    ): Product = transaction {
        val space = SpaceEntity.findById(UUID.fromString(spaceId)) ?: return@transaction throw IllegalArgumentException("Space not found")
        ProductEntity.new {
            this.name = name
            this.description = description
            this.space = space
        }.toProduct()
    }

    override fun getProduct(id: String): Product? = transaction {
        ProductEntity.findById(UUID.fromString(id))?.toProduct()
    }

    override fun getProducts(): List<Product> = transaction {
        ProductEntity.all().toList().map { it.toProduct() }
    }

    override fun updateProduct(
        id: String,
        name: String?,
        description: String?,
        spaceId: String?
    ): Product? = transaction {
        ProductEntity.findByIdAndUpdate(UUID.fromString(id)) { product ->
            name?.let { product.name = it }
            description?.let { product.description = it }
            spaceId?.let { product.space = SpaceEntity.findById(UUID.fromString(it)) ?: throw IllegalArgumentException("Space not found") }
        }?.toProduct()
    }

    override fun moveProduct(id: String, spaceId: String): Product? = transaction {
        val targetSpace = SpaceEntity.findById(UUID.fromString(spaceId)) ?: throw IllegalArgumentException("target Space not found")
        ProductEntity.findByIdAndUpdate(UUID.fromString(id)) { product ->
            product.space = targetSpace
        }?.toProduct()

    }

    override fun deleteProduct(id: String): Product? = transaction {
        ProductEntity.findById(UUID.fromString(id)).also { it?.delete() }?.toProduct()
    }

    override fun copyProduct(productId: String, targetSpaceId: String): Product {
        return transaction {

            val originalProduct = ProductEntity.findById(UUID.fromString(productId))
                ?: throw IllegalArgumentException("Product with ID $productId not found")

            val targetSpace = SpaceEntity.findById(UUID.fromString(targetSpaceId))
                ?: throw IllegalArgumentException("Space with ID $targetSpaceId not found")

            val newProductEntity = ProductEntity.new {
                name = originalProduct.name + " (Copy)"
                description = originalProduct.description
                space = targetSpace
            }

            originalProduct.attributes.forEach { attribute ->
                ProductAttributeEntity.new {
                    key = attribute.key
                    value = attribute.value
                    product = newProductEntity
                }
            }
            newProductEntity.toProduct()
        }
    }
}