package io.github.lagersystembackend.product

import io.github.lagersystembackend.plugins.configureHTTP
import io.github.lagersystembackend.plugins.configureSerialization
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.routing.routing
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test

class ProductRoutesKtTest {
    val mockSpaceRepository = mockk<ProductRepository>()
    fun ApplicationTestBuilder.createEnvironment() {
        application {
            configureHTTP()
            configureSerialization()
            routing { productRoutes(mockSpaceRepository) }
        }
    }


    @BeforeTest
    fun setUp() {
    }

    @Test
    fun `get Products should respond with List of NetworkProducts`() = testApplication {
        createEnvironment()
        val products = listOf(
            Product(UUID.randomUUID().toString(), "Space 1", 100f, "Description 1", UUID.randomUUID().toString()),
            Product(UUID.randomUUID().toString(), "Space 2", 200f, "Description 2", UUID.randomUUID().toString())
        )
        every { mockSpaceRepository.getProducts() } returns products
        client.get("/products").apply {
            status shouldBe HttpStatusCode.OK
            Json.decodeFromString<List<NetworkProduct>>(bodyAsText()) shouldBe products.map { it.toNetworkProduct() }
        }
    }

    @Test
    fun `get Products should respond with emptyList when Repository is empty`() = testApplication {
        createEnvironment()

        every { mockSpaceRepository.getProducts() } returns emptyList()
        client.get("/products").apply {
            status shouldBe HttpStatusCode.OK
            Json.decodeFromString<List<NetworkProduct>>(bodyAsText()) shouldBe emptyList()
        }
    }

    @Test
    fun `get Product by ID should respond with NetworkProduct`() = testApplication {
        createEnvironment()
        val product1 = Product(UUID.randomUUID().toString(), "Space 1", 100f, "Description 1", UUID.randomUUID().toString())
        every { mockSpaceRepository.getProduct(product1.id) } returns product1
        client.get("/products/${product1.id}").apply {
            status shouldBe HttpStatusCode.OK
            Json.decodeFromString<NetworkProduct>(bodyAsText()) shouldBe product1.toNetworkProduct()
        }
    }

    @Test
    fun `get Product should respond with BadRequest when id is invalid`() = testApplication {
        createEnvironment()
        client.get("/products/${"invalid id"}").apply {
            status shouldBe HttpStatusCode.BadRequest
            bodyAsText() shouldBe "Invalid UUID"
        }
    }

    @Test
    fun `get Product should respond with NotFound when Product not found`() = testApplication {
        createEnvironment()
        val id = UUID.randomUUID().toString()
        every { mockSpaceRepository.getProduct(id) } returns null
        client.get("/products/${id}").apply {
            status shouldBe HttpStatusCode.NotFound
            bodyAsText() shouldBe "Product not found"
        }
    }

    @Test
    fun `delete Product should delete Product`() = testApplication {
        createEnvironment()
        val id = UUID.randomUUID().toString()
        every { mockSpaceRepository.deleteProduct(id) } returns null
        client.delete("/products/$id").apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe "Product deleted"
            verify { mockSpaceRepository.deleteProduct(id) }
        }
    }

    @Test
    fun `delete Product should respond with BadRequest when id is invalid`() = testApplication {
        createEnvironment()
        val id = "invalid id"
        client.delete("/products/$id").apply {
            status shouldBe HttpStatusCode.BadRequest
            bodyAsText() shouldBe "Invalid UUID"
        }
    }

    @Test
    fun `delete Product should respond with NotFound when Product not found`() = testApplication {
        createEnvironment()
        val id = UUID.randomUUID().toString()
        every { mockSpaceRepository.deleteProduct(id) } returns null
        client.delete("/products/$id").apply {
            status shouldBe HttpStatusCode.NotFound
            bodyAsText() shouldBe "Product not found"
        }
    }

    @Test
    fun `post Product should create Product`() = testApplication {
        createEnvironment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val id = UUID.randomUUID().toString()
        val addProductNetworkRequest = AddProductNetworkRequest("Product 1", 100f, "Description 1", UUID.randomUUID().toString())
        addProductNetworkRequest.run {
            val product = Product(id, name, price, description, spaceId)
            every { mockSpaceRepository.createProduct(name, price, description, spaceId) } returns product
        }

        client.post("/products") {
            setBody(addProductNetworkRequest)
            contentType(ContentType.Application.Json)
        }.apply {
            status shouldBe HttpStatusCode.Created
            bodyAsText() shouldBe "Product created"
        }
    }

    @Test
    fun `post Product should create Product when size is null`() = testApplication {
        createEnvironment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val id = UUID.randomUUID().toString()
        val addProductNetworkRequest = AddProductNetworkRequest("Space 1", null, "Description 1", UUID.randomUUID().toString())
        addProductNetworkRequest.run {
            val product = Product(id, name, price, description, spaceId)
            every { mockSpaceRepository.createProduct(name, price, description, spaceId) } returns product
        }

        client.post("/products") {
            setBody(addProductNetworkRequest)
            contentType(ContentType.Application.Json)
        }.apply {
            status shouldBe HttpStatusCode.Created
            bodyAsText() shouldBe "Product created"
        }
    }

    @Test
    fun `post Product should respond BadRequest when invalid AddProductNetworkRequest`() = testApplication {
        createEnvironment()
        val badRequest = """
            {
                "name": "Space 1",
                "prize": 100.0
            }
        """.trimIndent()

        client.post("/products") {
            setBody(badRequest)
            contentType(ContentType.Application.Json)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            bodyAsText() shouldBe "Body should be Serialized AddProductNetworkRequest"
        }
    }
}