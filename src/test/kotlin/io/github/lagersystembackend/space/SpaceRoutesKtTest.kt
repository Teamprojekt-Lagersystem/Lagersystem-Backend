package io.github.lagersystembackend.space

import io.github.lagersystembackend.common.*
import io.github.lagersystembackend.plugins.configureHTTP
import io.github.lagersystembackend.plugins.configureSerialization
import io.github.lagersystembackend.storage.StorageRepository
import io.github.lagersystembackend.product.Product
import io.github.lagersystembackend.storage.Storage
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test

class SpaceRoutesKtTest {
    val mockSpaceRepository = mockk<SpaceRepository>()
    val mockStorageRepository = mockk<StorageRepository>()
    fun ApplicationTestBuilder.createEnvironment() {
        application {
            configureHTTP()
            configureSerialization()
            this.routing { spaceRoutes(mockSpaceRepository, mockStorageRepository) }
        }
    }

    @BeforeTest
    fun setUp() {
    }

    @Test
    fun `get Spaces should respond with List of NetworkSpaces`() = testApplication {
        createEnvironment()
        val spaces = listOf(
            Space(UUID.randomUUID().toString(), "Space 1", 100f, "Description 1", storageId = "any id", products = listOf(), createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()),
            Space(UUID.randomUUID().toString(), "Space 2", 200f, "Description 2", storageId = "any id", products = listOf(), createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())
        )
        every { mockSpaceRepository.getSpaces() } returns spaces
        client.get("/spaces").apply {
            status shouldBe HttpStatusCode.OK
            Json.decodeFromString<List<NetworkSpace>>(bodyAsText()) shouldBe spaces.map { it.toNetworkSpace() }
        }
    }

    @Test
    fun `get Spaces should respond with emptyList when Repository is empty`() = testApplication {
        createEnvironment()

        every { mockSpaceRepository.getSpaces() } returns emptyList()
        client.get("/spaces").apply {
            status shouldBe HttpStatusCode.OK
            Json.decodeFromString<List<NetworkSpace>>(bodyAsText()) shouldBe emptyList<NetworkSpace>()
        }
    }

    @Test
    fun `get Space by ID should respond with NetworkSpace`() = testApplication {
        createEnvironment()
        val space1 = Space(UUID.randomUUID().toString(), "Space 1", 100f, "Description 1", storageId = "any id", products = listOf(), createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())
        every { mockSpaceRepository.getSpace(space1.id) } returns space1
        client.get("/spaces/${space1.id}").apply {
            status shouldBe HttpStatusCode.OK
            Json.decodeFromString<NetworkSpace>(bodyAsText()) shouldBe space1.toNetworkSpace()
        }
    }

    @Test
    fun `get Space should respond with BadRequest when id is invalid`() = testApplication {
        createEnvironment()
        client.get("/spaces/${"invalid id"}").apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.INVALID_UUID_SPACE)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `get Space should respond with NotFound when Space not found`() = testApplication {
        createEnvironment()
        val id = UUID.randomUUID().toString()
        every { mockSpaceRepository.getSpace(id) } returns null
        client.get("/spaces/${id}").apply {
            status shouldBe HttpStatusCode.NotFound
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.SPACE_NOT_FOUND)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `delete Space should delete Space`() = testApplication {
        createEnvironment()
        val space1 = Space(UUID.randomUUID().toString(), "Space 1", 100f, "Description 1", storageId = "any id", products = listOf(), createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())
        every { mockSpaceRepository.deleteSpace(space1.id) } returns space1
        client.delete("/spaces/${space1.id}").apply {
            status shouldBe HttpStatusCode.OK
            Json.decodeFromString<NetworkSpace>(bodyAsText()) shouldBe space1.toNetworkSpace()
            verify { mockSpaceRepository.deleteSpace(space1.id) }
        }
    }

    @Test
    fun `delete Space should respond with BadRequest when id is invalid`() = testApplication {
        createEnvironment()
        val id = "invalid id"
        client.delete("/spaces/$id").apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.INVALID_UUID_SPACE)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `delete Space should respond with NotFound when Space not found`() = testApplication {
        createEnvironment()
        val id = UUID.randomUUID().toString()
        every { mockSpaceRepository.deleteSpace(id) } returns null
        client.delete("/spaces/$id").apply {
            status shouldBe HttpStatusCode.NotFound
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.SPACE_NOT_FOUND)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `post Space should create Space`() = testApplication {
        createEnvironment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val id = UUID.randomUUID().toString()
        val storageId = UUID.randomUUID().toString()
        val addSpaceNetworkRequest = AddSpaceNetworkRequest("Space 1", 100f, "Description 1", storageId = storageId)
        addSpaceNetworkRequest.run {
            val space = Space(id, name, size, description, products = listOf(), storageId, LocalDateTime.now(), LocalDateTime.now())
            every { mockSpaceRepository.createSpace(name, size, description, storageId) } returns space
            every { mockStorageRepository.storageExists(storageId) } returns true
            client.post("/spaces") {
                setBody(addSpaceNetworkRequest)
                contentType(ContentType.Application.Json)
            }.apply {
                status shouldBe HttpStatusCode.Created
                Json.decodeFromString<NetworkSpace>(bodyAsText()) shouldBe space.toNetworkSpace()
            }
        }
    }

    @Test
    fun `post Space should create Space when size is null`() = testApplication {
        createEnvironment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val id = UUID.randomUUID().toString()
        val storageId = UUID.randomUUID().toString()
        val addSpaceNetworkRequest = AddSpaceNetworkRequest("Space 1", null, "Description 1", storageId = storageId)
        addSpaceNetworkRequest.run {
            val space = Space(id, name, size, description, products = listOf(), storageId, LocalDateTime.now(), LocalDateTime.now())
            every { mockSpaceRepository.createSpace(name, size, description, storageId) } returns space
            every { mockStorageRepository.storageExists(storageId) } returns true
            client.post("/spaces") {
                setBody(addSpaceNetworkRequest)
                contentType(ContentType.Application.Json)
            }.apply {
                status shouldBe HttpStatusCode.Created
                Json.decodeFromString<NetworkSpace>(bodyAsText()) shouldBe space.toNetworkSpace()
            }
        }
    }

    @Test
    fun `post Space should respond BadRequest when invalid AddASpaceNetworkRequest`() = testApplication {
        createEnvironment()
        val badRequest = """
            {
                "name": "Space 1",
                "size": 100.0
            }
        """.trimIndent()

        client.post("/spaces") {
            setBody(badRequest)
            contentType(ContentType.Application.Json)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.BODY_NOT_SERIALIZED_SPACE)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `patch update should update Space`() = testApplication {
        createEnvironment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val id = UUID.randomUUID().toString()
        val updateSpaceNetworkRequest = UpdateSpaceNetworkRequest("Space 1", 100f, "Description 1")
        val createTime = LocalDateTime.now()
        updateSpaceNetworkRequest.run {
            val space = Space(id, name!!, size!!, description!!, products = listOf(), storageId = "any id", createTime, createTime)
            every { mockSpaceRepository.spaceExists(id) } returns true
            every { mockSpaceRepository.updateSpace(id, name, size, description) } returns space
            client.patch("/spaces/${id}/update") {
                setBody(updateSpaceNetworkRequest)
                contentType(ContentType.Application.Json)
            }.apply {
                status shouldBe HttpStatusCode.OK
                Json.decodeFromString<NetworkSpace>(bodyAsText()) shouldBe space.toNetworkSpace()
            }
        }
    }

    @Test
    fun `patch update Space should respond with BadRequest when invalid UpdateSpaceNetworkRequest`() = testApplication {
        createEnvironment()
        val badRequest = """
            {
                "name": "Space 1",
                "invalidAttr": "invalid"
            }
        """.trimIndent()

        client.patch("/spaces/${UUID.randomUUID()}/update") {
            setBody(badRequest)
            contentType(ContentType.Application.Json)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.BODY_NOT_SERIALIZED_SPACE)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `patch update Space should respond with BadRequest when id is invalid`() = testApplication {
        createEnvironment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val invalidId = "not-a-uuid"
        val updateSpaceNetworkRequest = UpdateSpaceNetworkRequest("Space 1", 100f, "Description 1")
        every { mockSpaceRepository.spaceExists(invalidId) } returns false
        client.patch("/spaces/${invalidId}/update") {
            setBody(updateSpaceNetworkRequest)
            contentType(ContentType.Application.Json)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(
                    ErrorMessages.INVALID_UUID_SPACE)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `patch update Space should respond with NotFound when Space not found`() = testApplication {
        createEnvironment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val id = UUID.randomUUID().toString()
        val updateSpaceNetworkRequest = UpdateSpaceNetworkRequest("Space 1", 100f, "Description 1")
        every { mockSpaceRepository.spaceExists(id) } returns false
        client.patch("/spaces/${id}/update") {
            setBody(updateSpaceNetworkRequest)
            contentType(ContentType.Application.Json)
        }.apply {
            status shouldBe HttpStatusCode.NotFound
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.SPACE_NOT_FOUND)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `patch move Space should respond with BadRequest when id is invalid`() = testApplication {
        createEnvironment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val invalidId = "not-a-uuid"
        val moveRequest = MoveSpaceNetworkRequest(targetStorageId = UUID.randomUUID().toString())
        every { mockSpaceRepository.getSpace(any()) } returns null
        every { mockStorageRepository.storageExists(any()) } returns true

        client.patch("/spaces/$invalidId/move") {
            contentType(ContentType.Application.Json)
            setBody(moveRequest)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.INVALID_UUID_SPACE.withContext("ID: $invalidId"))
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `patch move Space should respond with BadRequest when body is not serialized`() = testApplication {
        createEnvironment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val id = UUID.randomUUID().toString()

        every { mockSpaceRepository.getSpace(id) } returns null
        every { mockStorageRepository.storageExists(any()) } returns true

        client.patch("/spaces/$id/move") {
            contentType(ContentType.Application.Json)
            setBody("invalid body")
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.BODY_NOT_SERIALIZED_SPACE)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `patch move Space should respond with BadRequest when targetStorageId is invalid`() = testApplication {
        createEnvironment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val id = UUID.randomUUID().toString()
        val moveRequest = MoveSpaceNetworkRequest(targetStorageId = "invalid-uuid")

        every { mockSpaceRepository.getSpace(id) } returns mockk()
        every { mockStorageRepository.storageExists(any()) } returns true

        client.patch("/spaces/$id/move") {
            contentType(ContentType.Application.Json)
            setBody(moveRequest)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.INVALID_UUID_STORAGE.withContext("Target Storage ID: invalid-uuid"))
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `patch move Space should respond with NotFound when targetStorageId storage is not found`() = testApplication {
        createEnvironment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val id = UUID.randomUUID().toString()
        val targetStorageId = UUID.randomUUID().toString()
        val moveRequest = MoveSpaceNetworkRequest(targetStorageId = targetStorageId)

        every { mockSpaceRepository.getSpace(id) } returns mockk()
        every { mockStorageRepository.storageExists(targetStorageId) } returns false

        client.patch("/spaces/$id/move") {
            contentType(ContentType.Application.Json)
            setBody(moveRequest)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.STORAGE_NOT_FOUND.withContext("ID: $targetStorageId"))
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `patch move Space should respond with NotFound when space is not found`() = testApplication {
        createEnvironment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val id = UUID.randomUUID().toString()
        val targetStorageId = UUID.randomUUID().toString()
        val moveRequest = MoveSpaceNetworkRequest(targetStorageId = targetStorageId)

        every { mockSpaceRepository.getSpace(id) } returns null
        every { mockStorageRepository.storageExists(targetStorageId) } returns true

        client.patch("/spaces/$id/move") {
            contentType(ContentType.Application.Json)
            setBody(moveRequest)
        }.apply {
            status shouldBe HttpStatusCode.NotFound
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.SPACE_NOT_FOUND.withContext("ID: $id"))
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `copySpace should duplicate space structure and return the copied space`() = testApplication {
        createEnvironment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val storageId = UUID.randomUUID().toString()
        val spaceId = UUID.randomUUID().toString()
        val productId = UUID.randomUUID().toString()
        val originalSpace = Space(
            id = spaceId,
            name = "Original Space",
            size = 100f,
            description = "A space description",
            storageId = storageId,
            products = listOf(
                Product(
                    id = productId,
                    name = "Original Product",
                    description = "A product description",
                    attributes = emptyMap(),
                    spaceId = spaceId,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                )
            ),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        val mockStorage = Storage(
            id = storageId,
            name = "Target Storage",
            description = "Target Storage Description",
            spaces = listOf(originalSpace),
            parentId = null,
            subStorages = listOf(),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        every { mockSpaceRepository.getSpace(spaceId) } returns originalSpace
        every { mockSpaceRepository.copySpace(spaceId, storageId) } answers {
            originalSpace.copy(
                id = UUID.randomUUID().toString(),
                name = "${originalSpace.name} (Copy)",
                products = originalSpace.products.map { product ->
                    product.copy(
                        id = UUID.randomUUID().toString(),
                        name = "${product.name} (Copy)"
                    )
                }
            )
        }
        every { mockStorageRepository.getStorage(storageId) } returns mockStorage
        val copyRequest = CopySpaceRequest(targetStorageId = storageId)
        val response = client.post("/spaces/$spaceId/copy") {
            contentType(ContentType.Application.Json)
            setBody(copyRequest)
        }
        response.status shouldBe HttpStatusCode.Created
        val expectedCopiedSpace = mockSpaceRepository.copySpace(spaceId, storageId).toNetworkSpace()
        val actualCopiedSpace = Json.decodeFromString<NetworkSpace>(response.bodyAsText())

        expectedCopiedSpace.apply {
            actualCopiedSpace.apply {
                name shouldBe this@apply.name
                size shouldBe this@apply.size
                description shouldBe this@apply.description
                products?.size shouldBe this@apply.products?.size
                products?.zip(this@apply.products ?: listOf())?.forEach { (expectedProduct, actualProduct) ->
                    expectedProduct.apply {
                        actualProduct.apply {
                            name shouldBe this@apply.name
                            description shouldBe this@apply.description
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `copySpace should return BadRequest if id is not a valid UUID`() = testApplication {
        createEnvironment()
        val invalidId = "invalid-uuid"
        client.post("/spaces/$invalidId/copy").apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.INVALID_UUID_SPACE)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `copySpace should respond with BadRequest when request body is missing or not serialized properly`() = testApplication {
        createEnvironment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val validId = UUID.randomUUID().toString()

        client.post("/spaces/$validId/copy").apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.BODY_NOT_SERIALIZED_SPACE)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }

        client.post("/spaces/$validId/copy") {
            contentType(ContentType.Application.Json)
            setBody("{ invalid json }") // Invalid JSON
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.BODY_NOT_SERIALIZED_SPACE)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `copySpace should respond with BadRequest when targetStorageId is not a valid UUID`() = testApplication {
        createEnvironment()
        val validId = UUID.randomUUID().toString()

        client.post("/spaces/$validId/copy") {
            contentType(ContentType.Application.Json)
            setBody("""{"targetStorageId": "invalid-uuid"}""")
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.INVALID_UUID_STORAGE)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }
    @Test
    fun `copySpace should respond with BadRequest when target storage does not exist`() = testApplication {
        createEnvironment()
        val validId = UUID.randomUUID().toString()
        val nonExistentStorageId = UUID.randomUUID().toString()

        val originalSpace = Space(validId, "Original Space", 100f, "Description", storageId = UUID.randomUUID().toString(), products = listOf(),createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now(),)
        every { mockSpaceRepository.getSpace(validId) } returns originalSpace
        every { mockStorageRepository.getStorage(nonExistentStorageId) } returns null

        client.post("/spaces/$validId/copy") {
            contentType(ContentType.Application.Json)
            setBody("""{"targetStorageId": "$nonExistentStorageId"}""")
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.STORAGE_NOT_FOUND.withContext("ID: $nonExistentStorageId"))
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }


}