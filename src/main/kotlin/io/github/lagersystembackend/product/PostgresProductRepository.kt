package io.github.lagersystembackend.product
import io.github.lagersystembackend.space.SpaceEntity
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class PostgresProductRepository : ProductRepository {

    override fun createProduct(
        name: String,
        description: String,
        spaceId: String
    ): Product = transaction {
        val space = SpaceEntity.findById(UUID.fromString(spaceId)) ?: return@transaction throw IllegalArgumentException("Space not found")
        val createTime = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
        ProductEntity.new {
            this.name = name
            this.description = description
            this.space = space
            this.creationTime = createTime
            this.updatedAt = createTime
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
            product.updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
        }?.toProduct()
    }

    override fun moveProduct(id: String, spaceId: String): Product? = transaction {
        val targetSpace = SpaceEntity.findById(UUID.fromString(spaceId)) ?: throw IllegalArgumentException("target Space not found")
        ProductEntity.findByIdAndUpdate(UUID.fromString(id)) { product ->
            product.space = targetSpace
            product.updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
        }?.toProduct()

    }

    override fun deleteProduct(id: String): Product? = transaction {
        ProductEntity.findById(UUID.fromString(id)).also { it?.delete() }?.toProduct()
    }

}