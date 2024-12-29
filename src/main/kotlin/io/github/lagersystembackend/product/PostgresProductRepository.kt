package io.github.lagersystembackend.product
import io.github.lagersystembackend.space.SpaceEntity
import io.github.lagersystembackend.attribute.ProductAttributeEntity
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class PostgresProductRepository : ProductRepository {

    override fun createProduct(
        name: String,
        description: String,
        size: Double?,
        unit: String?,
        spaceId: String
    ): Product = transaction {
        val space = SpaceEntity.findById(UUID.fromString(spaceId)) ?: return@transaction throw IllegalArgumentException("Space not found")
        if (space.unit != unit) {
            throw IllegalArgumentException("Unit of product and space must match")
        }
        if (size != null) {
            val currentSize = space.currentSize
            val totalSize = space.totalSize

            if (currentSize != null && totalSize != null) {
                if (currentSize + size > totalSize) {
                    throw IllegalArgumentException("product does not fit inside the space anymore")
                }
            }
            space.currentSize = space.currentSize?.plus(size)
        }
        ProductEntity.new {
            this.name = name
            this.description = description
            this.size = size
            this.unit = unit
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
        size: Double?,
    ): Product? = transaction {
        ProductEntity.findByIdAndUpdate(UUID.fromString(id)) { product ->
            val currentSize = product.space.currentSize
            val totalSize = product.space.totalSize
            val oldSize = product.size
            if (currentSize != null && totalSize != null && size != null && oldSize != null) {
                if (currentSize - oldSize + size <= totalSize) {
                    size.let { product.size = it }
                    product.space.currentSize = product.space.currentSize?.minus(oldSize)?.plus(size)
                } else {
                    throw IllegalArgumentException("Product does not fit inside the space anymore.")
                }
            }
            name?.let { product.name = it }
            description?.let { product.description = it }
            product.updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
        }?.toProduct()
    }

    override fun moveProduct(id: String, spaceId: String): Product? = transaction {
        val targetSpace = SpaceEntity.findById(UUID.fromString(spaceId)) ?: throw IllegalArgumentException("target Space not found")
        ProductEntity.findByIdAndUpdate(UUID.fromString(id)) { product ->
            val size = product.size
            val totalSize = targetSpace.totalSize
            val currentSize = targetSpace.currentSize
            if (size != null && totalSize != null && currentSize != null) {
                if (currentSize.plus(size) <= totalSize) {
                    product.space.currentSize = product.space.currentSize?.minus(size)
                    targetSpace.currentSize = currentSize.plus(size)
                } else {
                    throw IllegalArgumentException("Product does not fit inside the new space.")
                }
            }
            product.space = targetSpace
            product.updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
        }?.toProduct()
    }

    override fun deleteProduct(id: String): Product? = transaction {
        val product = ProductEntity.findById(UUID.fromString(id)).also { it?.delete() } //?.toProduct()
        val size = product?.size
        if (product != null && size != null) {
            product.space.currentSize = product.space.currentSize?.minus(size)
        }
        product?.toProduct()
    }

    override fun copyProduct(productId: String, targetSpaceId: String): Product {
        return transaction {

            val originalProduct = ProductEntity.findById(UUID.fromString(productId))
                ?: throw IllegalArgumentException("Product with ID $productId not found")

            val targetSpace = SpaceEntity.findById(UUID.fromString(targetSpaceId))
                ?: throw IllegalArgumentException("Space with ID $targetSpaceId not found")

            val newProductEntity = ProductEntity.new {
                name = originalProduct.name
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