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
    ): Product = transaction {
        ProductEntity.new {
            this.name = name
            this.description = description
            this.size = size
            this.unit = unit
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
        // Todo: Update size and check if the new size does not conflict with any space the product is stored in
        ProductEntity.findByIdAndUpdate(UUID.fromString(id)) { product ->
            name?.let { product.name = it }
            description?.let { product.description = it }
            product.updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
        }?.toProduct()
    }

    override fun deleteProduct(id: String): Product? = transaction {
        val product = ProductEntity.findById(UUID.fromString(id))

        product?.delete()

        product?.toProduct()
    }
}