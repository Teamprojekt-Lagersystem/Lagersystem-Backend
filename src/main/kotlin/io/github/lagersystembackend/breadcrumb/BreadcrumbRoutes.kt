package io.github.lagersystembackend.breadcrumb

import io.github.lagersystembackend.common.ApiResponse
import io.github.lagersystembackend.common.ErrorMessages
import io.github.lagersystembackend.common.isUUID
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.breadcrumbRoutes(breadcrumbsUseCase: BreadcrumbUseCase) {
    route("/breadcrumb/{id}") {
        get {
            val id = call.parameters["id"]!!
            if (!id.isUUID())
                return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(listOf(ErrorMessages.INVALID_UUID)))

            if (!breadcrumbsUseCase.objectWithIdExists(id))
                return@get call.respond(HttpStatusCode.NotFound, ApiResponse.Error(listOf(ErrorMessages.OBJECT_NOT_FOUND)))

            breadcrumbsUseCase.getBreadcrumb(id)!!.let { call.respond(it) }
        }
    }
}