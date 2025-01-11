package io.github.lagersystembackend.search

import io.github.lagersystembackend.common.ApiResponse
import io.github.lagersystembackend.common.ErrorMessages
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
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.test.Test

class SearchRoutesTest {
    val searchUseCase = mockk<SearchUseCase>()
    fun ApplicationTestBuilder.createEnvironment() {
        application {
            configureHTTP()
            configureSerialization()
            routing { searchRoutes(searchUseCase) }
        }
    }

    @Test
    fun `fullTextSearch should return list of NetworkSearchResults`() = testApplication {
        createEnvironment()
        val searchResults = listOf(
            SearchResult(
                type = "product",
                rank = 0.5,
                id = UUID.randomUUID().toString(),
                name = "Product 1",
                description = "Description 1",
                createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                updatedAt = null
            ),
            SearchResult(
                type = "product",
                rank = 0.5213,
                id = UUID.randomUUID().toString(),
                name = "Product 2",
                description = "Description 2",
                createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                updatedAt = null
            )
        )
        every { searchUseCase.fullTextSearch(any()) } returns searchResults
        client.get("/search?q=any").apply {
            status shouldBe HttpStatusCode.OK
            Json.decodeFromString<List<NetworkSearchResult>>(bodyAsText()) shouldBe searchResults.map { it.toNetworkSearchResult() }
        }
    }

    @Test
    fun `fullTextSearch should respond with BadRequest when query is omitted`() = testApplication {
        createEnvironment()
        client.get("/search").apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.INVALID_SEARCH_QUERY)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }

    @Test
    fun `fullTextSearch should respond with BadRequest when query is blank`() = testApplication {
        createEnvironment()
        client.get("/search?q=").apply {
            status shouldBe HttpStatusCode.BadRequest
            val expectedResponse = ApiResponse.Error(
                listOf(ErrorMessages.INVALID_SEARCH_QUERY)
            )
            Json.decodeFromString<ApiResponse.Error>(bodyAsText()) shouldBe expectedResponse
        }
    }
}
