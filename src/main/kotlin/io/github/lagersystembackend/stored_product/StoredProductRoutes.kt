package io.github.lagersystembackend.stored_product

import io.github.lagersystembackend.common.ApiError
import io.github.lagersystembackend.common.ApiResponse
import io.github.lagersystembackend.common.ErrorMessages
import io.github.lagersystembackend.common.isUUID
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.storedProductRoutes(storedProductRepository: StoredProductRepository) {
    route("/storedProducts") {
        get {
            call.respond(storedProductRepository.getStoredProducts().map { it.toNetworkStoredProduct() })
        }

        route("/{id}") {
            get {
                val id = call.parameters["id"]!!
                val errors = mutableListOf<ApiError>()
                if (!id.isUUID()) {
                    errors.add(ErrorMessages.SPACE_NOT_FOUND)
                }

                if (errors.isNotEmpty()) {
                    return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                }

                val storedProduct = storedProductRepository.getStoredProduct(id)
                if (storedProduct == null) {
                    errors.add(ErrorMessages.SPACE_NOT_FOUND)
                    return@get call.respond(HttpStatusCode.NotFound, ApiResponse.Error(errors))
                }

                call.respond(storedProduct.toNetworkStoredProduct())
            }
            delete {
                val id = call.parameters["id"]!!
                val errors = mutableListOf<ApiError>()

                if (!id.isUUID()) {
                    errors.add(ErrorMessages.SPACE_NOT_FOUND)
                }

                if (errors.isNotEmpty()) {
                    return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                }

                val deletedStoredProduct = storedProductRepository.deleteStoredProduct(id)
                if (deletedStoredProduct == null) {
                    errors.add(ErrorMessages.SPACE_NOT_FOUND)
                    return@delete call.respond(HttpStatusCode.NotFound, ApiResponse.Error(errors))
                }

                call.respond(deletedStoredProduct.toNetworkStoredProduct())
            }
        }
        post {
            val storedProduct = call.receive<AddStoredProductNetworkRequest>()
            val errors = mutableListOf<ApiError>()

            if (!storedProductRepository.spaceExists(storedProduct.spaceId)) {
                errors.add(ErrorMessages.SPACE_NOT_FOUND)
            }
            if (!storedProductRepository.productExists(storedProduct.productId)) {
                errors.add(ErrorMessages.PRODUCT_NOT_FOUND)
            }

            if (errors.isNotEmpty()) {
                return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
            }

            val createdStoredProduct = storedProductRepository.createStoredProduct(
                storedProduct.productId,
                storedProduct.spaceId,
                storedProduct.quantity
            )

            call.respond(createdStoredProduct.toNetworkStoredProduct())
        }
    }
}