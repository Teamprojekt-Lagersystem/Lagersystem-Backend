package io.github.lagersystembackend.search

import io.github.lagersystembackend.attribute.Attribute
import io.github.lagersystembackend.attribute.ProductAttributeEntity
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Test
import java.time.LocalDateTime


class CommonPostgresSearchUseCaseTest : BasePostgresSearchUseCaseTest() {

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
        val product = createProduct(createdAt = LocalDateTime.parse("2024-12-24T00:00:00"))
        sut.fullTextSearch("2024-12-24T00:00:00").apply {
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

    @Test
    fun `fullTextSearch should rank one name match higher than two description matches`() = testApplication {
        val product1 = createProduct(name = "product")
        val product2 = createProduct(description = "product description product")
        sut.fullTextSearch("product").apply {
            this.size shouldBe 2
            this.first().id shouldBe product1.id.toString()
            this.last().id shouldBe product2.id.toString()
            this.first().rank shouldBeGreaterThan this.last().rank
        }
    }

    @Test
    fun `fullTextSearch should discard dot but rank it lower `() = testApplication {
        val product1 = createProduct(name = "Product.name")
        val product2 = createProduct(name = "Product name")
        sut.fullTextSearch("Product.name").apply {
            this.size shouldBe 2
            this.first().id shouldBe product1.id.toString()
            this.last().id shouldBe product2.id.toString()
            this.first().rank shouldBeGreaterThan this.last().rank
        }
    }

    @Test
    fun `fullTextSearch should discard - but rank it lower `() = testApplication {
        val product1 = createProduct(name = "Product-name")
        val product2 = createProduct(name = "Product name")
        sut.fullTextSearch("Product-name").apply {
            this.size shouldBe 2
            this.first().id shouldBe product1.id.toString()
            this.last().id shouldBe product2.id.toString()
            this.first().rank shouldBeGreaterThan this.last().rank
        }
    }
    @Test
    fun `fullTextSearch should be able to search attributes by key`() = testApplication {
        val productEntity = createProductEntity(name = "Product-name")
            transaction {
            ProductAttributeEntity.new {
                key = "someKey"
                type = Attribute.NumberAttribute.TYPE
                value = 213.2.toString()
                product = productEntity
            }
        }
        sut.fullTextSearch("someKey").apply {
            this.size shouldBe 1
            this.first().id shouldBe productEntity.id.toString()
        }
    }

    @Test
    fun `fullTextSearch should be able to search attributes by type`() = testApplication {
        val productEntity = createProductEntity(name = "Product-name")
        transaction {
            ProductAttributeEntity.new {
                key = "someKey"
                type = Attribute.NumberAttribute.TYPE
                value = 213.2.toString()
                product = productEntity
            }
        }
        sut.fullTextSearch(Attribute.NumberAttribute.TYPE).apply {
            this.size shouldBe 1
            this.first().id shouldBe productEntity.id.toString()
        }
    }

    @Test
    fun `fullTextSearch should be able to search attributes by number`() = testApplication {
        val productEntity = createProductEntity(name = "Product-name")
        transaction {
            ProductAttributeEntity.new {
                key = "someKey"
                type = Attribute.NumberAttribute.TYPE
                value = 213.2.toString()
                product = productEntity
            }
        }
        sut.fullTextSearch(213.2.toString()).apply {
            this.size shouldBe 1
            this.first().id shouldBe productEntity.id.toString()
        }
    }

    @Test
    fun `fullTextSearch should be able to search attributes by boolean`() = testApplication {
        val productEntity = createProductEntity(name = "Product-name")
        transaction {
            ProductAttributeEntity.new {
                key = "someKey"
                type = Attribute.BooleanAttribute.TYPE
                value = false.toString()
                product = productEntity
            }
        }
        sut.fullTextSearch(false.toString()).apply {
            this.size shouldBe 1
            this.first().id shouldBe productEntity.id.toString()
        }
    }


    @Test
    fun `fullTextSearch should be able to search attributes by String`() = testApplication {
        val productEntity = createProductEntity(name = "Product-name")
       transaction {
            ProductAttributeEntity.new {
                key = "someKey"
                type = Attribute.StringAttribute.TYPE
                value = "someString"
                product = productEntity
            }
        }
        sut.fullTextSearch("someString").apply {
            this.size shouldBe 1
            this.first().id shouldBe productEntity.id.toString()
        }
    }
}
