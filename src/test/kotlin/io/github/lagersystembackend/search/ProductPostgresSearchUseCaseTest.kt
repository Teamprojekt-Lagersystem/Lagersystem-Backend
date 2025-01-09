package io.github.lagersystembackend.search

import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.testApplication
import org.junit.Test
import java.time.LocalDateTime

class ProductPostgresSearchUseCaseTest : BasePostgresSearchUseCaseTest() {

    @Test
    fun `search product name`() = testApplication {
        val product = createProduct(name = "some Name")
        sut.fullTextSearch(product.name).apply {
            this.size shouldBe 1
            this.first().apply {
                id shouldBe product.id.toString()
                name shouldBe product.name
            }
        }
    }

    @Test
    fun `search product description`() = testApplication {
        val product = createProduct(description = "some Description")
        sut.fullTextSearch(product.description).apply {
            this.size shouldBe 1
            this.first().apply {
                id shouldBe product.id.toString()
                description shouldBe product.description
            }
        }
    }

    @Test
    fun `search product createdAt`() = testApplication {
        val product = createProduct(createdAt = exampleLocalDateTime.plusDays(1))
        sut.fullTextSearch(product.createdAt.toString()).apply {
            this.size shouldBe 1
            this.first().apply {
                id shouldBe product.id.toString()
                createdAt shouldBe product.createdAt.toString()
            }
        }
    }

    @Test
    fun `search product updatedAt`() = testApplication {
        val product = createProduct(updatedAt = exampleLocalDateTime.plusDays(1))
        sut.fullTextSearch(product.updatedAt.toString()).apply {
            this.size shouldBe 1
            this.first().apply {
                id shouldBe product.id.toString()
                updatedAt shouldBe product.updatedAt.toString()
            }
        }
    }


    @Test
    fun `fullTextSearch should rank products name - description - dates`() = testApplication {
        val product1 = createProduct(description = "2024") // should be ranked second
        val product2 = createProduct(name = "2024") // should be ranked first
        val product3 = createProduct(createdAt = LocalDateTime.parse("2024-12-24T00:00:00")) // should be ranked third
        sut.fullTextSearch("2024").apply {
            this.size shouldBe 3
            this[0].id shouldBe product2.id.toString()
            this[1].id shouldBe product1.id.toString()
            this[2].id shouldBe product3.id.toString()
            this[0].rank shouldBeGreaterThan this[1].rank
            this[1].rank shouldBeGreaterThan this[2].rank
        }
    }
}