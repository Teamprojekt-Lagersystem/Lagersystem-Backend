package io.github.lagersystembackend.product

import io.github.lagersystembackend.attribute.Attribute
import io.github.lagersystembackend.plugins.configureDatabases
import io.github.lagersystembackend.space.Space
import io.github.lagersystembackend.space.SpaceEntity
import io.github.lagersystembackend.space.Spaces
import io.github.lagersystembackend.space.toSpace
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
    lateinit var exampleSpace: Space

    @BeforeTest
    fun setUp() {
        configureDatabases(isTest = true)
        transaction {
            SchemaUtils.create(Products, Spaces)
            transaction {
                exampleSpace = SpaceEntity.new(id = spaceId) {
                    name = "space name"
                    description = "space description"
                }.toSpace()
            }
        }
    }

    @AfterTest
    fun tearDown() {
        transaction {
            SchemaUtils.drop(Products, Spaces)
        }
    }

    @Test
    fun `create Product should return Product`() = testApplication {
        val expectedProduct = Product(
            "any id",
            "name",
            "description",
            emptyMap(),
            spaceId.toString()
        )

        expectedProduct.run { sut.createProduct(name, description, spaceId.toString()) }.apply {
            name shouldBe expectedProduct.name
            description shouldBe expectedProduct.description
            this.spaceId shouldBe expectedProduct.spaceId
        }
    }

    @Test
    fun `create Product should throw IllegalStateException when SpaceUUID is unknown`() = testApplication {
        val expectedProduct = Product(
            "any id",
            "name",
            "description",
            emptyMap(),
            UUID.randomUUID().toString()
        )
        runCatching {
            expectedProduct.run { sut.createProduct(name, description, spaceId.toString()) }
        }.exceptionOrNull().run {
            this shouldNotBe null
            this!!::class shouldBe IllegalArgumentException::class
            this.message shouldBe "Space not found"
        }
    }

    @Test
    fun `create Product should throw IllegalStateException when SpaceUUID is invalid UUID`() = testApplication {
        val expectedProduct = Product(
            "any id",
            "name",
            "description",
            emptyMap(),
            "Invalid UUID"
        )
        runCatching {
            expectedProduct.run { sut.createProduct(name, description, spaceId.toString()) }
        }.exceptionOrNull().run {
            this shouldNotBe null
            this!!::class shouldBe IllegalArgumentException::class
            this.message shouldBe "Invalid UUID string: ${expectedProduct.spaceId}"
        }
    }

    @Test
    fun `get Product should return Product`() = testApplication {
        val expectedProduct = Product(
            "any id",
            "name",
            "description",
            emptyMap(),
            spaceId.toString()
        )
        val createdProduct = expectedProduct.run { sut.createProduct(name, description, spaceId.toString()) }
        sut.getProduct(createdProduct.id) shouldBe createdProduct
    }

    @Test
    fun `get Product should return null when Product not found`() = testApplication {
        sut.getProduct(UUID.randomUUID().toString()) shouldBe null
    }

    @Test
    fun `get Product should throw IllegalStateException when id is invalid UUID`() = testApplication {
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
                "description",
                emptyMap(),
                spaceId.toString()
            ),
            Product(
                "any id",
                "name2",
                "description",
                mapOf(
                    "someKey" to Attribute.NumberAttribute(1.2f),
                    "someOtherKey" to Attribute.StringAttribute("some text"),
                    "someBooleanKey" to Attribute.BooleanAttribute(true)
                ),
                spaceId.toString()
            ),
            Product(
                "any id",
                "name3",
                "description",
                emptyMap(),
                spaceId.toString()
            )
        )
        expectedProducts = expectedProducts.map { it.run { sut.createProduct(name, description, spaceId) } }
        sut.getProducts() shouldBe expectedProducts
    }

    @Test
    fun `get Products should return empty List when there are no Products`() = testApplication {
        sut.getProducts() shouldBe emptyList()
    }

    @Test
    fun `update Product should update Product`() = testApplication {
        val product = Product(
            "any id",
            "name",
            "description",
            emptyMap(),
            spaceId.toString()
        )
        val secondSpaceId = UUID.randomUUID()
        transaction {
            SpaceEntity.new(id = secondSpaceId) {
                name = "second space name"
                description = "second space description"
            }.toSpace()
        }
        val createdProduct = product.run { sut.createProduct(name, description, spaceId.toString()) }
        val updatedProduct = sut.updateProduct(createdProduct.id, "new name", "new description", secondSpaceId.toString())

        updatedProduct shouldBe Product(
            createdProduct.id,
            "new name",
            "new description",
            emptyMap(),
            secondSpaceId.toString()
        )
    }

    @Test
    fun `update Product should be optional for each attribute`() = testApplication {
        val product = Product(
            "any id",
            "name",
            "description",
            emptyMap(),
            spaceId.toString()
        )
        val createdProduct = product.run { sut.createProduct(name, description, spaceId.toString()) }
        val updatedProduct = sut.updateProduct(createdProduct.id, null, null, null)

        updatedProduct shouldBe createdProduct
    }

    @Test
    fun `update Product should return null when Product not found`() = testApplication {

        val updatedProduct = sut.updateProduct(UUID.randomUUID().toString(), "any new name", null, null)

        updatedProduct shouldBe null
    }

    @Test
    fun `update Product should throw IllegalStateException when id is invalid UUID`() = testApplication {
        val invalidUUID = "Invalid UUID"
        runCatching {
            sut.updateProduct(invalidUUID, null, null, null)
        }.exceptionOrNull().run {
            this shouldNotBe null
            this!!::class shouldBe IllegalArgumentException::class
            this.message shouldBe "Invalid UUID string: $invalidUUID"
        }
    }

    @Test
    fun `update Product should throw IllegalStateException when new space id is invalid UUID`() = testApplication {
        val invalidUUID = "Invalid UUID"
        val product = Product(
            "any id",
            "name",
            "description",
            emptyMap(),
            spaceId.toString()
        )
        product.run { sut.createProduct(name, description, spaceId.toString()) }
        runCatching {
            sut.updateProduct(invalidUUID, null, null, invalidUUID)
        }.exceptionOrNull().run {
            this shouldNotBe null
            this!!::class shouldBe IllegalArgumentException::class
            this.message shouldBe "Invalid UUID string: $invalidUUID"
        }
    }

    @Test
    fun `update Product should throw IllegalStateException when new SpaceUUID is unknown`() = testApplication {
        val product = Product(
            "any id",
            "name",
            "description",
            emptyMap(),
            spaceId.toString()
        )
        val createdProduct = product.run { sut.createProduct(name, description, spaceId.toString()) }
        runCatching {
            sut.updateProduct(createdProduct.id, null, null, spaceId = UUID.randomUUID().toString())
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
            "description",
            emptyMap(),
            spaceId.toString()
        )
        val createdProduct = product.run { sut.createProduct(name, description, spaceId.toString()) }
        sut.deleteProduct(createdProduct.id) shouldBe true
        sut.getProduct(createdProduct.id) shouldBe null

    }

    @Test
    fun `delete Product should return false when Product not found`() = testApplication {
        sut.deleteProduct(UUID.randomUUID().toString()) shouldBe false
    }

    @Test
    fun `delete Product should throw IllegalStateException when id is invalid UUID`() = testApplication {
        val invalidUUID = "Invalid UUID"
        runCatching {
            sut.deleteProduct(invalidUUID)
        }.exceptionOrNull().run {
            this shouldNotBe null
            this!!::class shouldBe IllegalArgumentException::class
            this.message shouldBe "Invalid UUID string: $invalidUUID"
        }
    }
}