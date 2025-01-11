package io.github.lagersystembackend.search

import io.github.lagersystembackend.space.SpaceEntity
import io.github.lagersystembackend.space.toSpace
import io.github.lagersystembackend.storage.StorageEntity
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Test
import java.time.LocalDateTime

class SpacePostgresSearchUseCaseTest : BasePostgresSearchUseCaseTest() {

    fun createSpace(
        name: String = "name",
        description: String = "description",
        unit: String? = null,
        totalSize: Double? = null,
        currentSize: Double? = null,
        storage: StorageEntity = exampleStorageEntity,
        createdAt: LocalDateTime = exampleLocalDateTime,
        updatedAt: LocalDateTime = exampleLocalDateTime
    ) = transaction {
        SpaceEntity.new {
            this.name = name
            this.description = description
            this.unit = unit
            this.totalSize = totalSize
            this.currentSize = currentSize
            this.storage = storage
            this.createdAt = createdAt
            this.updatedAt = updatedAt
        }.toSpace()
    }

    @Test
    fun `search space name`() = testApplication {
        val space = createSpace(name = "some Name")
        sut.fullTextSearch(space.name).apply {
            this.size shouldBe 1
            this.first().apply {
                id shouldBe space.id.toString()
                name shouldBe space.name
            }
        }
    }

    @Test
    fun `search space description`() = testApplication {
        val space = createSpace(description = "some Description")
        sut.fullTextSearch(space.description).apply {
            this.size shouldBe 1
            this.first().apply {
                id shouldBe space.id.toString()
                description shouldBe space.description
            }
        }
    }

    @Test
    fun `fullTextSearch should search unit`() = testApplication {
        val space = createSpace(unit = "cm^2", totalSize = 100.0, currentSize = 50.0)
        sut.fullTextSearch("cm^2").apply {
            this.size shouldBe 1
            this.first().apply {
                id shouldBe space.id.toString()
                unit shouldBe space.unit
            }
        }
    }

    @Test
    fun `fullTextSearch should search totalSize`() = testApplication {
        val space = createSpace(unit = "cm^2", totalSize = 100.0, currentSize = 50.0)
        sut.fullTextSearch(100.0.toString()).apply {
            this.size shouldBe 1
            this.first().apply {
                id shouldBe space.id.toString()
                totalSize shouldBe space.totalSize
            }
        }
    }

    @Test
    fun `fullTextSearch should search currentSize`() = testApplication {
        val space = createSpace(unit = "cm^2", totalSize = 100.0, currentSize = 50.0)
        sut.fullTextSearch(50.0.toString()).apply {
            this.size shouldBe 1
            this.first().apply {
                id shouldBe space.id.toString()
                currentSize shouldBe space.currentSize
            }
        }
    }

    @Test
    fun `search space createdAt`() = testApplication {
        val space = createSpace(createdAt = exampleLocalDateTime.plusDays(1))
        sut.fullTextSearch(space.createdAt.toString()).apply {
            this.size shouldBe 1
            this.first().apply {
                id shouldBe space.id.toString()
                createdAt shouldBe space.createdAt.toString()
            }
        }
    }

    @Test
    fun `search space updatedAt`() = testApplication {
        val space = createSpace(updatedAt = exampleLocalDateTime.plusDays(1))
        sut.fullTextSearch(space.updatedAt.toString()).apply {
            this.size shouldBe 1
            this.first().apply {
                id shouldBe space.id.toString()
                updatedAt shouldBe space.updatedAt.toString()
            }
        }
    }
}