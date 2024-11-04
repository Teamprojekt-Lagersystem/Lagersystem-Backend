package io.github.lagersystembackend.space

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
import io.mockk.verify
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test

class SpaceRoutesKtTest {
    val mockSpaceRepository = mockk<SpaceRepository>()
    fun ApplicationTestBuilder.createEnvironment() {
        application {
            configureHTTP()
            configureSerialization()
            this.routing { spaceRoutes(mockSpaceRepository) }
        }
    }

    @BeforeTest
    fun setUp() {
    }

    @Test
    fun `get Spaces should respond with List of NetworkSpaces`() = testApplication {
        createEnvironment()
        val spaces = listOf(
            Space(UUID.randomUUID().toString(), "Space 1", 100f, "Description 1", products = listOf()),
            Space(UUID.randomUUID().toString(), "Space 2", 200f, "Description 2", products = listOf())
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
            Json.decodeFromString<List<NetworkSpace>>(bodyAsText()) shouldBe emptyList()
        }
    }

    @Test
    fun `get Space by ID should respond with NetworkSpace`() = testApplication {
        createEnvironment()
        val space1 = Space(UUID.randomUUID().toString(), "Space 1", 100f, "Description 1", products = listOf())
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
            bodyAsText() shouldBe "Invalid UUID"
        }
    }

    @Test
    fun `get Space should respond with NotFound when Space not found`() = testApplication {
        createEnvironment()
        val id = UUID.randomUUID().toString()
        every { mockSpaceRepository.getSpace(id) } returns null
        client.get("/spaces/${id}").apply {
            status shouldBe HttpStatusCode.NotFound
            bodyAsText() shouldBe "Space not found"
        }
    }

    @Test
    fun `delete Space should delete Space`() = testApplication {
        createEnvironment()
        val id = UUID.randomUUID().toString()
        every { mockSpaceRepository.deleteSpace(id) } returns true
        client.delete("/spaces/$id").apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe "Space deleted"
            verify { mockSpaceRepository.deleteSpace(id) }
        }
    }

    @Test
    fun `delete Space should respond with BadRequest when id is invalid`() = testApplication {
        createEnvironment()
        val id = "invalid id"
        client.delete("/spaces/$id").apply {
            status shouldBe HttpStatusCode.BadRequest
            bodyAsText() shouldBe "Invalid UUID"
        }
    }

    @Test
    fun `delete Space should respond with NotFound when Space not found`() = testApplication {
        createEnvironment()
        val id = UUID.randomUUID().toString()
        every { mockSpaceRepository.deleteSpace(id) } returns false
        client.delete("/spaces/$id").apply {
            status shouldBe HttpStatusCode.NotFound
            bodyAsText() shouldBe "Space not found"
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
        val addSpaceNetworkRequest = AddSpaceNetworkRequest("Space 1", 100f, "Description 1")
        addSpaceNetworkRequest.run {
            val space = Space(id, name, size, description, products = listOf())
            every { mockSpaceRepository.createSpace(name, size, description) } returns space
        }

        client.post("/spaces") {
            setBody(addSpaceNetworkRequest)
            contentType(ContentType.Application.Json)
        }.apply {
            status shouldBe HttpStatusCode.Created
            bodyAsText() shouldBe "Space created"
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
        val addSpaceNetworkRequest = AddSpaceNetworkRequest("Space 1", null, "Description 1")
        addSpaceNetworkRequest.run {
            val space = Space(id, name, size, description, products = listOf())
            every { mockSpaceRepository.createSpace(name, size, description) } returns space
        }

        client.post("/spaces") {
            setBody(addSpaceNetworkRequest)
            contentType(ContentType.Application.Json)
        }.apply {
            status shouldBe HttpStatusCode.Created
            bodyAsText() shouldBe "Space created"
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
            bodyAsText() shouldBe "Body should be Serialized AddSpaceNetworkRequest"
        }
    }


}