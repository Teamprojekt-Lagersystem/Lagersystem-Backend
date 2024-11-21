package io.github.lagersystembackend.storage

import io.github.lagersystembackend.common.ApiResponse
import io.github.lagersystembackend.plugins.configureHTTP
import io.github.lagersystembackend.plugins.configureSerialization
import io.github.lagersystembackend.space.NetworkSpace
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
            val expectedResponse = ApiResponse.Success(
                message = "Listing every storage",
                data = storages.map { it.toNetworkStorage() }
            )
            Json.decodeFromString<ApiResponse.Success<List<NetworkStorage>>>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `get Storages should respond with emptyList when Repository is empty`() = testApplication {
        createEnviroment()

        every { mockStorageRepository.getStorages() } returns emptyList()
        client.get("/storages").apply {
            status shouldBe HttpStatusCode.OK
            val expectedResponse = ApiResponse.Success(
                message = "Listing every storage",
                data = emptyList<NetworkStorage>()
            )
            Json.decodeFromString<ApiResponse.Success<List<NetworkStorage>>>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `get Storage should respond with Bad Request when depth parameter is invalid`() = testApplication {
        createEnviroment()
        client.get("/storages?depth=invalid").apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                errorMessage = "Invalid 'depth' parameter 'invalid'. It must be a positive integer."
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
            val expectedResponse = ApiResponse.Success(
                message = "Storage found: ${storage1.id}",
                data = storage1.toNetworkStorage()
            )
            Json.decodeFromString<ApiResponse.Success<NetworkStorage>>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `get Storage should respond with Bad Request when id is invalid`() = testApplication {
        createEnviroment()
        client.get("/storages/${"invalid id"}").apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                errorMessage = "Invalid UUID",
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
                errorMessage = "Storage not found",
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
                errorMessage = "Invalid 'depth' parameter 'invalid'. It must be a positive integer."
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
            val expectedResponse = ApiResponse.Success(
                message = "Storage deleted: ${storage1.id}",
                data = storage1.toNetworkStorage()
            )
            Json.decodeFromString<ApiResponse.Success<NetworkStorage>>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `delete Storage should respond with BadRequest when id is invalid`() = testApplication {
        createEnviroment()
        val id = "invalid id"
        client.delete("/storages/$id").apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                errorMessage = "Invalid UUID",
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
                errorMessage = "Storage not found",
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
                val expectedResponse = ApiResponse.Success(
                    message = "Storage created: ${storage.id}",
                    data = storage.toNetworkStorage()
                )
                Json.decodeFromString<ApiResponse.Success<NetworkStorage>>(bodyAsText()) shouldBe expectedResponse
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
                    errorMessage = "Invalid UUID",
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
                    errorMessage = "Parent storage with ID $parentId not found",
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
                val expectedResponse = ApiResponse.Success(
                    message = "Storage created: ${storage.id}",
                    data = storage.toNetworkStorage()
                )
                Json.decodeFromString<ApiResponse.Success<NetworkStorage>>(bodyAsText()) shouldBe expectedResponse
            }
        }
    }
























}