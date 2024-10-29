package io.github.lagersystembackend.space

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
                val id = call.parameters["id"]
                if (id == null) {
                    call.respondText("Missing parameter 'id'", status = HttpStatusCode.BadRequest)
                    return@get
                }
                val space = spaceRepository.getSpace(id)
                if (space == null) {
                    call.respondText("Space not found", status = HttpStatusCode.NotFound)
                    return@get
                }
                call.respond(space.toNetworkSpace())
            }

            delete {
                val id = call.parameters["id"]
                if (id == null) {
                    call.respondText("Missing parameter 'name'", status = HttpStatusCode.BadRequest)
                    return@delete
                }
                if (!spaceRepository.deleteSpace(id)) {
                    call.respondText("Space not found", status = HttpStatusCode.NotFound)
                    return@delete
                }
                call.respondText("Space deleted")
            }
        }
        post {
            call.receive<AddSpaceNetworkRequest>().run {
                spaceRepository.createSpace(name, size, description)
            }
            call.respond(HttpStatusCode.Created, "Space created") }
    }
}