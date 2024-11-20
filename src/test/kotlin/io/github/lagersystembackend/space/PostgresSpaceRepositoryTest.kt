package io.github.lagersystembackend.space

import io.github.lagersystembackend.plugins.configureDatabases
import io.github.lagersystembackend.product.PostgresProductRepository
import io.github.lagersystembackend.product.Product
import io.github.lagersystembackend.product.Products
import io.github.lagersystembackend.space.PostgresSpaceRepository
import io.github.lagersystembackend.space.Space
import io.github.lagersystembackend.space.Spaces
import io.github.lagersystembackend.space.Spaces.storageId
import io.github.lagersystembackend.storage.StorageEntity
import io.github.lagersystembackend.storage.StorageToStorages
import io.github.lagersystembackend.storage.Storages
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test



class PostgresSpaceRepositoryTest {
    val sut = PostgresSpaceRepository()
    val exampleStorageId = UUID.randomUUID()
    lateinit var exampleStorageEntity: StorageEntity

    fun insertSpace(): Space = transaction {
        SpaceEntity.new {
            name = "Space"
            description = "Test space"
            size = 100f
            storage = exampleStorageEntity
        }.toSpace()
    }

    @BeforeTest
    fun setUp() {
        configureDatabases(isTest = true)
        transaction {
            SchemaUtils.create(Products, Spaces, Storages, StorageToStorages)
            exampleStorageEntity = StorageEntity.new(id = exampleStorageId) {
                name = "storage name"
                description = "storage description"
            }
        }
    }

    @AfterTest
    fun tearDown() {
        transaction {
            SchemaUtils.drop(Products, Spaces, Storages, StorageToStorages)
        }
    }

    @Test
    fun `create Space should return Space`() = testApplication {
        val expectedSpace =
            Space("anyId", "Space", 100f,"Space description", emptyList(), exampleStorageId.toString())
        val createdStorage = expectedSpace.run { sut.createSpace(name, size, description, storageId) }

        createdStorage.apply {
            name shouldBe expectedSpace.name
            size shouldBe expectedSpace.size
            description shouldBe expectedSpace.description
            storageId shouldBe expectedSpace.storageId
        }
    }

    @Test
    fun `create Space should throw IllegalArgumentException when storageUUID is invalid UUID`() = testApplication {
        val invalidUUID = "invalidUUID"
        runCatching { sut.createSpace("space", 100f, "description", invalidUUID) }
            .exceptionOrNull().run {
                this shouldNotBe null
                this!!::class shouldBe IllegalArgumentException::class
                this.message shouldBe "Invalid UUID string: $invalidUUID"
            }
    }

    @Test
    fun `get Space should return Space`() = testApplication {
        val createdSpace = insertSpace()
        sut.getSpace(createdSpace.id) shouldBe createdSpace
    }

    @Test
    fun `get Space should return null when Space not found`() = testApplication {
        sut.getSpace(UUID.randomUUID().toString()) shouldBe null
    }

    @Test
    fun `get Space should throw IllegalArgumentException when id is invalid UUID`() = testApplication {
        val invalidUUID = "invalidUUID"
        runCatching { sut.getSpace(invalidUUID) }
            .exceptionOrNull().run {
                this shouldNotBe null
                this!!::class shouldBe IllegalArgumentException::class
                this.message shouldBe "Invalid UUID string: $invalidUUID"
            }
    }

    @Test
    fun `get Spaces should return List of Spaces`() = testApplication {
        val expectedSpaces = listOf(
            Space("anyId", "Space1", 100f,"Space description", products = emptyList(), storageId = exampleStorageId.toString()),
            Space("anyId", "Space2", 100f,"Space description", products = emptyList(), storageId = exampleStorageId.toString()),
            Space("anyId", "Space3", 100f,"Space description", products = emptyList(), storageId = exampleStorageId.toString())
        )
        val createdStorages = expectedSpaces.map { it.run { sut.createSpace(name, size, description, storageId) } }
        sut.getSpaces() shouldBe createdStorages
    }

    @Test
    fun `update Space should return updated Space`() = testApplication {
        val createdSpace = insertSpace()
        val updatedSpace = sut.updateSpace(createdSpace.id, name = "newName", null, null)
        sut.getSpace(createdSpace.id)!!.apply {
            this shouldBe updatedSpace
            name shouldBe "newName"
            description shouldBe createdSpace.description
        }
    }

    @Test
    fun `update Space should update name`() = testApplication {
        val createdSpace = insertSpace()
        sut.updateSpace(createdSpace.id, name = "newName", null, null)
        sut.getSpace(createdSpace.id)!!.apply {
            name shouldBe "newName"
            description shouldBe createdSpace.description
        }
    }

    @Test
    fun `update Space should update description`() = testApplication {
        val createdSpace = insertSpace()
        sut.updateSpace(createdSpace.id, null, null, description = "newDescription")
        sut.getSpace(createdSpace.id)!!.apply {
            name shouldBe createdSpace.name
            description shouldBe "newDescription"
        }
    }

    @Test
    fun `update Space should return null when Space not found`() = testApplication {
        sut.updateSpace(UUID.randomUUID().toString(), name = "newName", null, null) shouldBe null
    }

    @Test
    fun `update Space should throw IllegalArgumentException when id is invalid UUID`() = testApplication {
        val invalidUUID = "invalidUUID"
        runCatching { sut.updateSpace(invalidUUID, name = "newName", null, null) }.exceptionOrNull().run {
            this shouldNotBe null
            this!!::class shouldBe IllegalArgumentException::class
            this.message shouldBe "Invalid UUID string: $invalidUUID"
        }
    }

    @Test
    fun `delete Space should return null when Space not found`() = testApplication {
        sut.deleteSpace(UUID.randomUUID().toString()) shouldBe null
    }

    @Test
    fun `delete Space should delete products`() = testApplication {
        val createdSpace = insertSpace()
        val products = listOf(
            Product("anyId", "Product1", 100f, "Space description", createdSpace.id),
            Product("anyId", "Product2", 200f, "Space description", createdSpace.id),
            Product("anyId", "Product3", 300f, "Space description", createdSpace.id)
        )
        val productRepository = PostgresProductRepository()
        val createdProducts = products.map { it.run { productRepository.createProduct(name, price, description, spaceId) } }

        sut.getSpace(createdSpace.id)!!.products shouldBe createdProducts
        sut.deleteSpace(createdSpace.id)
        productRepository.getProducts() shouldBe emptyList()
    }

}