package io.github.lagersystembackend.storage

import io.github.lagersystembackend.common.*
import io.github.lagersystembackend.plugins.configureHTTP
import io.github.lagersystembackend.plugins.configureSerialization
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
            Storage(UUID.randomUUID().toString(), "Storage 1", "Description 1", spaces = listOf(),parentId = null, subStorages = listOf()),
            Storage(UUID.randomUUID().toString(), "Storage 2", "Description 2", spaces = listOf(), parentId = null, subStorages = listOf())
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
        val storage1 = Storage(UUID.randomUUID().toString(), "Storage 1", "Description 1", spaces = listOf(), parentId = null, subStorages = listOf())
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
        val storage = Storage(id, "Storage 1", "Description 1", spaces = listOf(), parentId = null, subStorages = listOf())
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
        val storage1 = Storage(UUID.randomUUID().toString(), "Storage 1", "Description 1", spaces = listOf(), parentId = null, subStorages = listOf())
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
            val storage = Storage(id, name, description, spaces = listOf(), parentId = null, subStorages = listOf())
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
            val storageParent = Storage(parentId, "Storage Parent", "Description Parent", spaces = listOf(), parentId = null, subStorages = listOf())
            val storage = Storage(UUID.randomUUID().toString(), name, description, spaces = listOf(), parentId = parentId, subStorages = listOf())
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
    fun `patch Storage should respond with NetworkStorage`() = testApplication {
        createEnviroment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val id = UUID.randomUUID().toString()
        val updateStorageNetworkRequest = UpdateStorageNetworkRequest(id, "Storage 1", "Description 1")
        val storage = Storage(id, "Storage 1", "Description 1", spaces = listOf(), parentId = null, subStorages = listOf())
        every { mockStorageRepository.updateStorage(id, updateStorageNetworkRequest.name, updateStorageNetworkRequest.description) } returns storage
        every { mockStorageRepository.storageExists(id) } returns true
        client.patch("/storages/update") {
            setBody(updateStorageNetworkRequest)
            contentType(ContentType.Application.Json)
        }.apply {
            status shouldBe HttpStatusCode.OK
            Json.decodeFromString<NetworkStorage>(bodyAsText()) shouldBe storage.toNetworkStorage()
        }
    }

    @Test
    fun `patch Storage should respond with BadRequest when id is invalid`() = testApplication {
        createEnviroment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val id = "invalid id"
        val updateStorageNetworkRequest = UpdateStorageNetworkRequest(id, "Storage 1", "Description 1")
        every { mockStorageRepository.storageExists(id) } returns false
        client.patch("/storages/update") {
            setBody(updateStorageNetworkRequest)
            contentType(ContentType.Application.Json)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(
                    ErrorMessages.INVALID_UUID_STORAGE,
                    ErrorMessages.STORAGE_NOT_FOUND)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `patch Storage should respond with NotFound when Storage not found`() = testApplication {
        createEnviroment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val id = UUID.randomUUID().toString()
        val updateStorageNetworkRequest = UpdateStorageNetworkRequest(id, "Storage 1", "Description 1")
        every { mockStorageRepository.storageExists(id) } returns false
        client.patch("/storages/update") {
            setBody(updateStorageNetworkRequest)
            contentType(ContentType.Application.Json)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.STORAGE_NOT_FOUND)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `patch Storage should respond with BadRequest when request body is missing`() = testApplication {
        createEnviroment()
        val badRequest = """
            {
                "name": "Storage 1",
                "description": "Description 1"
            }
        """.trimIndent()
        client.patch("/storages/update") {
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
    fun `move Storage should respond with BadRequest when id is invalid`() = testApplication {
        createEnviroment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val invalidId = "invalid-id"
        val moveRequest = MoveStorageRequest(newParentId = UUID.randomUUID().toString())
        every { mockStorageRepository.getStorage(any()) } returns null

        client.post("/storages/$invalidId/move") {
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
    fun `move Storage should respond with BadRequest when request body is missing`() = testApplication {
        createEnviroment()
        val id = UUID.randomUUID().toString()
        client.post("/storages/$id/move").apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.BODY_NOT_SERIALIZED_STORAGE)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `move Storage should respond with BadRequest when newParentId is invalid`() = testApplication {
        createEnviroment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val id = UUID.randomUUID().toString()
        val invalidParentId = "invalid-id"
        val moveRequest = MoveStorageRequest(newParentId = invalidParentId)
        client.post("/storages/$id/move") {
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
    fun `move Storage should respond with NotFound when target Storage is not found`() = testApplication {
        createEnviroment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val id = UUID.randomUUID().toString()
        val newParentId = UUID.randomUUID().toString()
        val moveRequest = MoveStorageRequest(newParentId = newParentId)
        every { mockStorageRepository.getStorage(any()) } returns null

        client.post("/storages/$id/move") {
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
    fun `move Storage should respond with BadRequest when target parent Storage is not found`() = testApplication {
        createEnviroment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val id = UUID.randomUUID().toString()
        val newParentId = UUID.randomUUID().toString()
        val moveRequest = MoveStorageRequest(newParentId = newParentId)

        every { mockStorageRepository.getStorage(id) } returns Storage(
            id, "Storage A", "Description A", spaces = listOf(), parentId = null, subStorages = listOf()
        )
        every { mockStorageRepository.getStorage(newParentId) } returns null

        client.post("/storages/$id/move") {
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
    fun `move Storage should move successfully with valid input`() = testApplication {
        createEnviroment()
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val id = UUID.randomUUID().toString()
        val newParentId = UUID.randomUUID().toString()
        val moveRequest = MoveStorageRequest(newParentId = newParentId)

        val storageA = Storage(id, "Storage A", "Description A", spaces = listOf(), parentId = null, subStorages = listOf())
        val storageParent = Storage(newParentId, "Storage Parent", "Description Parent", spaces = listOf(), parentId = null, subStorages = listOf())

        every { mockStorageRepository.getStorage(id) } returns storageA
        every { mockStorageRepository.getStorage(newParentId) } returns storageParent
        every { mockStorageRepository.moveStorage(id, newParentId) } returns storageA.copy(parentId = newParentId)

        client.post("/storages/$id/move") {
            contentType(ContentType.Application.Json)
            setBody(moveRequest)
        }.apply {
            status shouldBe HttpStatusCode.OK
            val expectedResponse = storageA.copy(parentId = newParentId).toNetworkStorage()
            Json.decodeFromString<NetworkStorage>(bodyAsText()) shouldBe expectedResponse
        }
    }

}