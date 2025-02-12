package io.github.lagersystembackend.search

import io.github.lagersystembackend.attribute.ProductAttributes
import io.github.lagersystembackend.breadcrumb.Breadcrumb
import io.github.lagersystembackend.breadcrumb.BreadcrumbUseCase
import io.github.lagersystembackend.plugins.configureDatabases
import io.github.lagersystembackend.product.ProductEntity
import io.github.lagersystembackend.product.Products
import io.github.lagersystembackend.product.toProduct
import io.github.lagersystembackend.space.SpaceEntity
import io.github.lagersystembackend.space.Spaces
import io.github.lagersystembackend.storage.StorageEntity
import io.github.lagersystembackend.storage.StorageToStorages
import io.github.lagersystembackend.storage.Storages
import io.github.lagersystembackend.stored_product.StoredProducts
import io.mockk.every
import io.mockk.mockk
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest


open class BasePostgresSearchUseCaseTest {
    val breadcrumbUseCaseMock = mockk<BreadcrumbUseCase>()
    val sut = PostgresSearchUseCase(breadcrumbUseCaseMock)
    val spaceId = UUID.randomUUID()
    val storageId = UUID.randomUUID()
    val exampleLocalDateTime = LocalDateTime.parse("2025-01-01T00:00:00")
    lateinit var exampleSpaceEntity: SpaceEntity
    lateinit var exampleStorageEntity: StorageEntity

    fun createProductEntity(
        name: String = "name",
        description: String = "description",
        unit: String? = null,
        size: Double? = null,
        createdAt: LocalDateTime = exampleLocalDateTime,
        updatedAt: LocalDateTime = exampleLocalDateTime
    ) = transaction {
        ProductEntity.new {
            this.name = name
            this.description = description
            this.unit = unit
            this.size = size
            this.createdAt = createdAt
            this.updatedAt = updatedAt
        }
    }

    fun createProduct(
        name: String = "name",
        description: String = "description",
        unit: String? = null,
        size: Double? =  null,
        createdAt: LocalDateTime = exampleLocalDateTime,
        updatedAt: LocalDateTime = exampleLocalDateTime
    ) = createProductEntity(name,  description, unit, size, createdAt, updatedAt).toProduct()

    @BeforeTest
    fun setUp() {
        configureDatabases(isTest = true)
        transaction {
            SchemaUtils.create(Storages, StorageToStorages, Spaces, Products, ProductAttributes, StoredProducts)
            createPostgresFullTextSearchTriggers()
            exampleStorageEntity = StorageEntity.new(id = storageId) {
                name = ""
                description = ""
            }

            exampleSpaceEntity = SpaceEntity.new(id = spaceId) {
                name = ""
                description = ""
                storage = exampleStorageEntity
            }
        }
        every { breadcrumbUseCaseMock.getBreadcrumb(any()) } returns Breadcrumb(emptyList())
    }

    @AfterTest
    fun tearDown() {
        transaction {
            SchemaUtils.drop(Storages, StorageToStorages, Spaces, Products, ProductAttributes, StoredProducts)
        }
    }
}
