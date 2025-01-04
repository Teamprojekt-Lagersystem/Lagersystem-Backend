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
            call.respond(storedProductRepository.getStoredProducts().map { it })
        }

        route("/{id}") {
            get {
                val id = call.parameters["id"]!!
                val errors = mutableListOf<ApiError>()
                if (!id.isUUID()) {
                    errors.add(ErrorMessages.INVALID_UUID_STORED_PRODUCT)
                }

                if (errors.isNotEmpty()) {
                    return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                }

                val storedProductDTO = storedProductRepository.getStoredProduct(id)
                if (storedProductDTO == null) {
                    errors.add(ErrorMessages.STORED_PRODUCT_NOT_FOUND)
                    return@get call.respond(HttpStatusCode.NotFound, ApiResponse.Error(errors))
                }

                call.respond(storedProductDTO)
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
        route("/{spaceId}") {
            get {
                val spaceId = call.parameters["spaceId"]!!
                val errors = mutableListOf<ApiError>()
                if (!spaceId.isUUID()) {
                    errors.add(ErrorMessages.INVALID_UUID_SPACE)
                }

                if (!storedProductRepository.spaceExists(spaceId)) {
                    errors.add(ErrorMessages.SPACE_NOT_FOUND)
                }

                if (errors.isNotEmpty()) {
                    return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                }

                val storedProducts = storedProductRepository.getStoredProductsBySpaceId(spaceId)
                call.respond(storedProducts.map { it })
            }
        }
        post {
            val storedProduct = runCatching { call.receive<AddStoredProductNetworkRequest>() }.getOrNull()
            val errors = mutableListOf<ApiError>()

            if (storedProduct == null) {
                errors.add(ErrorMessages.BODY_NOT_SERIALIZED_STORED_PRODUCT)
            } else {
                if (!storedProduct.productId.isUUID()) {
                    errors.add(ErrorMessages.INVALID_UUID_PRODUCT)
                }
                if (!storedProduct.spaceId.isUUID()) {
                    errors.add(ErrorMessages.INVALID_UUID_SPACE)
                }

                if (!storedProductRepository.productExists(storedProduct.productId)) {
                    errors.add(ErrorMessages.PRODUCT_NOT_FOUND)
                }

                if (!storedProductRepository.spaceExists(storedProduct.spaceId)) {
                    errors.add(ErrorMessages.SPACE_NOT_FOUND)
                }

                if (storedProduct.quantity <= 0) {
                    errors.add(ErrorMessages.NEGATIVE_SIZE)
                }

                if (storedProductRepository.isStored(storedProduct.productId, storedProduct.spaceId)) {
                    errors.add(ErrorMessages.ALREADY_STORED)
                }
            }
            if (errors.isNotEmpty()) {
                return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
            }

            val createdStoredProduct = storedProduct?.let {
                storedProductRepository.createStoredProduct(it.productId, it.spaceId, it.quantity)
            }

            createdStoredProduct?.let {
                call.respond(HttpStatusCode.Created, it)
            }
        }
        //TODO: copy stored product
    }
}