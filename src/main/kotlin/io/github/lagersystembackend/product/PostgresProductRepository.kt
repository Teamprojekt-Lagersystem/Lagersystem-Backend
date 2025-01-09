package io.github.lagersystembackend.product
import io.github.lagersystembackend.space.SpaceEntity
import io.github.lagersystembackend.attribute.ProductAttributeEntity
import io.github.lagersystembackend.storage.StorageEntity
import io.github.lagersystembackend.stored_product.StoredProductEntity
import io.github.lagersystembackend.stored_product.StoredProducts
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
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
        depotId: String
    ): Product = transaction {
        val depot = StorageEntity.findById(UUID.fromString(depotId)) ?: throw IllegalArgumentException("Depot not found")
        ProductEntity.new {
            this.name = name
            this.description = description
            this.size = size
            this.unit = unit
            this.depot = depot
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

    override fun getProductsInDepot(depotId: String): List<Product> = transaction {
        StorageEntity.findById(UUID.fromString(depotId)) ?: throw IllegalArgumentException("Depot not found")
        ProductEntity.find { Products.depotId eq UUID.fromString(depotId)}.toList().map { it.toProduct() }
    }

    override fun isProductInUse(productId: String): Boolean = transaction {
        StoredProductEntity.find { StoredProducts.productId eq UUID.fromString(productId) }.count() > 0
    }

    override fun depotExists(id: String): Boolean = transaction {
        StorageEntity.findById(UUID.fromString(id)) != null
    }

    override fun isRootStorage(storageId: String): Boolean = transaction {
        val storage = StorageEntity.findById(UUID.fromString(storageId)) ?: throw IllegalArgumentException("Storage not found")
        storage.parent == null
    }
}