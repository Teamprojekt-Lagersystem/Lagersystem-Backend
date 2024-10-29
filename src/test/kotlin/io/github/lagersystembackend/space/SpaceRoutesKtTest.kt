package io.github.lagersystembackend.space

import io.github.lagersystembackend.plugins.configureHTTP
import io.github.lagersystembackend.plugins.configureSerialization
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
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
    fun `get Space should return NetworkSpace`() = testApplication {
        createEnvironment()
        val space1 = Space("id1", "Space 1", 100f, "Description 1", products = listOf())
        every { mockSpaceRepository.getSpace(space1.id) } returns space1
        client.get("/spaces/${space1.id}").apply {
            status shouldBe HttpStatusCode.OK
            Json.decodeFromString<NetworkSpace>(bodyAsText()) shouldBe space1.toNetworkSpace()
        }
    }

    @Test
    fun `delete Space should delete Space`() = testApplication {
        createEnvironment()
        val id = "id1"
        every { mockSpaceRepository.deleteSpace(id) } returns true
        client.delete("/spaces/$id").apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe "Space deleted"
            verify { mockSpaceRepository.deleteSpace(id) }
        }
    }
}