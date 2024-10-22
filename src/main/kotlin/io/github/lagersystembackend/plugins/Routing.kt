package io.github.lagersystembackend.plugins

import io.github.lagersystembackend.testing.SomeRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(someRepository: SomeRepository) {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause" , status = HttpStatusCode.InternalServerError)
        }
    }

    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        get("/") { call.respondRedirect("/swagger", true) }

        route("/items") {
            get { call.respond(someRepository.allItems()) }
            get("/{name}") { call.respond(someRepository.itemByName(call.parameters["name"] ?: "") ?: "Did not find Item")}
            post { someRepository.addItem(call.receive()); call.respond(HttpStatusCode.Created, "Item created") }
            delete {
                val name = call.parameters["name"]
                if (name == null) {
                    call.respondText("Missing parameter 'name'", status = HttpStatusCode.BadRequest)
                    return@delete
                }
                val item = someRepository.itemByName(name)
                if (item == null) {
                    call.respondText("No items found with name '${name}'")
                    return@delete
                }
                someRepository.removeItem(item)
                call.respondText("Deleted item '${name}'")
            }
        }
    }
}
