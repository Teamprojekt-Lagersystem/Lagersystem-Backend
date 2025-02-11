package io.github.lagersystembackend.storage

import io.github.lagersystembackend.attribute.ProductAttributes
import io.github.lagersystembackend.plugins.configureDatabases
import io.github.lagersystembackend.product.ProductEntity
import io.github.lagersystembackend.stored_product.PostgresStoredProductRepository
import io.github.lagersystembackend.stored_product.StoredProducts
import io.github.lagersystembackend.space.ProductInSpace
import io.github.lagersystembackend.space.PostgresSpaceRepository
import io.github.lagersystembackend.space.Space
import io.github.lagersystembackend.space.SpaceEntity
import io.github.lagersystembackend.space.Spaces
import io.kotest.matchers.collections.shouldContain
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

fun insertRootStorage() = transaction {
    StorageEntity.new {
        name = "Root"
        description = "Root storage"
    }.toStorage()
}

class PostgresStorageRepositoryTest {
    val sut = PostgresStorageRepository()


    @BeforeTest
    fun setUp() {
        configureDatabases(isTest = true)
        transaction {
            SchemaUtils.create(Storages, StorageToStorages, Spaces, StoredProducts, ProductAttributes)
        }
    }

    @AfterTest
    fun tearDown() {
        transaction {
            SchemaUtils.drop(Storages, StorageToStorages, Spaces, StoredProducts, ProductAttributes)
        }
    }

    @Test
    fun `create Storage should return Storage`() = testApplication {
        val rootStorage = insertRootStorage()
        val expectedStorage =
            Storage("anyId", "Storage", "Storage description", emptyList(), rootStorage.id, emptyList(), LocalDateTime.now(), LocalDateTime.now())
        val createdStorage = expectedStorage.run { sut.createStorage(name, description, parentId) }

        createdStorage.apply {
            name shouldBe expectedStorage.name
            description shouldBe expectedStorage.description
            parentId shouldBe expectedStorage.parentId
            createdAt shouldBeBefore LocalDateTime.now()
            updatedAt shouldBe null
        }
        sut.getStorage(rootStorage.id)!!.subStorages shouldContain createdStorage
    }

    @Test
    fun `create Storage should throw IllegalArgumentException when parentUUID is invalid UUID`() = testApplication {
        val invalidUUID = "invalidUUID"
        runCatching { sut.createStorage("storage", "description", invalidUUID) }
            .exceptionOrNull().run {
                this shouldNotBe null
                this!!::class shouldBe IllegalArgumentException::class
                this.message shouldBe "Invalid UUID string: $invalidUUID"
            }
    }

    @Test
    fun `get Storage should return Storage`() = testApplication {
        val rootStorage = insertRootStorage()
        sut.getStorage(rootStorage.id) shouldBe rootStorage
    }

    @Test
    fun `get Storage should return null when Storage not found`() = testApplication {
        sut.getStorage(UUID.randomUUID().toString()) shouldBe null
    }

    @Test
    fun `get Storage should throw IllegalArgumentException when id is invalid UUID`() = testApplication {
        val invalidUUID = "invalidUUID"
        runCatching { sut.getStorage(invalidUUID) }
            .exceptionOrNull().run {
                this shouldNotBe null
                this!!::class shouldBe IllegalArgumentException::class
                this.message shouldBe "Invalid UUID string: $invalidUUID"
            }
    }

    @Test
    fun `get Storages should return List of Storages`() = testApplication {
        val expectedStorages = listOf(
            Storage("anyId", "root1", "Storage description", spaces = emptyList(), parentId = null, subStorages =  emptyList(), createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()),
            Storage("anyId", "root2", "Storage description", spaces = emptyList(), parentId = null, subStorages = emptyList(), createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()),
            Storage("anyId", "root3", "Storage description", spaces = emptyList(), parentId = null, subStorages = emptyList(), createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())
        )
        val createdStorages = expectedStorages.map { it.run { sut.createStorage(name, description, parentId) } }
        sut.getStorages() shouldBe createdStorages
    }

    @Test
    fun `update Storage should return updated Storage`() = testApplication {
        val rootStorage = insertRootStorage()
        val updatedStorage = sut.updateStorage(rootStorage.id, name = "newName")
        sut.getStorage(rootStorage.id)!!.apply {
            this shouldBe updatedStorage
            name shouldBe "newName"
            description shouldBe rootStorage.description
            rootStorage.createdAt shouldBeBefore updatedStorage?.updatedAt!!
        }
    }

    @Test
    fun `update Storage should update name`() = testApplication {
        val rootStorage = insertRootStorage()
        sut.updateStorage(rootStorage.id, name = "newName")
        sut.getStorage(rootStorage.id)!!.apply {
            name shouldBe "newName"
            description shouldBe rootStorage.description
        }
    }

    @Test
    fun `update Storage should update description`() = testApplication {
        val rootStorage = insertRootStorage()
        sut.updateStorage(rootStorage.id, description = "newDescription")
        sut.getStorage(rootStorage.id)!!.apply {
            name shouldBe rootStorage.name
            description shouldBe "newDescription"
        }
    }

    @Test
    fun `update Storage should return null when Storage not found`() = testApplication {
        sut.updateStorage(UUID.randomUUID().toString(), name = "newName") shouldBe null
    }

    @Test
    fun `update Storage should throw IllegalArgumentException when id is invalid UUID`() = testApplication {
        val invalidUUID = "invalidUUID"
        runCatching { sut.updateStorage(invalidUUID, name = "newName") }.exceptionOrNull().run {
            this shouldNotBe null
            this!!::class shouldBe IllegalArgumentException::class
            this.message shouldBe "Invalid UUID string: $invalidUUID"
        }
    }

    @Test
    fun `delete Storage should null when Storage not found`() = testApplication {
        sut.deleteStorage(UUID.randomUUID().toString()) shouldBe null
    }

    @Test
    fun `delete Storage should delete subStorages`() = testApplication {
        val rootStorage = insertRootStorage()
        val storage1 = sut.createStorage("subStorage1", "Storage description", rootStorage.id)
        val storage2 = sut.createStorage("subStorage2", "Storage description", rootStorage.id)
        val storage3 = sut.createStorage("subStorage3", "Storage description", rootStorage.id)

        sut.getStorage(rootStorage.id)!!.subStorages shouldBe listOf(storage1, storage2, storage3)
        sut.deleteStorage(rootStorage.id)
        sut.getStorages() shouldBe emptyList()
    }

    @Test
    fun `delete Storage should delete spaces`() = testApplication {
        val rootStorage = insertRootStorage()
        val spaces = listOf(
            Space("anyId", "Space1", null, null, null, "Space description", emptyList(), rootStorage.id, LocalDateTime.now(), LocalDateTime.now()),
            Space("anyId", "Space2", null, null, null, "Space description", emptyList(), rootStorage.id, LocalDateTime.now(), LocalDateTime.now()),
            Space("anyId", "Space3", null, null, null, "Space description", emptyList(), rootStorage.id, LocalDateTime.now(), LocalDateTime.now())
        )
        val spaceRepository = PostgresSpaceRepository()
        val createdSpaces = spaces.map { it.run { spaceRepository.createSpace(name, description, null, null, storageId) } }

        sut.getStorage(rootStorage.id)!!.spaces shouldBe createdSpaces
        sut.deleteStorage(rootStorage.id)
        spaceRepository.getSpaces() shouldBe emptyList()
    }

    @Test
    fun `storageExists should return true when Storage is found`() = testApplication {
        val rootStorage = insertRootStorage()
        sut.storageExists(rootStorage.id) shouldBe true
    }

    @Test
    fun `storageExists should return false when Storage not found`() = testApplication {
        sut.storageExists(UUID.randomUUID().toString()) shouldBe false
    }

    @Test
    fun `move Storage should update parentId and return updated Storage`() = testApplication {
            val rootStorage = insertRootStorage()
            val subStorage = sut.createStorage("SubStorage", "A sub-storage", rootStorage.id)
            val newParentStorage = sut.createStorage("NewParentStorage", "Another storage", parentId = null)

            val movedStorage = sut.moveStorage(subStorage.id, newParentStorage.id)

            movedStorage.parentId shouldBe newParentStorage.id
            sut.getStorage(movedStorage.id)!!.apply {
                parentId shouldBe newParentStorage.id
            }

            val originalParent = sut.getStorage(rootStorage.id)!!
            originalParent.subStorages.none { it.id == subStorage.id } shouldBe true

            val newParent = sut.getStorage(newParentStorage.id)!!
            newParent.subStorages.any { it.id == movedStorage.id } shouldBe true
    }

    @Test
    fun `move Storage should update updatedAt timestamp`() = testApplication {
        val rootStorage = insertRootStorage()
        val subStorage = sut.createStorage("SubStorage", "A sub-storage", rootStorage.id)
        val newParentStorage = sut.createStorage("NewParentStorage", "Another storage", parentId = null)

        val movedStorage = sut.moveStorage(subStorage.id, newParentStorage.id)

        rootStorage.createdAt shouldBeBefore movedStorage.updatedAt!!
    }

    @Test
    fun `move Storage should handle null parent correctly`() = testApplication {
        val rootStorage = insertRootStorage()
        val subStorage = sut.createStorage("SubStorage", "A sub-storage", rootStorage.id)

        val movedStorage = sut.moveStorage(subStorage.id, null)

        movedStorage.parentId shouldBe null
        sut.getStorage(movedStorage.id)!!.apply {
            parentId shouldBe null
        }

        val originalParent = sut.getStorage(rootStorage.id)!!
        originalParent.subStorages.none { it.id == movedStorage.id } shouldBe true
    }


    @Test
    fun `move Storage should throw IllegalArgumentException when storage is not found`() = testApplication {
        val newParentStorage = sut.createStorage("NewParentStorage", "Another storage", parentId = null)

        val invalidStorageId = UUID.randomUUID().toString()
        runCatching { sut.moveStorage(invalidStorageId, newParentStorage.id) }.exceptionOrNull().run {
            this shouldNotBe null
            this!!::class shouldBe IllegalArgumentException::class
            this.message shouldBe "Storage with ID $invalidStorageId not found"
        }
    }

    @Test
    fun `move Storage should throw IllegalArgumentException when new parent is not found`() = testApplication {
        val rootStorage = insertRootStorage()
        val subStorage = sut.createStorage("SubStorage", "A sub-storage", rootStorage.id)

        val invalidParentId = UUID.randomUUID().toString()
        runCatching { sut.moveStorage(subStorage.id, invalidParentId) }.exceptionOrNull().run {
            this shouldNotBe null
            this!!::class shouldBe IllegalArgumentException::class
            this.message shouldBe "Storage with ID $invalidParentId not found"
        }
    }

    @Test
    fun `isCircularReference should return true for circular references`() = testApplication {
        val rootStorage = insertRootStorage()
        val subStorage1 = sut.createStorage("SubStorage1", "First sub-storage", rootStorage.id)
        val subStorage2 = sut.createStorage("SubStorage2", "Second sub-storage", subStorage1.id)

        sut.isCircularReference(rootStorage.id, subStorage2.id) shouldBe true
    }

    @Test
    fun `isCircularReference should return false when no circular references`() = testApplication {
        val rootStorage = insertRootStorage()
        val rootStorage2 = insertRootStorage()
        sut.isCircularReference(rootStorage.id, rootStorage2.id) shouldBe false
    }

    @Test
    fun `moveStorage should correctly adjust hierarchy when circular reference is detected`() = testApplication {
        val storageA = sut.createStorage("A", "Storage A", null)
        val storageB = sut.createStorage("B", "Storage B", storageA.id)
        val storageC = sut.createStorage("C", "Storage C", storageB.id)

        sut.moveStorage(storageA.id, storageC.id)

        val updatedStorageB = sut.getStorage(storageB.id)!!
        updatedStorageB.parentId shouldBe null
        val updatedStorageC = sut.getStorage(storageC.id)!!
        updatedStorageC.parentId shouldBe storageB.id
        val updatedStorageA = sut.getStorage(storageA.id)!!
        updatedStorageA.parentId shouldBe storageC.id

        val storageD = sut.createStorage("D", "Storage D", null)
        val storageE = sut.createStorage("E", "Storage E", storageD.id)
        sut.moveStorage(storageB.id, storageE.id)

        val finalStorageB = sut.getStorage(storageB.id)!!
        finalStorageB.parentId shouldBe storageE.id
        val finalStorageC = sut.getStorage(storageC.id)!!
        finalStorageC.parentId shouldBe storageB.id
        val finalStorageA = sut.getStorage(storageA.id)!!
        finalStorageA.parentId shouldBe storageC.id
        val finalStorageE = sut.getStorage(storageE.id)!!
        finalStorageE.parentId shouldBe storageD.id
    }
    @Test
    fun `copyStorage should correctly duplicate storage structure including spaces and products`() = testApplication {
        transaction {
            val productEntity = ProductEntity.new {
                name = "product name"
                description = "product description"
            }
            val rootStorage = insertRootStorage()
            val subStorage = sut.createStorage("SubStorage", "A sub-storage", rootStorage.id)
            val spaceRepository = PostgresSpaceRepository()
            val space = spaceRepository.createSpace("Space", "A space", null, null, subStorage.id)
            val productRepository = PostgresStoredProductRepository()
            val product =
                productRepository.createStoredProduct(productEntity.id.value.toString(), space.id, 1)

            val copiedStorage = sut.copyStorage(rootStorage.id, null)

            copiedStorage.name shouldBe rootStorage.name
            copiedStorage.description shouldBe rootStorage.description
            copiedStorage.parentId shouldBe null
            copiedStorage.subStorages.size shouldBe 1

            val copiedSubStorage = copiedStorage.subStorages.first()
            copiedSubStorage.name shouldBe subStorage.name
            copiedSubStorage.description shouldBe subStorage.description
            copiedSubStorage.parentId shouldBe copiedStorage.id

            copiedSubStorage.spaces.size shouldBe 1
            val copiedSpace = copiedSubStorage.spaces.first()
            copiedSpace.name shouldBe space.name
            copiedSpace.totalSize shouldBe space.totalSize
            copiedSpace.currentSize shouldBe space.currentSize
            copiedSpace.unit shouldBe space.unit
            copiedSpace.description shouldBe space.description
            copiedSpace.storageId shouldBe copiedSubStorage.id

            copiedSpace.storedProducts.size shouldBe 1
            val copiedProduct = copiedSpace.storedProducts.first()
            copiedProduct.name shouldBe product.productName
            copiedProduct.description shouldBe product.productDescription
            copiedProduct.attributes shouldBe emptyMap()
        }
    }


    @Test
    fun `copyStorage should throw IllegalArgumentException when original storage not found`() = testApplication {
        val invalidId = UUID.randomUUID().toString()

        runCatching { sut.copyStorage(invalidId, null) }.exceptionOrNull().run {
            this shouldNotBe null
            this!!::class shouldBe IllegalArgumentException::class
            this.message shouldBe "Storage with ID $invalidId not found"
        }
    }
    @Test
    fun `copyStorage should throw IllegalArgumentException when parent storage not found`() = testApplication {
        val rootStorage = insertRootStorage()
        val invalidParentId = UUID.randomUUID().toString()

        runCatching { sut.copyStorage(rootStorage.id, invalidParentId) }.exceptionOrNull().run {
            this shouldNotBe null
            this!!::class shouldBe IllegalArgumentException::class
            this.message shouldBe "Parent storage with ID $invalidParentId not found"
        }
    }
}