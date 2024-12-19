package io.github.lagersystembackend.product

import io.github.lagersystembackend.common.ApiResponse
import io.github.lagersystembackend.common.ErrorMessages
import io.github.lagersystembackend.attribute.Attribute
import io.github.lagersystembackend.plugins.configureHTTP
import io.github.lagersystembackend.plugins.configureSerialization
import io.github.lagersystembackend.space.SpaceRepository
import io.github.lagersystembackend.space.Space
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test

class ProductRoutesKtTest {
    val mockProductRepository = mockk<ProductRepository>()
    val mockSpaceRepository = mockk<SpaceRepository>()
    fun ApplicationTestBuilder.createEnvironment() {
        application {
            configureHTTP()
            configureSerialization()
            routing { productRoutes(mockProductRepository, mockSpaceRepository) }
        }
    }


    @BeforeTest
    fun setUp() {
    }

    @Test
    fun `get Products should respond with List of NetworkProducts`() = testApplication {
        createEnvironment()
        val products = listOf(
            Product(UUID.randomUUID().toString(), "Space 1",  "Description 1",  emptyMap(), UUID.randomUUID().toString(), LocalDateTime.now(), LocalDateTime.now()),
            Product(UUID.randomUUID().toString(), "Space 2", "Description 2", mapOf("someKey" to Attribute.NumberAttribute(123f)), UUID.randomUUID().toString(), LocalDateTime.now(), LocalDateTime.now())
        )
        every { mockProductRepository.getProducts() } returns products
        client.get("/products").apply {
            status shouldBe HttpStatusCode.OK
            Json.decodeFromString<List<NetworkProduct>>(bodyAsText()) shouldBe products.map { it.toNetworkProduct() }
        }
    }

    @Test
    fun `get Products should respond with emptyList when Repository is empty`() = testApplication {
        createEnvironment()

        every { mockProductRepository.getProducts() } returns emptyList()
        client.get("/products").apply {
            status shouldBe HttpStatusCode.OK
            Json.decodeFromString<List<NetworkProduct>>(bodyAsText()) shouldBe emptyList<NetworkProduct>()
        }
    }

    @Test
    fun `get Product by ID should respond with NetworkProduct`() = testApplication {
        createEnvironment()
        val product1 =
            Product(UUID.randomUUID().toString(), "Space 1", "Description 1", emptyMap(), UUID.randomUUID().toString(), LocalDateTime.now(), LocalDateTime.now())
        every { mockProductRepository.getProduct(product1.id) } returns product1
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
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.INVALID_UUID_PRODUCT)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `get Product should respond with NotFound when Product not found`() = testApplication {
        createEnvironment()
        val id = UUID.randomUUID().toString()
        every { mockProductRepository.getProduct(id) } returns null
        client.get("/products/${id}").apply {
            status shouldBe HttpStatusCode.NotFound
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.PRODUCT_NOT_FOUND)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `delete Product should delete Product`() = testApplication {
        createEnvironment()
        val product1 = Product(UUID.randomUUID().toString(), "Product 1", "Description 1", emptyMap(), "any id", LocalDateTime.now(), LocalDateTime.now())
        every { mockProductRepository.deleteProduct(product1.id) } returns product1
        client.delete("/products/${product1.id}").apply {
            status shouldBe HttpStatusCode.OK
            Json.decodeFromString<NetworkProduct>(bodyAsText()) shouldBe product1.toNetworkProduct()
            verify { mockProductRepository.deleteProduct(product1.id) }
        }
    }

    @Test
    fun `delete Product should respond with BadRequest when id is invalid`() = testApplication {
        createEnvironment()
        val id = "invalid id"
        client.delete("/products/$id").apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.INVALID_UUID_PRODUCT)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `delete Product should respond with NotFound when Product not found`() = testApplication {
        createEnvironment()
        val id = UUID.randomUUID().toString()
        every { mockProductRepository.deleteProduct(id) } returns null
        client.delete("/products/$id").apply {
            status shouldBe HttpStatusCode.NotFound
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.PRODUCT_NOT_FOUND)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `post Product should create Product`() = testApplication {
        createEnvironment()
        val createTime = LocalDateTime.now()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val id = UUID.randomUUID().toString()
        val addProductNetworkRequest =
            AddProductNetworkRequest("Product 1", "Description 1", UUID.randomUUID().toString())
        addProductNetworkRequest.run {
            val product = Product(id, name, description, emptyMap(), spaceId, createTime, createTime)
            every { mockProductRepository.createProduct(name, description, spaceId) } returns product
            every { mockSpaceRepository.spaceExists(spaceId) } returns true
        }

        client.post("/products") {
            setBody(addProductNetworkRequest)
            contentType(ContentType.Application.Json)
        }.apply {
            status shouldBe HttpStatusCode.Created
            val expectedResponse =
                Product(
                    id,
                    addProductNetworkRequest.name,
                    addProductNetworkRequest.description,
                    emptyMap(),
                    addProductNetworkRequest.spaceId,
                    createTime,
                    createTime
                ).toNetworkProduct()
            Json.decodeFromString<NetworkProduct>(bodyAsText()) shouldBe expectedResponse

        }
    }

    @Test
    fun `post Product should create Product when price is null`() = testApplication {
        createEnvironment()
        val createTime = LocalDateTime.now()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val id = UUID.randomUUID().toString()
        val addProductNetworkRequest =
            AddProductNetworkRequest("Space 1", "Description 1", UUID.randomUUID().toString())
        addProductNetworkRequest.run {
            val product = Product(id, name, description, emptyMap(), spaceId, createTime, createTime)
            every { mockProductRepository.createProduct(name, description, spaceId) } returns product
            every { mockSpaceRepository.spaceExists(spaceId) } returns true
        }

        client.post("/products") {
            setBody(addProductNetworkRequest)
            contentType(ContentType.Application.Json)
        }.apply {
            status shouldBe HttpStatusCode.Created
            val expectedResponse =
                Product(
                    id,
                    addProductNetworkRequest.name,
                    addProductNetworkRequest.description,
                    emptyMap(),
                    addProductNetworkRequest.spaceId,
                    createTime,
                    createTime
                ).toNetworkProduct()
            Json.decodeFromString<NetworkProduct>(bodyAsText()) shouldBe expectedResponse

        }
    }

    @Test
    fun `post Product should respond BadRequest when invalid AddProductNetworkRequest`() = testApplication {
        createEnvironment()
        val badRequest = """
    {
        "name": "Space 1",
        "description": "Description 1",
    }
""".trimIndent()

        client.post("/products") {
            setBody(badRequest)
            contentType(ContentType.Application.Json)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.BODY_NOT_SERIALIZED_PRODUCT)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `patch update Product should respond with NetworkProduct`() = testApplication {
        createEnvironment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val id = UUID.randomUUID().toString()
        val spaceId = UUID.randomUUID().toString()
        val updateProductNetworkRequest = UpdateProductNetworkRequest("Product 1", "Description 1")
        val createTime = LocalDateTime.now()
        updateProductNetworkRequest.run {
            val product = Product(id, name!!, description!!, emptyMap(), spaceId, createTime, createTime)
            every { mockProductRepository.updateProduct(id, name, description) } returns product
            every { mockSpaceRepository.spaceExists(spaceId) } returns true
        }

        client.patch("/products/${id}/update") {
            setBody(updateProductNetworkRequest)
            contentType(ContentType.Application.Json)
        }.apply {
            status shouldBe HttpStatusCode.OK
            val expectedResponse = Product(
                id,
                updateProductNetworkRequest.name!!,
                updateProductNetworkRequest.description!!,
                emptyMap(),
                spaceId,
                createTime,
                createTime
            ).toNetworkProduct()
            Json.decodeFromString<NetworkProduct>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `patch update Product should respond BadRequest when invalid UpdateProductNetworkRequest`() = testApplication {
        createEnvironment()
        val badRequest = """
    {
        "name": "Space 1",
        "invalidAttr": "invalid"
    }
""".trimIndent()

        client.patch("/products/${UUID.randomUUID()}/update") {
            setBody(badRequest)
            contentType(ContentType.Application.Json)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.BODY_NOT_SERIALIZED_PRODUCT)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `patch update Product should respond BadRequest when Product Id is invalid`() = testApplication {
        createEnvironment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val invalidId = "invalid-id"
        val spaceId = UUID.randomUUID().toString()
        val updateProductNetworkRequest = UpdateProductNetworkRequest("Product 1", "Description 1")
        updateProductNetworkRequest.run {
            every { mockSpaceRepository.spaceExists(spaceId) } returns true
        }
        client.patch("/products/${invalidId}/update") {
            setBody(updateProductNetworkRequest)
            contentType(ContentType.Application.Json)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.INVALID_UUID_PRODUCT)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `patch move Product should respond BadRequest when Product Id invalid`() = testApplication {
        createEnvironment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val invalidId = "invalid-id"
        val moveProductNetworkRequest = MoveProductNetworkRequest(UUID.randomUUID().toString())
        client.patch("/products/${invalidId}/move") {
            setBody(moveProductNetworkRequest)
            contentType(ContentType.Application.Json)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.INVALID_UUID_PRODUCT)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `patch move Product should respond BadRequest when Space Id invalid`() = testApplication {
        createEnvironment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val moveProductNetworkRequest = MoveProductNetworkRequest("invalid-uuid")
        client.patch("/products/${UUID.randomUUID()}/move") {
            setBody(moveProductNetworkRequest)
            contentType(ContentType.Application.Json)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.INVALID_UUID_SPACE)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `patch move Product should respond NotFound when Space not found`() = testApplication {
        createEnvironment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val productId = UUID.randomUUID().toString()
        val spaceId = UUID.randomUUID().toString()
        val moveProductNetworkRequest = MoveProductNetworkRequest(spaceId)
        every { mockSpaceRepository.spaceExists(spaceId) } returns false
        client.patch("/products/$productId/move") {
            setBody(moveProductNetworkRequest)
            contentType(ContentType.Application.Json)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.SPACE_NOT_FOUND)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `patch moveProduct should respond NotFound when Product not found`() = testApplication {
        createEnvironment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val productId = UUID.randomUUID().toString()
        val spaceId = UUID.randomUUID().toString()
        val moveProductNetworkRequest = MoveProductNetworkRequest(spaceId)
        every { mockSpaceRepository.spaceExists(spaceId) } returns true
        every { mockProductRepository.moveProduct(productId, spaceId) } returns null
        client.patch("/products/$productId/move") {
            setBody(moveProductNetworkRequest)
            contentType(ContentType.Application.Json)
        }.apply {
            status shouldBe HttpStatusCode.NotFound
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.PRODUCT_NOT_FOUND)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `copyProduct should duplicate product structure and return the copied product as NetworkProduct`() = testApplication {
        createEnvironment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val spaceId = UUID.randomUUID().toString()
        val productId = UUID.randomUUID().toString()
        val originalProduct = Product(
            id = productId,
            name = "Original Product",
            description = "A product description",
            attributes = emptyMap(),
            spaceId = spaceId,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        every { mockSpaceRepository.getSpace(spaceId) } returns Space(
            id = spaceId,
            name = "Space",
            size = 50f,
            description = "Space description",
            products = listOf(originalProduct),
            storageId = UUID.randomUUID().toString(),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        every { mockProductRepository.getProduct(productId) } returns originalProduct
        every { mockProductRepository.copyProduct(productId, spaceId) } answers {
            originalProduct.copy(
                id = UUID.randomUUID().toString(),
                name = "${originalProduct.name} (Copy)"
            )
        }

        val copyRequest = CopyProductRequest(targetSpaceId = spaceId)
        val response = client.post("/products/$productId/copy") {
            contentType(ContentType.Application.Json)
            setBody(copyRequest)
        }

        response.status shouldBe HttpStatusCode.Created

        val expectedCopiedProduct = mockProductRepository.copyProduct(productId, spaceId).toNetworkProduct()
        val actualNetworkProduct = Json.decodeFromString<NetworkProduct>(response.bodyAsText())

        expectedCopiedProduct.apply {
            actualNetworkProduct.apply {
                name shouldBe this@apply.name
                description shouldBe this@apply.description
                spaceId shouldBe this@apply.spaceId
                attributes shouldBe this@apply.attributes
            }
        }
    }

    @Test
    fun `copyProduct should return BadRequest if id is not a valid UUID`() = testApplication {
        createEnvironment()
        client.post("/products/invalid-uuid/copy").apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(listOf(ErrorMessages.INVALID_UUID_PRODUCT))
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `copyProduct should return NotFound if product does not exist`() = testApplication {
        createEnvironment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val productId = UUID.randomUUID().toString()
        val spaceId = UUID.randomUUID().toString()

        every { mockProductRepository.getProduct(productId) } returns null
        every { mockSpaceRepository.getSpace(spaceId) } returns Space(
            id = spaceId,
            name = "Space",
            size = 50f,
            description = "Space description",
            products = emptyList(),
            storageId = UUID.randomUUID().toString(),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

        client.post("/products/$productId/copy") {
            contentType(ContentType.Application.Json)
            setBody(CopyProductRequest(targetSpaceId = spaceId))
        }.apply {
            status shouldBe HttpStatusCode.NotFound
            val expectedResponse = ApiResponse.Error(listOf(ErrorMessages.PRODUCT_NOT_FOUND))
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `copyProduct should return BadRequest when target space ID is invalid`() = testApplication {
        createEnvironment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val productId = UUID.randomUUID().toString()

        client.post("/products/$productId/copy") {
            contentType(ContentType.Application.Json)
            setBody(CopyProductRequest(targetSpaceId = "invalid-uuid"))
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(listOf(ErrorMessages.INVALID_UUID_SPACE))
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `copyProduct should return BadRequest when target space does not exist`() = testApplication {
        createEnvironment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val productId = UUID.randomUUID().toString()
        val invalidSpaceId = UUID.randomUUID().toString()

        every { mockSpaceRepository.getSpace(invalidSpaceId) } returns null
        every { mockProductRepository.getProduct(productId) } returns Product(
            id = productId,
            name = "Original Product",
            description = "A product description",
            attributes = emptyMap(),
            spaceId = UUID.randomUUID().toString(),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

        client.post("/products/$productId/copy") {
            contentType(ContentType.Application.Json)
            setBody(CopyProductRequest(targetSpaceId = invalidSpaceId))
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(listOf(ErrorMessages.SPACE_NOT_FOUND))
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }
}


