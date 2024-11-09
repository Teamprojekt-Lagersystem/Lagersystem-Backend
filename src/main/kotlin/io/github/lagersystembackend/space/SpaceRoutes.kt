package io.github.lagersystembackend.space

import io.github.lagersystembackend.common.isUUID
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.spaceRoutes(spaceRepository: SpaceRepository) {
    route("/spaces") {
        get { call.respond(spaceRepository.getSpaces().map { it.toNetworkSpace() }) }

        route("/{id}") {
            get {
                val id = call.parameters["id"]!!

                if (!id.isUUID())
                    return@get call.respondText("Invalid UUID", status = HttpStatusCode.BadRequest)

                val space = spaceRepository.getSpace(id)
                space ?: return@get call.respondText("Space not found", status = HttpStatusCode.NotFound)

                call.respond(space.toNetworkSpace())
            }

            delete {
                val id = call.parameters["id"]!!

                if (!id.isUUID())
                    return@delete call.respondText("Invalid UUID", status = HttpStatusCode.BadRequest)

                if (!spaceRepository.deleteSpace(id))
                    return@delete call.respondText("Space not found", status = HttpStatusCode.NotFound)

                call.respondText("Space deleted")
            }
        }
        post {
            val addSpaceNetworkRequest = runCatching { call.receive<AddSpaceNetworkRequest>() }.getOrNull()
            addSpaceNetworkRequest ?: return@post call.respond(HttpStatusCode.BadRequest, "Body should be Serialized AddSpaceNetworkRequest")

            val createdSpace  = addSpaceNetworkRequest.run {
                if (!storageId.isUUID()) {
                    return@post call.respond(HttpStatusCode.BadRequest, "Invalid UUID")
                }

                if (!spaceRepository.storageExists(storageId)) {
                    return@post call.respond(HttpStatusCode.BadRequest, "Specified storage not found")
                }
            spaceRepository.createSpace(name, size, description, storageId)
        }
            call.respond(HttpStatusCode.Created, "Space created: ${createdSpace.id} ") }
    }
}