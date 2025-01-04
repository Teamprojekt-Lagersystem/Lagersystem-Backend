package io.github.lagersystembackend.search

import io.github.lagersystembackend.attribute.ProductAttributes
import io.github.lagersystembackend.plugins.configureDatabases
import io.github.lagersystembackend.product.ProductEntity
import io.github.lagersystembackend.product.Products
import io.github.lagersystembackend.product.toProduct
import io.github.lagersystembackend.space.SpaceEntity
import io.github.lagersystembackend.space.Spaces
import io.github.lagersystembackend.storage.StorageEntity
import io.github.lagersystembackend.storage.StorageToStorages
import io.github.lagersystembackend.storage.Storages
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest


open class BaseDefaultSearchUseCaseTest {
    val sut = DefaultSearchUseCase()
    val spaceId = UUID.randomUUID()
    val storageId = UUID.randomUUID()
    val exampleLocalDateTime = LocalDateTime.parse("2025-01-01T00:00:00")
    lateinit var exampleSpaceEntity: SpaceEntity
    lateinit var exampleStorageEntity: StorageEntity

    fun createProduct(
        name: String = "name",
        description: String = "description",
        space: SpaceEntity = exampleSpaceEntity,
        createdAt: LocalDateTime = exampleLocalDateTime,
        updatedAt: LocalDateTime = exampleLocalDateTime
    ) = transaction {
        ProductEntity.new {
            this.name = name
            this.description = description
            this.space = space
            this.createdAt = createdAt
            this.updatedAt = updatedAt
        }.toProduct()
    }

    @BeforeTest
    fun setUp() {
        configureDatabases(isTest = true)
        transaction {
            SchemaUtils.create(Storages, StorageToStorages, Spaces, Products, ProductAttributes)
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
    }

    @AfterTest
    fun tearDown() {
        transaction {
            SchemaUtils.drop(Storages, StorageToStorages, Spaces, Products, ProductAttributes)
        }
    }
}
