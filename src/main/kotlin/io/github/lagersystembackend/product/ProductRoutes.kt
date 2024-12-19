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
            route("/copy") {
                post {
                    val id = call.parameters["id"]!!
                    val errors = mutableListOf<ApiError>()

                    if (!id.isUUID()) {
                        errors.add(ErrorMessages.INVALID_UUID_PRODUCT)
                        return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    val copyRequest = runCatching { call.receive<CopyProductRequest>() }.getOrNull()

                    if (copyRequest == null) {
                        errors.add(ErrorMessages.BODY_NOT_SERIALIZED_PRODUCT)
                        return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    val targetSpaceId = copyRequest.targetSpaceId
                    if (!targetSpaceId.isUUID()) {
                        errors.add(ErrorMessages.INVALID_UUID_SPACE)
                    } else {
                        val targetSpace = spaceRepository.getSpace(targetSpaceId)
                        if (targetSpace == null) {
                            errors.add(ErrorMessages.SPACE_NOT_FOUND)
                        }
                    }

                    if (errors.isNotEmpty()) {
                        return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    val product = productRepository.getProduct(id)
                    if (product == null) {
                        errors.add(ErrorMessages.PRODUCT_NOT_FOUND)
                        return@post call.respond(HttpStatusCode.NotFound, ApiResponse.Error(errors))
                    }

                    val copiedProduct = productRepository.copyProduct(id, targetSpaceId)
                    call.respond(HttpStatusCode.Created, copiedProduct.toNetworkProduct())
                }
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

                    val updateProductNetworkRequest = runCatching { call.receive<UpdateProductNetworkRequest>() }.getOrNull()

                    if (updateProductNetworkRequest == null) {
                        errors.add(ErrorMessages.BODY_NOT_SERIALIZED_PRODUCT)
                        return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    val updatedProduct = updateProductNetworkRequest.let {
                        productRepository.updateProduct(id, it.name, it.description)
                    }

                    if (updatedProduct == null) {
                        errors.add(ErrorMessages.PRODUCT_NOT_FOUND)
                        return@patch call.respond(HttpStatusCode.NotFound, ApiResponse.Error(errors))
                    }

                    updatedProduct.let {
                        call.respond(
                            HttpStatusCode.OK,
                            it.toNetworkProduct()
                        )
                    }
                }
            }
            route("/move") {
                patch {
                    val id = call.parameters["id"]!!
                    val errors = mutableListOf<ApiError>()

                    if (!id.isUUID()) {
                        errors.add(ErrorMessages.INVALID_UUID_PRODUCT)
                        return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    val moveProductNetworkRequest = runCatching { call.receive<MoveProductNetworkRequest>() }.getOrNull()

                    if (moveProductNetworkRequest == null) {
                        errors.add(ErrorMessages.BODY_NOT_SERIALIZED_SPACE)
                        return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    val spaceId = moveProductNetworkRequest.targetSpaceId

                    if (!spaceId.isUUID()) {
                        errors.add(ErrorMessages.INVALID_UUID_SPACE)
                        return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    if (!spaceRepository.spaceExists(spaceId)) {
                        errors.add(ErrorMessages.SPACE_NOT_FOUND)
                        return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    val movedProduct = productRepository.moveProduct(id, spaceId)
                    if (movedProduct == null) {
                        errors.add(ErrorMessages.PRODUCT_NOT_FOUND)
                        return@patch call.respond(HttpStatusCode.NotFound, ApiResponse.Error(errors))
                    }
                    call.respond(movedProduct.toNetworkProduct())
                }
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
                productRepository.createProduct(it.name, it.description, it.spaceId)
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