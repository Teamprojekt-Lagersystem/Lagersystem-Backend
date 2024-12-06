package io.github.lagersystembackend.product

import io.github.lagersystembackend.plugins.configureDatabases
import io.github.lagersystembackend.space.Space
import io.github.lagersystembackend.space.SpaceEntity
import io.github.lagersystembackend.space.Spaces
import io.github.lagersystembackend.space.toSpace
import io.github.lagersystembackend.storage.StorageEntity
import io.github.lagersystembackend.storage.StorageToStorages
import io.github.lagersystembackend.storage.Storages
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.server.testing.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class PostgresProductRepositoryTest {
    val sut = PostgresProductRepository()
    val spaceId = UUID.randomUUID()
    val storageId = UUID.randomUUID()
    lateinit var exampleSpace: Space
    lateinit var exampleStorageEntity: StorageEntity

    @BeforeTest
    fun setUp() {
        configureDatabases(isTest = true)
        transaction {
            SchemaUtils.create(Products, Spaces, Storages, StorageToStorages)
            exampleStorageEntity = StorageEntity.new(id = storageId) {
                name = "storage name"
                description = "storage description"
            }

            exampleSpace = SpaceEntity.new(id = spaceId) {
                name = "space name"
                description = "space description"
                storage = exampleStorageEntity
            }.toSpace()
        }
    }

    @AfterTest
    fun tearDown() {
        transaction {
            SchemaUtils.drop(Products, Spaces, Storages, StorageToStorages)
        }
    }

    @Test
    fun `create Product should return Product`() = testApplication {
        val expectedProduct = Product(
            "any id",
            "name",
            1.2f,
            "description",
            spaceId.toString()
        )

        expectedProduct.run { sut.createProduct(name, price, description, spaceId.toString()) }.apply {
            name shouldBe expectedProduct.name
            price shouldBe expectedProduct.price
            description shouldBe expectedProduct.description
            this.spaceId shouldBe expectedProduct.spaceId
        }
    }

    @Test
    fun `create Product should throw IllegalArgumentException when SpaceUUID is unknown`() = testApplication {
        val expectedProduct = Product(
            "any id",
            "name",
            1.2f,
            "description",
            UUID.randomUUID().toString()
        )
        runCatching {
            expectedProduct.run { sut.createProduct(name, price, description, spaceId.toString()) }
        }.exceptionOrNull().run {
            this shouldNotBe null
            this!!::class shouldBe IllegalArgumentException::class
            this.message shouldBe "Space not found"
        }
    }

    @Test
    fun `create Product should throw IllegalArgumentException when SpaceUUID is invalid UUID`() = testApplication {
        val expectedProduct = Product(
            "any id",
            "name",
            1.2f,
            "description",
            "Invalid UUID"
        )
        runCatching {
            expectedProduct.run { sut.createProduct(name, price, description, spaceId.toString()) }
        }.exceptionOrNull().run {
            this shouldNotBe null
            this!!::class shouldBe IllegalArgumentException::class
            this.message shouldBe "Invalid UUID string: ${expectedProduct.spaceId}"
        }
    }

    @Test
    fun `create Product should return Product when prize is null`() = testApplication {
        val expectedProduct = Product(
            "any id",
            "name",
            null,
            "description",
            spaceId.toString()
        )
        expectedProduct.run { sut.createProduct(name, price, description, spaceId.toString()) }.apply {
            name shouldBe expectedProduct.name
            price shouldBe null
            description shouldBe expectedProduct.description
            this.spaceId shouldBe expectedProduct.spaceId
        }
    }

    @Test
    fun `get Product should return Product`() = testApplication {
        val expectedProduct = Product(
            "any id",
            "name",
            null,
            "description",
            spaceId.toString()
        )
        val createdProduct = expectedProduct.run { sut.createProduct(name, price, description, spaceId.toString()) }
        sut.getProduct(createdProduct.id) shouldBe createdProduct
    }

    @Test
    fun `get Product should return null when Product not found`() = testApplication {
        sut.getProduct(UUID.randomUUID().toString()) shouldBe null
    }

    @Test
    fun `get Product should throw IllegalArgumentException when id is invalid UUID`() = testApplication {
        val invalidUUID = "Invalid UUID"
        runCatching {
            sut.getProduct(invalidUUID)
        }.exceptionOrNull().run {
            this shouldNotBe null
            this!!::class shouldBe IllegalArgumentException::class
            this.message shouldBe "Invalid UUID string: $invalidUUID"
        }
    }

    @Test
    fun `get Products should return List of Products`() = testApplication {
        var expectedProducts = listOf(
            Product(
                "any id",
                "name1",
                null,
                "description",
                spaceId.toString()
            ),
            Product(
                "any id",
                "name2",
                null,
                "description",
                spaceId.toString()
            ),
            Product(
                "any id",
                "name3",
                null,
                "description",
                spaceId.toString()
            )
        )
        expectedProducts = expectedProducts.map { it.run { sut.createProduct(name, price, description, spaceId) } }
        sut.getProducts() shouldBe expectedProducts
    }

    @Test
    fun `get Products should return empty List when there are no Products`() = testApplication {
        sut.getProducts() shouldBe emptyList()
    }

    @Test
    fun `update Product should update all attributes Product`() = testApplication {
        val product = Product(
            "any id",
            "name",
            null,
            "description",
            spaceId.toString()
        )
        val secondSpaceId = UUID.randomUUID()
        transaction {
            SpaceEntity.new(id = secondSpaceId) {
                name = "second space name"
                description = "second space description"
                storage = exampleStorageEntity
            }.toSpace()
        }
        val createdProduct = product.run { sut.createProduct(name, price, description, spaceId.toString()) }
        val updatedProduct = sut.updateProduct(createdProduct.id, "new name", 1.2f, "new description", secondSpaceId.toString())

        updatedProduct shouldBe Product(
            createdProduct.id,
            "new name",
            1.2f,
            "new description",
            secondSpaceId.toString()
        )
    }

    @Test
    fun `update Product should be optional for each attribute`() = testApplication {
        val product = Product(
            "any id",
            "name",
            null,
            "description",
            spaceId.toString()
        )
        val createdProduct = product.run { sut.createProduct(name, price, description, spaceId.toString()) }
        val updatedProduct = sut.updateProduct(createdProduct.id, null, null, null, null)

        updatedProduct shouldBe createdProduct
    }

    @Test
    fun `update Product should return null when Product not found`() = testApplication {

        val updatedProduct = sut.updateProduct(UUID.randomUUID().toString(), "any new name", null, null, null)

        updatedProduct shouldBe null
    }

    @Test
    fun `update Product should throw IllegalArgumentException when id is invalid UUID`() = testApplication {
        val invalidUUID = "Invalid UUID"
        runCatching {
            sut.updateProduct(invalidUUID, null, 12.2f, null, null)
        }.exceptionOrNull().run {
            this shouldNotBe null
            this!!::class shouldBe IllegalArgumentException::class
            this.message shouldBe "Invalid UUID string: $invalidUUID"
        }
    }

    @Test
    fun `update Product should throw IllegalArgumentException when new space id is invalid UUID`() = testApplication {
        val invalidUUID = "Invalid UUID"
        val product = Product(
            "any id",
            "name",
            null,
            "description",
            spaceId.toString()
        )
        product.run { sut.createProduct(name, price, description, spaceId.toString()) }
        runCatching {
            sut.updateProduct(invalidUUID, null, 12.2f, null, invalidUUID)
        }.exceptionOrNull().run {
            this shouldNotBe null
            this!!::class shouldBe IllegalArgumentException::class
            this.message shouldBe "Invalid UUID string: $invalidUUID"
        }
    }

    @Test
    fun `update Product should throw IllegalArgumentException when new SpaceUUID is unknown`() = testApplication {
        val product = Product(
            "any id",
            "name",
            null,
            "description",
            spaceId.toString()
        )
        val createdProduct = product.run { sut.createProduct(name, price, description, spaceId.toString()) }
        runCatching {
            sut.updateProduct(createdProduct.id, null, 12.2f, null, spaceId = UUID.randomUUID().toString())
        }.exceptionOrNull().run {
            this shouldNotBe null
            this!!::class shouldBe IllegalArgumentException::class
            this.message shouldBe "Space not found"
        }
    }

    @Test
    fun `delete Product should return true when Product is deleted`() = testApplication {
        val product = Product(
            "any id",
            "name",
            null,
            "description",
            spaceId.toString()
        )
        val createdProduct = product.run { sut.createProduct(name, price, description, spaceId.toString()) }
        sut.deleteProduct(createdProduct.id) shouldBe createdProduct
        sut.getProduct(createdProduct.id) shouldBe null

    }

    @Test
    fun `delete Product should return false when Product not found`() = testApplication {
        sut.deleteProduct(UUID.randomUUID().toString()) shouldBe null
    }

    @Test
    fun `delete Product should throw IllegalArgumentException when id is invalid UUID`() = testApplication {
        val invalidUUID = "Invalid UUID"
        runCatching {
            sut.deleteProduct(invalidUUID)
        }.exceptionOrNull().run {
            this shouldNotBe null
            this!!::class shouldBe IllegalArgumentException::class
            this.message shouldBe "Invalid UUID string: $invalidUUID"
        }
    }

    @Test
    fun `moveProduct should return Product with new Space`() = testApplication {
        val product = Product(
            "any id",
            "name",
            null,
            "description",
            spaceId.toString()
        )

        val secondSpaceId = UUID.randomUUID()
        transaction {
            SpaceEntity.new(id = secondSpaceId) {
                name = "second space name"
                description = "second space description"
                storage = exampleStorageEntity
            }.toSpace()
        }

        val createdProduct = product.run { sut.createProduct(name, price, description, spaceId.toString()) }
        val movedProduct = sut.moveProduct(createdProduct.id, secondSpaceId.toString())

        movedProduct shouldBe Product(
            createdProduct.id,
            createdProduct.name,
            createdProduct.price,
            createdProduct.description,
            secondSpaceId.toString()
        )
    }

    @Test
    fun `moveProduct should throw IllegalArgumentException when id is invalid UUID`() = testApplication {
        val invalidUUID = "Invalid UUID"
        runCatching {
            sut.moveProduct(invalidUUID, spaceId.toString())
        }.exceptionOrNull().run {
            this shouldNotBe null
            this!!::class shouldBe IllegalArgumentException::class
            this.message shouldBe "Invalid UUID string: Invalid UUID"
        }
    }


    @Test
    fun `moveProduct should throw IllegalArgumentException when to SpaceUUID is unknown`() = testApplication {
        val product = Product(
            "any id",
            "name",
            null,
            "description",
            spaceId.toString()
        )
        val createdProduct = product.run { sut.createProduct(name, price, description, spaceId.toString()) }
        runCatching {
            sut.moveProduct(createdProduct.id, UUID.randomUUID().toString())
        }.exceptionOrNull().run {
            this shouldNotBe null
            this!!::class shouldBe IllegalArgumentException::class
            this.message shouldBe "to Space not found"
        }
    }

    @Test
    fun `moveProduct should throw IllegalArgumentException when product is not found`() = testApplication {
        runCatching {
            sut.moveProduct(UUID.randomUUID().toString(), spaceId.toString())
        }.exceptionOrNull().run {
            this shouldBe  null
        }
    }
}