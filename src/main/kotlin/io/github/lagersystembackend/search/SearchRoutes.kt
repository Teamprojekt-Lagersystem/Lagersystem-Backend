package io.github.lagersystembackend.search

import io.github.lagersystembackend.common.ApiError
import io.github.lagersystembackend.common.ApiResponse
import io.github.lagersystembackend.common.ErrorMessages
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.searchRoutes(searchUseCase: SearchUseCase) {
    route("/search") {
        get {
            val errors = mutableListOf<ApiError>()
            val query = call.request.queryParameters["q"]

            if (query == null || query.isBlank()) {
                errors.add(ErrorMessages.INVALID_SEARCH_QUERY)
                return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
            }
            call.respond(searchUseCase.fullTextSearch(query).map { it.toNetworkSearchResult() })
        }
    }
}