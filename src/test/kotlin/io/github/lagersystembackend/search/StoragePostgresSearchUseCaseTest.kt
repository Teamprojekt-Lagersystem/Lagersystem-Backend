package io.github.lagersystembackend.search

import io.github.lagersystembackend.storage.StorageEntity
import io.github.lagersystembackend.storage.toStorage
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Test
import java.time.LocalDateTime

class StoragePostgresSearchUseCaseTest : BasePostgresSearchUseCaseTest() {

    fun createStorage(
        name: String = "name",
        description: String = "description",
        createdAt: LocalDateTime = exampleLocalDateTime,
        updatedAt: LocalDateTime = exampleLocalDateTime
    ) = transaction {
        StorageEntity.new {
            this.name = name
            this.description = description
            this.createdAt = createdAt
            this.updatedAt = updatedAt
        }.toStorage()
    }

    @Test
    fun `search storage name`() = testApplication {
        val storage = createStorage(name = "some Name")
        sut.fullTextSearch(storage.name).apply {
            this.size shouldBe 1
            this.first().apply {
                id shouldBe storage.id.toString()
                name shouldBe storage.name
            }
        }
    }

    @Test
    fun `search storage description`() = testApplication {
        val storage = createStorage(description = "some Description")
        sut.fullTextSearch(storage.description).apply {
            this.size shouldBe 1
            this.first().apply {
                id shouldBe storage.id.toString()
                description shouldBe storage.description
            }
        }
    }

    @Test
    fun `search storage createdAt`() = testApplication {
        val storage = createStorage(createdAt = exampleLocalDateTime.plusDays(1))
        sut.fullTextSearch(storage.createdAt.toString()).apply {
            this.size shouldBe 1
            this.first().apply {
                id shouldBe storage.id.toString()
                createdAt shouldBe storage.createdAt.toString()
            }
        }
    }

    @Test
    fun `search storage updatedAt`() = testApplication {
        val storage = createStorage(updatedAt = exampleLocalDateTime.plusDays(1))
        sut.fullTextSearch(storage.updatedAt.toString()).apply {
            this.size shouldBe 1
            this.first().apply {
                id shouldBe storage.id.toString()
                updatedAt shouldBe storage.updatedAt.toString()
            }
        }
    }
}