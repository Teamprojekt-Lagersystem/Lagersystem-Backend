package io.github.lagersystembackend.search

import io.kotest.matchers.shouldBe
import io.ktor.server.testing.testApplication
import org.junit.Test
import java.time.LocalDateTime


open class CommonDefaultSearchUseCaseTest : BaseDefaultSearchUseCaseTest() {

    @Test
    fun `fullTextSearch should be able to search text`() = testApplication {
        val product = createProduct(name = "Some Product name jump")
        sut.fullTextSearch("jumping").apply {
            this.size shouldBe 1
            this.first().apply {
                id shouldBe product.id.toString()
                name shouldBe product.name
            }
        }
    }

    @Test
    fun `fullTextSearch should be able to search dates in ISO_DATE_TIME format`() = testApplication {
        val product = createProduct(createdAt = LocalDateTime.parse(exampleLocalDateTime.toString()))
        sut.fullTextSearch(exampleLocalDateTime.toString()).apply {
            this.size shouldBe 1
            this.first().apply {
                id shouldBe product.id.toString()
                createdAt shouldBe product.createdAt.toString()
            }
        }
    }

    @Test
    fun `fullTextSearch should be able to search part of dates in ISO_DATE_TIME format`() = testApplication {
        val product = createProduct(createdAt = LocalDateTime.parse("2024-12-24T00:00:00"))
        sut.fullTextSearch("12-24").apply {
            this.size shouldBe 1
            this.first().apply {
                id shouldBe product.id.toString()
                createdAt shouldBe product.createdAt.toString()
            }
        }
    }

    @Test
    fun `fullTextSearch should be able to search part of dates in german date format`() = testApplication {
        val product = createProduct(createdAt = LocalDateTime.parse("2024-12-24T00:00:00"))
        sut.fullTextSearch("24.12.2024").apply {
            this.size shouldBe 1
            this.first().apply {
                id shouldBe product.id.toString()
                createdAt shouldBe product.createdAt.toString()
            }
        }
    }

    @Test
    fun `fullTextSearch should be able to search part of dates in american date format`() = testApplication {
        val product = createProduct(createdAt = LocalDateTime.parse("2024-12-24T00:00:00"))
        sut.fullTextSearch("12.24.2024").apply {
            this.size shouldBe 1
            this.first().apply {
                id shouldBe product.id.toString()
                createdAt shouldBe product.createdAt.toString()
            }
        }
    }
}
