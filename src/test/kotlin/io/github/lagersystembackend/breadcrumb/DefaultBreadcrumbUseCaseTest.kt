package io.github.lagersystembackend.breadcrumb

import io.github.lagersystembackend.space.ProductInSpace
import io.github.lagersystembackend.space.Space
import io.github.lagersystembackend.space.SpaceRepository
import io.github.lagersystembackend.storage.Storage
import io.github.lagersystembackend.storage.StorageRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class DefaultBreadcrumbUseCaseTest {
    val storageRepositoryMock = mockk<StorageRepository>()
    val spaceRepositoryMock = mockk<SpaceRepository>()

    val fakeDate = LocalDateTime.parse("2025-01-01T00:00:00")
    val sut = DefaultBreadcrumbUseCase(
        storageRepositoryMock,
        spaceRepositoryMock
    )

    @BeforeTest
    fun setUp() {
    }

    @AfterTest
    fun tearDown() {
    }

    fun createFakeStorage(
        id: String =  UUID.randomUUID().toString(),
        name: String = "storage name",
        description: String = "storage description",
        spaces: List<Space> = emptyList(),
        parentId: String? = null,
        subStorages: List<Storage> = emptyList(),
        createdAt: LocalDateTime = fakeDate,
        updatedAt: LocalDateTime? = null

    ) = Storage(id, name, description, spaces, parentId, subStorages, createdAt, updatedAt)

    fun createFakeSpace(
        id: String =  UUID.randomUUID().toString(),
        name: String = "space name",
        totalSize: Double? = null,
        currentSize: Double? = null,
        unit: String? = null,
        description: String = "space description",
        storedProducts: List<ProductInSpace> = emptyList(),
        storageId: String = UUID.randomUUID().toString(),
        createdAt: LocalDateTime = fakeDate,
        updatedAt: LocalDateTime? = null
    ) = Space(
        id = id,
        name = name,
        totalSize = totalSize,
        currentSize = currentSize,
        unit = unit,
        description = description,
        storedProducts = storedProducts,
        storageId = storageId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
    @Test
    fun `getBreadcrumb should find depots`() = testApplication {
        val depot = createFakeStorage(parentId = null)
        every { storageRepositoryMock.getStorage(depot.id) } returns depot

        sut.getBreadcrumb(depot.id).entries.apply {
            this.size shouldBe  1
            this.first() shouldBe Breadcrumb.BreadcrumbEntry(depot.id, depot.name, "storage")
        }
    }

    @Test
    fun `getBreadcrumb should find storages`() = testApplication {
        val depot = createFakeStorage(name = "depot", parentId = null)
        val storage = createFakeStorage(name = "storage", parentId = depot.id)
        every { storageRepositoryMock.getStorage(depot.id) } returns depot
        every { storageRepositoryMock.getStorage(storage.id) } returns storage

        sut.getBreadcrumb(storage.id).entries.apply {
            this.size shouldBe  2
            this.first() shouldBe Breadcrumb.BreadcrumbEntry(depot.id, depot.name, "storage")
            this.last() shouldBe Breadcrumb.BreadcrumbEntry(storage.id, storage.name, "storage")
        }
    }

    @Test
    fun `getBreadcrumb should find spaces`() = testApplication {
        val depot = createFakeStorage(name = "depot", parentId = null)
        val storage = createFakeStorage(name = "storage", parentId = depot.id)
        val space =  createFakeSpace(storageId = storage.id)
        every { storageRepositoryMock.getStorage(any()) } returns null
        every { storageRepositoryMock.getStorage(depot.id) } returns depot
        every { storageRepositoryMock.getStorage(storage.id) } returns storage
        every { spaceRepositoryMock.getSpace(space.id) } returns space

        sut.getBreadcrumb(space.id).entries.apply {
            this.size shouldBe  3
            this[0] shouldBe Breadcrumb.BreadcrumbEntry(depot.id, depot.name, "storage")
            this[1] shouldBe Breadcrumb.BreadcrumbEntry(storage.id, storage.name, "storage")
            this[2] shouldBe Breadcrumb.BreadcrumbEntry(space.id, space.name, "space")
        }
    }

    @Test
    fun `getBreadcrumb should throw IllegalArgumentException when Id not found`() = testApplication {
        every { storageRepositoryMock.getStorage(any()) } returns null
        every { spaceRepositoryMock.getSpace(any()) } returns null

        runCatching { sut.getBreadcrumb(UUID.randomUUID().toString()) }.exceptionOrNull().run {
            this shouldNotBe null
            this!!::class shouldBe IllegalArgumentException::class
            this.message shouldBe "ID not found"
        }
    }
}

