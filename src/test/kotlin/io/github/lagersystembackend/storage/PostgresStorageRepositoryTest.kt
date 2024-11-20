package io.github.lagersystembackend.storage

import io.github.lagersystembackend.plugins.configureDatabases
import io.github.lagersystembackend.product.Products
import io.github.lagersystembackend.space.PostgresSpaceRepository
import io.github.lagersystembackend.space.Space
import io.github.lagersystembackend.space.Spaces
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
            SchemaUtils.create(Products, Spaces, Storages, StorageToStorages)
        }
    }

    @AfterTest
    fun tearDown() {
        transaction {
            SchemaUtils.drop(Products, Spaces, Storages, StorageToStorages)
        }
    }

    @Test
    fun `create Storage should return Storage`() = testApplication {
        val rootStorage = insertRootStorage()
        val expectedStorage =
            Storage("anyId", "Storage", "Storage description", emptyList(), rootStorage.id, emptyList())
        val createdStorage = expectedStorage.run { sut.createStorage(name, description, parentId) }

        createdStorage.apply {
            name shouldBe expectedStorage.name
            description shouldBe expectedStorage.description
            parentId shouldBe expectedStorage.parentId
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
            Storage("anyId", "root1", "Storage description", spaces = emptyList(), parentId = null, subStorages =  emptyList()),
            Storage("anyId", "root2", "Storage description", spaces = emptyList(), parentId = null, subStorages = emptyList()),
            Storage("anyId", "root3", "Storage description", spaces = emptyList(), parentId = null, subStorages = emptyList())
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
            Space("anyId", "Space1", 100f, "Space description", emptyList(), rootStorage.id),
            Space("anyId", "Space2", 200f, "Space description", emptyList(), rootStorage.id),
            Space("anyId", "Space3", 300f, "Space description", emptyList(), rootStorage.id)
        )
        val spaceRepository = PostgresSpaceRepository()
        val createdSpaces = spaces.map { it.run { spaceRepository.createSpace(name, size, description, storageId) } }

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
}