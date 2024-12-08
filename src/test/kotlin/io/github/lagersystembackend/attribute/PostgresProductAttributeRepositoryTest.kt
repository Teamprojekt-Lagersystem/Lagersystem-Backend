package io.github.lagersystembackend.attribute

import io.github.lagersystembackend.plugins.configureDatabases
import io.github.lagersystembackend.product.ProductEntity
import io.github.lagersystembackend.product.Products
import io.github.lagersystembackend.product.toProduct
import io.github.lagersystembackend.space.SpaceEntity
import io.github.lagersystembackend.space.Spaces
import io.github.lagersystembackend.storage.StorageEntity
import io.github.lagersystembackend.storage.StorageToStorages
import io.github.lagersystembackend.storage.Storages
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.server.testing.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class PostgresProductAttributeRepositoryTest {
    val sut = PostgresProductAttributeRepository()
    val storageId = UUID.randomUUID()
    val spaceId = UUID.randomUUID()
    val productId = UUID.randomUUID()

    @BeforeTest
    fun setUp() {
        configureDatabases(isTest = true)
        transaction {
            SchemaUtils.create(Storages, StorageToStorages, Spaces, ProductAttributes, Products)
            transaction {
                val storage = StorageEntity.new(id = storageId) {
                    name = "storage name"
                    description = "storage description"
                }
                val space = SpaceEntity.new(id = spaceId) {
                    name = "space name"
                    description = "space description"
                    this.storage = storage
                }
                ProductEntity.new(id = productId) {
                    name = "product name"
                    description = "product description"
                    this.space = space
                }
            }
        }
    }

    @AfterTest
    fun tearDown() {
        transaction {
            SchemaUtils.drop(ProductAttributes, Products, Spaces)
        }
    }

    @Test
    fun `create NumberAttribute should return NumberAttribute`() = testApplication {
        val expectedAttribute = Attribute.NumberAttribute(123f)
        val key = "someKey"

        sut.createOrUpdateAttribute(key, expectedAttribute, productId.toString()).apply {
            this shouldBe expectedAttribute
        }
        transaction {
            ProductEntity.findById(productId)!!.toProduct()
        }.attributes shouldContain (key to expectedAttribute)
    }


    @Test
    fun `create StringAttribute should return NumberAttribute`() = testApplication {
        val expectedAttribute = Attribute.StringAttribute("some value")
        val key = "someKey"

        sut.createOrUpdateAttribute(key, expectedAttribute, productId.toString()).apply {
            this shouldBe expectedAttribute
        }
        transaction {
            ProductEntity.findById(productId)!!.toProduct()
        }.attributes shouldContain (key to expectedAttribute)
    }

    @Test
    fun `create BooleanAttribute should return NumberAttribute`() = testApplication {
        val expectedAttribute = Attribute.BooleanAttribute(true)
        val key = "someKey"

        sut.createOrUpdateAttribute(key, expectedAttribute, productId.toString()).apply {
            this shouldBe expectedAttribute
        }
        transaction {
            ProductEntity.findById(productId)!!.toProduct()
        }.attributes shouldContain (key to expectedAttribute)
    }

    @Test
    fun `create String AttributeList should return String AttributeList`() = testApplication {
        val expectedAttribute = Attribute.ListAttribute.fromStings(listOf("some value", "some other value"))
        val key = "someKey"

        sut.createOrUpdateAttribute(key, expectedAttribute, productId.toString()).apply {
            this shouldBe expectedAttribute
        }
        transaction {
            ProductEntity.findById(productId)!!.toProduct()
        }.attributes shouldContain (key to expectedAttribute)
    }


    @Test
    fun `create Number AttributeList should return Number AttributeList`() = testApplication {
        val expectedAttribute = Attribute.ListAttribute.fromNumbers(listOf(1f, 2.2f, 3f, 4f))
        val key = "someKey"

        sut.createOrUpdateAttribute(key, expectedAttribute, productId.toString()).apply {
            this shouldBe expectedAttribute
        }
        transaction {
            ProductEntity.findById(productId)!!.toProduct()
        }.attributes shouldContain (key to expectedAttribute)
    }

    @Test
    fun `create Boolean AttributeList should return Boolean AttributeList`() = testApplication {
        val expectedAttribute = Attribute.ListAttribute.fromBooleans(listOf(true, false, false, true))
        val key = "someKey"

        sut.createOrUpdateAttribute(key, expectedAttribute, productId.toString()).apply {
            this shouldBe expectedAttribute
        }
        transaction {
            ProductEntity.findById(productId)!!.toProduct()
        }.attributes shouldContain (key to expectedAttribute)
    }

    @Test
    fun `create Attribute should throw IllegalArgumentException when Product not found`() = testApplication {
        val expectedAttribute = Attribute.BooleanAttribute(true)

        runCatching { sut.createOrUpdateAttribute("someKey", expectedAttribute, UUID.randomUUID().toString()) }
            .exceptionOrNull().run {
                this shouldNotBe null
                this!!::class shouldBe IllegalArgumentException::class
                this.message shouldBe "Product not found"
            }
    }

    @Test
    fun `create Attribute should throw IllegalArgumentException when ProductUUID is invalid UUID`() = testApplication {
        val expectedAttribute = Attribute.BooleanAttribute(true)
        val invalidUUID = "InvalidUUID"

        runCatching { sut.createOrUpdateAttribute("someKey", expectedAttribute, invalidUUID) }
            .exceptionOrNull().run {
                this shouldNotBe null
                this!!::class shouldBe IllegalArgumentException::class
                this.message shouldBe "Invalid UUID string: $invalidUUID"
            }
    }

    @Test
    fun `create Attribute should replace existing Attribute`() = testApplication {
        val attribute = Attribute.StringAttribute("someValue")
        val key = "someKey"
        sut.createOrUpdateAttribute(key, attribute, productId.toString())
        val updatedAttribute = Attribute.StringAttribute("updatedValue")
        sut.createOrUpdateAttribute(key, updatedAttribute, productId.toString()) shouldBe updatedAttribute

        transaction {
            ProductEntity.findById(productId)!!.toProduct()
        }.attributes shouldContain (key to updatedAttribute)
    }

    @Test
    fun `create Attribute should replace existing Attribute even type`() = testApplication {
        val attribute = Attribute.StringAttribute("someValue")
        val key = "someKey"
        sut.createOrUpdateAttribute(key, attribute, productId.toString())
        val updatedAttribute = Attribute.NumberAttribute(12321f)
        sut.createOrUpdateAttribute(key, updatedAttribute, productId.toString()) shouldBe updatedAttribute

        transaction {
            ProductEntity.findById(productId)!!.toProduct()
        }.attributes shouldContain (key to updatedAttribute)
    }

    @Test
    fun `create Attribute should replace existing AttributeList`() = testApplication {
        val attribute = Attribute.ListAttribute.fromBooleans(listOf(true, false, false, true))
        val key = "someKey"
        sut.createOrUpdateAttribute(key, attribute, productId.toString())
        val updatedAttribute = Attribute.NumberAttribute(12321f)
        sut.createOrUpdateAttribute(key, updatedAttribute, productId.toString()) shouldBe updatedAttribute

        transaction {
            ProductEntity.findById(productId)!!.toProduct()
        }.attributes shouldContain (key to updatedAttribute)
    }

    @Test
    fun `delete Attribute should return true when deleted`() = testApplication {
        val attribute = Attribute.StringAttribute("someValue")
        val key = "someKey"
        sut.createOrUpdateAttribute(key, attribute, productId.toString())
        sut.deleteAttribute(key, productId.toString()) shouldBe true
        transaction {
            ProductEntity.findById(productId)!!.toProduct()
        }.attributes shouldHaveSize 0
    }

    @Test
    fun `delete Attribute should return false when not found`() = testApplication {
        val key = "someKey"
        sut.deleteAttribute(key, productId.toString()) shouldBe false
    }

    @Test
    fun `get Attribute with wrong type should throw IllegalArgumentException`() = testApplication {
        val attribute = Attribute.StringAttribute("someValue")
        val key = "someKey"
        transaction {
            ProductAttributeEntity.new {
                this.product = ProductEntity.findById(productId)!!
                this.key = key
                this.type = "invalidType"
                this.value = attribute.value()
            }
        }
        runCatching { transaction { ProductEntity.findById(productId)!!.toProduct() } }
            .exceptionOrNull().run {
                this shouldNotBe null
                this!!::class shouldBe IllegalArgumentException::class
                this.message shouldBe "Unknown attribute type: invalidType"
            }
    }

}