package io.github.lagersystembackend.product

import io.github.lagersystembackend.common.ApiResponse
import io.github.lagersystembackend.common.ErrorMessages
import io.github.lagersystembackend.common.isUUID
import io.github.lagersystembackend.common.ApiError
import io.github.lagersystembackend.space.SpaceRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.productRoutes(productRepository: ProductRepository, spaceRepository: SpaceRepository) {
    route("/products") {
        get {
            call.respond(productRepository.getProducts().map { it.toNetworkProduct() })
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

                call.respond(product.toNetworkProduct())
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

                call.respond(deletedProduct.toNetworkProduct())
            }
            route("/update") {
                patch {
                    val id = call.parameters["id"]!!
                    val errors = mutableListOf<ApiError>()

                    if (!id.isUUID()) {
                        errors.add(ErrorMessages.INVALID_UUID_PRODUCT)
                        return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    val updateProductNetworkRequest =
                        runCatching { call.receive<UpdateProductNetworkRequest>() }.getOrNull()

                    if (updateProductNetworkRequest == null) {
                        errors.add(ErrorMessages.BODY_NOT_SERIALIZED_PRODUCT)
                        return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    val product = productRepository.getProduct(id)

                    if (product == null) {
                        errors.add(ErrorMessages.PRODUCT_NOT_FOUND)
                        return@patch call.respond(HttpStatusCode.NotFound, ApiResponse.Error(errors))
                    }

                    if (errors.isNotEmpty()) {
                        return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    val updatedProduct = updateProductNetworkRequest.let {
                        productRepository.updateProduct(id, it.name, it.description, it.size)
                    }

                    updatedProduct?.let {
                        call.respond(
                            HttpStatusCode.OK,
                            it.toNetworkProduct()
                        )
                    }
                }
            }
        }
        post {
            val errors = mutableListOf<ApiError>()
            val addProductNetworkRequest = runCatching { call.receive<AddProductNetworkRequest>() }.getOrNull()

            if (addProductNetworkRequest == null) {
                errors.add(ErrorMessages.BODY_NOT_SERIALIZED_PRODUCT)
            } else {

                if (addProductNetworkRequest.unit != null && addProductNetworkRequest.size == null ||
                    addProductNetworkRequest.unit == null && addProductNetworkRequest.size != null) {
                    errors.add(ErrorMessages.WRONG_SPECIFICATION)
                }
                /*
                if (addProductNetworkRequest.unit != null) {
                    if (!spaceRepository.checkcUnit(addProductNetworkRequest.spaceId, addProductNetworkRequest.unit)) {
                        errors.add(ErrorMessages.UNIT_NOT_FITTING)
                    }
                }

                 */

                if (addProductNetworkRequest.size != null) {
                    if (addProductNetworkRequest.size <= 0) {
                        errors.add(ErrorMessages.NEGATIVE_SIZE)
                    }
                }
            }

            if (errors.isNotEmpty()) {
                return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
            }

            val createdProduct = addProductNetworkRequest?.let {
                productRepository.createProduct(it.name, it.description, it.size, it.unit)
            }

            createdProduct?.let {
                call.respond(
                    HttpStatusCode.Created,
                    it.toNetworkProduct()
                )
            }
        }
    }
}