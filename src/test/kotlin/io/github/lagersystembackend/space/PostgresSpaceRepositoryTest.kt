package io.github.lagersystembackend.space

import io.github.lagersystembackend.attribute.ProductAttributes
import io.github.lagersystembackend.plugins.configureDatabases
import io.github.lagersystembackend.product.PostgresProductRepository
import io.github.lagersystembackend.product.Product
import io.github.lagersystembackend.product.Products
import io.github.lagersystembackend.storage.StorageEntity
import io.github.lagersystembackend.storage.StorageToStorages
import io.github.lagersystembackend.storage.Storages
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test



class PostgresSpaceRepositoryTest {
    val sut = PostgresSpaceRepository()
    val exampleStorageId = UUID.randomUUID()
    val targetStorageId = UUID.randomUUID()
    lateinit var exampleStorageEntity: StorageEntity
    lateinit var targetStorageEntity: StorageEntity

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
            SchemaUtils.create(Storages, StorageToStorages, Spaces, Products, ProductAttributes)
            exampleStorageEntity = StorageEntity.new(id = exampleStorageId) {
                name = "storage name"
                description = "storage description"
            }
            targetStorageEntity = StorageEntity.new(id = targetStorageId) {
                name = "storage name2"
                description = "storage description2"
            }
        }
    }

    @AfterTest
    fun tearDown() {
        transaction {
            SchemaUtils.drop(Storages, StorageToStorages, Spaces, Products, ProductAttributes)
        }
    }

    @Test
    fun `create Space should return Space`() = testApplication {
        val expectedSpace =
            Space("anyId", "Space", 100f,"Space description", emptyList(), exampleStorageId.toString(), LocalDateTime.now())
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
        val createTime = LocalDateTime.now()
        val expectedSpaces = listOf(
            Space("anyId", "Space1", 100f,"Space description", products = emptyList(), storageId = exampleStorageId.toString(), createTime),
            Space("anyId", "Space2", 100f,"Space description", products = emptyList(), storageId = exampleStorageId.toString(), createTime),
            Space("anyId", "Space3", 100f,"Space description", products = emptyList(), storageId = exampleStorageId.toString(), createTime)
        )
        val createdSpaces = expectedSpaces.map { it.run { sut.createSpace(name, size, description, storageId) } }
        sut.getSpaces() shouldBe createdSpaces
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
            Product("anyId", "Product1", "Space description", emptyMap(), createdSpace.id, LocalDateTime.now()),
            Product("anyId", "Product2", "Space description", emptyMap(), createdSpace.id, LocalDateTime.now()),
            Product("anyId", "Product3", "Space description", emptyMap(), createdSpace.id, LocalDateTime.now())
        )
        val productRepository = PostgresProductRepository()
        val createdProducts = products.map { it.run { productRepository.createProduct(name, description, spaceId) } }

        sut.getSpace(createdSpace.id)!!.products shouldBe createdProducts
        sut.deleteSpace(createdSpace.id)
        productRepository.getProducts() shouldBe emptyList()
    }

    @Test
    fun `spaceExists should return false when Storage not found`() = testApplication {
        sut.spaceExists(UUID.randomUUID().toString()) shouldBe false
    }

    @Test
    fun `moveSpace should move space to new storage`() = testApplication {
        val createdSpace = insertSpace()
        val movedSpace = sut.moveSpace(createdSpace.id, targetStorageId.toString())

        movedSpace.storageId shouldBe targetStorageId.toString()
    }

    @Test
    fun `moveSpace should throw IllegalArgumentException when space not found`() = testApplication {
        val invalidSpaceId = UUID.randomUUID().toString()
        runCatching { sut.moveSpace(invalidSpaceId, targetStorageId.toString()) }
            .exceptionOrNull().run {
                this shouldNotBe null
                this!!::class shouldBe IllegalArgumentException::class
                this.message shouldBe "Space with ID $invalidSpaceId not found"
            }
    }

    @Test
    fun `moveSpace should throw IllegalArgumentException when target storage not found`() = testApplication {
        val invalidStorageId = UUID.randomUUID().toString()
        val createdSpace = insertSpace()
        runCatching { sut.moveSpace(createdSpace.id, invalidStorageId) }
            .exceptionOrNull().run {
                this shouldNotBe null
                this!!::class shouldBe IllegalArgumentException::class
                this.message shouldBe "Storage with ID $invalidStorageId not found"
            }
    }

    @Test
    fun `moveSpace should return space with updated storage after successful move`() = testApplication {
        val createdSpace = insertSpace()
        val movedSpace = sut.moveSpace(createdSpace.id, targetStorageId.toString())

        movedSpace.storageId shouldBe targetStorageId.toString()
    }

    @Test
    fun `moveSpace should keep other properties of the space intact after move`() = testApplication {
        val createdSpace = insertSpace()
        val movedSpace = sut.moveSpace(createdSpace.id, targetStorageId.toString())

        movedSpace.name shouldBe createdSpace.name
        movedSpace.description shouldBe createdSpace.description
        movedSpace.size shouldBe createdSpace.size
    }

    @Test
    fun `moveSpace should keep products after move`() = testApplication {
        val createdSpace = insertSpace()
        val products = listOf(
            Product("anyId", "Product1", "Space description", emptyMap(), createdSpace.id, LocalDateTime.now()),
            Product("anyId", "Product2", "Space description", emptyMap(), createdSpace.id, LocalDateTime.now()),
            Product("anyId", "Product3", "Space description", emptyMap(), createdSpace.id, LocalDateTime.now())
        )
        val productRepository = PostgresProductRepository()
        val createdProducts = products.map { it.run { productRepository.createProduct(name, description, createdSpace.id) } }

        sut.getSpace(createdSpace.id)!!.products shouldBe createdProducts
        val movedSpace = sut.moveSpace(createdSpace.id, targetStorageId.toString())
        movedSpace.products shouldBe createdProducts
    }
}