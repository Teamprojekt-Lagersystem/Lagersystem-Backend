package io.github.lagersystembackend.product

import io.github.lagersystembackend.common.ApiResponse
import io.github.lagersystembackend.common.ErrorMessages
import io.github.lagersystembackend.common.isUUID
import io.github.lagersystembackend.common.ApiError
import io.github.lagersystembackend.space.SpaceRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.productRoutes(productRepository: ProductRepository, spaceRepository: SpaceRepository) {
    route("/products") {
        get {
            call.respond(
                ApiResponse.Success(
                    "Listing every product",
                    productRepository.getProducts().map { it.toNetworkProduct() })
            )
        }

        route("/{id}") {
            get {
                val id = call.parameters["id"]!!
                val errors = mutableListOf<ApiError>()
                if (!id.isUUID()) {
                    errors.add(ErrorMessages.INVALID_UUID_PRODUCT)
                }

                if (errors.isNotEmpty()) {
                    return@get call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                }

                val product = productRepository.getProduct(id)
                if (product == null) {
                    errors.add(ErrorMessages.PRODUCT_NOT_FOUND)
                    return@get call.respond(HttpStatusCode.NotFound, ApiResponse.Error(errors))
                }

                call.respond(ApiResponse.Success("Found product: $id", product.toNetworkProduct()))
            }

            delete {
                val id = call.parameters["id"]!!
                val errors = mutableListOf<ApiError>()

                if (!id.isUUID()) {
                    errors.add(ErrorMessages.INVALID_UUID_PRODUCT)
                }
                if (errors.isNotEmpty()) {
                    return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                }

                val deletedProduct = productRepository.deleteProduct(id)
                if (deletedProduct == null) {
                    errors.add(ErrorMessages.PRODUCT_NOT_FOUND)
                    return@delete call.respond(HttpStatusCode.NotFound, ApiResponse.Error(errors))
                }

                call.respond(ApiResponse.Success("Deleted product: $id", deletedProduct.toNetworkProduct()))
            }
        }
        post {
            val errors = mutableListOf<ApiError>()
            val addProductNetworkRequest = runCatching { call.receive<AddProductNetworkRequest>() }.getOrNull()

            if (addProductNetworkRequest == null) {
                errors.add(ErrorMessages.BODY_NOT_SERIALIZED_PRODUCT)
            } else {
                if (!addProductNetworkRequest.spaceId.isUUID()) {
                    errors.add(ErrorMessages.INVALID_UUID_SPACE)
                }

                if (addProductNetworkRequest.spaceId.isUUID() && !spaceRepository.spaceExists(addProductNetworkRequest.spaceId)) {
                    errors.add(ErrorMessages.SPACE_NOT_FOUND)
                }
            }

            if (errors.isNotEmpty()) {
                return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
            }

            val createdProduct = addProductNetworkRequest?.let {
                productRepository.createProduct(it.name, it.price, it.description, it.spaceId)
            }

            createdProduct?.let {
                call.respond(
                    HttpStatusCode.Created,
                    ApiResponse.Success("Created product: ${it.id}", it.toNetworkProduct())
                )
            }
        }
    }
}