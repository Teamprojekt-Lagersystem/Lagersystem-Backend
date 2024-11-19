package io.github.lagersystembackend.space

import io.github.lagersystembackend.common.ApiResponse
import io.github.lagersystembackend.common.ErrorMessages
import io.github.lagersystembackend.common.isUUID
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.spaceRoutes(spaceRepository: SpaceRepository) {
    route("/spaces") {
        get { call.respond(
            ApiResponse.Success("Listing every space", spaceRepository.getSpaces().map { it.toNetworkSpace() })) }

        route("/{id}") {
            get {
                val id = call.parameters["id"]!!

                if (!id.isUUID())
                    return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(ErrorMessages.INVALID_UUID_SPACE))

                val space = spaceRepository.getSpace(id)
                space ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse.Error(ErrorMessages.SPACE_NOT_FOUND))

                call.respond(ApiResponse.Success("Found space: ${id}", space.toNetworkSpace()))
            }

            delete {
                val id = call.parameters["id"]!!

                if (!id.isUUID())
                    return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(ErrorMessages.INVALID_UUID_SPACE))

                val deletedSpace = spaceRepository.deleteSpace(id)
                deletedSpace ?: return@delete call.respond(HttpStatusCode.NotFound, ApiResponse.Error(ErrorMessages.SPACE_NOT_FOUND))

                call.respond(ApiResponse.Success("Space deleted: ${id}", deletedSpace.toNetworkSpace()))
            }
        }
        post {
            val addSpaceNetworkRequest = runCatching { call.receive<AddSpaceNetworkRequest>() }.getOrNull()
            addSpaceNetworkRequest ?: return@post call.respond(HttpStatusCode.BadRequest,
                ApiResponse.Error(ErrorMessages.BODY_NOT_SERIALIZED_SPACE))

            val createdSpace  = addSpaceNetworkRequest.run {
                if (!storageId.isUUID()) {
                    return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(ErrorMessages.INVALID_UUID_STORAGE))
                }

                if (!spaceRepository.storageExists(storageId)) {
                    return@post call.respond(HttpStatusCode.NotFound, ApiResponse.Error(ErrorMessages.STORAGE_NOT_FOUND))
                }
            spaceRepository.createSpace(name, size, description, storageId)
        }
            call.respond(HttpStatusCode.Created,
                ApiResponse.Success( "Space created: ${createdSpace.id}", createdSpace.toNetworkSpace())) }
    }
}