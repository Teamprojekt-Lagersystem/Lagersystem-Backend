package io.github.lagersystembackend.product

import io.github.lagersystembackend.attribute.Attribute
import io.github.lagersystembackend.attribute.PostgresProductAttributeRepository
import io.github.lagersystembackend.attribute.ProductAttributeEntity
import io.github.lagersystembackend.attribute.ProductAttributes
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
import java.time.LocalDateTime
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
            SchemaUtils.create(Storages, StorageToStorages, Spaces, Products, ProductAttributes)
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
            SchemaUtils.drop(Storages, StorageToStorages, Spaces, Products, ProductAttributes)
        }
    }

    @Test
    fun `create Product should return Product`() = testApplication {
        val expectedProduct = Product(
            "any id",
            "name",
            "description",
            emptyMap(),
            spaceId.toString(),
            LocalDateTime.now()
        )

        expectedProduct.run { sut.createProduct(name, description, spaceId.toString()) }.apply {
            name shouldBe expectedProduct.name
            description shouldBe expectedProduct.description
            this.spaceId shouldBe expectedProduct.spaceId
        }
    }

    @Test
    fun `create Product should throw IllegalArgumentException when SpaceUUID is unknown`() = testApplication {
        val expectedProduct = Product(
            "any id",
            "name",
            "description",
            emptyMap(),
            UUID.randomUUID().toString(),
            LocalDateTime.now()
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
    fun `create Product should throw IllegalArgumentException when SpaceUUID is invalid UUID`() = testApplication {
        val expectedProduct = Product(
            "any id",
            "name",
            "description",
            emptyMap(),
            "Invalid UUID",
            LocalDateTime.now()
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
            spaceId.toString(),
            LocalDateTime.now()
        )
        val createdProduct = expectedProduct.run { sut.createProduct(name, description, spaceId.toString()) }
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
                "description",
                emptyMap(),
                spaceId.toString(),
                LocalDateTime.now()
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
                spaceId.toString(),
                LocalDateTime.now()
            ),
            Product(
                "any id",
                "name3",
                "description",
                emptyMap(),
                spaceId.toString(),
                LocalDateTime.now()
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
            spaceId.toString(),
            LocalDateTime.now()
        )
        val secondSpaceId = UUID.randomUUID()
        transaction {
            SpaceEntity.new(id = secondSpaceId) {
                name = "second space name"
                description = "second space description"
                storage = exampleStorageEntity
            }.toSpace()
        }
        val createdProduct = product.run { sut.createProduct(name, description, spaceId.toString()) }
        val updatedProduct = sut.updateProduct(createdProduct.id, "new name", "new description", secondSpaceId.toString())

        updatedProduct shouldBe Product(
            createdProduct.id,
            "new name",
            "new description",
            emptyMap(),
            secondSpaceId.toString(),
            createdProduct.creationTime
        )
    }

    @Test
    fun `update Product should be optional for each attribute`() = testApplication {
        val product = Product(
            "any id",
            "name",
            "description",
            emptyMap(),
            spaceId.toString(),
            LocalDateTime.now()
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
    fun `update Product should throw IllegalArgumentException when id is invalid UUID`() = testApplication {
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
    fun `update Product should throw IllegalArgumentException when new space id is invalid UUID`() = testApplication {
        val invalidUUID = "Invalid UUID"
        val product = Product(
            "any id",
            "name",
            "description",
            emptyMap(),
            spaceId.toString(),
            LocalDateTime.now()
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
    fun `update Product should throw IllegalArgumentException when new SpaceUUID is unknown`() = testApplication {
        val product = Product(
            "any id",
            "name",
            "description",
            emptyMap(),
            spaceId.toString(),
            LocalDateTime.now()
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
    fun `delete Product should return deleted Product`() = testApplication {
        val product = Product(
            "any id",
            "name",
            "description",
            emptyMap(),
            spaceId.toString(),
            LocalDateTime.now()
        )
        val createdProduct = product.run { sut.createProduct(name, description, spaceId.toString()) }
        sut.deleteProduct(createdProduct.id) shouldBe createdProduct
        sut.getProduct(createdProduct.id) shouldBe null

    }

    @Test
    fun `delete Product should return null when Product not found`() = testApplication {
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
    fun `delete Product should delete all of its attributes`() = testApplication {
        val product = sut.createProduct("name", "description", spaceId.toString())
        val productAttributeRepository = PostgresProductAttributeRepository()
        product.run {
            productAttributeRepository.createOrUpdateAttribute("someKey", Attribute.NumberAttribute(1.2f), id)
            productAttributeRepository.createOrUpdateAttribute("someOtherKey", Attribute.StringAttribute("some text"), id)
            productAttributeRepository.createOrUpdateAttribute("someBooleanKey", Attribute.BooleanAttribute(true), id)
        }
        transaction { ProductAttributeEntity.all().count() } shouldBe 3
        sut.getProduct(product.id)!!.attributes.size shouldBe 3
        sut.deleteProduct(product.id)
        transaction { ProductAttributeEntity.all().count() } shouldBe 0
    }

    @Test
    fun `moveProduct should return Product with new Space`() = testApplication {
        val product = Product(
            "any id",
            "name",
            "description",
            emptyMap(),
            spaceId.toString(),
            LocalDateTime.now()
        )

        val secondSpaceId = UUID.randomUUID()
        transaction {
            SpaceEntity.new(id = secondSpaceId) {
                name = "second space name"
                description = "second space description"
                storage = exampleStorageEntity
            }.toSpace()
        }

        val createdProduct = product.run { sut.createProduct(name, description, spaceId.toString()) }
        val movedProduct = sut.moveProduct(createdProduct.id, secondSpaceId.toString())

        movedProduct shouldBe Product(
            createdProduct.id,
            createdProduct.name,
            createdProduct.description,
            emptyMap(),
            secondSpaceId.toString(),
            createdProduct.creationTime
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
            "description",
            emptyMap(),
            spaceId.toString(),
            LocalDateTime.now()
        )
        val createdProduct = product.run { sut.createProduct(name, description, spaceId.toString()) }
        runCatching {
            sut.moveProduct(createdProduct.id, UUID.randomUUID().toString())
        }.exceptionOrNull().run {
            this shouldNotBe null
            this!!::class shouldBe IllegalArgumentException::class
            this.message shouldBe "target Space not found"
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