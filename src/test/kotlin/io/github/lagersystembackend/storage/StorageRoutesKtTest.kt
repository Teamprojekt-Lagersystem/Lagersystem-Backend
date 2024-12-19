package io.github.lagersystembackend.storage

import io.github.lagersystembackend.common.*
import io.github.lagersystembackend.plugins.configureHTTP
import io.github.lagersystembackend.plugins.configureSerialization
import io.github.lagersystembackend.product.Product
import io.github.lagersystembackend.space.Space
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
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test

class StorageRoutesKtTest {
    val mockStorageRepository = mockk<StorageRepository>()
    fun ApplicationTestBuilder.createEnviroment() {
        application {
            configureHTTP()
            configureSerialization()
            routing { storageRoutes(mockStorageRepository) }
        }
    }

    @BeforeTest
    fun setUp() {
    }

    @Test
    fun `get Storages should respond with List of Parent NetworkStorages`() = testApplication {
        createEnviroment()
        val storages = listOf(
            Storage(UUID.randomUUID().toString(), "Storage 1", "Description 1", spaces = listOf(),parentId = null, subStorages = listOf(), createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()),
            Storage(UUID.randomUUID().toString(), "Storage 2", "Description 2", spaces = listOf(), parentId = null, subStorages = listOf(), createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())
        )
        every { mockStorageRepository.getStorages() } returns storages
        client.get("/storages").apply {
            status shouldBe HttpStatusCode.OK
            Json.decodeFromString<List<NetworkStorage>>(bodyAsText()) shouldBe storages.map { it.toNetworkStorage() }
        }
    }

    @Test
    fun `get Storages should respond with emptyList when Repository is empty`() = testApplication {
        createEnviroment()

        every { mockStorageRepository.getStorages() } returns emptyList()
        client.get("/storages").apply {
            status shouldBe HttpStatusCode.OK
            Json.decodeFromString<List<NetworkStorage>>(bodyAsText()) shouldBe emptyList<NetworkStorage>()
        }
    }

    @Test
    fun `get Storage should respond with Bad Request when depth parameter is invalid`() = testApplication {
        createEnviroment()
        client.get("/storages?depth=invalid").apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.INVALID_DEPTH)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `get Storage by ID should respond with NetworkStorage`() = testApplication {
        createEnviroment()
        val storage1 = Storage(UUID.randomUUID().toString(), "Storage 1", "Description 1", spaces = listOf(), parentId = null, subStorages = listOf(), createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())
        every { mockStorageRepository.getStorage(storage1.id) } returns storage1
        client.get("/storages/${storage1.id}").apply {
            status shouldBe HttpStatusCode.OK
            Json.decodeFromString<NetworkStorage>(bodyAsText()) shouldBe storage1.toNetworkStorage()
        }
    }

    @Test
    fun `get Storage should respond with Bad Request when id is invalid`() = testApplication {
        createEnviroment()
        client.get("/storages/${"invalid id"}").apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.INVALID_UUID_STORAGE)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `get Storage should respond with NotFound when Storage not found`() = testApplication {
        createEnviroment()
        val id = UUID.randomUUID().toString()
        every { mockStorageRepository.getStorage(id) } returns null
        client.get("/storages/${id}").apply {
            status shouldBe HttpStatusCode.NotFound
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.STORAGE_NOT_FOUND)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `get Storage by id should respond with Bad Request when depth parameter is invalid`() = testApplication {
        createEnviroment()
        val id = UUID.randomUUID().toString()
        val storage = Storage(id, "Storage 1", "Description 1", spaces = listOf(), parentId = null, subStorages = listOf(), createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())
        every { mockStorageRepository.getStorage(id) } returns storage
        client.get("/storages/${id}?depth=invalid").apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.INVALID_DEPTH)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `delete Storage should delete Storage`() = testApplication {
        createEnviroment()
        val storage1 = Storage(UUID.randomUUID().toString(), "Storage 1", "Description 1", spaces = listOf(), parentId = null, subStorages = listOf(), createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())
        every { mockStorageRepository.deleteStorage(storage1.id) } returns storage1
        client.delete("/storages/${storage1.id}").apply {
            status shouldBe HttpStatusCode.OK
            Json.decodeFromString<NetworkStorage>(bodyAsText()) shouldBe storage1.toNetworkStorage()
        }
    }

    @Test
    fun `delete Storage should respond with BadRequest when id is invalid`() = testApplication {
        createEnviroment()
        val id = "invalid id"
        client.delete("/storages/$id").apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.INVALID_UUID_STORAGE)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `delete Storage should respond with NotFound when Storage not found`() = testApplication {
        createEnviroment()
        val id = UUID.randomUUID().toString()
        every { mockStorageRepository.deleteStorage(id) } returns null
        client.delete("/storages/$id").apply {
            status shouldBe HttpStatusCode.NotFound
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.STORAGE_NOT_FOUND)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `create Storage should respond with NetworkStorage`() = testApplication {
        createEnviroment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val id = UUID.randomUUID().toString()
        val addStorageNetworkRequest = AddStorageNetworkRequest("Storage 1", "Description 1", parentId = null)
        addStorageNetworkRequest.run {
            val storage = Storage(id, name, description, spaces = listOf(), parentId = null, subStorages = listOf(), createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())
            every { mockStorageRepository.createStorage(name, description, parentId) } returns storage
            client.post("/storages") {
                setBody(addStorageNetworkRequest)
                contentType(ContentType.Application.Json)
            }.apply {
                status shouldBe HttpStatusCode.Created
                Json.decodeFromString<NetworkStorage>(bodyAsText()) shouldBe storage.toNetworkStorage()
            }
        }
    }

    @Test
    fun `create Storage should respond with BadRequest when parentId is invalid`() = testApplication {
        createEnviroment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val parentId = "invalid id"
        val addStorageNetworkRequest = AddStorageNetworkRequest("Storage 1", "Description 1", parentId = parentId)
        addStorageNetworkRequest.run {
            client.post("/storages") {
                setBody(addStorageNetworkRequest)
                contentType(ContentType.Application.Json)
            }.apply {
                status shouldBe HttpStatusCode.BadRequest
                val expectedResponse = ApiResponse.Error(
                    listOf(ErrorMessages.INVALID_UUID_STORAGE)
                )
                Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
            }
        }
    }

    @Test
    fun `create Storage should respond with BadRequest when parentId is not found`() = testApplication {
        createEnviroment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val parentId = UUID.randomUUID().toString()
        val addStorageNetworkRequest = AddStorageNetworkRequest("Storage 1", "Description 1", parentId = parentId)
        addStorageNetworkRequest.run {
            every { mockStorageRepository.getStorage(parentId) } returns null
            client.post("/storages") {
                setBody(addStorageNetworkRequest)
                contentType(ContentType.Application.Json)
            }.apply {
                status shouldBe HttpStatusCode.BadRequest
                val expectedResponse = ApiResponse.Error(
                    listOf(ApiError("STORAGE_NOT_FOUND", "The specified storage was not found.", "ID: $parentId"))
                )
                Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
            }
        }
    }

    @Test
    fun `create Storage with valid parentId should respond with NetworkStorage`() = testApplication {
        createEnviroment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val parentId = UUID.randomUUID().toString()
        val addStorageNetworkRequest = AddStorageNetworkRequest("Storage 1", "Description 1", parentId = parentId)
        addStorageNetworkRequest.run {
            val storageParent = Storage(parentId, "Storage Parent", "Description Parent", spaces = listOf(), parentId = null, subStorages = listOf(), createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())
            val storage = Storage(UUID.randomUUID().toString(), name, description, spaces = listOf(), parentId = parentId, subStorages = listOf(), createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())
            every { mockStorageRepository.getStorage(parentId) } returns storageParent
            every { mockStorageRepository.createStorage(name, description, parentId) } returns storage
            client.post("/storages") {
                setBody(addStorageNetworkRequest)
                contentType(ContentType.Application.Json)
            }.apply {
                status shouldBe HttpStatusCode.Created
                Json.decodeFromString<NetworkStorage>(bodyAsText()) shouldBe storage.toNetworkStorage()
            }
        }
    }

    @Test
    fun `patch update Storage should respond with NetworkStorage`() = testApplication {
        createEnviroment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val id = UUID.randomUUID().toString()
        val updateStorageNetworkRequest = UpdateStorageNetworkRequest("Storage 1", "Description 1")
        val createTime = LocalDateTime.now()
        val storage = Storage(id, "Storage 1", "Description 1", spaces = listOf(), parentId = null, subStorages = listOf(), createTime, createTime)
        every { mockStorageRepository.updateStorage(id, updateStorageNetworkRequest.name, updateStorageNetworkRequest.description) } returns storage
        every { mockStorageRepository.storageExists(id) } returns true
        client.patch("/storages/${id}/update") {
            setBody(updateStorageNetworkRequest)
            contentType(ContentType.Application.Json)
        }.apply {
            status shouldBe HttpStatusCode.OK
            Json.decodeFromString<NetworkStorage>(bodyAsText()) shouldBe storage.toNetworkStorage()
        }
    }

    @Test
    fun `patch update Storage should respond with BadRequest when id is invalid`() = testApplication {
        createEnviroment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val id = "invalid id"
        val updateStorageNetworkRequest = UpdateStorageNetworkRequest("Storage 1", "Description 1")
        every { mockStorageRepository.storageExists(id) } returns false
        client.patch("/storages/${id}/update") {
            setBody(updateStorageNetworkRequest)
            contentType(ContentType.Application.Json)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(
                    ErrorMessages.INVALID_UUID_STORAGE)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `patch update Storage should respond with NotFound when Storage not found`() = testApplication {
        createEnviroment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val id = UUID.randomUUID().toString()
        val updateStorageNetworkRequest = UpdateStorageNetworkRequest("Storage 1", "Description 1")
        every { mockStorageRepository.storageExists(id) } returns false
        client.patch("/storages/${id}/update") {
            setBody(updateStorageNetworkRequest)
            contentType(ContentType.Application.Json)
        }.apply {
            status shouldBe HttpStatusCode.NotFound
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.STORAGE_NOT_FOUND)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `patch update Storage should respond with BadRequest when request body is wrong`() = testApplication {
        createEnviroment()
        val badRequest = """
            {
                "name": "Storage 1",
                "invalidAttr": "invalid"
            }
        """.trimIndent()
        client.patch("/storages/${UUID.randomUUID()}/update") {
            setBody(badRequest)
            contentType(ContentType.Application.Json)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.BODY_NOT_SERIALIZED_STORAGE)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `patch move Storage should respond with BadRequest when id is invalid`() = testApplication {
        createEnviroment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val invalidId = "invalid-id"
        val moveRequest = MoveStorageRequest(newParentId = UUID.randomUUID().toString())
        every { mockStorageRepository.getStorage(any()) } returns null

        client.patch("/storages/$invalidId/move") {
            contentType(ContentType.Application.Json)
            setBody(moveRequest)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.INVALID_UUID_STORAGE)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `patch move Storage should respond with BadRequest when request body is missing`() = testApplication {
        createEnviroment()
        val id = UUID.randomUUID().toString()
        client.patch("/storages/$id/move").apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.BODY_NOT_SERIALIZED_STORAGE)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `patch move Storage should respond with BadRequest when newParentId is invalid`() = testApplication {
        createEnviroment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val id = UUID.randomUUID().toString()
        val invalidParentId = "invalid-id"
        val moveRequest = MoveStorageRequest(newParentId = invalidParentId)
        client.patch("/storages/$id/move") {
            contentType(ContentType.Application.Json)
            setBody(moveRequest)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.INVALID_UUID_STORAGE)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `patch move Storage should respond with NotFound when target Storage is not found`() = testApplication {
        createEnviroment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val id = UUID.randomUUID().toString()
        val newParentId = UUID.randomUUID().toString()
        val moveRequest = MoveStorageRequest(newParentId = newParentId)
        every { mockStorageRepository.storageExists(id) } returns false
        every { mockStorageRepository.getStorage(id) } returns null
        every { mockStorageRepository.getStorage(newParentId) } returns Storage(
            id, "Storage A", "Description A", spaces = listOf(), parentId = null, subStorages = listOf(), LocalDateTime.now(), LocalDateTime.now()
        )
        every { mockStorageRepository.storageExists(newParentId) } returns true

        client.patch("/storages/$id/move") {
            contentType(ContentType.Application.Json)
            setBody(moveRequest)
        }.apply {
            status shouldBe HttpStatusCode.NotFound
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.STORAGE_NOT_FOUND)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }
    @Test
    fun `patch move Storage should respond with BadRequest when target parent Storage is not found`() = testApplication {
        createEnviroment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val id = UUID.randomUUID().toString()
        val newParentId = UUID.randomUUID().toString()
        val moveRequest = MoveStorageRequest(newParentId = newParentId)
        every { mockStorageRepository.storageExists(id) } returns true
        every { mockStorageRepository.getStorage(id) } returns Storage(
            id, "Storage A", "Description A", spaces = listOf(), parentId = null, subStorages = listOf(), LocalDateTime.now(), LocalDateTime.now()
        )
        every { mockStorageRepository.storageExists(newParentId) } returns false
        every { mockStorageRepository.getStorage(newParentId) } returns null

        client.patch("/storages/$id/move") {
            contentType(ContentType.Application.Json)
            setBody(moveRequest)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.STORAGE_NOT_FOUND.withContext("ID: $newParentId"))
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `patch move Storage should move successfully with valid input`() = testApplication {
        createEnviroment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val id = UUID.randomUUID().toString()
        val newParentId = UUID.randomUUID().toString()
        val moveRequest = MoveStorageRequest(newParentId = newParentId)

        val storageA = Storage(id, "Storage A", "Description A", spaces = listOf(), parentId = null, subStorages = listOf(), createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())
        val storageParent = Storage(newParentId, "Storage Parent", "Description Parent", spaces = listOf(), parentId = null, subStorages = listOf(), createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())

        every { mockStorageRepository.storageExists(id) } returns true
        every { mockStorageRepository.storageExists(newParentId) } returns true
        every { mockStorageRepository.getStorage(id) } returns storageA
        every { mockStorageRepository.getStorage(newParentId) } returns storageParent
        every { mockStorageRepository.moveStorage(id, newParentId) } returns storageA.copy(parentId = newParentId)

        client.patch("/storages/$id/move") {
            contentType(ContentType.Application.Json)
            setBody(moveRequest)
        }.apply {
            status shouldBe HttpStatusCode.OK
            val expectedResponse = storageA.copy(parentId = newParentId).toNetworkStorage()
            Json.decodeFromString<NetworkStorage>(bodyAsText()) shouldBe expectedResponse
        }
    }
    @Test
    fun `copyStorage should duplicate storage structure and return the copied storage`() = testApplication {
        createEnviroment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val rootStorageId = UUID.randomUUID().toString()
        val subStorageId = UUID.randomUUID().toString()
        val spaceId = UUID.randomUUID().toString()
        val productId = UUID.randomUUID().toString()
        val rootStorage = Storage(
            rootStorageId,
            "Root Storage",
            "Description Root",
            spaces = listOf(),
            parentId = null,
            subStorages = listOf(
                Storage(
                    subStorageId,
                    "Sub Storage",
                    "Description Sub",
                    spaces = listOf(
                        Space(
                            spaceId,
                            "Space",
                            0.5f,
                            "A space",
                            storageId = subStorageId,
                            products = listOf(
                                Product(
                                    productId,
                                    "Product",
                                    "A product",
                                    attributes = emptyMap(),
                                    spaceId = spaceId,
                                    createdAt = LocalDateTime.now(),
                                    updatedAt = LocalDateTime.now(),
                                )
                            ),
                            createdAt = LocalDateTime.now(),
                            updatedAt = LocalDateTime.now(),
                        )
                    ),
                    parentId = rootStorageId,
                    subStorages = listOf(),
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                )
            ),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        every { mockStorageRepository.getStorage(rootStorageId) } returns rootStorage
        every { mockStorageRepository.copyStorage(rootStorageId, null) } answers {
            rootStorage.copy(
                id = UUID.randomUUID().toString(),
                name = "${rootStorage.name} (Copy)",
                subStorages = rootStorage.subStorages.map { subStorage ->
                    subStorage.copy(
                        id = UUID.randomUUID().toString(),
                        name = "${subStorage.name} (Copy)",
                        spaces = subStorage.spaces.map { space ->
                            space.copy(
                                id = UUID.randomUUID().toString(),
                                name = "${space.name} (Copy)",
                                products = space.products.map { product ->
                                    product.copy(
                                        id = UUID.randomUUID().toString(),
                                        name = "${product.name} (Copy)"
                                    )
                                }
                            )
                        }
                    )
                }
            )
        }
        val copyRequest = CopyStorageRequest()

        val response = client.post("/storages/$rootStorageId/copy") {
            contentType(ContentType.Application.Json)
            setBody(copyRequest)
        }

        response.status shouldBe HttpStatusCode.Created

        val expectedCopiedStorage = mockStorageRepository.copyStorage(rootStorageId, null).toNetworkStorage()
        val actualCopiedStorage = Json.decodeFromString<NetworkStorage>(response.bodyAsText())

        expectedCopiedStorage.apply {
            actualCopiedStorage.apply {
                name shouldBe "${this.name}"
                description shouldBe this@apply.description
                subStorages.size shouldBe this@apply.subStorages.size
                subStorages.zip(this@apply.subStorages).forEach { (expectedSub, actualSub) ->
                    expectedSub.apply {
                        actualSub.apply {
                            name shouldBe "${this.name}"
                            description shouldBe this@apply.description
                            spaces.size shouldBe this@apply.spaces.size
                            spaces.zip(this@apply.spaces).forEach { (expectedSpace, actualSpace) ->
                                expectedSpace.apply {
                                    actualSpace.apply {
                                        name shouldBe "${this.name}"
                                        description shouldBe this@apply.description
                                        products?.size shouldBe this@apply.products?.size
                                        products?.zip(this@apply.products ?: listOf())?.forEach { (expectedProduct, actualProduct) ->
                                            expectedProduct.apply {
                                                actualProduct.apply {
                                                    name shouldBe "${this.name}"
                                                    description shouldBe this@apply.description
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    @Test
    fun `copy should return BadRequest if id is not a valid UUID`() = testApplication {
        createEnviroment()
        val invalidId = "invalid-uuid"
        client.post("/storages/$invalidId/copy").apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.INVALID_UUID_STORAGE)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }
    @Test
    fun `copy should respond with BadRequest when request body is missing or not serialized properly`() = testApplication {
        createEnviroment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val validId = UUID.randomUUID().toString()

        client.post("/storages/$validId/copy").apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.BODY_NOT_SERIALIZED_STORAGE)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }

        client.post("/storages/$validId/copy") {
            contentType(ContentType.Application.Json)
            setBody("{ invalid json }") // Invalid JSON
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.BODY_NOT_SERIALIZED_STORAGE)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }
    @Test
    fun `copy should respond with BadRequest when newParentId is not a valid UUID`() = testApplication {
        createEnviroment()
        val validId = UUID.randomUUID().toString()

        client.post("/storages/$validId/copy") {
            contentType(ContentType.Application.Json)
            setBody("""{"newParentId": "invalid-uuid"}""")
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.INVALID_UUID_STORAGE)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }
    @Test
    fun `copy should respond with NotFound when storage is not found`() = testApplication {
        createEnviroment()
        val invalidId = UUID.randomUUID().toString()

        every { mockStorageRepository.getStorage(invalidId) } returns null

        client.post("/storages/$invalidId/copy") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }.apply {
            status shouldBe HttpStatusCode.NotFound
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.STORAGE_NOT_FOUND)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }
    @Test
    fun `copy should respond with BadRequest when newParentId storage does not exist`() = testApplication {
        createEnviroment()
        val validId = UUID.randomUUID().toString()
        val nonExistentParentId = UUID.randomUUID().toString()

        val originalStorage = Storage(validId, "Original Storage", "Description", spaces = listOf(), parentId = null, subStorages = listOf(), createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now(),)
        every { mockStorageRepository.getStorage(validId) } returns originalStorage
        every { mockStorageRepository.getStorage(nonExistentParentId) } returns null

        client.post("/storages/$validId/copy") {
            contentType(ContentType.Application.Json)
            setBody("""{"newParentId": "$nonExistentParentId"}""")
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.STORAGE_NOT_FOUND.withContext("ID: $nonExistentParentId"))
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }
}

