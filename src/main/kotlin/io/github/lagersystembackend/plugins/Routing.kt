package io.github.lagersystembackend.plugins

import io.github.lagersystembackend.testing.Item
import io.github.lagersystembackend.testing.SomeRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(someRepository: SomeRepository) {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause" , status = HttpStatusCode.InternalServerError)
        }
    }

    routing {
        get("/") {
            someRepository.addItem(Item(name = "testItem"))
            call.respondText(someRepository.itemByName(name = "testItem")?.name ?: "")
        }
    }
}
