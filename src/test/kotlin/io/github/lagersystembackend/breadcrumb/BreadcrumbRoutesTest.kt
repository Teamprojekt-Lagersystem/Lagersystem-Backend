package io.github.lagersystembackend.breadcrumb

import io.github.lagersystembackend.common.ApiResponse
import io.github.lagersystembackend.common.ErrorMessages
import io.github.lagersystembackend.plugins.configureHTTP
import io.github.lagersystembackend.plugins.configureSerialization
import io.github.lagersystembackend.search.NetworkSearchResult
import io.github.lagersystembackend.search.toNetworkSearchResult
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.test.Test

class BreadcrumbRoutesTest {
    val breadcrumbUseCase = mockk<BreadcrumbUseCase>()
    fun ApplicationTestBuilder.createEnvironment() {
        application {
            configureHTTP()
            configureSerialization()
            routing { breadcrumbRoutes(breadcrumbUseCase) }
        }
    }

    @Test
    fun `breadcrumb should return Breadcrumb when found`() = testApplication {
        createEnvironment()
        val id = UUID.randomUUID().toString()
        val breadcrumb = Breadcrumb(listOf(Breadcrumb.BreadcrumbEntry("id", "name", "type"), Breadcrumb.BreadcrumbEntry(id, "name", "type")))
        every { breadcrumbUseCase.objectWithIdExists(id) } returns true
        every { breadcrumbUseCase.getBreadcrumb(id) } returns breadcrumb
        client.get("/breadcrumb/$id").apply {
            status shouldBe HttpStatusCode.OK
            Json.decodeFromString<Breadcrumb>(bodyAsText()) shouldBe breadcrumb
        }
    }

    @Test
    fun `breadcrumb should respond with BadRequest when id is invalid`() = testApplication {
        createEnvironment()
        client.get("/breadcrumb/invalid-id").apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.INVALID_UUID)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `breadcrumb should respond with NotFound when no storage or space is found`() = testApplication {
        createEnvironment()
        val id = UUID.randomUUID().toString()
        every { breadcrumbUseCase.objectWithIdExists(id) } returns false
        client.get("/breadcrumb/$id").apply {
            status shouldBe HttpStatusCode.NotFound
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.OBJECT_NOT_FOUND)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }
}
