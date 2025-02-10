package io.github.lagersystembackend.space

import io.github.lagersystembackend.attribute.ProductAttributes
import io.github.lagersystembackend.plugins.configureDatabases
import io.github.lagersystembackend.stored_product.PostgresStoredProductRepository
import io.github.lagersystembackend.stored_product.StoredProduct
import io.github.lagersystembackend.stored_product.StoredProducts
import io.github.lagersystembackend.storage.StorageEntity
import io.github.lagersystembackend.storage.StorageToStorages
import io.github.lagersystembackend.storage.Storages
import io.kotest.matchers.date.shouldBeBefore
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
            storage = exampleStorageEntity
        }.toSpace()
    }

    @BeforeTest
    fun setUp() {
        configureDatabases(isTest = true)
        transaction {
            SchemaUtils.create(Storages, StorageToStorages, Spaces, StoredProducts, ProductAttributes)
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
            SchemaUtils.drop(Storages, StorageToStorages, Spaces, StoredProducts, ProductAttributes)
        }
    }

    @Test
    fun `create Space should return Space`() = testApplication {
        val expectedSpace =
            Space("anyId", "Space", null, null, null,"Space description", emptyList(), exampleStorageId.toString(), LocalDateTime.now(), LocalDateTime.now())
        val createdStorage = expectedSpace.run { sut.createSpace(name, description, totalSize, unit, storageId) }

        createdStorage.apply {
            name shouldBe expectedSpace.name
            totalSize shouldBe expectedSpace.totalSize
            currentSize shouldBe expectedSpace.currentSize
            unit shouldBe expectedSpace.unit
            description shouldBe expectedSpace.description
            storageId shouldBe expectedSpace.storageId
            createdAt shouldBeBefore LocalDateTime.now()
            updatedAt shouldBe null
        }
    }

    @Test
    fun `create Space should throw IllegalArgumentException when storageUUID is invalid UUID`() = testApplication {
        val invalidUUID = "invalidUUID"
        runCatching { sut.createSpace("space","description", null, null, invalidUUID) }
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
            Space("anyId", "Space1", null, null, null,"Space description", storedProducts = emptyList(), storageId = exampleStorageId.toString(), createTime, createTime),
            Space("anyId", "Space2", null, null, null,"Space description", storedProducts = emptyList(), storageId = exampleStorageId.toString(), createTime, createTime),
            Space("anyId", "Space3", null, null, null,"Space description", storedProducts = emptyList(), storageId = exampleStorageId.toString(), createTime, createTime),
        )
        val createdSpaces = expectedSpaces.map { it.run { sut.createSpace(name, description, totalSize, unit, storageId) } }
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
            createdSpace.createdAt shouldBeBefore updatedSpace?.updatedAt!!
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
        sut.updateSpace(createdSpace.id, null, description = "newDescription", null)
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
            StoredProduct("anyId", "Product1", createdSpace.id, 1, LocalDateTime.now(), LocalDateTime.now()),
            StoredProduct("anyId", "Product2", createdSpace.id, 1, LocalDateTime.now(), LocalDateTime.now()),
            StoredProduct("anyId", "Product3", createdSpace.id, 1, LocalDateTime.now(), LocalDateTime.now())
        )
        val productRepository = PostgresStoredProductRepository()
        val createdProducts = products.map { it.run { productRepository.createStoredProduct("name", spaceId, 1) } }

        sut.getSpace(createdSpace.id)!!.storedProducts shouldBe createdProducts
        sut.deleteSpace(createdSpace.id)
        productRepository.getStoredProducts() shouldBe emptyList()
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
    fun `moveSpace should update updatedAt timestamp`() = testApplication {
        val createdSpace = insertSpace().copy(updatedAt = LocalDateTime.now())
        val movedSpace = sut.moveSpace(createdSpace.id, targetStorageId.toString())

        createdSpace.createdAt shouldBeBefore movedSpace.updatedAt!!
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
        movedSpace.totalSize shouldBe createdSpace.totalSize
        movedSpace.currentSize shouldBe createdSpace.totalSize
        movedSpace.unit shouldBe createdSpace.unit
    }

    @Test
    fun `moveSpace should keep products after move`() = testApplication {
        val createdSpace = insertSpace()
        val products = listOf(
            StoredProduct("anyId", "Product1", createdSpace.id, 1, LocalDateTime.now(), LocalDateTime.now()),
            StoredProduct("anyId", "Product2", createdSpace.id, 1, LocalDateTime.now(), LocalDateTime.now()),
            StoredProduct("anyId", "Product3", createdSpace.id, 1, LocalDateTime.now(), LocalDateTime.now())
        )
        val productRepository = PostgresStoredProductRepository()
        val createdProducts = products.map { it.run { productRepository.createStoredProduct("name", spaceId, 1) } }

        sut.getSpace(createdSpace.id)!!.storedProducts shouldBe createdProducts
        val movedSpace = sut.moveSpace(createdSpace.id, targetStorageId.toString())
        movedSpace.storedProducts shouldBe createdProducts
    }

    @Test
    fun `copySpace should correctly duplicate space structure including products`() = testApplication {
        val storage = exampleStorageEntity
        val productRepository = PostgresStoredProductRepository()
        val space = insertSpace()
        val product = productRepository.createStoredProduct("Product", space.id, 1)

        val copiedSpace = sut.copySpace(space.id, exampleStorageId.toString())

        copiedSpace.name shouldBe space.name
        copiedSpace.totalSize shouldBe space.totalSize
        copiedSpace.currentSize shouldBe space.currentSize
        copiedSpace.unit shouldBe space.unit
        copiedSpace.description shouldBe space.description
        copiedSpace.storageId shouldBe storage.id.toString()

        copiedSpace.storedProducts.size shouldBe 1
        val copiedProduct = copiedSpace.storedProducts.first()
        copiedProduct.name shouldBe product.productName
        copiedProduct.description shouldBe product.productDescription
        copiedProduct.id shouldBe copiedSpace.id
        copiedProduct.attributes shouldBe emptyMap()
    }

    @Test
    fun `copySpace should throw IllegalArgumentException when original space not found`() = testApplication {
        val invalidSpaceId = UUID.randomUUID().toString()

        runCatching { sut.copySpace(invalidSpaceId, exampleStorageId.toString()) }.exceptionOrNull().run {
            this shouldNotBe null
            this!!::class shouldBe IllegalArgumentException::class
            this.message shouldBe "Space with ID $invalidSpaceId not found"
        }
    }
    @Test
    fun `copySpace should throw IllegalArgumentException when target storage not found`() = testApplication {
        val spaceRepository = PostgresSpaceRepository()
        val space = spaceRepository.createSpace("Space", "Original Space", null, null, exampleStorageId.toString())
        val invalidStorageId = UUID.randomUUID().toString()

        runCatching { sut.copySpace(space.id, invalidStorageId) }.exceptionOrNull().run {
            this shouldNotBe null
            this!!::class shouldBe IllegalArgumentException::class
            this.message shouldBe "Storage with ID $invalidStorageId not found"
        }
    }
}