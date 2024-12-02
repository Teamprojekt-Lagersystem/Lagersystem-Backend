package io.github.lagersystembackend.plugins

import io.github.lagersystembackend.product.ProductRepository
import io.github.lagersystembackend.product.productRoutes
import io.github.lagersystembackend.space.SpaceRepository
import io.github.lagersystembackend.space.spaceRoutes
import io.github.lagersystembackend.storage.StorageRepository
import io.github.lagersystembackend.storage.storageRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(productRepository: ProductRepository, spaceRepository: SpaceRepository, storageRepository: StorageRepository) {
    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            call.respondText(text = "400: ${cause.message}", status = HttpStatusCode.BadRequest)
        }
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause" , status = HttpStatusCode.InternalServerError)
        }
    }

    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        get("/") { call.respondRedirect("/swagger", true) }

        productRoutes(productRepository, spaceRepository)
        spaceRoutes(spaceRepository, storageRepository)
        storageRoutes(storageRepository)
    }
}
