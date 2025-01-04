package io.github.lagersystembackend.search

import io.kotest.matchers.shouldBe
import io.ktor.server.testing.testApplication
import org.junit.Test

class ProductDefaultSearchUseCaseTest : BaseDefaultSearchUseCaseTest() {

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
        val product = createProduct(createdAt = exampleLocalDateTime)
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
        val product = createProduct(updatedAt = exampleLocalDateTime)
        sut.fullTextSearch(product.updatedAt.toString()).apply {
            this.size shouldBe 1
            this.first().apply {
                id shouldBe product.id.toString()
                updatedAt shouldBe product.updatedAt.toString()
            }
        }
    }
}