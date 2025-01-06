package io.github.lagersystembackend.stored_product

import io.github.lagersystembackend.common.*
import io.github.lagersystembackend.space.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.storedProductRoutes(storedProductRepository: StoredProductRepository, spaceRepository: SpaceRepository) {
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
            route("/update") {
                patch {
                    val id = call.parameters["id"]!!
                    val errors = mutableListOf<ApiError>()

                    if (!id.isUUID()) {
                        errors.add(ErrorMessages.INVALID_UUID_STORED_PRODUCT)
                        return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    val updateStoredProductRequest = runCatching { call.receive<UpdateStoredProductRequest>() }.getOrNull()

                    if (updateStoredProductRequest == null) {
                        errors.add(ErrorMessages.BODY_NOT_SERIALIZED_STORED_PRODUCT)
                        return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    val storedProduct = storedProductRepository.getStoredProduct(id)
                    if (storedProduct == null) {
                        errors.add(ErrorMessages.STORED_PRODUCT_NOT_FOUND.withContext("ID: $id"))
                        return@patch call.respond(HttpStatusCode.NotFound, ApiResponse.Error(errors))
                    }

                    if (updateStoredProductRequest.quantity <= 0) {
                        errors.add(ErrorMessages.NEGATIVE_SIZE)
                    }

                    if (!storedProductRepository.fitsInSpace(storedProduct.productId, storedProduct.spaceId,
                            updateStoredProductRequest.quantity - storedProduct.quantity))
                    {
                        errors.add(ErrorMessages.SIZE_NOT_FITTING)
                    }

                    if (errors.isNotEmpty()) {
                        return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    val updatedStoredProduct = updateStoredProductRequest.let {
                        storedProductRepository.updateStoredProduct(id, it.quantity)
                    }

                    call.respond(updatedStoredProduct.toNetworkStoredProduct())
                }
            }
            route("/move") {
                patch {
                    val id = call.parameters["id"]!!
                    val errors = mutableListOf<ApiError>()

                    if (!id.isUUID()) {
                        errors.add(ErrorMessages.INVALID_UUID_STORED_PRODUCT.withContext("ID: $id"))
                        return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    val moveStoredProductRequest = runCatching { call.receive<MoveStoredProductRequest>() }.getOrNull()

                    if (moveStoredProductRequest == null) {
                        errors.add(ErrorMessages.BODY_NOT_SERIALIZED_STORED_PRODUCT)
                        return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    val storedProduct = storedProductRepository.getStoredProduct(id)
                    if (storedProduct == null) {
                        errors.add(ErrorMessages.STORED_PRODUCT_NOT_FOUND.withContext("ID: $id"))
                        return@patch call.respond(HttpStatusCode.NotFound, ApiResponse.Error(errors))
                    }

                    val targetSpaceId = moveStoredProductRequest.targetSpaceId
                    if (!targetSpaceId.isUUID()) {
                        errors.add(ErrorMessages.INVALID_UUID_SPACE.withContext("Target Storage ID: $targetSpaceId"))
                    } else if (!spaceRepository.spaceExists(targetSpaceId)) {
                        errors.add(ErrorMessages.SPACE_NOT_FOUND.withContext("ID: $targetSpaceId"))
                    }

                    if (errors.isNotEmpty()) {
                        return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    if (!storedProductRepository.fitsInSpace(storedProduct.productId, targetSpaceId, storedProduct.quantity)) {
                        errors.add(ErrorMessages.SIZE_NOT_FITTING)
                        return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    if (!storedProductRepository.checkUnit(storedProduct.productId, targetSpaceId)) {
                        errors.add(ErrorMessages.UNIT_NOT_FITTING)
                        return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    val movedStoredProduct = storedProductRepository.moveStoredProduct(id, moveStoredProductRequest.targetSpaceId)

                    call.respond(movedStoredProduct)
                }
            }
            route("/copy") {
                post {
                    val id = call.parameters["id"]!!
                    val errors = mutableListOf<ApiError>()

                    if (!id.isUUID()) {
                        errors.add(ErrorMessages.INVALID_UUID_STORED_PRODUCT)
                        return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    val copyRequest = runCatching { call.receive<CopyStoredProductRequest>() }.getOrNull()

                    if (copyRequest == null) {
                        errors.add(ErrorMessages.BODY_NOT_SERIALIZED_STORED_PRODUCT)
                        return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    val targetSpaceId = copyRequest.targetSpaceId
                    if (!targetSpaceId.isUUID()) {
                        errors.add(ErrorMessages.INVALID_UUID_SPACE)
                    } else {
                        val targetSpace = spaceRepository.getSpace(targetSpaceId)
                        if (targetSpace == null) {
                            errors.add(ErrorMessages.SPACE_NOT_FOUND.withContext("ID: $targetSpaceId"))
                        }
                    }

                    if (errors.isNotEmpty()) {
                        return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    val storedProduct = storedProductRepository.getStoredProduct(id)
                    if (storedProduct == null) {
                        errors.add(ErrorMessages.STORED_PRODUCT_NOT_FOUND)
                        return@post call.respond(HttpStatusCode.NotFound, ApiResponse.Error(errors))
                    }

                    if (!storedProductRepository.fitsInSpace(storedProduct.productId, targetSpaceId, storedProduct.quantity)) {
                        errors.add(ErrorMessages.SIZE_NOT_FITTING)
                        return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    if (!storedProductRepository.checkUnit(storedProduct.productId, targetSpaceId)) {
                        errors.add(ErrorMessages.UNIT_NOT_FITTING)
                        return@post call.respond(HttpStatusCode.BadRequest, ApiResponse.Error(errors))
                    }

                    val copiedStoredProduct = storedProductRepository.copyStoredProduct(id, targetSpaceId)
                    call.respond(HttpStatusCode.Created, copiedStoredProduct)
                }
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

                if (!storedProductRepository.fitsInSpace(storedProduct.productId, storedProduct.spaceId, storedProduct.quantity)) {
                    errors.add(ErrorMessages.SIZE_NOT_FITTING)
                }

                if (!storedProductRepository.checkUnit(storedProduct.productId, storedProduct.spaceId)) {
                    errors.add(ErrorMessages.UNIT_NOT_FITTING)
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
    }
}